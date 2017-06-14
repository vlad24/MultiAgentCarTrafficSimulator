package ru.spbu.math.ais.mas.citycars.wrappers.communication;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import ru.spbu.math.ais.mas.citycars.wrappers.Pair;

@SuppressWarnings("serial")
@AllArgsConstructor
@EqualsAndHashCode
@ToString(doNotUseGetters=false)
@Getter
public class RoadOccupyPermission implements CityCommunicationUnit{
	private Pair road;
	private boolean permitted;
	private int newRoadWorkload;
	
	@Override
	public CityMessageSubject getSubject() {
		return CityMessageSubject.ROADS_OCCUPATION;
	}
}
