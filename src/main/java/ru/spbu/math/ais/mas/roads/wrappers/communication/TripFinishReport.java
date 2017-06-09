package ru.spbu.math.ais.mas.roads.wrappers.communication;

import java.io.Serializable;

public class TripFinishReport implements Serializable{
	String carName;
	int src;
	public TripFinishReport(String carName, int src, int dst) {
		super();
		this.carName = carName;
		this.src = src;
		this.dst = dst;
	}
	int dst;
	public String getCarName() {
		return carName;
	}
	public void setCarName(String carName) {
		this.carName = carName;
	}
	public int getSrc() {
		return src;
	}
	public void setSrc(int src) {
		this.src = src;
	}
	public int getDst() {
		return dst;
	}
	public void setDst(int dst) {
		this.dst = dst;
	}

}
