package ru.spbu.math.ais.mas.citycars.wrappers;

import lombok.EqualsAndHashCode;
import lombok.Getter;

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
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("(");
		builder.append(first);
		builder.append(",");
		builder.append(second);
		builder.append(")");
		return builder.toString();
	}
	
}
