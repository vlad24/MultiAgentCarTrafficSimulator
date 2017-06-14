package ru.spbu.math.ais.mas.citycars.wrappers;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@ToString
@EqualsAndHashCode
@Getter
public class Pair{
	private int first;
	private int second;
	
	public Pair(int first, int second) {
		super();
		this.first  = Math.min(first, second);
		this.second = Math.max(first, second);
	}
	
}
