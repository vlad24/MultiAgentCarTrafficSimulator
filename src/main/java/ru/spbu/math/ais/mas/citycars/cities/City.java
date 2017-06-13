package ru.spbu.math.ais.mas.citycars.cities;

import com.google.gson.Gson;

import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.lang.acl.ACLMessage;
import lombok.extern.slf4j.Slf4j;
import ru.spbu.math.ais.mas.citycars.wrappers.Statistics;
import ru.spbu.math.ais.mas.citycars.wrappers.communication.CityCommunicationUnit;
import ru.spbu.math.ais.mas.citycars.wrappers.communication.TripFinishReport;

@Slf4j
public class City extends Agent {
	private static final long serialVersionUID = 1L;

	private Statistics carStats;
	private int activeCarsAmount;

	@Override
	protected void setup() {
		log.info("{} is ready. Got args:{}", getLocalName(), getArguments());
		carStats = new Statistics();
		activeCarsAmount = 0;
		addBehaviour(new StatisticsCollectionBehaviour(this));
	}

	@SuppressWarnings("serial")
	private class StatisticsCollectionBehaviour extends Behaviour{
		private Gson gson;

		public StatisticsCollectionBehaviour(City city) {
			super(city);
			gson = new Gson();
		}

		@Override
		public void action() {
			ACLMessage message = myAgent.receive();
			if (message != null) {
				CityCommunicationUnit unit = gson.fromJson(message.getContent(), CityCommunicationUnit.class);
				switch (unit.getSubject()) {
				case CAR_REGISTER:
					carStats.increaseCount();	
					activeCarsAmount++;
					break;
				case CAR_STATS:
					TripFinishReport request = gson.fromJson(message.getContent(), TripFinishReport.class);
					carStats.increaseSum(request.getSpentTime());
					carStats.updateMax(request.getSpentTime());
					activeCarsAmount--;
					break;
				default:
					break;
				}
			}else {
				block();
			}
		}

		@Override
		public boolean done() {
			return activeCarsAmount == 0;
		}

		@Override
		public int onEnd() {
			log.info("City is destroyed");
			if (activeCarsAmount != 0){
				log.error("City has driving cars =(");
				return 1;
			}else{
				log.info("City has finished successfully. Car stats:\n{}", carStats);
				return 0;
			}
		}

	}

}