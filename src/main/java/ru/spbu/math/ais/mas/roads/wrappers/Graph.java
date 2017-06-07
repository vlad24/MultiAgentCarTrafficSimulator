package ru.spbu.math.ais.mas.roads.wrappers;

import java.io.File;
import java.util.ArrayList;

/**
 * @author vlad
 * Graph implementation.
 * Graph stores all the data in adjacency matrix form. For simplicity
 */
public class Graph {

	private ArrayList<ArrayList<Integer>> adjMatrix;
	
	public Graph(ArrayList<ArrayList<Integer>> matrix) {
		adjMatrix = matrix;
	}
	
	public Graph(File file) {
		//TODO implement
	}
}
