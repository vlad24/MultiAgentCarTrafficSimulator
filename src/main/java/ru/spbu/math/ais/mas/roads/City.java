package ru.spbu.math.ais.mas.roads;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jade.core.Agent;

public class City extends Agent {
	private static final long serialVersionUID = 1L;
	

	private static final Logger log = LoggerFactory.getLogger(City.class);
	
	@Override
	protected void setup() {
		log.info("{} is ready. Got args:{}", getLocalName(), getArguments());
	}

}