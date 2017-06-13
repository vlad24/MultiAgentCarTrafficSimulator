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
public class TripFinishReport implements CityCommunicationUnit{
	private String carName;
	private int spentTime;
	
	@Override
	public CityMessageSubject getSubject() {
		return CityMessageSubject.CAR_STATS;
	}
}
