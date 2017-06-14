package ru.spbu.math.ais.mas.citycars.wrappers.communication;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import ru.spbu.math.ais.mas.citycars.wrappers.Pair;


@AllArgsConstructor
@EqualsAndHashCode
@Getter
@ToString
public class CarMoveRequest implements CityCommunicationUnit{
	
	String carName;
	Long requestTime;
	Pair requestedRoad;
	
	@Override
	public CityMessageSubject getSubject() {
		return CityMessageSubject.CAR_MOVE_REQUEST;
	}

}
