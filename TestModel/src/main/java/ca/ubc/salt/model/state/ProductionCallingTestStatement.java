package ca.ubc.salt.model.state;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Formatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Set;

import Comparator.NaturalOrderComparator;
import ca.ubc.salt.model.instrumenter.Instrumenter;
import ca.ubc.salt.model.utils.FileUtils;
import ca.ubc.salt.model.utils.Settings;
import ca.ubc.salt.model.utils.Utils;

public class ProductionCallingTestStatement {

	public static void main(String[] args) throws FileNotFoundException {
		// writeStatToFile();
		getCommonStmsForEachTestCase();
	}

	public static void getCommonStmsForEachTestCase() throws FileNotFoundException {

		Map<String, List<String>> results = new HashMap<String, List<String>>();

		Map<LinkedHashSet<String>, List<String>> uniqueTestStatements = getUniqueTestStatements();

		Formatter fw = new Formatter(Settings.SUBJECT + "-numberOfCommonsStmts.csv");

		for (Entry<LinkedHashSet<String>, List<String>> entry : uniqueTestStatements.entrySet()) {

			// consider only those statements that share a PMC
			if (entry.getValue().size() != 1) {

				List<String> list = entry.getValue();
				for (String string : list) {

					String test = Utils.getTestCaseNameFromTestStatementWithoutClassName(string);
					String statement = Utils.getTestClassNameFromTestCase(string);
					//
					if (results.containsKey(test)) {
						List<String> v = results.get(test);
						v.add(statement);
						results.put(test, v);
					} else {
						List<String> v = new LinkedList<String>();
						v.add(statement);
						results.put(test, v);
					}
				}
			}
		}

		for (String test : results.keySet()) {
			System.out.println(test + "\t" + results.get(test).size());
			fw.format("%s,%d\n", test, results.get(test).size());
		}

		fw.close();
		// System.out.println(results.values());

	}

	public static List<Set<String>> getTestCasesThatShareTestStatement(int cutOff) {
		return getTestCasesThatShareTestStatement(cutOff, getUniqueTestStatements());
	}

	public static List<Set<String>> getTestCasesThatShareTestStatement(int cutOff,
			Map<LinkedHashSet<String>, List<String>> uniqueTestStatements) {

		Map<String, Map<String, Integer>> conGraph = new HashMap<String, Map<String, Integer>>();
		conGraph = getConnectivityGraph(uniqueTestStatements, conGraph);

		// System.out.println(conGraph);
		Set<String> visited = new HashSet<String>();
		List<Set<String>> connectedComponents = new LinkedList<Set<String>>();

		for (Entry<String, Map<String, Integer>> entry : conGraph.entrySet()) {
			if (!visited.contains(entry.getKey())) {
				Set<String> connectedComponent = BFS(entry.getKey(), conGraph, visited, cutOff);
				connectedComponents.add(connectedComponent);
			}
		}

		return connectedComponents;

	}

	public static List<Set<String>> getTestCasesThatShareTestStatement(int cutOff,
			List<Map<LinkedHashSet<String>, List<String>>> uniqueTestStatementsSet) {

		Map<String, Map<String, Integer>> conGraph = new HashMap<String, Map<String, Integer>>();

		for (Map<LinkedHashSet<String>, List<String>> uniqueTestStatements : uniqueTestStatementsSet)
			conGraph = getConnectivityGraph(uniqueTestStatements, conGraph);

		// System.out.println(conGraph);
		Set<String> visited = new HashSet<String>();
		List<Set<String>> connectedComponents = new LinkedList<Set<String>>();

		for (Entry<String, Map<String, Integer>> entry : conGraph.entrySet()) {
			if (!visited.contains(entry.getKey())) {
				Set<String> connectedComponent = BFS(entry.getKey(), conGraph, visited, cutOff);
				connectedComponents.add(connectedComponent);
			}
		}

		return connectedComponents;

	}

	public static Map<String, List<String>> convertTheSetToMap(
			List<Map<LinkedHashSet<String>, List<String>>> uniqueTestStatementsSet) {
		Map<String, List<String>> connectedComponentMap = new HashMap<String, List<String>>();
		for (Map<LinkedHashSet<String>, List<String>> uniqueTestStatements : uniqueTestStatementsSet)
			for (Entry<LinkedHashSet<String>, List<String>> connectedComponent : uniqueTestStatements.entrySet())
				for (String testState : connectedComponent.getValue()) {
					Utils.addAllTheListInMap(connectedComponentMap, testState, connectedComponent.getValue());
					// connectedComponentMap.put(testState,
					// connectedComponent.getValue());
				}

		return connectedComponentMap;

	}

	// returns connected components with the specific cut off value
	private static Set<String> BFS(String startNode, Map<String, Map<String, Integer>> conGraph, Set<String> visited,
			int cutOff) {
		Set<String> connectedComponent = new HashSet<String>();
		LinkedList<String> queue = new LinkedList<String>();

		queue.add(startNode);

		visited.add(startNode);
		while (!queue.isEmpty()) {
			String parent = queue.removeFirst();
			connectedComponent.add(parent);
			Map<String, Integer> children = conGraph.get(parent);
			for (Entry<String, Integer> child : children.entrySet()) {
				if (!visited.contains(child.getKey())) {
					// System.out.println("child.getValue(): " +
					// child.getValue());
					if (child.getValue() >= cutOff) {
						visited.add(child.getKey());
						queue.addLast(child.getKey());
					}
				}
			}
		}

		return connectedComponent;

	}

	// returns the graph, edges are weighted, the map is from each node to set
	// of its neighbors
	private static Map<String, Map<String, Integer>> getConnectivityGraph(
			Map<LinkedHashSet<String>, List<String>> uniqueTestStatements, Map<String, Map<String, Integer>> conGraph) {

		for (Entry<LinkedHashSet<String>, List<String>> entry : uniqueTestStatements.entrySet()) {
			List<String> commonTestStmts = entry.getValue();
			for (String commonTestStmt1 : commonTestStmts) {
				String testCase1 = getTestCaseNameFromState(commonTestStmt1);
				Map<String, Integer> adjSet = conGraph.get(testCase1);
				if (adjSet == null) {
					adjSet = new HashMap<String, Integer>();
					conGraph.put(testCase1, adjSet);
				}
				for (String commonTestStmt2 : commonTestStmts) {
					String testCase2 = getTestCaseNameFromState(commonTestStmt2);

					Integer num = adjSet.get(testCase2);
					adjSet.put(testCase2, num == null ? 1 : num + 1);
				}
			}
		}

		return conGraph;
	}

	private static String getTestCaseNameFromState(String state) {
		int index = state.indexOf('-');
		if (index != -1) {
			return state.substring(0, index);
		}
		return null;
	}

	private static Map<String, Set<String>> splitMethodCalls(Map<String, List<String>> uniqueTestStatements) {
		Map<String, Set<String>> uniqueMethodCalls = new HashMap<String, Set<String>>();

		for (Entry<String, List<String>> entry : uniqueTestStatements.entrySet()) {
			String methodCall = entry.getKey();
			String[] methodCalls = methodCall.split("<methodCall>");
			for (String call : methodCalls) {
				Set<String> testStatements = uniqueMethodCalls.get(call);
				if (testStatements == null) {
					testStatements = new HashSet<String>();
					uniqueMethodCalls.put(call, testStatements);
				}
				testStatements.addAll(entry.getValue());
			}
		}

		return uniqueMethodCalls;
	}

	public static void writeStatToFile() throws FileNotFoundException {

		int totalRedudant = 0;
		int totalRedudantUnique = 0;

		// Map<String, Set<String>> uniqueTestStatements =
		// splitMethodCalls(getUniqueTestStatements());
		Map<LinkedHashSet<String>, List<String>> uniqueTestStatements = getUniqueTestStatements();

		Formatter fw = new Formatter(Settings.SUBJECT + "-expnmethod.csv");

		for (Entry<LinkedHashSet<String>, List<String>> entry : uniqueTestStatements.entrySet()) {
			// System.out.println(entry.getKey()+","+entry.getValue().size());
			LinkedHashSet<String> key = entry.getKey();
			int limit = 1000;

			if (key != null) {
				/*if (key.length() > limit)
					key = key.substring(0, limit);
				key = key.replaceAll("\n", "");
				key = key.replaceAll(",", "");*/
				if(keyGTlimit(key, limit)){
					key = subset(limit, key);
				}
				replaceNLandComma(key);
			}

			// System.out.println(entry.getValue());

			String entr = entry.getValue().toString().replaceAll(",", " ");
			fw.format("%s,%s,%d\n", key, entr, entry.getValue().size());

			/*if (!key.equals("") && entry.getValue().size() > 1) {
				totalRedudant += entry.getValue().size();
				totalRedudantUnique++;
			}*/
			if (!emptyString(key) && entry.getValue().size() > 1) {
				totalRedudant += entry.getValue().size();
				totalRedudantUnique++;
			}
		}

		System.out.println("Redundant Statements: " + totalRedudant);
		System.out.println("Redundant Unique Statements: " + totalRedudantUnique);
		System.out.println("Redundant Statements to be reduced: " + (totalRedudant - totalRedudantUnique));
		fw.format("%s,%s,%d\n", "", "Common", totalRedudant);
		fw.format("%s,%s,%d\n", "", "Unique", totalRedudantUnique);
		fw.format("%s,%s,%d\n", "", "To be reduced", (totalRedudant - totalRedudantUnique));
		fw.close();
		// System.out.println(uniqueTestStatements.size());
	}

	private static boolean emptyString(LinkedHashSet<String> key) {
		boolean b = true;
		for(String s : key) {
			b = b && s.equals("");
		}
		return b;
	}

	private static void replaceNLandComma(LinkedHashSet<String> key) {
		for(String s : key) {
			s.replaceAll("\n", "");
			s.replaceAll(",", "");
		}
	}

	private static LinkedHashSet<String> subset(int limit, LinkedHashSet<String> key) {
		LinkedHashSet<String> subset = new LinkedHashSet<>();
		int soma = 0;
		for(String s : key) {
			int newLimit = limit - soma;
			if(newLimit == 0)
				break;
			if(s.length() > newLimit) {
				String subStr = s.substring(0, newLimit);
				subset.add(subStr);
				soma += subStr.length();
			} else {
				subset.add(s);
				soma += s.length();
			}
		}
		return subset;
	}

	private static boolean keyGTlimit(LinkedHashSet<String> key, int limit) {
		int soma = 0;
		for(String s : key) {
			soma += s.length();
			if(soma > limit)
				break;
		}
		return soma > limit;
	}

	public static long getTime(String stmt) {
		try {
			Scanner sc = new Scanner(new File(Settings.PROJECT_INSTRUMENTED_PATH + "\\time\\" + stmt));
			return sc.nextLong();
		} catch (Exception e) {
			// e.printStackTrace();
		}
		return 0;

	}

	// returns the map from each test statement to set of equivalent test
	// statements
	public static Map<LinkedHashSet<String>, List<String>> getUniqueTestStatements() {
		Map<LinkedHashSet<String>, List<String>> uniqueTestStatements = new HashMap<LinkedHashSet<String>, List<String>>();

		File folder = new File(Settings.tracePaths);

		ArrayList<String> tracesStrs = new ArrayList<>(Arrays.asList(folder.list()));
		Collections.sort(tracesStrs, new NaturalOrderComparator());

		//File[] traces = folder.listFiles();
		int counter = 1;
		for (String trace : folder.list()) {
		//for (String trace : folder.list()) {
			Settings.consoleLogger.error(counter + " - Trace: " + trace);
			if (Instrumenter.parameterizedClasses.contains(Utils.getTestClassNameFromTestStatement(trace))) {
				counter++;
				continue;
			}
			LinkedHashSet<String> methodCalled;
			methodCalled = FileUtils.getMethodCalledForBigFiles(trace);
			/*if (methodCalled == null || methodCalled == "")
				continue;*/
			if(methodCalled == null || (methodCalled.size() == 1 && methodCalled.contains(""))) {
				counter++;
				continue;
			}
			List<String> states = uniqueTestStatements.get(methodCalled);
			String traceName = Utils.nextOrPrevState(trace, tracesStrs, false);
			String nextTraceName = Utils.nextOrPrevState(trace, tracesStrs, true);
			if (!traceName.equals("") && !nextTraceName.equals("")) {
				if (states == null) {
					states = new LinkedList<String>();
					states.add(traceName);
					uniqueTestStatements.put(methodCalled, states);
				} else
					states.add(traceName);
			}
			counter++;
			if (counter % 1000 == 0) {
				Settings.consoleLogger.error(String.format("processed %d logs", counter));
			}
		}

		return uniqueTestStatements;
	}

	public static Map<LinkedHashSet<String>, List<String>> getUniqueTestStatementsForTestClass(String testClass) {
		Map<LinkedHashSet<String>, List<String>> uniqueTestStatements = new HashMap<LinkedHashSet<String>, List<String>>();
		File folder = new File(Settings.tracePaths);
		String[] tracesNames = folder.list();
		ArrayList<String> tracesStrs = new ArrayList(Arrays.asList(tracesNames));
		Collections.sort(tracesStrs, new NaturalOrderComparator());
		File[] traces = folder.listFiles();
		int counter = 1;
		for (File trace : traces) {
			if (!testClass.equals(Utils.getTestClassNameFromTestStatement(trace.getName())))
				continue;	
			LinkedHashSet<String> methodCalled = FileUtils.getMethodCalledForBigFiles(trace.getName());
			if (methodCalled == null || emptyString(methodCalled))
				continue;
			List<String> states = uniqueTestStatements.get(methodCalled);
				
			String traceName = Utils.nextOrPrevState(trace.getName(), tracesStrs, false);
			String nextTraceName = Utils.nextOrPrevState(trace.getName(), tracesStrs, true);
			
			if (!traceName.equals("") && !nextTraceName.equals("")) {
				if (states == null) {
					states = new LinkedList<String>();
					states.add(traceName);
					uniqueTestStatements.put(methodCalled, states);
				} else
					states.add(traceName);
			}
		
			counter++;
			if (counter % 1000 == 0) {
				Settings.consoleLogger.error(String.format("processed %d logs", counter));
			}

		}

		return uniqueTestStatements;
	}

}
