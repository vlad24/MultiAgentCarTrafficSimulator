package ru.spbu.math.ais.mas.roads.wrappers;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author vlad and polina
 * Graph implementation.
 * Graph stores all the data in adjacency matrix form. For simplicity
 */
@SuppressWarnings("serial")
public class Graph implements Serializable{
	
	public static final String DISTANCE_KEY = "distance_to_destination";
	public static final String ALL_DISTANCES_KEY = "all_distances";
	public static final String PATH_KEY = "path";
	
	private static final Logger log = LoggerFactory.getLogger(Graph.class);
	private int countRoads;

	private ArrayList<ArrayList<Integer>> adjMatrix;
	
	public Graph(ArrayList<ArrayList<Integer>> matrix) {
		adjMatrix = matrix;
		countRoads = adjMatrix.size();
		log.trace("Graph has been constructed");
	}
	
	@Override
	public String toString() {
		return "Graph [adjMatrix=" + adjMatrix + "]";
	}
	public Map<String, Object> getMinDistances(int source, int destination){
		boolean[] visited = new boolean[countRoads];
		int[] ancestor = new int[countRoads];
		int[] distances = new int[countRoads];
		Map<String, Object> distanceInfo = new HashMap<String, Object>();
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
		distanceInfo.put(ALL_DISTANCES_KEY, distances);
		distanceInfo.put(DISTANCE_KEY, distances[destination]);
		distanceInfo.put(PATH_KEY, getPathToDestination(ancestor, source, destination));
		return distanceInfo;
	}
	private Queue<Integer> getPathToDestination(int[] ancestors, int source, int destination){
		Deque<Integer> path = new LinkedList<Integer>();
		int v = ancestors[destination];
		path.addFirst(destination);
		while (v != source){
			path.addFirst(v);
			v = ancestors[v];
		}
		path.addFirst(source);
		return path;
	}
	public int increaseEdgeLength(int i, int j, int delta){
		int currentWorkload = adjMatrix.get(i).get(j);
		adjMatrix.get(i).set(j, currentWorkload + delta);
		adjMatrix.get(j).set(i, currentWorkload + delta);
		return adjMatrix.get(i).get(j);
	}
	public int decreaseEdgeLength(int i, int j, int delta){
		int currentWorkload = adjMatrix.get(i).get(j);
		adjMatrix.get(i).set(j, currentWorkload - delta);
		adjMatrix.get(j).set(i, currentWorkload - delta);
		return adjMatrix.get(i).get(j);
	}
	private Integer getEdgeLength(int i, int j) {
		return adjMatrix.get(i).get(j);
	}

	private boolean areConnected(int i, int j) {
		return getEdgeLength(i, j) != 0;
	}
	
}
