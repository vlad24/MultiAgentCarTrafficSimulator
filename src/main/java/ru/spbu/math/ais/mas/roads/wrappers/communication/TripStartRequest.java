package ru.spbu.math.ais.mas.roads.wrappers.communication;

import java.io.Serializable;

@SuppressWarnings("serial")
public class TripStartRequest implements Serializable{
	String carName;
	public TripStartRequest(String carName) {
		super();
		this.carName = carName;
	}
	public String getCarName() {
		return carName;
	}
	public void setCarName(String carName) {
		this.carName = carName;
	}

}
