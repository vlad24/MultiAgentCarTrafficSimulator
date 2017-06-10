package ru.spbu.math.ais.mas.roads.wrappers.communication;

import java.io.Serializable;

@SuppressWarnings("serial")
public class RoadsUpdateRequest implements Serializable{
	Pair roadLeft;
	Pair roadOccupied;
	public Pair getRoadLeft() {
		return roadLeft;
	}
	public void setRoadLeft(Pair roadLleft) {
		this.roadLeft = roadLleft;
	}
	public Pair getRoadOccupied() {
		return roadOccupied;
	}
	public void setRoadOccupied(Pair roadOccupied) {
		this.roadOccupied = roadOccupied;
	}
	public RoadsUpdateRequest(Pair roadLleft, Pair roadOccupied) {
		super();
		this.roadLeft = roadLleft;
		this.roadOccupied = roadOccupied;
	}
}
