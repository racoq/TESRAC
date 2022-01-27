package ca.ubc.salt.model.state;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;

import Comparator.NaturalOrderComparator;
import ca.ubc.salt.model.utils.FileUtils;
import ca.ubc.salt.model.utils.Settings;
import ca.ubc.salt.model.utils.Utils;

public class StateComparator {

	static HashMap<Integer, List<String>> xmlHashes;

	public static Map<String, TestState> createGraph(List<String> testCases) throws FileNotFoundException {
		Collections.sort(testCases);
		
		xmlHashes = createHashes(testCases);

		Map<String, TestState> testStateOfState = new HashMap<String, TestState>();
		List<TestState> testStates = new LinkedList<TestState>();

		for (Entry<Integer, List<String>> entry : xmlHashes.entrySet()) {
			compareStates(entry.getKey(), testStateOfState, testStates);
		}

		ArrayList<String> sortedTestStates = FileUtils.getStatesForTestCase(testCases);
		Collections.sort(sortedTestStates, new NaturalOrderComparator());

		populateGraph(testStateOfState, testStates, sortedTestStates);

		return testStateOfState;
	}

	private static void populateGraph(Map<String, TestState> testStateMap, List<TestState> testStates,
			ArrayList<String> sortedTestStates) {

		for (int i = 0; i < testStates.size(); i++) {
			TestState ts = testStates.get(i);
			for (String state : ts.getStates()) {
				String nextState = Utils.nextOrPrevState(state, sortedTestStates, true);
				if (testStateMap.containsKey(nextState)) {
					TestState nextStateTS = testStateMap.get(nextState);
					TestStatement stmt = new TestStatement(ts, nextStateTS, state);
					ts.getChildren().put(state, stmt);
					nextStateTS.getParents().put(state, stmt);

				}
			}
		}

	}

	private static void compareStates(int hash, Map<String, TestState> testStateOfState, List<TestState> testStates) {

		List<String> states = xmlHashes.get(hash);

		for (ListIterator<String> it = states.listIterator(); it.hasNext();) {
			String firstState = it.next();
			it.remove();

			TestState ts = new TestState();
			testStates.add(ts);

			ts.states.add(firstState);
			testStateOfState.put(firstState, ts);

			for (ListIterator<String> it2 = states.listIterator(); it2.hasNext();) {
				String secondState = it2.next();
				if (compareStates(firstState, secondState)) {

					ts.states.add(secondState);
					testStateOfState.put(secondState, ts);
					it2.remove();
				}
			}
		}

	}

	private static boolean compareStates(String first, String second) {
		String firstState = FileUtils.getState(first);
		String secondState = FileUtils.getState(second);
		return firstState.equals(secondState);
	}

	private static HashMap<Integer, List<String>> createHashes() throws FileNotFoundException {

		HashMap<Integer, List<String>> hashes = new HashMap<Integer, List<String>>();

		File folder = new File(Settings.tracePaths);
		File[] traces = folder.listFiles();

		for (File trace : traces) {
			int hash = hash(trace);
			if (hashes.containsKey(hash)) {
				hashes.get(hash).add(trace.getName());
			} else {
				List<String> list = new LinkedList<String>();
				list.add(trace.getName());
				hashes.put(hash, list);
			}
		}

		return hashes;
	}

	private static HashMap<Integer, List<String>> createHashes(List<String> testCases) throws FileNotFoundException {
		HashMap<Integer, List<String>> hashes = new HashMap<Integer, List<String>>();

		for (final String testCase : testCases) {

			File[] traces = FileUtils.getStateFilesForTestCase(testCase);

			for (File trace : traces) {
				int hash = hash(trace);
				if (hashes.containsKey(hash)) {
					hashes.get(hash).add(trace.getName());
				} else {
					List<String> list = new LinkedList<String>();
					list.add(trace.getName());
					hashes.put(hash, list);
				}
			}
		}

		return hashes;
	}

	private static int hash(File file) throws FileNotFoundException {
		
		String xml = FileUtils.getState(file);
		int hash = xml.hashCode();

		return hash;
	}

}
