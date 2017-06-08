package ru.spbu.math.ais.mas.roads.wrappers.communication;

import java.io.Serializable;

public class ShortestWayRequest implements Serializable{
	int from;
	int to;
	
	public ShortestWayRequest(int from, int to) {
		super();
		this.from = from;
		this.to = to;
	}
	public int getFrom() {
		return from;
	}
	public void setFrom(int from) {
		this.from = from;
	}
	public int getTo() {
		return to;
	}
	public void setTo(int to) {
		this.to = to;
	}

}
