package ru.spbu.math.ais.mas.roads.cars;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Queue;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.SequentialBehaviour;
import jade.lang.acl.ACLMessage;
import ru.spbu.math.ais.mas.roads.City;
import ru.spbu.math.ais.mas.roads.wrappers.Graph;
import ru.spbu.math.ais.mas.roads.wrappers.communication.Pair;
import ru.spbu.math.ais.mas.roads.wrappers.communication.RoadsUpdateRequest;
import ru.spbu.math.ais.mas.roads.wrappers.communication.RoadsUpdateResonse;
import ru.spbu.math.ais.mas.roads.wrappers.communication.ShortestWayRequest;
import ru.spbu.math.ais.mas.roads.wrappers.communication.ShortestWayResponse;

@SuppressWarnings("serial")
public class Car extends Agent {
	private static final Logger log = LoggerFactory.getLogger(Car.class);
	private static final long serialVersionUID = 1L;

	private String city;
	private int src;
	private int dst;
	private Pair currentRoad;
	private int lastVertex;
	private long startTime;
	private long spentTime;
	private DrivingStrategy strategy;

	@Override
	protected void setup() {
		log.info("Car {} inited. Got args: {}", getLocalName(), getArguments());
		city = String.valueOf(getArguments()[0]);
		src  = Integer.parseInt(getArguments()[1].toString());
		dst  = Integer.parseInt(getArguments()[2].toString());
		strategy = DrivingStrategy.valueOf(getArguments()[3].toString());
		switch (strategy) {
		case DUMMY:
			addBehaviour(new DummyDrivingBehaviour(this));
			break;
		case ITERATIVE:
			throw new UnsupportedOperationException();
		case INTELLIGENT:
			throw new UnsupportedOperationException();
		default:
			addBehaviour(new DummyDrivingBehaviour(this));
			break;
		}
	}

	/**
	 * @author vlad, polina
	 *
	 *	Class representing dummy driving behaviour:
	 *	Car figures out its shortest way and keeps sticking to it regardless traffic conditions
	 */
	private class DummyDrivingBehaviour extends SequentialBehaviour{
		
		protected Queue<Integer> path;
		
		public DummyDrivingBehaviour(Car car) {
			super(car);
			//WAY CALCULATION STAGE (Figuring out where we should drive)
			this.addSubBehaviour(new OneShotBehaviour(car) {
				@Override
				public void action() {
					try {
						log.debug("Car {} is estimating its way.", getLocalName());
						send(constructMessageForCity(
								City.SHORTEST_WAY_CONVERSATION,
								new ShortestWayRequest(src, dst))
						);
						log.debug("Car {} has asked the city for the shortest way. Waiting...", getLocalName());
						ShortestWayResponse response = (ShortestWayResponse) myAgent.blockingReceive().getContentObject();
						log.debug("Car {} has got a response : {}", getLocalName(), response.toString());
						DummyDrivingBehaviour.this.path = (Queue<Integer>)response.getWayInfo().get(Graph.PATH_KEY);
						startTime = 0;
					} catch (Exception e) {
						log.error("Error while getting initial way {}", e);
					}
				}

			});
			//DRIVING STAGE(actual driving simulation)
			this.addSubBehaviour(new Behaviour(car) {
				@Override
				public void action() {
					try {
						Integer nextVertex = path.remove();
						Pair nextRoad = new Pair(lastVertex, nextVertex);
						send(constructMessageForCity(
								City.SHORTEST_WAY_CONVERSATION,
								new RoadsUpdateRequest(currentRoad, nextRoad))
						);
						RoadsUpdateResonse response = (RoadsUpdateResonse) myAgent.blockingReceive().getContentObject();
						int timeOnNewRoad = response.getNewRoadWorkload();
						currentRoad = nextRoad;
						log.debug("Car {} is driving at {} for {} sec.", getLocalName(), currentRoad, timeOnNewRoad);
						TimeUnit.SECONDS.sleep(timeOnNewRoad);
						log.debug("Car {} has drived the road.", getLocalName());
						lastVertex = currentRoad.getSecond();
						spentTime += timeOnNewRoad;
					} catch (Exception e) {
						log.error("Car {} has crashed.", getLocalName());
					}
				}
				@Override
				public boolean done() {
					return lastVertex == dst;
				}
			});
		}


		@Override
		public int onEnd() {
			log.debug("Car {} has reached its destination spending {} s.", getLocalName(), spentTime);
			myAgent.doDelete();
			log.debug("Car {} is shut down.", getLocalName());
			return 0;
		}
		// HELPER METHODS
		private ACLMessage constructMessageForCity(String subject, Serializable content) throws IOException {
			ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
			msg.addReceiver(new AID(city, AID.ISLOCALNAME));
			msg.setConversationId(subject);
			msg.setContentObject(content);
			return msg;
		}
		
	}	
	

}
