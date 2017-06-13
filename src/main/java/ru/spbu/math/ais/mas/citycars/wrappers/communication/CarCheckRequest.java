package ru.spbu.math.ais.mas.citycars.wrappers.communication;

public class CarCheckRequest implements CityCommunicationUnit{

	@Override
	public CityMessageSubject getSubject() {
		return CityMessageSubject.CAR_CHECK;
	}

}
