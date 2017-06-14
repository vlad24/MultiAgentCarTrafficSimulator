package ru.spbu.math.ais.mas.citycars.roads;

import java.util.Date;
import java.util.Set;

import org.omg.CORBA.INTERNAL;

import com.google.gson.Gson;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import lombok.extern.slf4j.Slf4j;
import ru.spbu.math.ais.mas.citycars.wrappers.Pair;
import ru.spbu.math.ais.mas.citycars.wrappers.communication.CarMoveRequest;
import ru.spbu.math.ais.mas.citycars.wrappers.communication.CarMoveResponse;
import ru.spbu.math.ais.mas.citycars.wrappers.communication.CityCommunicationUnit;
import ru.spbu.math.ais.mas.citycars.wrappers.communication.RoadOccupyPermission;
import ru.spbu.math.ais.mas.citycars.wrappers.communication.RoadOccupyRequest;
import ru.spbu.math.ais.mas.citycars.wrappers.communication.RoadStatusChange;

@SuppressWarnings("serial")
@Slf4j
public class Road extends Agent{

	private String cityName;
	private Pair bounds;
	private int workLoad;
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
		workLoad = 0;
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
		
		public RoadBehaviour() {
			gson = new Gson();
		}

		@Override
		public void action() {
			ACLMessage msg = receive();
			if (msg != null) {
				CityCommunicationUnit unit = gson.fromJson(msg.getContent(), CityCommunicationUnit.class); 
				switch (unit.getSubject()) {
				case ROADS_OCCUPATION:
					RoadOccupyRequest roadOccRequest = gson.fromJson(msg.getContent(), RoadOccupyRequest.class); 
					if(bounds.equals(roadOccRequest.getRoadWished()) && 
							roadOccRequest.getRoadLeft().getSecond() == bounds.getFirst()) {
						ACLMessage reqToAnotherRoad = new ACLMessage(ACLMessage.REQUEST);
						reqToAnotherRoad.addReceiver(new AID(Road.nameOf(roadOccRequest.getRoadWished()), AID.ISLOCALNAME));
						reqToAnotherRoad.setContent(gson.toJson(new CarMoveRequest(roadOccRequest.getCarName(), new Date(), bounds)));
						send(reqToAnotherRoad);
					} else {
						ACLMessage reply = msg.createReply();
						reply.setContent(gson.toJson(new RoadOccupyPermission(bounds, false, Integer.MAX_VALUE)));
						send(reply);						
					}
					log.debug("wanna be occupied");
					break;
				case CAR_MOVE_REQUEST:
					CarMoveRequest carMoveRequest = gson.fromJson(msg.getContent(), CarMoveRequest.class);
					//TODO go on
					log.debug("wanna check car");
					break;
				case CAR_MOVE_RESPONSE:
					CarMoveResponse carMoveResponse = gson.fromJson(msg.getContent(), CarMoveResponse.class);
					if (carMoveResponse.isPermitted()){
						ACLMessage reply = new ACLMessage(ACLMessage.CONFIRM);
						reply.addReceiver(new AID(carMoveResponse.getCarId(), AID.ISLOCALNAME));
						workLoad += workloadDelta;
						reply.setContent(gson.toJson(new RoadOccupyPermission(bounds, carMoveResponse.isPermitted(), workLoad)));
						send(reply);
						broadcast(workloadDelta);
					} else {
						ACLMessage reply = new ACLMessage(ACLMessage.DISCONFIRM);
						reply.addReceiver(new AID(carMoveResponse.getCarId(), AID.ISLOCALNAME));
						reply.setContent(gson.toJson(new RoadOccupyPermission(bounds, carMoveResponse.isPermitted(), Integer.MAX_VALUE)));
						send(reply);
					}
					break;
				default:
					break;
				}
			}else {
				block();
			}
		}

		private void broadcast(int delta) {
			ACLMessage broadcast = new ACLMessage(ACLMessage.INFORM);
			for (String id : carIds) {
				broadcast.addReceiver(new AID(id, AID.ISLOCALNAME));
			}
			broadcast.setContent(gson.toJson(new RoadStatusChange(bounds, delta)));
			send(broadcast);
		}



	}

}
