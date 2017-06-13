package ru.spbu.math.ais.mas.citycars.wrappers.communication;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@SuppressWarnings("serial")
@AllArgsConstructor
@EqualsAndHashCode
@ToString
@Getter
public class TripStartRequest implements CityCommunicationUnit{
	private String carName;

	@Override
	public CityMessageSubject getSubject() {
		return CityMessageSubject.CAR_REGISTER;
	}
}
