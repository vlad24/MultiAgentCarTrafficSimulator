package ru.spbu.math.ais.mas.citycars.roads;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;
import ru.spbu.math.ais.mas.citycars.wrappers.Pair;
import ru.spbu.math.ais.mas.citycars.wrappers.communication.CarMoveRequest;
import ru.spbu.math.ais.mas.citycars.wrappers.communication.CarMoveResponse;
import ru.spbu.math.ais.mas.citycars.wrappers.communication.CityCommunicationUnit;
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
		StringBuilder builder = new StringBuilder();
		builder.append("(");
		builder.append(edge.getFirst());
		builder.append(",");
		builder.append(edge.getSecond());
		builder.append(")");
		return builder.toString();
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void setup() {
		super.setup();
		workload = 0;
		int argPos = 0;
		cityName      = String.valueOf(getArguments()[argPos++]);
		bounds        = (Pair)(getArguments()[argPos++]);
		workloadDelta = Integer.parseInt(getArguments()[argPos++].toString());
		//for broadcasting 
		carIds        = (Set<String>) getArguments()[argPos++];
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
			ACLMessage msg = receive();
			if (msg != null) {
				CityCommunicationUnit unit = gson.fromJson(msg.getContent(), CityCommunicationUnit.class); 
				switch (unit.getSubject()) {
				case ROADS_OCCUPATION:
					RoadOccupyRequest roadOccRequest = gson.fromJson(msg.getContent(), RoadOccupyRequest.class);
					if (roadOccRequest.getRoadLeft() != null &&
							roadOccRequest.getRoadWished() != null){
						if(bounds.equals(roadOccRequest.getRoadWished()) && 
								roadOccRequest.getRoadLeft().getSecond() == bounds.getFirst()) {
							ACLMessage reqToAnotherRoad = new ACLMessage(ACLMessage.REQUEST);
							reqToAnotherRoad.addReceiver(new AID(Road.nameOf(roadOccRequest.getRoadWished()), AID.ISLOCALNAME));
							reqToAnotherRoad.setContent(gson.toJson(new CarMoveRequest(roadOccRequest.getCarName(), System.currentTimeMillis(), bounds)));
							send(reqToAnotherRoad);
						} else {
							sendOccupyReject(roadOccRequest.getCarName());
						}						
					} else if (roadOccRequest.getRoadLeft() == null){ //first standing
						if (!carLeavingTime.containsKey(roadOccRequest.getCarName())){
							workload += workloadDelta;
							carLeavingTime.put(roadOccRequest.getCarName(), addSecondsToNow(workload));
							sendOccupyAccept(roadOccRequest.getCarName());
							broadcast(workloadDelta);
						} else{
							sendOccupyReject(roadOccRequest.getCarName());
						}
					} else if (roadOccRequest.getRoadWished() == null){ //leaving
						workload -= workloadDelta;
						broadcast(-workloadDelta);
						carLeavingTime.remove(roadOccRequest.getCarName());
					}
					log.debug("wanna be occupied");
					break;
				case CAR_MOVE_REQUEST:
					CarMoveRequest carMoveRequest = gson.fromJson(msg.getContent(), CarMoveRequest.class);
					String carName = carMoveRequest.getCarName();
					ACLMessage reply = msg.createReply();
					boolean permitted = carLeavingTime.containsKey(carName) && carLeavingTime.get(carName) < carMoveRequest.getRequestTime();
					reply.setContent(gson.toJson(new CarMoveResponse(carName, permitted)));
					send(reply);
					log.debug("wanna check car");
					break;
				case CAR_MOVE_RESPONSE:
					CarMoveResponse carMoveResponse = gson.fromJson(msg.getContent(), CarMoveResponse.class);
					if (carMoveResponse.isPermitted()){
						workload += workloadDelta;
						carLeavingTime.put(carMoveResponse.getCarName(), addSecondsToNow(workload));
						sendOccupyAccept(carMoveResponse.getCarName());
						broadcast(workloadDelta);
					} else {
						sendOccupyReject(carMoveResponse.getCarName());
					}
					break;
				default:
					break;
				}
			}else {
				block();
			}
		}

		private void sendOccupyReject(String carName) {
			ACLMessage reply = new ACLMessage(ACLMessage.DISCONFIRM);
			reply.addReceiver(new AID(carName, AID.ISLOCALNAME));
			reply.setContent(gson.toJson(new RoadOccupyPermission(bounds, false, Integer.MAX_VALUE)));
			send(reply);		
		}
		
		private void sendOccupyAccept(String carName) {
			ACLMessage reply = new ACLMessage(ACLMessage.CONFIRM);
			reply.addReceiver(new AID(carName, AID.ISLOCALNAME));
			reply.setContent(gson.toJson(new RoadOccupyPermission(bounds, true, workload)));
			send(reply);		
		}

		private void broadcast(int delta) {
			ACLMessage broadcast = new ACLMessage(ACLMessage.INFORM);
			for (String id : carIds) {
				broadcast.addReceiver(new AID(id, AID.ISLOCALNAME));
			}
			broadcast.setContent(gson.toJson(new RoadStatusChange(bounds, delta)));
			send(broadcast);
		}
		
		private Long addSecondsToNow(int seconds) {
			return System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(seconds);
		}



	}

}
