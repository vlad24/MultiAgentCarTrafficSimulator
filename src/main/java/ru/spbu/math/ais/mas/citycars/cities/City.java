package ru.spbu.math.ais.mas.citycars.cities;

import com.google.gson.Gson;

import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.lang.acl.ACLMessage;
import lombok.extern.slf4j.Slf4j;
import ru.spbu.math.ais.mas.citycars.wrappers.Statistics;
import ru.spbu.math.ais.mas.citycars.wrappers.communication.CityMessageType;
import ru.spbu.math.ais.mas.citycars.wrappers.communication.TripFinishReport;

@Slf4j
public class City extends Agent {
	private static final long serialVersionUID = 1L;

	private Statistics carStats;
	private int activeCarsAmount;


	@Override
	protected void setup() {
		log.info("City {} is ready.", getLocalName(), getArguments());
		carStats = new Statistics();
		activeCarsAmount = 0;
		addBehaviour(new StatisticsCollectionBehaviour(this));
	}

	@SuppressWarnings("serial")
	private class StatisticsCollectionBehaviour extends Behaviour{
		private Gson gson;
		private boolean carsDetected;

		public StatisticsCollectionBehaviour(City city) {
			super(city);
			gson = new Gson();
			carsDetected = false;
		}

		@Override
		public void action() {
			ACLMessage message = myAgent.receive();
			if (message != null) {
				CityMessageType messageType = CityMessageType.valueOf(message.getOntology());
				switch (messageType) {
				case CAR_REGISTER:
					log.debug("Received registering message");
					carsDetected = true;
					carStats.increaseCount();
					activeCarsAmount++;
					break;
				case CAR_STATS_REPORT:
					TripFinishReport request = gson.fromJson(message.getContent(), TripFinishReport.class);
					carStats.increaseSum(request.getSpentTime());
					carStats.updateMax(request.getSpentTime());
					activeCarsAmount--;
					log.debug("Got report. Current active cars:{}", activeCarsAmount);
					break;
				default:
					log.warn("Strange message got: {}", message.getContent());
					break;
				}
			}else {
				block();
			}
		}

		@Override
		public boolean done() {
			return carsDetected && activeCarsAmount == 0;
		}

		@Override
		public int onEnd() {
			log.info("City is destroyed");
			if (activeCarsAmount != 0){
				log.error("City still has driving cars =(");
				return 1;
			}else{
				log.info("City agent cannot see any active cars. Driving finished successfully. Car stats:\n{}", carStats);
				return 0;
			}
		}
		
	}

}