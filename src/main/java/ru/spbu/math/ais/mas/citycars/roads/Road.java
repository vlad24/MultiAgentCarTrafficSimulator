package ru.spbu.math.ais.mas.citycars.roads;

import java.util.Set;

import com.google.gson.Gson;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import lombok.extern.slf4j.Slf4j;
import ru.spbu.math.ais.mas.citycars.wrappers.Pair;
import ru.spbu.math.ais.mas.citycars.wrappers.communication.CarCheckRequest;
import ru.spbu.math.ais.mas.citycars.wrappers.communication.CityCommunicationUnit;
import ru.spbu.math.ais.mas.citycars.wrappers.communication.RoadsUpdateRequest;

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
					RoadsUpdateRequest roadOccRequest = gson.fromJson(msg.getContent(), RoadsUpdateRequest.class); 
					//TODO go on
					log.debug("wanna be occupied");
					break;
				case CAR_CHECK:
					CarCheckRequest carCheckRequest = gson.fromJson(msg.getContent(), CarCheckRequest.class);
					//TODO go on
					log.debug("wanna check car");
					break;
				default:
					break;
				}
			}else {
				block();
			}
		}



	}

}
