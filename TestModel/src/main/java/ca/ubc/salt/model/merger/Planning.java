package ca.ubc.salt.model.merger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.TreeMap;

import Comparator.NaturalOrderComparator;
import ca.ubc.salt.model.composer.RunningState;
import ca.ubc.salt.model.composer.TestCaseComposer;
import ca.ubc.salt.model.state.TestState;
import ca.ubc.salt.model.state.TestStatement;
import ca.ubc.salt.model.state.VarDefinitionPreq;
import ca.ubc.salt.model.utils.Counter;
import ca.ubc.salt.model.utils.Pair;
import ca.ubc.salt.model.utils.Settings;
import ca.ubc.salt.model.utils.TwoPair;
import ca.ubc.salt.model.utils.Utils;

public class Planning {

	public static TwoPair<TestStatement, RunningState, Map<String, String>, LinkedHashSet<String>> forward(
			TwoPair<TestStatement, RunningState, Map<String, String>, LinkedHashSet<String>> frontier,
			Map<String, TestState> graph, Map<String, Map<String, String>> readValues,
			Map<String, List<String>> connectedComponentsMap, Map<String, TestStatement> testStatementMap,
			Set<String> assertions, Map<String, Set<VarDefinitionPreq>> definitionPreq, int cutoff, int branchNum,
			Set<String> testCases) throws CloneNotSupportedException {

		TestStatement root = frontier.getFirst();
		root.curStart = root;
		root.distFrom.put(root, (long) 0);
		PriorityQueue<TwoPair<TestStatement, RunningState, Map<String, String>, LinkedHashSet<String>>> queue = new PriorityQueue<TwoPair<TestStatement, RunningState, Map<String, String>, LinkedHashSet<String>>>();

		queue.add(frontier);

		int size = frontier.getForth().size();

		boolean first = true;
		while (queue.size() != 0) {
			TwoPair<TestStatement, RunningState, Map<String, String>, LinkedHashSet<String>> pair = queue.poll();
			TestStatement parent = pair.getFirst();
			RunningState runningState = pair.getSecond();
			Map<String, String> batchRename = pair.getThird();
			LinkedHashSet<String> path = pair.getForth();

			if (path.size() - size > cutoff)
				return null;

			if (connectedComponentsMap.containsKey(parent.getName()) || assertions.contains(parent.getName())) {
				Map<String, String> renameClone = new HashMap<String, String>();
				renameClone.putAll(batchRename);
				return new TwoPair<TestStatement, RunningState, Map<String, String>, LinkedHashSet<String>>(parent,
						runningState.clone(), renameClone, path);
			}

			TestCaseComposer.updateRunningState(parent, runningState, readValues, definitionPreq, batchRename);

			Map<String, TestStatement> testStatements = new TreeMap<String, TestStatement>(
					new NaturalOrderComparator());

			List<String> adjStates = Utils.getAdjStates(
					Arrays.asList(new String[] { Utils.getTestCaseNameFromTestStatement(parent.getName()) }),
					parent.getName(), branchNum);

			for (String adjState : adjStates) {
				testStatements.put(adjState, testStatementMap.get(adjState));
			}

			if (first) {
				first = false;

				for (String testCase : testCases) {
					for (int i = 0; i < branchNum; i++) {
						String name = testCase + "-" + i + ".xml";
						TestStatement stmt = testStatementMap.get(name);
						if (stmt != null)
							testStatements.put(name, stmt);
					}
				}
			}

			List<Pair<Integer, TestStatement>> comps = BackwardTestMerger
					.getAllPairCompatibleTestStatements(testStatements, readValues, runningState, null, definitionPreq);

			Collections.sort(comps, Collections.reverseOrder());

			for (Pair<Integer, TestStatement> stmtPair : comps) {

				TestStatement stmt = stmtPair.getSecond();
				if (!path.contains(stmt.getName()))
					relaxChild(root, queue, parent, stmt, runningState,
							stmtPair.getFirst() + readValues.get(stmt.getName()).size(), batchRename, path);
			}
		}

		return null;
	}

	public static List<TestStatement> getAllCompatibleTestStatements(Map<String, TestStatement> allTestStatements,
			Map<String, Map<String, String>> readValues, RunningState runningState, Set<TestStatement> visited,
			Map<String, Set<VarDefinitionPreq>> definitionPreq) {
		List<TestStatement> comps = new ArrayList<TestStatement>();
		
		for (TestStatement stmt : allTestStatements.values()) {
			if (visited != null && visited.contains(stmt))
				continue;
			Map<String, String> readVals = readValues.get(stmt.getName());
			if (readVals == null)
				continue;
			// if (stmt.equals("Array2DRowRealMatrixTest.testSetRow-1.xml"))
			// Settings.consoleLogger.error();
			boolean isComp = true;
			Counter<String> counter = new Counter<String>();
			for (Entry<String, String> entry : readVals.entrySet()) {
				String readVal = entry.getValue();
				Set<String> varsNameInState = runningState.getName(readVal);
				if (varsNameInState == null || varsNameInState.size() == 0) {
					isComp = false;
					break;
				} else {
					counter.increment(readVal);
					if (counter.get(readVal) > varsNameInState.size()) {
						isComp = false;
						break;
					}
				}

			}

			Set<VarDefinitionPreq> defPreqs = definitionPreq.get(stmt.getName());
			if (defPreqs != null) {
				for (VarDefinitionPreq defPreq : defPreqs) {
					String neededType = defPreq.getType();
					Set<String> varsInState = runningState.getNameForType(neededType);
					if (varsInState == null || varsInState.isEmpty()) {
						isComp = false;
						break;
					}
				}
			}

			if (isComp)
				comps.add(stmt);

		}

		return comps;
	}

	public static Map<String, Set<VarDefinitionPreq>> getTheVarDefMap(Set<VarDefinitionPreq> defPreq) {
		Map<String, Set<VarDefinitionPreq>> preqs = new HashMap<String, Set<VarDefinitionPreq>>();
		if (defPreq != null) {
			for (VarDefinitionPreq def : defPreq) {
				Utils.addToTheSetInMap(preqs, def.getType(), def);
			}
		}
		return preqs;
	}

	public static List<TestStatement> backward(TestStatement goal, RunningState initialState,
			Map<String, Map<String, String>> readValues, Map<String, List<String>> connectedComponentsMap,
			Map<String, TestStatement> testStatementMap, Map<String, Set<VarDefinitionPreq>> definitionPreq)
			throws CloneNotSupportedException {
		PriorityQueue<TwoPair<Long, List<TestStatement>, Map<String, Set<String>>, Map<String, Set<VarDefinitionPreq>>>> queue = new PriorityQueue<TwoPair<Long, List<TestStatement>, Map<String, Set<String>>, Map<String, Set<VarDefinitionPreq>>>>();
		ArrayList<TestStatement> initialList = new ArrayList<TestStatement>();
		initialList.add(goal);
		Map<String, Set<String>> initGoals = initGoal(goal, readValues);
		queue.add(new TwoPair<Long, List<TestStatement>, Map<String, Set<String>>, Map<String, Set<VarDefinitionPreq>>>(
				(long) 0, initialList, initGoals, getTheVarDefMap(definitionPreq.get(goal.getName()))));
		while (queue.size() != 0) {
			TwoPair<Long, List<TestStatement>, Map<String, Set<String>>, Map<String, Set<VarDefinitionPreq>>> frontier = queue
					.poll();
			Long cost = frontier.getFirst();
			List<TestStatement> listTillNow = frontier.getSecond();
			Map<String, Set<String>> pGoals = frontier.getThird();
			Map<String, Set<VarDefinitionPreq>> pDefines = frontier.getForth();
			if (areGoalsSatisfied(pGoals, pDefines, initialState)) {
				return frontier.getSecond();
			}
			TestStatement parent = listTillNow.get(0);
			List<TestStatement> preqs = getPreqStmts(parent, pGoals, pDefines, testStatementMap);
			int parentInd = TestCaseComposer.getTestStatementNumber(parent.getName());
			for (TestStatement preq : preqs) {
				int preqInd = TestCaseComposer.getTestStatementNumber(preq.getName());
				if (preqInd >= parentInd)
					continue;
				if (listTillNow.contains(preq))
					continue;
				Map<String, Set<String>> newGoals = new HashMap<String, Set<String>>();
				for (Entry<String, Set<String>> entry : pGoals.entrySet()) {
					Utils.addAllTheSetInMap(newGoals, entry.getKey(), entry.getValue());
				}

				Map<String, Set<VarDefinitionPreq>> newDefGoal = new HashMap<String, Set<VarDefinitionPreq>>();
				for (Entry<String, Set<VarDefinitionPreq>> entry : pDefines.entrySet()) {
					Utils.addAllTheSetInMap(newDefGoal, entry.getKey(), entry.getValue());
				}
				List<TestStatement> newList = new ArrayList<TestStatement>();
				newList.add(preq);
				newList.addAll(listTillNow);
				int score = updateGoals(preq, newGoals, initialState, newDefGoal, readValues, definitionPreq,
						new HashMap<String, String>());
				queue.add(
						new TwoPair<Long, List<TestStatement>, Map<String, Set<String>>, Map<String, Set<VarDefinitionPreq>>>(
								cost + preq.time - score * 100, newList, newGoals, newDefGoal));
			}
		}
		return null;

	}

	public static Map<String, Set<String>> initGoal(TestStatement goal, Map<String, Map<String, String>> readValues) {
		Map<String, Set<String>> initGoals = new HashMap<String, Set<String>>();
		// if (readValues.get(goal.getName()) != null)
		for (Entry<String, String> entry : readValues.get(goal.getName()).entrySet()) {
			Utils.addToTheSetInMap(initGoals, entry.getValue(), entry.getKey());
		}
		return initGoals;
	}

	private static boolean areGoalsSatisfied(Map<String, Set<String>> readGoals,
			Map<String, Set<VarDefinitionPreq>> defineGoals, RunningState initialState) {
		for (Entry<String, Set<String>> entry : readGoals.entrySet()) {
			String readGoal = entry.getKey();
			Set<String> goalNames = entry.getValue();
			Set<String> names = initialState.getName(readGoal);
			if (goalNames.size() != 0 && (names == null || names.size() < goalNames.size()))
				return false;
		}
		for (Entry<String, Set<VarDefinitionPreq>> entry : defineGoals.entrySet()) {

			String neededType = entry.getKey();
			Set<VarDefinitionPreq> preqs = entry.getValue();
			Set<String> varsInState = initialState.getNameForType(neededType);
			if ((varsInState == null ? 0 : varsInState.size()) < preqs.size())
				return false;

		}
		return true;
	}

	public static int updateGoals(TestStatement stmt, Map<String, Set<String>> readGoals, RunningState rs,
			Map<String, Set<VarDefinitionPreq>> defineGoals, Map<String, Map<String, String>> readValues,
			Map<String, Set<VarDefinitionPreq>> definitionPreq, Map<String, String> renameMap) {
		int score = 0;
		for (Entry<String, Pair<String, String>> entry : stmt.getSideEffects().entrySet()) {
			String name = entry.getKey();
			Pair<String, String> changedVal = entry.getValue();
			if (readGoals.containsKey(changedVal.getSecond())) {
				Set<String> set = readGoals.get(changedVal.getSecond());
				if (set.contains(name))
					set.remove(name);
				else {
					if (set.size() != 0) {
						String str = set.iterator().next();
						set.remove(str);
						renameMap.put(name, str);
						if (set.isEmpty()) {
							readGoals.remove(changedVal.getSecond());
						}
					}
				}
				if (rs.getName(changedVal.getSecond()) == null)
					score += 10;
			}
		}
		if (stmt.getNewVars() != null)
			for (Entry<String, String> entry : stmt.getNewVars().entrySet()) {
				String name = entry.getKey();
				String newVal = entry.getValue();
				if (readGoals.containsKey(newVal)) {
					Set<String> set = readGoals.get(newVal);
					if (set.contains(name))
						set.remove(name);
					else {
						if (set.size() != 0) {
							String str = set.iterator().next();
							set.remove(str);
							renameMap.put(name, str);
							if (set.isEmpty()) {
								readGoals.remove(newVal);
							}
						}
					}
					if (rs.getName(newVal) == null)
						score += 10;
				}
				String type = stmt.getTypeOfVar(name);
				if (defineGoals.containsKey(type)) {
					Set<VarDefinitionPreq> set = defineGoals.get(type);
					if (set.size() > 0) {
						VarDefinitionPreq preq = set.iterator().next();
						set.remove(preq);
						renameMap.put(name, preq.getName().getIdentifier());
					} else
						defineGoals.remove(set);
					if (rs.getNameForType(type) == null)
						score += 10;
				}

			}
		if (readValues.get(stmt.getName()) != null)
			for (Entry<String, String> entry : readValues.get(stmt.getName()).entrySet()) {
				String name = entry.getKey();
				String readVal = entry.getValue();
				Utils.addToTheSetInMap(readGoals, readVal, name);
			}

		if (definitionPreq.get(stmt.getName()) != null)
			for (VarDefinitionPreq defPreq : definitionPreq.get(stmt.getName())) {
				Utils.addToTheSetInMap(defineGoals, defPreq.getType(), defPreq);
			}
		return score;

	}

	private static List<TestStatement> getPreqStmts(TestStatement stmt, Map<String, Set<String>> readGoals,
			Map<String, Set<VarDefinitionPreq>> defineGoals, Map<String, TestStatement> testStatementMap) {
		List<TestStatement> preqs = new LinkedList<TestStatement>();
		outer: for (Entry<String, TestStatement> entry : testStatementMap.entrySet()) {

			TestStatement testStmt = entry.getValue();
			for (Pair<String, String> changedVal : testStmt.getSideEffects().values()) {
				if (readGoals.containsKey(changedVal.getSecond())) {
					preqs.add(testStmt);
					continue outer;
				}
			}

			for (Entry<String, String> newEntry : testStmt.getNewVars().entrySet()) {
				String name = newEntry.getKey();
				String newVal = newEntry.getValue();
				if (readGoals.containsKey(newVal)) {
					preqs.add(testStmt);
					continue outer;
				}

				if (defineGoals.containsKey(stmt.getTypeOfVar(name))) {
					preqs.add(testStmt);
					continue outer;
				}

			}

		}

		return preqs;
	}

	private static void relaxChild(TestStatement root,
			PriorityQueue<TwoPair<TestStatement, RunningState, Map<String, String>, LinkedHashSet<String>>> queue,
			TestStatement parent, TestStatement stmt, RunningState runningState, int bonus,
			Map<String, String> batchRename, LinkedHashSet<String> path) throws CloneNotSupportedException {
		long newD = parent.distFrom.get(root) + stmt.time - bonus + stmt.getSideEffects().size() * 1000000
				+ TestCaseComposer.getTestStatementNumber(stmt.getName());
		Long childDist = stmt.distFrom.get(root);
		stmt = stmt.clone();
		stmt.parent.put(root, parent);
		stmt.distFrom.put(root, newD);
		stmt.curStart = root;
		Map<String, String> renameClone = new HashMap<String, String>();
		renameClone.putAll(batchRename);
		LinkedHashSet<String> newPath = new LinkedHashSet<String>(path);
		newPath.add(stmt.getName());
		queue.add(new TwoPair<TestStatement, RunningState, Map<String, String>, LinkedHashSet<String>>(stmt,
				runningState.clone(), renameClone, newPath));
	}

	public static Map<String, Set<TestStatement>> getTestCaseTestStatementMapping(Collection<TestStatement> stmts) {
		Map<String, Set<TestStatement>> mapping = new HashMap<String, Set<TestStatement>>();
		for (TestStatement stmt : stmts) {
			String testCaseName = Utils.getTestCaseNameFromTestStatement(stmt.getName());
			Utils.addToTheSetInMap(mapping, testCaseName, stmt);
		}

		return mapping;

	}

	public static Map<String, Map<String, TestStatement>> getTestCaseTestStatementMapping(
			Map<String, TestStatement> stmts) {
		Map<String, Map<String, TestStatement>> mapping = new HashMap<String, Map<String, TestStatement>>();
		for (Entry<String, TestStatement> entry : stmts.entrySet()) {
			String testCaseName = Utils.getTestCaseNameFromTestStatement(entry.getKey());
			Utils.addToTheMapInMap(mapping, testCaseName, entry.getKey(), entry.getValue());
		}

		return mapping;

	}

	public static Map<String, Set<String>> getTestCaseTestStatementStringMapping(Collection<String> stmts) {
		Map<String, Set<String>> mapping = new HashMap<String, Set<String>>();
		for (String stmt : stmts) {
			String testCaseName = Utils.getTestCaseNameFromTestStatement(stmt);
			Utils.addToTheSetInMap(mapping, testCaseName, stmt);
		}

		return mapping;

	}

	public static Set<TestStatement> getTestStatementSet(Set<String> stmts, Map<String, TestStatement> stmtMap) {
		Set<TestStatement> stmtSet = new HashSet<TestStatement>();
		for (String stmt : stmts) {
			stmtSet.add(stmtMap.get(stmt));
		}
		return stmtSet;
	}
}
