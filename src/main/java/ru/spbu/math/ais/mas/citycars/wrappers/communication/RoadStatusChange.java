package ru.spbu.math.ais.mas.citycars.wrappers.communication;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import ru.spbu.math.ais.mas.citycars.wrappers.Pair;

@SuppressWarnings("serial")
@AllArgsConstructor
@ToString
@Getter
public class RoadStatusChange implements CityCommunicationUnit{

	private Pair road;
	private int delta;
	
	@Override
	public CityMessageSubject getSubject() {
		return CityMessageSubject.ROADS_OCCUPATION;
	}

}
