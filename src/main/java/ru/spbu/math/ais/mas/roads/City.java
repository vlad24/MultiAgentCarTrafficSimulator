package ru.spbu.math.ais.mas.roads;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import jade.core.Agent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.spbu.math.ais.mas.roads.wrappers.Graph;

public class City extends Agent {
	private static final long serialVersionUID = 1L;
	private Graph cityGraph;

	private static final Logger log = LoggerFactory.getLogger(City.class);
	
	@Override
	protected void setup() {
		log.info("{} is ready. Got args:{}", getLocalName(), getArguments());
		String carsFilename  = String.valueOf(getArguments()[0]);
		String roadsFilename = String.valueOf(getArguments()[1]);
		Path filePath = Paths.get("src", "main", "resources", roadsFilename);
		cityGraph = new Graph(filePath.toFile());
		log.info("Got graph: {}", cityGraph);
		log.info("minDistances {}", cityGraph.getMinDistances(1));
	}

}