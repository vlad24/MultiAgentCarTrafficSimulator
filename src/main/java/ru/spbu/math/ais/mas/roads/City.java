package ru.spbu.math.ais.mas.roads;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.wrapper.AgentController;
import jade.wrapper.ControllerException;
import jade.wrapper.PlatformController;
import ru.spbu.math.ais.mas.roads.cars.Car;
import ru.spbu.math.ais.mas.roads.cars.DrivingStrategy;
import ru.spbu.math.ais.mas.roads.communication.ShortestWayResponse;
import ru.spbu.math.ais.mas.roads.wrappers.Graph;

public class City extends Agent {
	private static final long serialVersionUID = 1L;
	private static final Logger log = LoggerFactory.getLogger(City.class);
	
	public static final String SHORTEST_WAY_CONVERSATION = "shortestWayConversation";
	public static final String ROAD_UPDATE_CONVERSATION   = "roadUpdateConversation";
	
	private Parser entityParser;
	private Graph cityGraph;
	
	@Override
	protected void setup() {
		log.info("{} is ready. Got args:{}", getLocalName(), getArguments());
		Path carsFilePath  = Paths.get("src", "main", "resources", String.valueOf(getArguments()[0]));
		Path roadsFilePath = Paths.get("src", "main", "resources", String.valueOf(getArguments()[1]));
		entityParser = new Parser();
		setupRoads(roadsFilePath.toFile());
		setupCars(carsFilePath.toFile());
		addBehaviour(new MonitoringBehaviour(this));
	}
	
	private void setupRoads(File fileWithRoads) {
		cityGraph = entityParser.parseGraph(fileWithRoads);
		log.debug("Got graph: {}", cityGraph);
	}
	
	private void setupCars(File fileWithCars) {
		PlatformController container = getContainerController();
		Map<String, Object> carConfig = this.entityParser.parseCarFile(fileWithCars);
		for (ArrayList<String> carParts: (ArrayList<ArrayList<String>>)carConfig.get(Parser.CAR_ITEMS_KEY)) {
			String carName = carParts.get(0);
			String carSrc  = carParts.get(1);
			String carDst  = carParts.get(2);
			try {
				AgentController carController = container.createNewAgent(carName, 
						Car.class.getCanonicalName(), 
						new Object[] {
							getLocalName(),
							carSrc,
							carDst,
							carConfig.getOrDefault(Parser.CAR_DRIVING_STRATEGY_KEY, DrivingStrategy.DUMMY) 
						}
				);
				carController.start();
			} catch (ControllerException e) {
				log.error("Error while creating agent: {}", e);
			}   
		}
	}
	
	
	@SuppressWarnings("serial")
	private class MonitoringBehaviour extends CyclicBehaviour{
		public MonitoringBehaviour(City city) {
			super(city);
		}

		@Override
		public void action() {
			try {
				ACLMessage message = myAgent.blockingReceive();
				if (SHORTEST_WAY_CONVERSATION.equalsIgnoreCase(message.getConversationId())){
					//FIXME
					ShortestWayResponse response = new ShortestWayResponse(null);
					ACLMessage reply = message.createReply();
					reply.setContentObject(response);
					send(reply);
				}else if(ROAD_UPDATE_CONVERSATION.equalsIgnoreCase(message.getConversationId())){
					//TODO add road update
				}
			} catch (Exception e) {
				log.error("Error in big city life: {}", e);
			}
		}

	}

}