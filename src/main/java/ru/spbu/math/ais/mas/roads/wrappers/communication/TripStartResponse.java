package ru.spbu.math.ais.mas.roads.wrappers.communication;

import java.io.Serializable;

@SuppressWarnings("serial")
public class TripStartResponse implements Serializable{
	boolean permitted;


	public TripStartResponse(boolean ipermitted) {
		super();
		this.permitted = ipermitted;
	}


	public boolean isPermitted() {
		return permitted;
	}


	public void setPermitted(boolean permitted) {
		this.permitted = permitted;
	}	
}
