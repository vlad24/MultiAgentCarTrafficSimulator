package ru.spbu.math.ais.mas.roads;

import jade.core.Agent;
import jade.wrapper.AgentController;
import jade.wrapper.ControllerException;
import jade.wrapper.PlatformController;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.spbu.math.ais.mas.roads.car.Car;
import ru.spbu.math.ais.mas.roads.car.DrivingStrategy;
import ru.spbu.math.ais.mas.roads.city.City;
import ru.spbu.math.ais.mas.roads.wrappers.Graph;

@SuppressWarnings("serial")
public class Configurator extends Agent{
	
	private static final String dataFolder = Paths.get("src", "main", "resources", "data").toAbsolutePath().toString();
	
	public static final String CITY_GRAPH_KEY           = "cityGraph";
	public static final String CITY_NAME_KEY            = "cityName";
	public static final String CITY_WORKLOAD_KEY        = "workload";
	public static final String CAR_ITEMS_KEY            = "carItems";
	public static final String CAR_DRIVING_STRATEGY_KEY = "strategy";
	public static final String CAR_REFRESH_KEY          = "refresh";
	
	
	private static final Logger log = LoggerFactory.getLogger(Configurator.class);
	private Parser parser;
	private PlatformController container;
	private String cityName;

	@Override
	protected void setup() {
		Path carsFilePath  = Paths.get(dataFolder, String.valueOf(getArguments()[0]));
		Path roadsFilePath = Paths.get(dataFolder, String.valueOf(getArguments()[1]));
		parser = new Parser();
		container = getContainerController();
		setupCity(roadsFilePath.toFile());
		setupCars(carsFilePath.toFile());
	}

	@SuppressWarnings("unchecked")
	private void setupCars(File fileWithCars) {
		if (cityName == null) {
			throw new IllegalStateException("Creating car without creating city");
		}
		Map<String, Object> carConfig = this.parser.parseCarFile(fileWithCars);
		DrivingStrategy strategy = DrivingStrategy.valueOf(
					carConfig.getOrDefault(CAR_DRIVING_STRATEGY_KEY, DrivingStrategy.DUMMY).toString().toUpperCase()
				);
		Map<String, Object> strategyParams = new HashMap<String, Object>();
		
		if (strategy.equals(DrivingStrategy.ITERATIVE)) {
			strategyParams.put(CAR_REFRESH_KEY, carConfig.get(CAR_REFRESH_KEY));
		}
		
		for (ArrayList<String> carParts: (ArrayList<ArrayList<String>>)carConfig.get(CAR_ITEMS_KEY)) {
			String carName = carParts.get(0);
			String carSrc  = carParts.get(1);
			String carDst  = carParts.get(2);
			Object[] args = new Object[] {cityName, carSrc, carDst, strategy, strategyParams};
			try {
				AgentController carController = container.createNewAgent(carName, Car.class.getCanonicalName(), args);
				carController.start();
			} catch (ControllerException e) {
				log.error("Error while creating car: {}", e);
			}   
		}
	}
	
	private void setupCity(File fileWithRoads) {
		Map<String, Object> parseResults = parser.parseGraphFile(fileWithRoads);
		cityName = (String) parseResults.get(CITY_NAME_KEY);
		Graph cityGraph = (Graph) parseResults.get(CITY_GRAPH_KEY);
		int workloadDelta = Integer.parseInt(parseResults.get(CITY_WORKLOAD_KEY).toString());
		try {
			AgentController cityController = container.createNewAgent(
					cityName,
					City.class.getCanonicalName(),
					new Object[] {cityGraph, workloadDelta}
			);
			cityController.start();
		} catch (ControllerException e) {
			log.error("Error while creating city: {}", e);
		}
	}
	
}
