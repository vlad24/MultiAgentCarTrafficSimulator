package ru.spbu.math.ais.mas.roads.wrappers.communication;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

public class RoadsUpdateResonse implements Serializable{
	private int newRoadWorkload;

	public RoadsUpdateResonse(int newRoadWorload) {
		super();
		this.newRoadWorkload = newRoadWorload;
	}

	public int getNewRoadWorkload() {
		return newRoadWorkload;
	}

	public void setNewRoadWorkload(int newRoadWorload) {
		this.newRoadWorkload = newRoadWorload;
	}
}
