package ru.spbu.math.ais.mas.citycars.wrappers.communication;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import ru.spbu.math.ais.mas.citycars.wrappers.Pair;

@AllArgsConstructor
@ToString
@Getter
public class RoadOccupyResponse{
	private Pair road;
	private int newRoadWorkload;
}
