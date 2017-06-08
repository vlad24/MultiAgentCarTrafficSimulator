package ru.spbu.math.ais.mas.roads.wrappers;

import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author vlad and polina
 * Graph implementation.
 * Graph stores all the data in adjacency matrix form. For simplicity
 */
public class Graph {
	
	private static final Logger log = LoggerFactory.getLogger(Graph.class);
	private int countRoads;

	private ArrayList<ArrayList<Integer>> adjMatrix;
	
	public Graph(ArrayList<ArrayList<Integer>> matrix) {
		adjMatrix = matrix;
		countRoads = adjMatrix.size();
	}
	
	@Override
	public String toString() {
		return "Graph [adjMatrix=" + adjMatrix + "]";
	}
	public int[] getMinDistances(int source, int destination){
		boolean[] visited = new boolean[countRoads];
		int[] ancestor = new int[countRoads];
		int[] distances = new int[countRoads];
		for (int i = 0; i < countRoads; i++) {
			visited[i] = false;
			distances[i] = Integer.MAX_VALUE;
		}
		visited[source] = true;
		distances[source] = 0;
		int minVertex = source;
		int countVisited = 1;
		while (countVisited != countRoads){
			visited[minVertex] = true;
			int minDistance = Integer.MAX_VALUE;
			int index = -1;
			for (int i = 0; i < countRoads; i++) {
				if (areConnected(minVertex, i) && !visited[i] ){
					int newDistance = distances[minVertex] + getEdgeLength(minVertex, i);
					if (distances[i] > newDistance){
						distances[i] = newDistance;
						ancestor[i] = minVertex;						
					}
				}					
				if (!visited[i] && minDistance > distances[i]){
					minDistance = distances[i];
					index = i;
				}
			}
			countVisited++;
			minVertex = index;
		}
		ArrayList<Integer> path = getPathToDestination(ancestor, source, destination);
		log.debug("Path to destination: {}", path );
		return distances;
	}
	private ArrayList<Integer> getPathToDestination(int[] ancestors, int source, int destination){
		log.debug("ancestors: {}", ancestors);
		log.debug("Find path from {} to {} ", source, destination);
		ArrayList<Integer> path = new ArrayList<Integer>();
		int v = ancestors[destination];
		path.add(destination);
		while (v != source){
			path.add(v);
			v = ancestors[v];
		}
		path.add(source);
		return path;
	}

	private Integer getEdgeLength(int i, int j) {
		return adjMatrix.get(i).get(j);
	}

	private boolean areConnected(int i, int j) {
		return getEdgeLength(i, j) != 0;
	}
	
}
