package ru.spbu.math.ais.mas.citycars.wrappers.communication;

import java.util.Date;

import ru.spbu.math.ais.mas.citycars.wrappers.Pair;


//TODO lombok
public class CarCheckRequest implements CityCommunicationUnit{
	
	String carId;
	Date requestTime;
	Pair requestedRoad;
	
	@Override
	public CityMessageSubject getSubject() {
		return CityMessageSubject.CAR_CHECK;
	}

}
