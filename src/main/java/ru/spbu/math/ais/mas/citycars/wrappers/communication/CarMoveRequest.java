package ru.spbu.math.ais.mas.citycars.wrappers.communication;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import ru.spbu.math.ais.mas.citycars.wrappers.Pair;


@AllArgsConstructor
@EqualsAndHashCode
@ToString
@Getter
public class CarMoveRequest{
	
	String carName;
	Long requestTime;
	Pair requestedRoad;
	
}
