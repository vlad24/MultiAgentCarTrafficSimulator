package ru.spbu.math.ais.mas.roads.wrappers.communication;

import java.io.Serializable;

public class TripFinishReport implements Serializable{
	private String carName;
	private Pair stopRoad;
	private int spentTime;

	public TripFinishReport(String carName, int spentTime, Pair stopRoad) {
		super();
		this.carName = carName;
		this.spentTime = spentTime;
		this.stopRoad = stopRoad;
	}
	
	public Pair getStopRoad() {
		return stopRoad;
	}

	public void setStopRoad(Pair stopRoad) {
		this.stopRoad = stopRoad;
	}

	public String getCarName() {
		return carName;
	}
	public void setCarName(String carName) {
		this.carName = carName;
	}
	public int getSpentTime() {
		return spentTime;
	}
	public void setSpentTime(int spentTime) {
		this.spentTime = spentTime;
	}
}
