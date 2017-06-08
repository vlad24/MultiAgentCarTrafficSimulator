package ru.spbu.math.ais.mas.roads;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;

public class Car extends Agent {

	private int src;
	private int dst;

	private static final Logger log = LoggerFactory.getLogger(Car.class);
	private static final long serialVersionUID = 1L;
	
	@SuppressWarnings("serial")
	@Override
	protected void setup() {
		log.info("{} inited. Got args: {}", getLocalName(), getArguments());
		
//		addBehaviour(new CyclicBehaviour() {
//			@Override
//			public void action() {
//				ACLMessage msg = receive();
//				if (msg != null) {
//					if (msg.getConversationId().equals("Initializing")) {
//					} else if (msg.getConversationId().equals("Negotiating")) {
//					}
//				} else
//					block();
//			}
//		});
	}

}
