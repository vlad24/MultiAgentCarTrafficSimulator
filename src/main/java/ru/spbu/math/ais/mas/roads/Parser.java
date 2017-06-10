package ru.spbu.math.ais.mas.roads;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jade.lang.acl.ParseException;
import ru.spbu.math.ais.mas.roads.wrappers.Graph;

public class Parser {

	private static final Logger log = LoggerFactory.getLogger(Parser.class);

	private static final String COMMENT_INDICATOR = "#";
	private static final String CONFIG_INDICATOR  = "@";
	private static final String INNER_SEPARATOR   = "\t";
	private static final String PROP_SEPEARATOR   = "=";

	public Map<String, Object> parseGraphFile(File file) {
		Map<String, Object> result = new HashMap<String, Object>();
		ArrayList<ArrayList<Integer>> adjMatrix = new ArrayList<ArrayList<Integer>>();
		try(BufferedReader reader = new BufferedReader(new FileReader(file))){
			String currentLine;
			while ((currentLine = reader.readLine()) != null) {
				if (currentLine.startsWith(CONFIG_INDICATOR)) {
					String[] configKV = currentLine.substring(1).trim().replace(" ", "").split(PROP_SEPEARATOR);
					result.put(configKV[0], configKV[1]);
				}else if (!currentLine.startsWith(COMMENT_INDICATOR) && !currentLine.isEmpty()){
					String[] elements =  currentLine.split(INNER_SEPARATOR);
					ArrayList<Integer> matrixLine = new ArrayList<Integer>();
					for (String el : elements) {
						matrixLine.add(Integer.parseInt(el));
					}
					adjMatrix.add(matrixLine);
				}
			}
			result.put(Configurator.CITY_GRAPH_KEY, new Graph(adjMatrix));
		}catch (IOException readerExceprion) {
			readerExceprion.printStackTrace();
		}catch (NumberFormatException wrongNumberException) {
			log.error("Wrong number {}", wrongNumberException);
		}
		return result;
	}

	public Map<String, Object> parseCarFile(File file) {
		Map<String, Object> result = new HashMap<String, Object>();
		ArrayList<ArrayList<String>> carParts = new ArrayList<ArrayList<String>>();
		try(BufferedReader reader = new BufferedReader(new FileReader(file))){
			String currentLine;
			while ((currentLine = reader.readLine()) != null) {
				if (currentLine.startsWith(CONFIG_INDICATOR)) {
					String[] configKV = currentLine.substring(1).trim().replace(" ", "").split(PROP_SEPEARATOR);
					result.put(configKV[0], configKV[1]);
				}else if (!currentLine.startsWith(COMMENT_INDICATOR) && !currentLine.isEmpty()){
					String[] elements =  currentLine.split(INNER_SEPARATOR);
					if (elements.length != 3) {
						throw new ParseException("Incorrect number of car args at line");
					}
					carParts.add(new ArrayList<String>(Arrays.asList(elements)));
				}
			}
			result.put(Configurator.CAR_ITEMS_KEY, carParts);
		}
		catch (ParseException pexc) {
			log.error("Error while parsing cars: {}", pexc);
		}
		catch (IOException exc) {
			log.error("Error with cars file: {} ({})", file.getAbsolutePath(), exc);
		}
		return result;
	}
}
