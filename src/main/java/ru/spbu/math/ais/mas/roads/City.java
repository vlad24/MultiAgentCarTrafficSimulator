package ru.spbu.math.ais.mas.roads;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jade.core.Agent;
import jade.wrapper.AgentController;
import jade.wrapper.ControllerException;
import jade.wrapper.PlatformController;
import ru.spbu.math.ais.mas.roads.cars.Car;
import ru.spbu.math.ais.mas.roads.cars.DrivingStrategy;
import ru.spbu.math.ais.mas.roads.wrappers.Graph;

public class City extends Agent {
	private static final long serialVersionUID = 1L;
	private static final Logger log = LoggerFactory.getLogger(City.class);
	
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
		log.debug("Min distances {}", cityGraph.getMinDistances(1));
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

}