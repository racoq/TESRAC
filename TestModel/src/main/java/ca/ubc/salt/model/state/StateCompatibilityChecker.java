package ca.ubc.salt.model.state;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import ca.ubc.salt.model.utils.FileUtils;
import ca.ubc.salt.model.utils.Settings;
import ca.ubc.salt.model.utils.Utils;
import ca.ubc.salt.model.utils.XMLUtils;

public class StateCompatibilityChecker {
	// eg. <Object1, objField1, objobjField1, ..., int, 2> -> <state1, state2,
	// state3,...>
	public HashMap<String, Set<String>> varStateSet = new HashMap<String, Set<String>>();

	/*
	 * public static void main(String[] args) throws SAXException, IOException {
	 * StateCompatibilityChecker scc = new StateCompatibilityChecker(); //
	 * scc.processState("testSubtract-17.xml"); scc.populateVarStateSet(); //
	 * System.out.println(scc.varStateSet); Map<String, Set<SimpleName>>
	 * readVars =
	 * ReadVariableDetector.populateReadVarsForFile(Settings.TEST_CLASS);
	 * Map<String, Set<List<String>>> readValues =
	 * ReadVariableDetector.getReadValues(readVars); //
	 * System.out.println(readValues); Map<String, Set<String>> compatibleStates
	 * = new HashMap<String, Set<String>>();
	 * getCompatibleStates(compatibleStates, scc.varStateSet, readValues); //
	 * System.out.println(compatibleStates);
	 * 
	 * Map<String, TestState> graph = StateComparator.createGraph();
	 * 
	 * setCompabilityFields(graph, compatibleStates);
	 * 
	 * TestState root = graph.get("init.xml");
	 * System.out.println(root.printDot(true));
	 * 
	 * // List<List<TestStatement>> paths = root.getAllPaths(); //
	 * System.out.println(paths.size()); // for (List<TestStatement> path :
	 * paths) // System.out.println(path);
	 * 
	 * }
	 */

	public static void setCompabilityFields(Map<String, TestState> graph, Map<String, Set<String>> compatibleStates) {
		for (Entry<String, Set<String>> entry : compatibleStates.entrySet()) {
			String stateName = entry.getKey();
			Set<String> compatibleState = entry.getValue();

			TestState parentTestState = graph.get(stateName);
			if (parentTestState != null) {
				TestStatement testStatement = parentTestState.getChildren().get(stateName);
				if (testStatement != null) {
					for (String state : compatibleState) {
						TestState compState = graph.get(state);
						testStatement.getCompatibleStates().add(compState);
						compState.compatibleStatements.put(testStatement.getName(), testStatement);
					}
				} else {
					// !!!
				}

			} else {
				// !!!
			}
		}
	}

	public static void getCompatibleStates(Map<String, Set<String>> compatibleStates,
			HashMap<String, Set<String>> varStateSet, Map<String, Set<String>> readValues, Set<String> allStates) {
		for (Entry<String, Set<String>> entry : readValues.entrySet()) {
			String stateName = entry.getKey();
			Set<String> readValue = entry.getValue();

			List<Set<String>> comp = new LinkedList<Set<String>>();
			for (String varValue : readValue) {
				comp.add(getAllStatesWithVariableValue(varStateSet, varValue));
			}

			if (comp.size() > 0) {
				Set<String> compatibleStatesOfState = Utils.intersection(comp);
				compatibleStates.put(stateName, compatibleStatesOfState);
			} else {
				compatibleStates.put(stateName, allStates);

			}
		}
	}

	public static Set<String> getAllStatesWithVariableValue(HashMap<String, Set<String>> varStateSet, String value) {
		return varStateSet.get(value);
	}

	public void populateVarStateSet() {
		File folder = new File(Settings.tracePaths);
		String[] traces = folder.list();
		populateVarStateSet(Arrays.asList(traces));
	}

	public void populateVarStateSet(List<String> testCases) {

		for (String testCase : testCases) {
			List<String> states = FileUtils.getStatesForTestCase(testCase, null);
			for (String state : states) {
				try {
					processState(state);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

	public void processState(String stateName) throws SAXException, IOException {
		NodeList nodeList = XMLUtils.getNodeList(stateName);
		if (nodeList == null)
			return;

		for (int i = 0; i < nodeList.getLength(); i++) {
			Node object = nodeList.item(i);
			if (object instanceof Element) {
				Utils.addToTheSetInMap(this.varStateSet, XMLUtils.getXMLString(object), stateName);
				// processObject(object, this.varStateSet, new
				// LinkedList<String>(), stateName);
			}
		}
	}

	public static void processObjectIndex(String stateName, int i, HashMap<List<String>, Set<String>> varStateSet,
			NodeList nodeList) throws SAXException, IOException {
		Node object = nodeList.item(i);
		if (object instanceof Element) {
			processObject(object, varStateSet, new LinkedList<String>(), stateName);
		}
	}

	public static void processObject(Node object, HashMap<List<String>, Set<String>> varStateSet,
			LinkedList<String> key, String stateName) {

		key.add(object.getNodeName());

		NodeList children = object.getChildNodes();
		if (children.getLength() == 0) {
			key.add(object.getNodeValue());
			addToList(varStateSet, key, stateName);
			key.removeLast();
		} else {
			for (int i = 0; i < children.getLength(); i++) {
				processObject(children.item(i), varStateSet, key, stateName);
			}
		}

		key.removeLast();
	}

	private static void addToList(HashMap<List<String>, Set<String>> varStateSet, List<String> key, String stateName) {
		Set<String> states = varStateSet.get(key);
		if (states != null) {
			states.add(stateName);
		} else {
			states = new HashSet<String>();
			states.add(stateName);
			// TODO check if you need to clone key object!
			varStateSet.put(new LinkedList<String>(key), states);
		}
	}

	private Set<String> getCompatibleStates(List<List<String>> vars) {
		List<Set<String>> stateSets = new LinkedList<Set<String>>();
		for (List<String> var : vars) {
			Set<String> states = varStateSet.get(var);
			if (states != null)
				stateSets.add(states);
		}

		return Utils.intersection(stateSets);
	}
}
