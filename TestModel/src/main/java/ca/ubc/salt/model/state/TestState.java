package ca.ubc.salt.model.state;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import Comparator.NaturalOrderComparator;

import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public class TestState extends TestModelNode {
	
	Map<String, TestStatement> children;
	Map<String, TestStatement> parents;
	List<String> states;
	List<String> asserts;
	Map<String, TestStatement> compatibleStatements;

	public TestState() {
		children = new TreeMap<String, TestStatement>(new NaturalOrderComparator());
		parents = new HashMap<String, TestStatement>();
		states = new LinkedList<String>();
		asserts = new LinkedList<String>();
		compatibleStatements = new TreeMap<String, TestStatement>(new NaturalOrderComparator());
	}
	
	public Map<String, TestStatement> getCompatibleStatements() {
		return compatibleStatements;
	}

	public void setCompatibleStatements(Map<String, TestStatement> compatibleStatements) {
		this.compatibleStatements = compatibleStatements;
	}

	public String printDot(boolean printWithTestStatements) {
		StringBuilder sb = new StringBuilder();
		HashSet<TestState> visited = new HashSet<TestState>();
		sb.append("digraph{\n");
		DFSPrint(this, sb, visited, printWithTestStatements);
		sb.append("}");

		return sb.toString();
	}

	public List<List<TestStatement>> getAllPaths() {
		List<List<TestStatement>> paths = new LinkedList<List<TestStatement>>();
		LinkedList<TestStatement> thisPath = new LinkedList<TestStatement>();
		Set<TestStatement> visited = new HashSet<TestStatement>();
		Map<TestState, Set<TestStatement>> compVisited = new HashMap<TestState, Set<TestStatement>>();
		getAllPaths(paths, thisPath, visited, compVisited, this);
		return paths;
	}

	public void getAllPaths(List<List<TestStatement>> paths, LinkedList<TestStatement> thisPath,
			Set<TestStatement> visited, Map<TestState, Set<TestStatement>> compVisited, TestState state) {
		boolean thisIsLeaf = true;
		for (Entry<String, TestStatement> entry : state.getChildren().entrySet()) {
			TestStatement ts = entry.getValue();
			if (!visited.contains(ts)) {
				thisIsLeaf = false;
				thisPath.add(ts);
				visited.add(ts);
				getAllPaths(paths, thisPath, visited, compVisited, ts.getEnd());
				visited.remove(ts);
				thisPath.removeLast();
			}
		}

		// for (TestStatement ts : state.compatibleStatements)
		// {
		//
		// Set<TestStatement> stateVisited = compVisited.get(state);
		// if (stateVisited == null)
		// {
		// stateVisited = new HashSet<TestStatement>();
		// compVisited.put(state, stateVisited);
		// }
		//
		// if (!stateVisited.contains(ts))
		// {
		// thisIsLeaf = false;
		// thisPath.add(ts);
		// stateVisited.add(ts);
		// getAllPaths(paths, thisPath, visited, compVisited, ts.getEnd());
		// stateVisited.remove(ts);
		// thisPath.removeLast();
		// }
		// }

		if (thisIsLeaf) {
			paths.add((List<TestStatement>) thisPath.clone());
		}

	}

	public void DFSPrint(TestState root, StringBuilder sb, HashSet<TestState> visited,
			boolean printWithTestStatements) {
		// System.out.println(root);
		visited.add(root);
		for (TestStatement child : root.getChildren().values()) {
			if (printWithTestStatements) {
				sb.append(printEdge(child));
				sb.append(printCompatibilityEdges(child));
			} else
				sb.append(printEdge(root, child.getEnd()));
			if (!visited.contains(child.getEnd()))
				DFSPrint(child.getEnd(), sb, visited, printWithTestStatements);
		}
	}

	public String printCompatibilityEdges(TestStatement ts) {
		StringBuilder sb = new StringBuilder();
		for (TestState state : ts.getCompatibleStates()) {
			// if (state != ts.getStart())
			sb.append("\"" + state.toString() + "\"" + " -> " + "\"" + ts.toString() + "\"[color=red]" + "\n");
		}
		return sb.toString();
	}

	public String printEdge(TestState root, TestState child) {
		return "\"" + root.toString() + "\"" + " -> " + "\"" + child.toString() + "\"" + "\n";
	}

	public String printEdge(TestStatement child) {
		String str = "\"" + child.toString() + "\"[shape=box];\n";
		str += "\"" + child.getStart().toString() + "\"" + " -> " + "\"" + child.toString() + "\"" + "\n";
		str += "\"" + child.toString() + "\"" + " -> " + "\"" + child.getEnd().toString() + "\"" + "\n";
		return str;
	}

	public String toString() {
		return states.toString();
	}

	public Map<String, TestStatement> getChildren() {
		return children;
	}

	public void setChildren(HashMap<String, TestStatement> children) {
		this.children = children;
	}

	public Map<String, TestStatement> getParents() {
		return parents;
	}

	public void setParents(HashMap<String, TestStatement> parents) {
		this.parents = parents;
	}

	public List<String> getStates() {
		return states;
	}

	public void setStates(List<String> states) {
		this.states = states;
	}

	public List<String> getAsserts() {
		return asserts;
	}

	public void setAsserts(List<String> asserts) {
		this.asserts = asserts;
	}

	@Override
	protected Object clone() throws CloneNotSupportedException {
		// TODO Auto-generated method stub
		return super.clone();
	}

}
