package ru.spbu.math.ais.mas.roads.car;

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.UnreadableException;
import ru.spbu.math.ais.mas.roads.Configurator;
import ru.spbu.math.ais.mas.roads.city.City;
import ru.spbu.math.ais.mas.roads.wrappers.Graph;
import ru.spbu.math.ais.mas.roads.wrappers.communication.Pair;
import ru.spbu.math.ais.mas.roads.wrappers.communication.RoadsUpdateRequest;
import ru.spbu.math.ais.mas.roads.wrappers.communication.RoadsUpdateResonse;
import ru.spbu.math.ais.mas.roads.wrappers.communication.ShortestWayRequest;
import ru.spbu.math.ais.mas.roads.wrappers.communication.ShortestWayResponse;
import ru.spbu.math.ais.mas.roads.wrappers.communication.TripFinishReport;
import ru.spbu.math.ais.mas.roads.wrappers.communication.TripStartRequest;
import ru.spbu.math.ais.mas.roads.wrappers.communication.TripStartResponse;

@SuppressWarnings("serial")
public class Car extends Agent {
	private static final Logger log = LoggerFactory.getLogger(Car.class);
	private static final long serialVersionUID = 1L;

	private String carName;
	private DrivingStrategy strategy;

	@Override
	@SuppressWarnings("unchecked")
	protected void setup() {
		carName          = getLocalName();
		String cityName  = String.valueOf(getArguments()[0]);
		int source       = Integer.parseInt(getArguments()[1].toString());
		int destination  = Integer.parseInt(getArguments()[2].toString());
		strategy         = DrivingStrategy.valueOf(getArguments()[3].toString());
		Map<String, Object> strategyParams = (Map<String, Object>) getArguments()[4];
		log.info("Car {} inited.", carName , getArguments());
		switch (strategy) {
		case DUMMY:
			addBehaviour(new BasicDrivingBehaviour(this, cityName, source, destination, null));
			break;
		case ITERATIVE:
			int refreshPeriod = Integer.parseInt(strategyParams.get(Configurator.CAR_REFRESH_KEY).toString());
			addBehaviour(new BasicDrivingBehaviour(this, cityName, source, destination, refreshPeriod));
			break;
		case INTELLIGENT:
			throw new UnsupportedOperationException();
		default:
			addBehaviour(new BasicDrivingBehaviour(this, cityName, source, destination, null));
			break;
		}
	}

	/**
	 * @author vlad, polina
	 * <br>
	 *	Class representing basic driving behavior:
	 *	Car figures out its shortest way and keeps sticking to it.
	 *  A car can update the information about current city traffic state with some fixed period (if provided).
	 */
	private class BasicDrivingBehaviour extends Behaviour{
		private String cityName;
		private int source;
		private int destination;
		private Pair currentRoad;
		private int lastReachedVertex;
		private int spentTime;
		private int roadsPassed;
		private int refreshPeriod;
		protected Queue<Integer> optimalRoute;
		private boolean carRegistered;

		public BasicDrivingBehaviour(Car car, String cityName, int src, int dst, Integer refreshPeriod) {
			super(car);
			this.refreshPeriod = (refreshPeriod == null)? Integer.MAX_VALUE : refreshPeriod;
			this.source = src;
			this.destination = dst;
			this.lastReachedVertex = src;
			this.roadsPassed = 0;
			this.spentTime = 0;
			this.cityName = cityName;
		}
		@Override
		public void action() {
			try {
				if (!carRegistered){
					register();
				}
				if (roadsPassed % refreshPeriod == 0) {
					updateRoute();
				}
				Integer nextVertex = optimalRoute.remove();
				Pair targetRoad = new Pair(lastReachedVertex, nextVertex);
				log.debug("Car {} wants to turn on road {}.", carName, targetRoad);
				send(constructMessageForCity(
						ACLMessage.REQUEST,
						City.ROADS_UPDATE_CONVERSATION,
						new RoadsUpdateRequest(currentRoad, targetRoad))
						);
				log.debug("Car {} is waiting for city response...", carName);
				RoadsUpdateResonse response = (RoadsUpdateResonse) myAgent.blockingReceive().getContentObject();
				int timeOnTargetRoad = response.getNewRoadWorkload();
				currentRoad = targetRoad;
				log.debug("Car {} is driving at road {} for {} sec.", carName, currentRoad, timeOnTargetRoad);
				TimeUnit.SECONDS.sleep(timeOnTargetRoad);
				log.debug("Car {} has driven the road.", carName);
				lastReachedVertex = nextVertex;
				spentTime += timeOnTargetRoad;
				roadsPassed++;
			} catch (Exception e) {
				log.error("Car {} has crashed:{}", carName, e);
				e.printStackTrace();
				doDelete();
			}
		}

		@Override
		public boolean done() {
			return lastReachedVertex == destination;
		}

		@Override
		public int onEnd() {
			try {
				log.debug("Car {} has reached its destination and spent {} sec in total", carName, spentTime);
				send(constructMessageForCity(
						ACLMessage.INFORM, City.FINISH_TRIP_CONVERSATION, 
						new TripFinishReport(carName, spentTime, currentRoad)));
				log.debug("Car {} has sent its report.", carName);
				myAgent.doDelete();
				log.debug("Car {} is shut down.", carName);
				return 0;
			} catch (IOException e) {
				e.printStackTrace();
				return 1;
			}
		}

		/*
		 * Helper methods
		 */
		private void register() {
			log.debug("Car {} is registering in city.", carName);
			try {
				send(constructMessageForCity(
							ACLMessage.REQUEST,	City.START_TRIP_CONVERSATION,
							new TripStartRequest(carName)
					));
				TripStartResponse response = (TripStartResponse) myAgent.blockingReceive().getContentObject();
				this.carRegistered = response.isPermitted();
			} catch (IOException | UnreadableException e) {
				log.error("Error registering : {}", e);
				e.printStackTrace();
			}
			
		}
		
		@SuppressWarnings("unchecked")
		private void updateRoute() {
			log.debug("Car {} is estimating its way.", carName);
			try {
				send(constructMessageForCity(
						ACLMessage.REQUEST,	City.SHORTEST_WAY_CONVERSATION,
						new ShortestWayRequest(lastReachedVertex, destination))
					);
				log.debug("Car {} has asked the city for the shortest way from {} to {}. Waiting...", carName, source, destination);
				ShortestWayResponse response = (ShortestWayResponse) myAgent.blockingReceive().getContentObject();
				log.debug("Car {} has got a response for shortest way request: {}", carName, response.toString());
				BasicDrivingBehaviour.this.optimalRoute = (Queue<Integer>)response.getWayInfo().get(Graph.PATH_KEY);
				assert (optimalRoute.size() > 1);
				lastReachedVertex = source;
				optimalRoute.remove(); // get rid of the first 'source'-vertex
			} catch (IOException | UnreadableException e) {
				log.error("Error rebuilding route : {}", e);
			}
		}

		private ACLMessage constructMessageForCity(int performative, String conversationTopic, Serializable content) throws IOException {
			ACLMessage msg = new ACLMessage(performative);
			msg.addReceiver(new AID(cityName, AID.ISLOCALNAME));
			msg.setConversationId(conversationTopic);
			msg.setContentObject(content);
			return msg;
		}

	}	

}
