package ru.spbu.math.ais.mas.citycars.wrappers.communication;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import ru.spbu.math.ais.mas.citycars.wrappers.Pair;

@SuppressWarnings("serial")
@AllArgsConstructor
@EqualsAndHashCode
@ToString
@Getter
public class RoadOccupyRequest implements CityCommunicationUnit{
	private String carName;
	private Pair roadLeft;
	private Pair roadWished;
	
	@Override
	public CityMessageSubject getSubject() {
		return CityMessageSubject.ROADS_OCCUPATION;
	}
}
