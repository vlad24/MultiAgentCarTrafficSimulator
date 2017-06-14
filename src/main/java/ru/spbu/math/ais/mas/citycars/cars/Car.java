package ru.spbu.math.ais.mas.citycars.cars;

import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.TimeUnit;

import com.google.gson.Gson;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.lang.acl.ACLMessage;
import lombok.extern.slf4j.Slf4j;
import ru.spbu.math.ais.mas.citycars.Configurator;
import ru.spbu.math.ais.mas.citycars.roads.Road;
import ru.spbu.math.ais.mas.citycars.wrappers.Graph;
import ru.spbu.math.ais.mas.citycars.wrappers.Pair;
import ru.spbu.math.ais.mas.citycars.wrappers.communication.CityMessageType;
import ru.spbu.math.ais.mas.citycars.wrappers.communication.RoadOccupyPermission;
import ru.spbu.math.ais.mas.citycars.wrappers.communication.RoadOccupyRequest;
import ru.spbu.math.ais.mas.citycars.wrappers.communication.RoadStatusChange;
import ru.spbu.math.ais.mas.citycars.wrappers.communication.TripFinishReport;
import ru.spbu.math.ais.mas.citycars.wrappers.communication.TripStartRequest;

@Slf4j
@SuppressWarnings("serial")
public class Car extends Agent {

	private String carName;
	private DrivingStrategy strategy;

	@Override
	@SuppressWarnings("unchecked")
	protected void setup() {
		carName          = getLocalName();
		int argPos = 0;
		String cityName  = String.valueOf(getArguments()[argPos++]);
		Graph cityGraph  = (Graph)(getArguments()[argPos++]);
		int source       = Integer.parseInt(getArguments()[argPos++].toString());
		int destination  = Integer.parseInt(getArguments()[argPos++].toString());
		strategy         = DrivingStrategy.valueOf(getArguments()[argPos++].toString());
		Map<String, Object> strategyParams = (Map<String, Object>) getArguments()[5];
		log.info("Inited.");
		switch (strategy) {
		case DUMMY:
			addBehaviour(new BasicDrivingBehaviour(this, cityName, cityGraph, source, destination, Integer.MAX_VALUE));
			break;
		case ITERATIVE:
			int refreshPeriod = Integer.parseInt(strategyParams.get(Configurator.CAR_REFRESH_KEY).toString());
			addBehaviour(new BasicDrivingBehaviour(this, cityName, cityGraph, source, destination, refreshPeriod));
			break;
		case INTELLIGENT:
			throw new UnsupportedOperationException();
		default:
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
		private Graph cityGraph;
		private int source;
		private int destination;
		private Pair currentRoad;
		private Pair wishedRoad;
		private int lastReachedVertex;
		private int spentTime;
		private int roadsPassed;
		private Queue<RoadStatusChange> roadChanges;
		private Queue<Integer> currentOptimalRoute;
		private int refreshCount; 
		private Gson gson;
		private boolean isTurning;


		public BasicDrivingBehaviour(Car car, String cityName, Graph cityGraph, int src, int dst, int refresh) {
			super(car);
			this.cityName = cityName;
			this.cityGraph = cityGraph;
			this.source = src;
			this.destination = dst;
			this.currentRoad = null;
			this.wishedRoad = null;
			this.lastReachedVertex = src;
			this.refreshCount = refresh;
			this.spentTime = 0;
			this.roadsPassed = 0;
			this.roadChanges = new LinkedList<>();
			this.isTurning = false;
			this.gson = new Gson();
		}

		@Override
		public void onStart() {
			super.onStart();
			log.debug("Registering in city...");
			ACLMessage registerMessage = new ACLMessage(ACLMessage.INFORM);
			registerMessage.addReceiver(new AID(cityName, AID.ISLOCALNAME));
			registerMessage.setOntology(CityMessageType.CAR_REGISTER.toString());
			registerMessage.setContent(gson.toJson(new TripStartRequest(carName)));
			send(registerMessage);
		}

		@SuppressWarnings("unchecked")
		@Override
		public void action() {
			try {
				if (!isTurning) {
					if (roadsPassed % refreshCount == 0) {
						log.info("Passed {} roads. Time to build new route...", roadsPassed);
						for (RoadStatusChange change: this.roadChanges) {
							log.debug("Applying changes to city graph {}", change);
							cityGraph.changeEdgeLength(change.getRoad().getFirst(), change.getRoad().getSecond(), change.getDelta());
						}
						currentOptimalRoute = (Queue<Integer>) cityGraph.getMinDistances(lastReachedVertex, destination).get(Graph.PATH_KEY);
						currentOptimalRoute.remove();//remove first src
						log.debug("Optimal route built: {}", currentOptimalRoute);
					}
					int nextVertex = currentOptimalRoute.element();
					wishedRoad = new Pair(lastReachedVertex, nextVertex);
					log.debug("Sending occupy request to road {}", wishedRoad);
					ACLMessage outMessage = new ACLMessage(ACLMessage.REQUEST);
					outMessage.addReceiver(new AID(Road.nameOf(wishedRoad), AID.ISLOCALNAME));
					outMessage.setOntology(CityMessageType.ROADS_OCCUPATION.toString());
					String json = gson.toJson(new RoadOccupyRequest(carName, currentRoad, wishedRoad));
					outMessage.setContent(json);
					send(outMessage);
					isTurning = true;
				}
				ACLMessage reply = receive();
				if (reply != null) {
					log.debug("*** Got json:{}", reply.getContent());
					CityMessageType messageType = CityMessageType.valueOf(reply.getOntology());
					switch (messageType) {
					case ROADS_OCCUPATION:
						RoadOccupyPermission roadOccPermission = gson.fromJson(reply.getContent(), RoadOccupyPermission.class);
						if (roadOccPermission.isPermitted() && roadOccPermission.getRoad().equals(wishedRoad)) {
							isTurning = false;
							currentRoad = wishedRoad;
							log.debug("Driving at new road {} for {} secs", currentRoad, roadOccPermission.getNewRoadWorkload());
							// If the car does not sleep the next road will not let the car occupy it
							TimeUnit.SECONDS.sleep(roadOccPermission.getNewRoadWorkload());
							spentTime  += roadOccPermission.getNewRoadWorkload();
							log.debug("Road {} passed!", currentRoad);
							roadsPassed++;
							lastReachedVertex = currentOptimalRoute.remove();
							log.debug("Reached:{}", lastReachedVertex);
						}
						break;
					case ROAD_STATUS_CHANGE_NOTIFICATION:
						RoadStatusChange roadChange = gson.fromJson(reply.getContent(), RoadStatusChange.class);
						log.debug("Road {} has changed its workload. Memorized...", roadChange.getRoad());
						roadChanges.add(roadChange);
						break;
					default:
						log.debug("Strange message got: {}. Ignoring...", reply.getContent());
						break;
					}
				} else {
					log.debug("Standing at crossroads...");
					block();
				}	
			}catch (InterruptedException crashedDuringDriving) {
				log.error("Error while driving!", crashedDuringDriving);
			}
		}
		
		@Override
		public boolean done() {
			return lastReachedVertex == destination;
		}

		@Override
		public int onEnd() {
			log.info("Car {} has reached its destination and spent {} sec in total.", carName, spentTime);
			log.debug("Releasing last road...");
			ACLMessage lastRoadMsg = new ACLMessage(ACLMessage.REQUEST);
			lastRoadMsg.addReceiver(new AID(Road.nameOf(currentRoad), AID.ISLOCALNAME));
			lastRoadMsg.setOntology(CityMessageType.ROADS_OCCUPATION.toString());
			lastRoadMsg.setContent(gson.toJson(new RoadOccupyRequest(carName, currentRoad, null)));
			send(lastRoadMsg);
			log.debug("Sending stats report to city stats agent...");
			ACLMessage statsMsg = new ACLMessage(ACLMessage.INFORM);
			statsMsg.addReceiver(new AID(cityName, AID.ISLOCALNAME));
			statsMsg.setOntology(CityMessageType.CAR_STATS_REPORT.toString());
			statsMsg.setContent(gson.toJson(new TripFinishReport(carName, spentTime)));
			send(statsMsg);
			log.debug("Car {} has sent its report.", carName);
			return 0;
		}
		
	}


}	

