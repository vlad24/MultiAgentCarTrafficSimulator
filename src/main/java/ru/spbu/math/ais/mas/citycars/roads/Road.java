package ru.spbu.math.ais.mas.citycars.roads;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.google.gson.Gson;

import jade.core.AID;
import jade.core.Agent;
import jade.core.ServiceException;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.messaging.TopicManagementHelper;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import lombok.extern.slf4j.Slf4j;
import ru.spbu.math.ais.mas.citycars.wrappers.Pair;
import ru.spbu.math.ais.mas.citycars.wrappers.communication.CarMoveRequest;
import ru.spbu.math.ais.mas.citycars.wrappers.communication.CarMoveResponse;
import ru.spbu.math.ais.mas.citycars.wrappers.communication.RoadOccupyRequest;
import ru.spbu.math.ais.mas.citycars.wrappers.communication.RoadOccupyResponse;
import ru.spbu.math.ais.mas.citycars.wrappers.communication.RoadStatusChange;

/**
 * @author polina
 *
 */
@SuppressWarnings("serial")
@Slf4j
public class Road extends Agent{

	private String cityName;
	private Pair bounds;
	private int workload;
	private int workloadDelta;

	public static String nameOf(Pair edge) {
		return new StringBuilder()
		.append("(")
			.append(Math.min(edge.getFirst(), edge.getSecond()))
			.append(",")
			.append(Math.max(edge.getFirst(), edge.getSecond()))
		.append(")")
		.toString();
	}

	@Override
	protected void setup() {
		super.setup();
		int argPos = 0;
		cityName      = String.valueOf(getArguments()[argPos++]);
		bounds        = (Pair)(getArguments()[argPos++]);
		workload      = Integer.parseInt(getArguments()[argPos++].toString());
		workloadDelta = Integer.parseInt(getArguments()[argPos++].toString());
		log.info("Road {} in {} is created.", getLocalName(), cityName);
		addBehaviour(new BasicRoadBehaviour(this));

	}

	/**
	 * @author vlad, polina
	 *
	 */
	public class BasicRoadBehaviour extends CyclicBehaviour{

		public final static String ROAD_WORKLOAD_UPDATE_TOPIC = "road_workload_update";

		private Gson gson;
		private Map<String, Long> carLeavingTime;
		private AID workloadUpdateTopic;
		private TopicManagementHelper topicHelper;
		private MessageTemplate messageFilter;

		public BasicRoadBehaviour(Road road) {
			super(road);
			gson = new Gson();
			carLeavingTime = new HashMap<String, Long>();
			try {
				topicHelper = (TopicManagementHelper) myAgent.getHelper(TopicManagementHelper.SERVICE_NAME);
				workloadUpdateTopic = topicHelper.createTopic(ROAD_WORKLOAD_UPDATE_TOPIC);
			} catch (ServiceException e) {
				log.error("Cannot create workload update topic", e);
				throw new IllegalStateException("Road has not been properly initialized. It is in inconsistent state");
			}
			messageFilter = MessageTemplate.or(
					MessageTemplate.MatchPerformative(ACLMessage.PROPOSE),
					MessageTemplate.or(
							MessageTemplate.MatchPerformative(ACLMessage.INFORM_IF),
							MessageTemplate.MatchPerformative(ACLMessage.INFORM)
					)
			);
		}

		@Override
		public void action() {
			ACLMessage message = receive(messageFilter);
			if (message != null) {
				log.trace("Got json message: {}", message.getContent());
				switch (message.getPerformative()) {
				case ACLMessage.PROPOSE:
					RoadOccupyRequest roadOccRequest = gson.fromJson(message.getContent(), RoadOccupyRequest.class);
					if (roadOccRequest.getRoadLeft() != null && roadOccRequest.getRoadWished() != null){
						log.debug("Occupy request got. Road left:{}. Road wished: {}", roadOccRequest.getRoadLeft(), roadOccRequest.getRoadWished());
						if(bounds.equals(roadOccRequest.getRoadWished()) && isIncident(roadOccRequest.getRoadLeft())) {
							checkRequestWithAnotherRoad(roadOccRequest);
						} else {
							sendOccupyReject(roadOccRequest.getCarName());
						}
					} else if (roadOccRequest.getRoadLeft() == null){ //first standing
						log.debug("INITIAL occupy request from car {} got.", roadOccRequest.getCarName());
						if (!carLeavingTime.containsKey(roadOccRequest.getCarName())){
							workload += workloadDelta;
							carLeavingTime.put(roadOccRequest.getCarName(), addSecondsToNow(workload));
							sendOccupyAccept(roadOccRequest.getCarName());
							broadcastWorkloadUpdate(workloadDelta);
						} else{
							sendOccupyReject(roadOccRequest.getCarName());
						}
					} else if (roadOccRequest.getRoadWished() == null){ //leaving
						log.debug("LEAVE occupy request from car {} got.", roadOccRequest.getCarName());
						workload -= workloadDelta;
						broadcastWorkloadUpdate(-workloadDelta);
						carLeavingTime.remove(roadOccRequest.getCarName());
					}
					break;
				case ACLMessage.INFORM_IF:
					CarMoveRequest carMoveRequest = gson.fromJson(message.getContent(), CarMoveRequest.class);
					String carName = carMoveRequest.getCarName();
					log.debug("Move request got. Somebody wants to check if car {} has left me", carName);
					log.debug("Car {} has to leave me not earlier than {}. Can it be not on me at {}?", carName,
							carLeavingTime.get(carName), carMoveRequest.getRequestTime());
					boolean permitted = carLeavingTime.containsKey(carName) && carLeavingTime.get(carName) <= carMoveRequest.getRequestTime();
					if (permitted) {
						workload -= workloadDelta;
						broadcastWorkloadUpdate(-workloadDelta);
					}
					respondHavingCarChecked(carMoveRequest, permitted);
					break;
				case ACLMessage.INFORM:
					CarMoveResponse carMoveResponse = gson.fromJson(message.getContent(), CarMoveResponse.class);
					log.debug("Move response got. We can decide if car {} can ride here", carMoveResponse.getCarName());
					if (carMoveResponse.isPermitted()){
						workload += workloadDelta;
						long timeBound = addSecondsToNow(workload);
						carLeavingTime.put(carMoveResponse.getCarName(), timeBound);
						log.debug("All good. New workload: {}. Car {} leaves me not earlier than {}", carMoveResponse.getCarName(), workload, timeBound);
						sendOccupyAccept(carMoveResponse.getCarName());
						broadcastWorkloadUpdate(workloadDelta);
					} else {
						sendOccupyReject(carMoveResponse.getCarName());
					}
					break;
				default:
					log.warn("Strange message got: {}. Ignoring...", message.getContent());
					break;
				}
			}else {
				block();
			}
		}

		private void respondHavingCarChecked(CarMoveRequest carMoveRequest, boolean isPermitted) {
			ACLMessage response = new ACLMessage(ACLMessage.INFORM);
			response.addReceiver(new AID(Road.nameOf(carMoveRequest.getRequestedRoad()), AID.ISLOCALNAME));
			log.debug("Car has left me: {}", isPermitted);
			response.setContent(gson.toJson(new CarMoveResponse(carMoveRequest.getCarName(), isPermitted)));
			send(response);
		}

		private void checkRequestWithAnotherRoad(RoadOccupyRequest roadOccRequest) {
			long now = System.currentTimeMillis();
			log.debug("Asking road {} about car {} position at time {}", roadOccRequest.getRoadLeft(), roadOccRequest.getCarName(), now);
			ACLMessage requestToAnotherRoad = new ACLMessage(ACLMessage.INFORM_IF);
			requestToAnotherRoad.addReceiver(new AID(Road.nameOf(roadOccRequest.getRoadLeft()), AID.ISLOCALNAME));
			requestToAnotherRoad.setContent(gson.toJson(new CarMoveRequest(roadOccRequest.getCarName(), now, bounds)));
			log.debug("Move request to road {} sent", roadOccRequest.getRoadLeft());
			send(requestToAnotherRoad);
		}

		private void sendOccupyAccept(String carName) {
			log.debug("Accepting car {} for occupation", carName);
			ACLMessage reply = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
			reply.addReceiver(new AID(carName, AID.ISLOCALNAME));
			reply.setContent(gson.toJson(new RoadOccupyResponse(bounds, workload)));
			send(reply);
		}

		private void sendOccupyReject(String carName) {
			log.debug("Rejecting car {}", carName);
			ACLMessage reply = new ACLMessage(ACLMessage.REJECT_PROPOSAL);
			reply.addReceiver(new AID(carName, AID.ISLOCALNAME));
			reply.setContent(gson.toJson(new RoadOccupyResponse(bounds, Integer.MAX_VALUE)));
			send(reply);
		}


		private void broadcastWorkloadUpdate(int delta) {
			log.debug("Sending broadcast with workload delta: {}", delta);
			ACLMessage broadcast = new ACLMessage(ACLMessage.INFORM);
			broadcast.addReceiver(workloadUpdateTopic);
			broadcast.setContent(gson.toJson(new RoadStatusChange(bounds, delta)));
			send(broadcast);
		}

		private Long addSecondsToNow(int seconds) {
			return System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(seconds);
		}

		private boolean isIncident(Pair anotherRoad) {
			return 	anotherRoad.getFirst()  == bounds.getSecond() ||
					anotherRoad.getFirst()  == bounds.getFirst()  ||
					anotherRoad.getSecond() == bounds.getSecond() ||
					anotherRoad.getSecond()  == bounds.getFirst();
		}

	}

}
