package ru.spbu.math.ais.mas.roads;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jade.core.Agent;
import jade.wrapper.AgentController;
import jade.wrapper.ControllerException;
import jade.wrapper.PlatformController;
import ru.spbu.math.ais.mas.roads.wrappers.Graph;

public class City extends Agent {
	private static final long serialVersionUID = 1L;
	private static final Logger log = LoggerFactory.getLogger(City.class);
	
	private Parser entityParser;
	private Graph cityGraph;
	
	@Override
	protected void setup() {
		log.debug("{} is ready. Got args:{}", getLocalName(), getArguments());
		Path carsFilePath  = Paths.get("src", "main", "resources", String.valueOf(getArguments()[0]));
		Path roadsFilePath = Paths.get("src", "main", "resources", String.valueOf(getArguments()[1]));
		entityParser = new Parser();
		setupRoads(roadsFilePath.toFile());
		setupCars(carsFilePath.toFile());
		log.debug("minDistances {}", cityGraph.getMinDistances(1));
	}
	
	private void setupRoads(File fileWithRoads) {
		cityGraph = entityParser.parseGraph(fileWithRoads);
		log.debug("Got graph: {}", cityGraph);
	}
	
	private void setupCars(File fileWithCars) {
		PlatformController container = getContainerController();
		for (ArrayList<String> carParts: this.entityParser.parseCarParts(fileWithCars)) {
			String carName = carParts.get(0);
			String carSrc  = carParts.get(1);
			String carDst  = carParts.get(2);
			try {
				AgentController carController = container.createNewAgent(carName, "ru.spbu.math.ais.mas.roads.Car", 
						new Object[] {carSrc, carDst});
				carController.start();
			} catch (ControllerException e) {
				log.error("Error: {}", e);
			}   
		}
		
	}

}