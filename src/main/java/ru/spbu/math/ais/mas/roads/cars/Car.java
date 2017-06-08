package ru.spbu.math.ais.mas.roads.cars;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.SequentialBehaviour;
import jade.lang.acl.ACLMessage;
import ru.spbu.math.ais.mas.roads.communication.ShortestWayRequest;

@SuppressWarnings("serial")
public class Car extends Agent {
	private static final Logger log = LoggerFactory.getLogger(Car.class);
	private static final long serialVersionUID = 1L;

	private String city;
	private int src;
	private int dst;
	private int current;
	private long startTime;
	private long spentTime;
	private DrivingStrategy strategy;

	@Override
	protected void setup() {
		log.info("Car {} inited. Got args: {}", getLocalName(), getArguments());
		city = String.valueOf(getArguments()[0]);
		src  = Integer.parseInt(getArguments()[1].toString());
		dst  = Integer.parseInt(getArguments()[2].toString());
		strategy = DrivingStrategy.valueOf(getArguments()[3].toString());
		switch (strategy) {
		case DUMMY:
			addBehaviour(new DummyDrivingBehaviour(this));
			break;
		case ITERATIVE:
			throw new UnsupportedOperationException();
		case INTELLIGENT:
			throw new UnsupportedOperationException();
		default:
			addBehaviour(new DummyDrivingBehaviour(this));
			break;
		}
	}

	/**
	 * @author vlad, polina
	 *
	 *	Class representing dummy driving behaviour:
	 *	Car figures out its shortest way and keeps sticking to it regardless traffic conditions
	 */
	private class DummyDrivingBehaviour extends SequentialBehaviour{

		//WAY CALCULATION STAGE
		public DummyDrivingBehaviour(Car car) {
			super(car);
			// Figuring out where we should drive
			this.addSubBehaviour(new OneShotBehaviour(car) {
				@Override
				public void action() {
					try {
						log.debug("Car {} is estimating its way.", getLocalName());
						ACLMessage wayRequest = new ACLMessage(ACLMessage.REQUEST);
						wayRequest.addReceiver(new AID(city, AID.ISLOCALNAME));
						wayRequest.setConversationId("WAY ESTIMATION");
						wayRequest.setContentObject(new ShortestWayRequest(src, dst));
						send(wayRequest);
						log.debug("Car {} has asked the city for the shortest way. Waiting...", getLocalName());
						ACLMessage wayResponse = myAgent.blockingReceive();
						assert (wayResponse != null);
						log.debug("Car {} has got a response!", getLocalName());
						startTime = 0;
					} catch (Exception e) {
						log.error("Error while getting initial way {}", e);
					}
				}
			});

			//DRIVING STAGE
			this.addSubBehaviour(new Behaviour(car) {
				@Override
				public void action() {
					//FIXME dummy driving
					log.debug("Car {} is driving. It is now at {} cross", getLocalName(), current);
					if (current == src) {
						current = dst - 100;
					}else {
						current++;
					}
					spentTime += 1;
				}
				@Override
				public boolean done() {
					return current == dst;
				}
			});
		}

		@Override
		public int onEnd() {
			log.debug("Car {} has reached its destination spending {} s.", getLocalName(), spentTime);
			myAgent.doDelete();
			log.debug("Car {} is shut down.", getLocalName());
			return 0;
		}
	}

}
