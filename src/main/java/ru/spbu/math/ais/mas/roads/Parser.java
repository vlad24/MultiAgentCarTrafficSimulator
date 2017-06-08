package ru.spbu.math.ais.mas.roads;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jade.lang.acl.ParseException;
import ru.spbu.math.ais.mas.roads.wrappers.Graph;

public class Parser {
	private static final Logger log = LoggerFactory.getLogger(Parser.class);

	private static final String SEPARATOR = "\t";
	private static final String COMMENT = "#";

	public Graph parseGraph(File file) {
		ArrayList<ArrayList<Integer>> adjMatrix = new ArrayList<ArrayList<Integer>>();
		try(BufferedReader reader = new BufferedReader(new FileReader(file))){
			String currentLine;
			while ((currentLine = reader.readLine()) != null) {
				if (!currentLine.startsWith(COMMENT) && !currentLine.isEmpty()){
					String[] elements =  currentLine.split(SEPARATOR);
					ArrayList<Integer> matrixLine = new ArrayList<Integer>();
					for (String el : elements) {
						matrixLine.add(Integer.parseInt(el));
					}
					adjMatrix.add(matrixLine);
				}
			}
		}catch (IOException readerExceprion) {
			readerExceprion.printStackTrace();
		}catch (NumberFormatException wrongNumberException) {
			log.error("Wrong number {}", wrongNumberException);
		}
		return new Graph(adjMatrix);
	}
	
	public ArrayList<ArrayList<String>> parseCarParts(File file) {
		ArrayList<ArrayList<String>> carParts = new ArrayList<ArrayList<String>>();
		try(BufferedReader reader = new BufferedReader(new FileReader(file))){
			String currentLine;
			while ((currentLine = reader.readLine()) != null) {
				if (!currentLine.startsWith(COMMENT) && !currentLine.isEmpty()){
					String[] elements =  currentLine.split(SEPARATOR);
					if (elements.length != 3) {
						throw new ParseException("Incorrect number of car args at line");
					}
					carParts.add(new ArrayList<String>(Arrays.asList(elements)));
				}
			}
		}
		catch (ParseException pexc) {
			log.error("Error while parsing cars: {}", pexc);
		}
		catch (IOException exc) {
			log.error("Error with cars file: {} ({})", file.getAbsolutePath(), exc);
		}
		return carParts;
	}
}
