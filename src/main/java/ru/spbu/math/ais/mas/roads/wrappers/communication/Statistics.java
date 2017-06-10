package ru.spbu.math.ais.mas.roads.wrappers.communication;

public class Statistics {
	private int count;
	private int sum;
	private int max;
	
	public Statistics() {
		count = 0;
		sum = 0;
		max = Integer.MIN_VALUE;
	}

	@Override
	public String toString() {
		return "Statistics [count=" + count + ", sum=" + sum + ", max=" + max + ", avg=" + getAvg() + "]";
	}

	public int getCount() {
		return count;
	}

	public void increaseCount() {
		this.count++;
	}

	public int getSum() {
		return sum;
	}

	public void increaseSum(int delta) {
		this.sum += delta;
	}

	public float getAvg() {
		return (float) sum / count;
	}

	public int getMax() {
		return max;
	}

	public void updateMax(int max) {
		this.max = Math.max(this.max, max);
	}

}
