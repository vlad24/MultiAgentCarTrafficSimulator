package ru.spbu.math.ais.mas.citycars.wrappers.communication;

import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import ru.spbu.math.ais.mas.citycars.wrappers.Pair;


@AllArgsConstructor
@EqualsAndHashCode
@Getter
@ToString
public class CarMoveResponse implements CityCommunicationUnit{
	
	String carName;
	boolean permitted;
	
	@Override
	public CityMessageSubject getSubject() {
		return CityMessageSubject.CAR_MOVE_REQUEST;
	}

}
