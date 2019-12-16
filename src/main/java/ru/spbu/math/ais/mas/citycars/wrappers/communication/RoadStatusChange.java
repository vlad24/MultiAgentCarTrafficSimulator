package ru.spbu.math.ais.mas.citycars.wrappers.communication;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import ru.spbu.math.ais.mas.citycars.wrappers.Pair;

@SuppressWarnings("serial")
@AllArgsConstructor
@ToString
@Getter
public class RoadStatusChange{

	private Pair road;
	private int delta;
	
}
