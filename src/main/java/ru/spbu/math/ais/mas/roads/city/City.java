package ru.spbu.math.ais.mas.roads.city;

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import ru.spbu.math.ais.mas.roads.wrappers.Graph;
import ru.spbu.math.ais.mas.roads.wrappers.communication.RoadsUpdateRequest;
import ru.spbu.math.ais.mas.roads.wrappers.communication.ShortestWayRequest;
import ru.spbu.math.ais.mas.roads.wrappers.communication.ShortestWayResponse;
import ru.spbu.math.ais.mas.roads.wrappers.communication.TripFinishReport;
import ru.spbu.math.ais.mas.roads.wrappers.communication.TripStartRequest;
import ru.spbu.math.ais.mas.roads.wrappers.communication.TripStartResponse;

public class City extends Agent {
	private static final long serialVersionUID = 1L;
	private static final Logger log = LoggerFactory.getLogger(City.class);
	
	public static final String SHORTEST_WAY_CONVERSATION = "shortestWayConversation";
	public static final String ROADS_UPDATE_CONVERSATION  = "roadUpdateConversation";
	public static final String START_TRIP_CONVERSATION = "registerTrip";
	public static final String FINISH_TRIP_CONVERSATION = "carStat";
	private Graph cityGraph;
	
	
	@Override
	protected void setup() {
		log.info("{} is ready. Got args:{}", getLocalName(), getArguments());
		this.cityGraph = (Graph) getArguments()[0];
		addBehaviour(new ServerBehaviour(this));
		log.trace("Min distances {}", cityGraph.getMinDistances(1,7));
	}
	
	@SuppressWarnings("serial")
	private class ServerBehaviour extends CyclicBehaviour{
		
		public ServerBehaviour(City city) {
			super(city);
		}

		@Override
		public void action() {
			try {
				ACLMessage message = myAgent.blockingReceive();
				if (START_TRIP_CONVERSATION.equalsIgnoreCase(message.getConversationId())) {
					TripStartRequest request = (TripStartRequest)message.getContentObject();
					//TODO monitor cars smarter
					replyWithContent(message, new TripStartResponse(true));
				}else if (SHORTEST_WAY_CONVERSATION.equalsIgnoreCase(message.getConversationId())){
					//TODO check this code
					ShortestWayRequest request = (ShortestWayRequest)message.getContentObject();
					Map<String, Object> wayInfo = cityGraph.getMinDistances(request.getFrom(), request.getTo());
					replyWithContent(message, new ShortestWayResponse(wayInfo));
				}else if(ROADS_UPDATE_CONVERSATION.equalsIgnoreCase(message.getConversationId())){
					RoadsUpdateRequest request = (RoadsUpdateRequest)message.getContentObject();
					//TODO process changes
					
				}else if(FINISH_TRIP_CONVERSATION.equalsIgnoreCase(message.getConversationId())){
					TripFinishReport request = (TripFinishReport)message.getContentObject();
					//TODO process report
					
				}
			} catch (Exception e) {
				log.error("Error in big city life!", e);
			}
		}

		private void replyWithContent(ACLMessage message, Serializable response) throws IOException {
			ACLMessage reply = message.createReply();
			reply.setContentObject(response);
			send(reply);
		}

	}

}