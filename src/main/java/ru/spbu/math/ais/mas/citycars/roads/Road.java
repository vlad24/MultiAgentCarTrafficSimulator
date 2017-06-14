package ru.spbu.math.ais.mas.citycars.roads;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;
import ru.spbu.math.ais.mas.citycars.wrappers.Pair;
import ru.spbu.math.ais.mas.citycars.wrappers.communication.CarMoveRequest;
import ru.spbu.math.ais.mas.citycars.wrappers.communication.CarMoveResponse;
import ru.spbu.math.ais.mas.citycars.wrappers.communication.CityMessageType;
import ru.spbu.math.ais.mas.citycars.wrappers.communication.RoadOccupyPermission;
import ru.spbu.math.ais.mas.citycars.wrappers.communication.RoadOccupyRequest;
import ru.spbu.math.ais.mas.citycars.wrappers.communication.RoadStatusChange;

import com.google.gson.Gson;

@SuppressWarnings("serial")
@Slf4j
public class Road extends Agent{

	private String cityName;
	private Pair bounds;
	private int workload;
	private int workloadDelta;
	private Set<String> carIds;
	

	public static String nameOf(Pair edge) {
		return new StringBuilder()
		.append("(")
			.append(edge.getFirst())
			.append(",")
			.append(edge.getSecond())
		.append(")")
		.toString();
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void setup() {
		super.setup();
		int argPos = 0;
		cityName      = String.valueOf(getArguments()[argPos++]);
		bounds        = (Pair)(getArguments()[argPos++]);
		workload      = Integer.parseInt(getArguments()[argPos++].toString());
		workloadDelta = Integer.parseInt(getArguments()[argPos++].toString());
		carIds        = (Set<String>) getArguments()[argPos++];
		log.info("Road {} in {} is created.", getLocalName(), cityName);
		addBehaviour(new RoadBehaviour());

	}

	private class RoadBehaviour extends CyclicBehaviour{

		private Gson gson;
		private Map<String, Long> carLeavingTime;
		
		public RoadBehaviour() {
			gson = new Gson();
			carLeavingTime = new HashMap<String, Long>();
		}

		@Override
		public void action() {
			ACLMessage message = receive();
			if (message != null) {
				log.debug("/// Got json message: {}", message.getContent());
				CityMessageType messageType = CityMessageType.valueOf(message.getOntology());
				switch (messageType) {
				case ROADS_OCCUPATION:
					RoadOccupyRequest roadOccRequest = gson.fromJson(message.getContent(), RoadOccupyRequest.class);
					if (roadOccRequest.getRoadLeft() != null && roadOccRequest.getRoadWished() != null){
						log.debug("Occupy request got. Road left:{}. Road wished: {}", roadOccRequest.getRoadLeft(), roadOccRequest.getRoadWished());
						if(bounds.equals(roadOccRequest.getRoadWished()) && roadOccRequest.getRoadLeft().getSecond() == bounds.getFirst()) {
							checkRequestWithAnotherRoad(roadOccRequest);
						} else {
							sendOccupyReject(roadOccRequest.getCarName());
						}						
					} else if (roadOccRequest.getRoadLeft() == null){ //first standing
						log.debug("FIRST Occupy request from car {} got.", roadOccRequest.getCarName());
						if (!carLeavingTime.containsKey(roadOccRequest.getCarName())){
							workload += workloadDelta;
							carLeavingTime.put(roadOccRequest.getCarName(), addSecondsToNow(workload));
							sendOccupyAccept(roadOccRequest.getCarName());
							broadcast(workloadDelta);
						} else{
							sendOccupyReject(roadOccRequest.getCarName());
						}
					} else if (roadOccRequest.getRoadWished() == null){ //leaving
						log.debug("LAST Occupy request from car {} got.", roadOccRequest.getCarName());
						workload -= workloadDelta;
						broadcast(-workloadDelta);
						carLeavingTime.remove(roadOccRequest.getCarName());
					}
					break;
				case CAR_MOVE_REQUEST:
					CarMoveRequest carMoveRequest = gson.fromJson(message.getContent(), CarMoveRequest.class);
					String carName = carMoveRequest.getCarName();
					log.debug("Move request got. Sombody wants to check if car {} has left me", carName);
					boolean permitted = carLeavingTime.containsKey(carName) && carLeavingTime.get(carName) < carMoveRequest.getRequestTime();
					respondHavingCarChecked(carMoveRequest, permitted);
					break;
				case CAR_MOVE_RESPONSE:
					CarMoveResponse carMoveResponse = gson.fromJson(message.getContent(), CarMoveResponse.class);
					log.debug("Move response got. We can decide if car {} can ride here", carMoveResponse.getCarName());
					if (carMoveResponse.isPermitted()){
						workload += workloadDelta;
						long timeBound = addSecondsToNow(workload);
						carLeavingTime.put(carMoveResponse.getCarName(), timeBound);
						log.debug("All good. New workload: {}. Car leaves me not earlier than {}", workload, timeBound);
						sendOccupyAccept(carMoveResponse.getCarName());
						broadcast(workloadDelta);
					} else {
						sendOccupyReject(carMoveResponse.getCarName());
					}
					break;
				default:
					log.warn("Strange message got: {}", message.getContent());
					break;
				}
			}else {
				block();
			}
		}

		private void respondHavingCarChecked(CarMoveRequest carMoveRequest, boolean isPermitted) {
			ACLMessage response = new ACLMessage(ACLMessage.INFORM);
			response.addReceiver(new AID(Road.nameOf(carMoveRequest.getRequestedRoad()), AID.ISLOCALNAME));
			response.setOntology(CityMessageType.CAR_MOVE_RESPONSE.toString());
			log.debug("Car has left me: {}", isPermitted);
			response.setContent(gson.toJson(new CarMoveResponse(carMoveRequest.getCarName(), isPermitted)));
			send(response);
		}

		private void checkRequestWithAnotherRoad(RoadOccupyRequest roadOccRequest) {
			log.debug("Asking road {} about car {}", roadOccRequest.getRoadLeft(), roadOccRequest.getCarName());
			ACLMessage requestToAnotherRoad = new ACLMessage(ACLMessage.REQUEST);
			requestToAnotherRoad.addReceiver(new AID(Road.nameOf(roadOccRequest.getRoadLeft()), AID.ISLOCALNAME));
			requestToAnotherRoad.setOntology(CityMessageType.CAR_MOVE_REQUEST.toString());
			requestToAnotherRoad.setContent(gson.toJson(new CarMoveRequest(roadOccRequest.getCarName(), System.currentTimeMillis(), bounds)));
			log.debug("Move request to road {} sent", roadOccRequest.getRoadWished());
			send(requestToAnotherRoad);
		}

		private void sendOccupyReject(String carName) {
			log.debug("Rejecting car {}", carName);
			ACLMessage reply = new ACLMessage(ACLMessage.DISCONFIRM);
			reply.addReceiver(new AID(carName, AID.ISLOCALNAME));
			reply.setOntology(CityMessageType.ROADS_OCCUPATION.toString());
			reply.setContent(gson.toJson(new RoadOccupyPermission(bounds, false, Integer.MAX_VALUE)));
			send(reply);		
		}
		
		private void sendOccupyAccept(String carName) {
			log.debug("Accepting car {} for occupation", carName);
			ACLMessage reply = new ACLMessage(ACLMessage.CONFIRM);
			reply.addReceiver(new AID(carName, AID.ISLOCALNAME));
			reply.setOntology(CityMessageType.ROADS_OCCUPATION.toString());
			reply.setContent(gson.toJson(new RoadOccupyPermission(bounds, true, workload)));
			send(reply);		
		}

		private void broadcast(int delta) {
			log.debug("Sending broadcast to all cars with workload delta: {}", delta);
			ACLMessage broadcast = new ACLMessage(ACLMessage.INFORM);
			for (String id : carIds) {
				broadcast.addReceiver(new AID(id, AID.ISLOCALNAME));
			}
			broadcast.setOntology(CityMessageType.ROAD_STATUS_CHANGE_NOTIFICATION.toString());
			broadcast.setContent(gson.toJson(new RoadStatusChange(bounds, delta)));
			send(broadcast);
		}
		
		private Long addSecondsToNow(int seconds) {
			return System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(seconds);
		}
		
	}

}
