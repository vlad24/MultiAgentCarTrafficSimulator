package ru.spbu.math.ais.mas.citycars;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jade.core.Agent;
import jade.wrapper.AgentController;
import jade.wrapper.ControllerException;
import jade.wrapper.PlatformController;
import lombok.extern.slf4j.Slf4j;
import ru.spbu.math.ais.mas.citycars.cars.Car;
import ru.spbu.math.ais.mas.citycars.cars.DrivingStrategy;
import ru.spbu.math.ais.mas.citycars.cities.City;
import ru.spbu.math.ais.mas.citycars.roads.Road;
import ru.spbu.math.ais.mas.citycars.wrappers.Graph;
import ru.spbu.math.ais.mas.citycars.wrappers.Pair;

/**
 * @author vlad
 *
 */
@SuppressWarnings("serial")
@Slf4j
public class Configurator extends Agent{

	private static final String dataFolder = Paths.get("src", "main", "resources", "data").toString();

	public static final String CITY_GRAPH_KEY           = "cityGraph";
	public static final String CITY_NAME_KEY            = "cityName";
	public static final String CITY_WORKLOAD_KEY        = "workload";
	public static final String CAR_ITEMS_KEY            = "carItems";
	public static final String CAR_DRIVING_STRATEGY_KEY = "strategy";
	public static final String CAR_REFRESH_KEY          = "refresh";

	private Parser parser;
	private PlatformController container;

	private String cityName;
	private Graph cityGraph;
	private int workloadDelta;

	@Override
	protected void setup() {
		Path carsFilePath  = Paths.get(dataFolder, String.valueOf(getArguments()[0]));
		Path roadsFilePath = Paths.get(dataFolder, String.valueOf(getArguments()[1]));
		log.info("Configuring app with configs \n for cars : {} \n for city: {}", carsFilePath.toString(), roadsFilePath.toString());
		parser = new Parser();
		container = getContainerController();
		startRoadsAgents(roadsFilePath.toFile());
		startCityAgent(cityName);
		startCarAgents(carsFilePath.toFile());
	}

	private List<AgentController> startRoadsAgents(File fileWithRoads) {
		Map<String, Object> parseResults = parser.parseGraphFile(fileWithRoads);
		cityName = (String) parseResults.get(CITY_NAME_KEY);
		cityGraph  = (Graph) parseResults.get(CITY_GRAPH_KEY);
		workloadDelta = Integer.parseInt(parseResults.get(CITY_WORKLOAD_KEY).toString());
		log.info("Configuring roads {} with {} vertices and {} edges. Workload delta={}",
				cityName, cityGraph.getVerticesAmount(), cityGraph.getEdgesAmount(), workloadDelta);
		List<AgentController> result = new ArrayList<>();
		for (Pair edge : cityGraph.getEdges()) {
			try {
				String roadName = Road.nameOf(edge);
				AgentController roadController = container.createNewAgent(
						roadName,
						Road.class.getCanonicalName(),
						new Object[] {cityName, edge, cityGraph.getEdgeLength(edge), workloadDelta}
						);
				result.add(roadController);
				roadController.start();
			} catch (ControllerException e) {
				log.error("Error while creating road agent: {}", e);
			}
		}
		return result;
	}

	private void startCityAgent(String cityName) {
		try {
			AgentController cityController = container.createNewAgent(cityName, City.class.getCanonicalName(), null);
			cityController.start();
		} catch (ControllerException e) {
			log.error("Error while creating city agent: {}", e);
		}
	}

	@SuppressWarnings("unchecked")
	private List<AgentController> startCarAgents(File fileWithCars) {
		if (cityGraph == null) {
			throw new IllegalStateException("Creating car without creating city");
		}
		Map<String, Object> carConfig = this.parser.parseCarFile(fileWithCars);
		log.debug("Parsed car file:{}", carConfig);
		DrivingStrategy strategy = DrivingStrategy.valueOf(
					carConfig.getOrDefault(CAR_DRIVING_STRATEGY_KEY, DrivingStrategy.DUMMY).toString().toUpperCase()
				);
		Map<String, Object> strategyParams = new HashMap<String, Object>();
		if (strategy.equals(DrivingStrategy.ITERATIVE)) {
			strategyParams.put(CAR_REFRESH_KEY, carConfig.get(CAR_REFRESH_KEY));
		}
		ArrayList<ArrayList<String>> carItems = (ArrayList<ArrayList<String>>)carConfig.get(CAR_ITEMS_KEY);
		log.info("Configuring {} cars that drive {} with params {}", carItems.size(), strategy, strategyParams);
		List<AgentController> result = new ArrayList<AgentController>(carItems.size());
		for (ArrayList<String> carParts: carItems) {
			String carName = carParts.get(0);
			String carSrc  = carParts.get(1);
			String carDst  = carParts.get(2);
			Object[] args = new Object[] {cityName, cityGraph, carSrc, carDst, strategy, strategyParams};
			try {
				AgentController carController = container.createNewAgent(carName, Car.class.getCanonicalName(), args);
				result.add(carController);
				carController.start();
			} catch (ControllerException e) {
				log.error("Error while creating car: {}", e);
			}
		}
		return result;
	}

}
