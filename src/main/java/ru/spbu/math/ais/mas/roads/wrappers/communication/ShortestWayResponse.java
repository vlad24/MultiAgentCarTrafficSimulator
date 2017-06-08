package ru.spbu.math.ais.mas.roads.wrappers.communication;

import java.io.Serializable;
import java.util.Map;

@SuppressWarnings("serial")
public class ShortestWayResponse implements Serializable{
	
	private Map<String, Object> wayInfo;

	public ShortestWayResponse(Map<String, Object> wayInfo) {
		super();
		this.wayInfo = wayInfo;
	}

	public Map<String, Object> getWayInfo() {
		return wayInfo;
	}

	public void setWayInfo(Map<String, Object> wayInfo) {
		this.wayInfo = wayInfo;
	}

}
