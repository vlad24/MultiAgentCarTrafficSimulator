package ru.spbu.math.ais.mas.citycars.cars;

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
import ru.spbu.math.ais.mas.citycars.wrappers.communication.CityCommunicationUnit;
import ru.spbu.math.ais.mas.citycars.wrappers.communication.RoadStatusChange;
import ru.spbu.math.ais.mas.citycars.wrappers.communication.RoadsOccupyPermission;
import ru.spbu.math.ais.mas.citycars.wrappers.communication.RoadsUpdateRequest;
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
		String cityName  = String.valueOf(getArguments()[0]);
		Graph cityGraph  = (Graph)(getArguments()[1]);
		int source       = Integer.parseInt(getArguments()[2].toString());
		int destination  = Integer.parseInt(getArguments()[3].toString());
		strategy         = DrivingStrategy.valueOf(getArguments()[4].toString());
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
		private int lastReachedVertex;
		private int spentTime;
		private int roadsPassed;
		private Queue<RoadStatusChange> roadChanges;
		private Queue<Integer> currentOptimalRoute;
		private int refreshCount; 
		private Gson gson;


		public BasicDrivingBehaviour(Car car, String cityName, Graph cityGraph, int src, int dst, int refresh) {
			super(car);
			this.cityName = cityName;
			this.cityGraph = cityGraph;
			this.source = src;
			this.destination = dst;
			this.lastReachedVertex = src;
			this.refreshCount = refresh;
			this.spentTime = 0;
			this.roadsPassed = 0;
			this.gson = new Gson();
		}

		@Override
		public void onStart() {
			super.onStart();
			log.debug("Registering in city...");
			ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
			msg.addReceiver(new AID(cityName, AID.ISLOCALNAME));
			msg.setContent(gson.toJson(new TripStartRequest(carName)));
		}

		@SuppressWarnings("unchecked")
		@Override
		public void action() {
			try {
				if (roadsPassed % refreshCount == 0) {
					log.info("Building optimal route...");
					for (RoadStatusChange change: this.roadChanges) {
						log.debug("Applying changes to city graph {}", change);
						cityGraph.changeEdgeLength(change.getRoad().getFirst(), change.getRoad().getSecond(), change.getDelta());
					}
					currentOptimalRoute = (Queue<Integer>) cityGraph.getMinDistances(source, destination).get(Graph.PATH_KEY);
					log.debug("Optimal route built: {}", currentOptimalRoute);
				}
				int nextVertex = currentOptimalRoute.element();
				Pair desiredRoad = new Pair(lastReachedVertex, nextVertex);
				ACLMessage outMessage = new ACLMessage(ACLMessage.REQUEST);
				outMessage.addReceiver(new AID(Road.nameOf(desiredRoad), AID.ISLOCALNAME));
				outMessage.setContent(gson.toJson(new RoadsUpdateRequest(currentRoad, desiredRoad)));
				ACLMessage reply = receive();
				if (reply != null) {
					CityCommunicationUnit unit = gson.fromJson(reply.getContent(), CityCommunicationUnit.class);
					switch (unit.getSubject()) {
					case ROADS_OCCUPATION:
						RoadsOccupyPermission roadOccPermission = gson.fromJson(reply.getContent(), RoadsOccupyPermission.class);
						if (roadOccPermission.isPermitted() && roadOccPermission.getRoad().equals(desiredRoad)) {
							currentRoad = roadOccPermission.getRoad();
							// If the car does not sleep the next road will not let the car occupy it
							TimeUnit.SECONDS.sleep(roadOccPermission.getNewRoadWorkload());
							spentTime  += roadOccPermission.getNewRoadWorkload();
							log.debug("Road {} passed!", nextVertex);
							roadsPassed++;
							lastReachedVertex = currentOptimalRoute.remove();
						}
						break;
					case ROAD_STATUS_CHANGE:
						RoadStatusChange roadChange = gson.fromJson(reply.getContent(), RoadStatusChange.class);
						log.debug("Road {} has changed its workload. Memorized...", roadChange.getRoad());
						roadChanges.add(roadChange);
						break;
					default:
						log.debug("Strange message got: {}. Ignoring...", reply.getContent());
						break;
					}
				} else {
					log.debug("Blocking and waiting for some message ...");
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
			log.debug("Cleaning up!");
			//release last road
			ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
			msg.addReceiver(new AID(cityName, AID.ISLOCALNAME));
			ACLMessage outMessage = new ACLMessage(ACLMessage.REQUEST);
			outMessage.addReceiver(new AID(Road.nameOf(currentRoad), AID.ISLOCALNAME));
			outMessage.setContent(gson.toJson(new RoadsUpdateRequest(currentRoad, null)));
			//report stats
			msg.setContent(gson.toJson(new TripFinishReport(carName, spentTime)));
			log.trace("Car {} has sent its report.", carName);
			myAgent.doDelete();
			log.debug("Car {} is shut down.", carName);
			return 0;
		}
		
	}


}	

