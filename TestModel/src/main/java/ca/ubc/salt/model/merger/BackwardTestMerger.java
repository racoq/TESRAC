package ca.ubc.salt.model.merger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Formatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.plaf.synth.SynthSeparatorUI;

import java.util.PriorityQueue;
import java.util.Set;

import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Statement;
import org.xml.sax.SAXParseException;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.StaxDriver;

import Comparator.NaturalOrderComparator;
import ca.ubc.salt.model.composer.RunningState;
import ca.ubc.salt.model.composer.TestCaseComposer;
import ca.ubc.salt.model.instrumenter.Instrumenter;
import ca.ubc.salt.model.state.ProductionCallingTestStatement;
import ca.ubc.salt.model.state.ReadVariableDetector;
import ca.ubc.salt.model.state.StateComparator;
import ca.ubc.salt.model.state.TestState;
import ca.ubc.salt.model.state.TestStatement;
import ca.ubc.salt.model.state.VarDefinitionPreq;
import ca.ubc.salt.model.utils.Counter;
import ca.ubc.salt.model.utils.FileUtils;
import ca.ubc.salt.model.utils.Pair;
import ca.ubc.salt.model.utils.Settings;
import ca.ubc.salt.model.utils.Triple;
import ca.ubc.salt.model.utils.TwoPair;
import ca.ubc.salt.model.utils.Utils;

public class BackwardTestMerger {

	public static Set<String> testCasesToRemove;
	public static MergingResult mergingResult;
	public static List<MergingResult> mergingResultsList = new ArrayList<MergingResult>();

	public static void main(String[] args) throws FileNotFoundException, ClassNotFoundException, IOException,
			CloneNotSupportedException, SAXParseException {

		System.out.println("Merging project: " + Settings.PROJECT_PATH);
		long startTime = System.currentTimeMillis();

		merge2();

		long stopTime = System.currentTimeMillis();
		long elapsedTime = stopTime - startTime;

		// ProductionCallingTestStatement.main(args);
		ProductionCallingTestStatement.writeStatToFile();
		ProductionCallingTestStatement.getCommonStmsForEachTestCase();
		
		System.out.println("Merging time: " + elapsedTime / 1000);
		System.out.println("Merging time: " + msToMin(elapsedTime) + ":" + msToS(elapsedTime));
	}
	
	private static long msToMin(long elapsedTime) {
		return (elapsedTime / 1000) / 60;
	}

	private static long msToS(long elapsedTime) {
        return (elapsedTime / 1000) % 60;
        
	}

	public static Map<String, TestStatement> getAllTestStatements(ArrayList<String> allStmtStr,
			Map<String, TestState> graph) {

		Map<String, TestStatement> allStmts = new HashMap<String, TestStatement>();

		for (String stmt : allStmtStr) {
			TestStatement ts = getTestStatementFromStr(allStmtStr, graph, stmt);
			if (ts != null)
				allStmts.put(stmt, ts);
		}

		return allStmts;
	}

	private static TestStatement getTestStatementFromStr(ArrayList<String> allStmtStr, Map<String, TestState> graph,
			String stmt) {
		String nextState = Utils.nextOrPrevState(stmt, allStmtStr, true);
		if (nextState.equals(""))
			nextState = "init.init-.xml";
		TestState end = graph.get(nextState);
		TestStatement ts = end.getParents().get(stmt);
		return ts;
	}

	public static void initSideEffectForStatements(Map<String, TestStatement> testStatements, List<String> testCases,
			Map<String, Set<VarDefinitionPreq>> defPreq) {
		for (Entry<String, TestStatement> entry : testStatements.entrySet()) {
			entry.getValue().initSideEffects(testCases, defPreq.get(entry.getKey()));
		}
	}

	private static void merge2() throws IOException, FileNotFoundException, ClassNotFoundException,
			CloneNotSupportedException, SAXParseException {


		// delete some unnecessary files
		Utils.cleanProjectBeforeMerging();

		Instrumenter.loadStructs();
		
	
		
		Formatter formatter = new Formatter(Settings.SUBJECT + "-mergingStat.csv");
		formatter.format(
				"merging test,merged test class,merged test case,before,after,saved,fatal error,warning,couldn't satisfy\n");	
		XStream xstream = new XStream(new StaxDriver());
		File file = new File(Settings.SUBJECT + "-components.xml");
		List<Set<String>> connectedComponents = null;
		Map<String, List<String>> connectedComponentsMap = null;

	
		
		if (!file.exists()) {

			long setupCost = 10;
			List<Map<LinkedHashSet<String>, List<String>>> uniqueTestStatementSet = new ArrayList<Map<LinkedHashSet<String>, List<String>>>();
			for (String paramClass : Instrumenter.parameterizedClasses) {

				uniqueTestStatementSet
						.add(ProductionCallingTestStatement.getUniqueTestStatementsForTestClass(paramClass));

			}
			Map<LinkedHashSet<String>, List<String>> uniqueTestStatements = ProductionCallingTestStatement.getUniqueTestStatements();
			uniqueTestStatementSet.add(uniqueTestStatements);

			int commonStms = 3;
			connectedComponents = ProductionCallingTestStatement.getTestCasesThatShareTestStatement(commonStms,
					uniqueTestStatementSet);
			// connectedComponents.remove(0);
			connectedComponentsMap = ProductionCallingTestStatement.convertTheSetToMap(uniqueTestStatementSet);
			String components = xstream.toXML(connectedComponents);
			FileWriter fw = new FileWriter(Settings.SUBJECT + "-components.xml");
			fw.write(components);
			fw.close();

			ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(Settings.SUBJECT + "-unique.xml"));
			out.writeObject(connectedComponentsMap);
			out.close();
			
		} else {
			connectedComponents = (List<Set<String>>) xstream.fromXML(new File(Settings.SUBJECT + "-components.xml"));
			ObjectInputStream in = new ObjectInputStream(new FileInputStream(Settings.SUBJECT + "-unique.xml"));
			connectedComponentsMap = (Map<String, List<String>>) in.readObject();
			in.close();
		}
	
		writeStatsToFile(connectedComponents);
		int totalNumberOfStatementsBeforeMerging = 0, totalNumberOfStatementsAftermerging = 0;
		int numberOfMergedTests = 0;
		int counter = 0;
		int limit = 0;

		Map<String, Map<String, List<String>>> equivalentTestStmtsPerTestCase = getTheMapOfConnectedComponentsMap(
				connectedComponentsMap);
		for (Set<String> connectedComponent : connectedComponents) {

			if (connectedComponent.size() < 2)
				continue;
	
			Settings.consoleLogger.error(
					String.format("merging %d tests : %s", connectedComponent.size(), connectedComponent.toString()));

			String mergedTestcaseName;

			counter++;
			if (counter < limit)
				continue;
			List<String> testCases = new LinkedList<String>();
			testCases.addAll(connectedComponent);
			Map<String, Set<VarDefinitionPreq>> definitionPreq = new HashMap<String, Set<VarDefinitionPreq>>();
			Set<String> corruptedTestCases = new HashSet<String>();
			Map<String, Statement> allASTStatements = new HashMap<String, Statement>();
			Map<String, Map<String, String>> readValues = getAllReadValues(testCases, definitionPreq,
					corruptedTestCases, allASTStatements);
	
			removingCorruptedTestCases(connectedComponent, corruptedTestCases);
			if (connectedComponent.size() == 0)
				continue;
			Map<String, List<String>> connectedComponentsMapCpy = connectedComponentsMap;
			Map<String, Map<String, List<String>>> equivalentTestStmtsPerTestCaseCpy = equivalentTestStmtsPerTestCase;
			
			connectedComponentsMap = new HashMap<String, List<String>>(connectedComponentsMapCpy);
			equivalentTestStmtsPerTestCase = getTheMapOfConnectedComponentsMap(connectedComponentsMapCpy);
			boolean loop = true;
			while (loop && connectedComponent.size() > 1) {
				testCasesToRemove = new HashSet<String>();
				loop = false;

				testCases.clear();
				testCases.addAll(connectedComponent);

				testCases.add("init.init");
				Map<String, TestState> graph = StateComparator.createGraph(testCases);
				TestState root = graph.get("init.init-.xml");

				List<List<TestStatement>> paths = new LinkedList<List<TestStatement>>();

				ArrayList<String> allStates = FileUtils.getStatesForTestCase(testCases);
				
				Collections.sort(allStates, new NaturalOrderComparator());
				Map<String, TestStatement> allTestStatements = getAllTestStatements(allStates, graph);
				populateStatementField(allASTStatements, allTestStatements);
				initSideEffectForStatements(allTestStatements, testCases, definitionPreq);
				Set<String> assertions = getAllAssertions(allTestStatements);
				Map<String, Set<String>> testClasses = Utils.getTestClassMapFromTestCases(connectedComponent);
				String mainClassName = Utils.getTestClassWithMaxNumberOfTestCases(testClasses);
				RunningState initialState = new RunningState(connectedComponent, mainClassName);
				mergingResult = new MergingResult(mainClassName, connectedComponent);

				LinkedList<TestStatement> firstPhasePath = new LinkedList<TestStatement>();
				TestStatement rootStmt = new TestStatement(root, root, "init.xml");
				TwoPair<TestStatement, RunningState, Map<String, String>, LinkedHashSet<String>> prevFrontier = performFirstPhaseGreedyAlg(
						connectedComponentsMap, equivalentTestStmtsPerTestCase, definitionPreq, readValues, graph,
						allTestStatements, assertions, initialState, firstPhasePath, rootStmt, connectedComponent);
				List<TestStatement> secondPhasePath = performSecondPhaseBackwardAlg(connectedComponentsMap,
						equivalentTestStmtsPerTestCase, connectedComponent, definitionPreq, readValues,
						allTestStatements, assertions, mainClassName, prevFrontier);
				if (testCasesToRemove.size() > 0) {
					removingCorruptedTestCases(connectedComponent, testCasesToRemove);
					Settings.consoleLogger.error("retrying...");
					loop = true;
					connectedComponentsMap = new HashMap<String, List<String>>(connectedComponentsMapCpy);
					equivalentTestStmtsPerTestCase = getTheMapOfConnectedComponentsMap(connectedComponentsMapCpy);
					continue;
				}
				
				List<TestStatement> firstPhaseMergedPath = firstPhasePath;
				paths.add(firstPhaseMergedPath);
				paths.add(secondPhasePath);
				ArrayList<TestStatement> arrMergedPath = new ArrayList<TestStatement>();
				arrMergedPath.addAll(firstPhaseMergedPath);

				int totalNumberOfStatements = allStates.size() - testCases.size() * 2 + 1;
				int totalMerged = 0;

				for (List<TestStatement> mpath : paths) {
					totalMerged += mpath.size();
				}
				if (totalMerged < totalNumberOfStatements) {
					mergedTestcaseName = TestCaseComposer.generateTestCaseName(connectedComponent);
					TestCaseComposer.composeTestCase(arrMergedPath, connectedComponent, mergedTestcaseName, readValues,
							definitionPreq, secondPhasePath);
					if (testCasesToRemove.size() > 0) {
						removingCorruptedTestCases(connectedComponent, testCasesToRemove);
						Settings.consoleLogger.error("retrying...");
						loop = true;
						equivalentTestStmtsPerTestCase = equivalentTestStmtsPerTestCaseCpy;
						connectedComponentsMap = new HashMap<String, List<String>>(connectedComponentsMapCpy);
						equivalentTestStmtsPerTestCase = getTheMapOfConnectedComponentsMap(connectedComponentsMapCpy);
						continue;
					}
				} else {
					counter--;
					continue;
				}
			
				totalNumberOfStatementsBeforeMerging += totalNumberOfStatements;
				totalNumberOfStatementsAftermerging += totalMerged;
				numberOfMergedTests += connectedComponent.size();
				Settings.consoleLogger.error(String.format("Before Merging : %d, After Merging %d, saved : %d",
						totalNumberOfStatements, totalMerged, totalNumberOfStatements - totalMerged));
				writeStatToFileAndConsole(formatter, totalNumberOfStatementsBeforeMerging,
						totalNumberOfStatementsAftermerging, numberOfMergedTests, counter, connectedComponent,
						mergedTestcaseName, mainClassName, totalNumberOfStatements, totalMerged);
				updateMergingResultStruct(mergedTestcaseName, totalNumberOfStatements, totalMerged);
			}
		}
		writeTotalsToConsole(numberOfMergedTests, counter, totalNumberOfStatementsBeforeMerging,
				totalNumberOfStatementsAftermerging);
		writeMergingResultsToFile(formatter);

	}

	private static void writeTotalsToConsole(int numberOfMergedTests, int counter, int totalBeforeMerging,
			int totalAftermerging) {

		if (numberOfMergedTests == 0) {
			Settings.consoleLogger.error(String.format("NumberOfTestsBefore : %d, NumberOfTestsAfter : %d, Saved: %d",
					numberOfMergedTests, counter, 0));
		} else {
			Settings.consoleLogger.error(String.format("NumberOfTestsBefore : %d, NumberOfTestsAfter : %d, Saved: %d",
					numberOfMergedTests, counter, numberOfMergedTests - counter));
		}

		Settings.consoleLogger.error(
				String.format("Statements Before merging : %d, Statements After merging : %d, Reduced Statements: %d",
						totalBeforeMerging, totalAftermerging, totalBeforeMerging - totalAftermerging));

	}

	private static void writeStatToFileAndConsole(Formatter formatter, int totalBeforeMerging, int totalAftermerging,
			int numberOfMergedTests, int counter, Set<String> connectedComponent, String mergedTestcaseName,
			String mainClassName, int totalNumberOfStatements, int totalMerged) {

		Settings.consoleLogger.error(String.format("NumberOfTestsBefore : %d, NumberOfTestsAfter : %d, Saved: %d",
				numberOfMergedTests, counter, numberOfMergedTests - counter));

		Settings.consoleLogger.error(
				String.format("Statements Before merging : %d, Statements After merging : %d, Reduced Statements: %d",
						totalBeforeMerging, totalAftermerging, totalBeforeMerging - totalAftermerging));

		// Settings.consoleLogger.error(String.format(
		// "Total Before merging : %d, After merging : %d, NumberOfTestsBefore :
		// %d, NumberOfTestsAfter : %d",
		// totalBeforeMerging, totalAftermerging, numberOfMergedTests,
		// counter));

		formatter.format("%s,%s,%s,%d,%d,%d,%b,%b,%b\n", connectedComponent.toString().replaceAll(",", " "),
				mainClassName, mergedTestcaseName, totalNumberOfStatements, totalMerged,
				totalNumberOfStatements - totalMerged, mergingResult.fatalError, mergingResult.warning,
				mergingResult.couldntsatisfy);
	}

	private static void writeMergingResultsToFile(Formatter formatter) throws IOException, FileNotFoundException {

		ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(Settings.SUBJECT + "-mergingResult.xml"));
		out.writeObject(mergingResultsList);
		out.flush();
		out.close();

		formatter.flush();
		formatter.close();
	}

	private static void updateMergingResultStruct(String mergedTestcaseName, int totalNumberOfStatements,
			int totalMerged) {
		mergingResult.after = totalMerged;
		mergingResult.before = totalNumberOfStatements;
		mergingResult.mergedTestCaseName = mergedTestcaseName;
		mergingResultsList.add(mergingResult);
	}

	public static Map<String, Map<String, List<String>>> getTheMapOfConnectedComponentsMap(
			Map<String, List<String>> connectedComponentsMap) {
		Map<String, Map<String, List<String>>> map = new HashMap<String, Map<String, List<String>>>();
		for (Entry<String, List<String>> entry : connectedComponentsMap.entrySet()) {
			String testCaseName = Utils.getTestCaseNameFromTestStatement(entry.getKey());
			Utils.addToTheMapInMap(map, testCaseName, entry.getKey(), entry.getValue());
		}
		return map;
	}

	private static boolean isThereUnCoveredStmts(Map<String, Map<String, List<String>>> equivalentTestStmtsPerTestCase,
			Set<String> testCases) {
		for (String testCase : testCases) {
			Map<String, List<String>> mapView = equivalentTestStmtsPerTestCase.get(testCase);
			if (mapView != null && mapView.isEmpty() == false)
				return true;
		}
		return false;
	}

	private static List<TestStatement> performSecondPhaseBackwardAlg(Map<String, List<String>> connectedComponentsMap,
			Map<String, Map<String, List<String>>> equivalentTestStmtsPerTestCase, Set<String> connectedComponent,
			Map<String, Set<VarDefinitionPreq>> definitionPreq, Map<String, Map<String, String>> readValues,
			Map<String, TestStatement> allTestStatements, Set<String> assertions, String mainClassName,
			TwoPair<TestStatement, RunningState, Map<String, String>, LinkedHashSet<String>> prevFrontier)
			throws CloneNotSupportedException {

		List<TestStatement> secondPhasePath = new LinkedList<TestStatement>();

		Map<String, Map<String, TestStatement>> allStmtsView = Planning
				.getTestCaseTestStatementMapping(allTestStatements);
		RunningState runningState = prevFrontier.getSecond();
		coverAllAssertions(connectedComponentsMap, equivalentTestStmtsPerTestCase, connectedComponent, definitionPreq,
				readValues, allTestStatements, assertions, mainClassName, secondPhasePath, allStmtsView, runningState);
		coverAllProductionCallingStmts(connectedComponentsMap, equivalentTestStmtsPerTestCase, connectedComponent,
				definitionPreq, readValues, allTestStatements, mainClassName, secondPhasePath, allStmtsView,
				runningState);
		
		Settings.consoleLogger.error("second phase finished");

		return secondPhasePath;
	}

	public static void populateStatementField(Map<String, Statement> allASTStatements,
			Map<String, TestStatement> allStatements) {
		for (Entry<String, TestStatement> entry : allStatements.entrySet()) {
			Statement stmt = allASTStatements.get(entry.getKey());
			entry.getValue().statement = stmt;
		}
	}

	private static void coverAllProductionCallingStmts(Map<String, List<String>> connectedComponentsMap,
			Map<String, Map<String, List<String>>> equivalentTestStmtsPerTestCase, Set<String> connectedComponent,
			Map<String, Set<VarDefinitionPreq>> definitionPreq, Map<String, Map<String, String>> readValues,
			Map<String, TestStatement> allTestStatements, String mainClassName, List<TestStatement> secondPhasePath,
			Map<String, Map<String, TestStatement>> allStmtsView, RunningState runningState)
			throws CloneNotSupportedException {
		if (isThereUnCoveredStmts(equivalentTestStmtsPerTestCase, connectedComponent)) {
			for (String testCase : connectedComponent) {
				Map<String, List<String>> uncoveredStmtsOriginal = equivalentTestStmtsPerTestCase.get(testCase);
				if (uncoveredStmtsOriginal != null && uncoveredStmtsOriginal.isEmpty() == false) {
					Map<String, List<String>> uncoveredStmts = Utils.cloneListInMap(uncoveredStmtsOriginal);
					for (Entry<String, List<String>> entry : uncoveredStmts.entrySet()) {
						String testStmt = entry.getKey();
						if (uncoveredStmtsOriginal.containsKey(testStmt) == false)
							continue;
						List<TestStatement> stmts = Planning.backward(allTestStatements.get(testStmt), runningState,
								readValues, connectedComponentsMap, allStmtsView.get(testCase), definitionPreq);

						if (stmts == null) {
							Settings.consoleLogger.error(String.format("Couldn't satisfay %s - %s",
									allTestStatements.get(testStmt), testStmt));
							testCasesToRemove.add(Utils.getTestCaseNameFromTestStatement(testStmt));
							mergingResult.couldntsatisfy = true;
							break;
						}

						for (TestStatement stmt : stmts) {
							TestMerger.markAsCovered(stmt, connectedComponentsMap, equivalentTestStmtsPerTestCase);
						}
						stmts = TestCaseComposer.performRenamingWithRunningState(stmts, connectedComponent,
								mainClassName, readValues, definitionPreq, runningState);
						secondPhasePath.addAll(stmts);

					}
				}
			}
		}
	}

	private static void coverAllAssertions(Map<String, List<String>> connectedComponentsMap,
			Map<String, Map<String, List<String>>> equivalentTestStmtsPerTestCase, Set<String> connectedComponent,
			Map<String, Set<VarDefinitionPreq>> definitionPreq, Map<String, Map<String, String>> readValues,
			Map<String, TestStatement> allTestStatements, Set<String> assertions, String mainClassName,
			List<TestStatement> secondPhasePath, Map<String, Map<String, TestStatement>> allStmtsView,
			RunningState runningState) throws CloneNotSupportedException {
		if (!assertions.isEmpty()) {
			Map<String, Set<String>> assertionView = Planning.getTestCaseTestStatementStringMapping(assertions);
			for (Entry<String, Set<String>> testCaseEntry : assertionView.entrySet()) {

				String testCase = testCaseEntry.getKey();
				Set<String> assertionsToCover = testCaseEntry.getValue();
				outer: for (String assertion : assertionsToCover) {

					List<TestStatement> stmts = Planning.backward(allTestStatements.get(assertion), runningState, 
							readValues, connectedComponentsMap, allStmtsView.get(testCase), definitionPreq);
					if (stmts == null) {
						Settings.consoleLogger.error(
								String.format("Couldn't satisfy %s - %s", allTestStatements.get(assertion), assertion));
						mergingResult.couldntsatisfy = true;
						testCasesToRemove.add(Utils.getTestCaseNameFromTestStatement(assertion));
						continue outer;
					}

					for (TestStatement stmt : stmts) {
						TestMerger.markAsCovered(stmt, connectedComponentsMap, equivalentTestStmtsPerTestCase);
					}
					stmts = TestCaseComposer.performRenamingWithRunningState(stmts, connectedComponent, mainClassName,
							readValues, definitionPreq, runningState);
					secondPhasePath.addAll(stmts);
				}
			}
		}
	}

	private static TwoPair<TestStatement, RunningState, Map<String, String>, LinkedHashSet<String>> performFirstPhaseGreedyAlg(
			Map<String, List<String>> connectedComponentsMap,
			Map<String, Map<String, List<String>>> equivalentTestStmtsPerTestCase,
			Map<String, Set<VarDefinitionPreq>> definitionPreq, Map<String, Map<String, String>> readValues,
			Map<String, TestState> graph, Map<String, TestStatement> allTestStatements, Set<String> assertions,
			RunningState initialState, LinkedList<TestStatement> path, TestStatement rootStmt,
			Set<String> connectedComponent) throws CloneNotSupportedException {

		TwoPair<TestStatement, RunningState, Map<String, String>, LinkedHashSet<String>> frontier = new TwoPair<TestStatement, RunningState, Map<String, String>, LinkedHashSet<String>>(
				rootStmt, initialState, new HashMap<String, String>(), new LinkedHashSet<String>());
		TwoPair<TestStatement, RunningState, Map<String, String>, LinkedHashSet<String>> prevFrontier;

		do {
			prevFrontier = frontier;
			// frontier = dijkstra(frontier.getFirst(), graph,
			// frontier.getSecond(), readValues, connectedComponentsMap,
			// allTestStatements, assertions, definitionPreq);

			// frontier = Planning.forward(frontier, graph, readValues,
			// connectedComponentsMap, allTestStatements,
			// assertions, definitionPreq, 7, 7, connectedComponent);

			frontier = Planning.forward(frontier, graph, readValues, connectedComponentsMap, allTestStatements,
					assertions, definitionPreq, 1, 1, connectedComponent);

			if (frontier == null)
				break;
			TestMerger.markAsCovered(frontier.getFirst(), connectedComponentsMap, equivalentTestStmtsPerTestCase);
			assertions.remove(frontier.getFirst().getName());
			// path.add(frontier.getFirst());
			// path = new LinkedList<TestStatement>();
			// for (String state : frontier.getForth())
			// {
			// path.add(allTestStatements.get(state));
			// }

		} while (frontier != null);

		// path = new LinkedList<TestStatement>();
		for (String state : prevFrontier.getForth()) {
			path.add(allTestStatements.get(state));
		}

		Settings.consoleLogger.error("first phase finished");
		return prevFrontier;
	}

	private static void removingCorruptedTestCases(Set<String> connectedComponent, Set<String> corruptedTestCases) {
		if (corruptedTestCases.size() != 0) {
			Settings.consoleLogger.error("removing corrupted Test Cases " + corruptedTestCases);
		}
		for (String corruptedTest : corruptedTestCases) {
			connectedComponent.remove(corruptedTest);
		}
	}

	private static void writeStatsToFile(List<Set<String>> connectedComponents) throws FileNotFoundException {
		Formatter fr = new Formatter(Settings.SUBJECT + "-stat.csv");
		for (Set<String> connectedComponent : connectedComponents) {
			fr.format("%d,%s\n", connectedComponent.size(), connectedComponent.toString());
		}
		fr.close();
	}

	public static void populateGoalsInStatements(Map<String, Set<VarDefinitionPreq>> definitionPreq,
			Map<String, Map<String, String>> readValues, RunningState runningState, List<TestStatement> stmts) {
		Map<String, Set<String>> readGoals = Planning.initGoal(stmts.get(stmts.size() - 1), readValues);
		Map<String, Set<VarDefinitionPreq>> defineGoals = Planning
				.getTheVarDefMap(definitionPreq.get(stmts.get(stmts.size() - 1).getName()));

		for (int i = stmts.size() - 2; i >= 0; i--) {

			Map<String, Set<String>> newGoals = new HashMap<String, Set<String>>();
			for (Entry<String, Set<String>> entry : readGoals.entrySet()) {
				Utils.addAllTheSetInMap(newGoals, entry.getKey(), entry.getValue());
			}

			Map<String, Set<VarDefinitionPreq>> newDefGoal = new HashMap<String, Set<VarDefinitionPreq>>();
			for (Entry<String, Set<VarDefinitionPreq>> entry : defineGoals.entrySet()) {
				Utils.addAllTheSetInMap(newDefGoal, entry.getKey(), entry.getValue());
			}
			stmts.get(i).defineGoals = newDefGoal;
			stmts.get(i).readGoals = newGoals;
			Map<String, String> renameMap = new HashMap<String, String>();
			Planning.updateGoals(stmts.get(i), readGoals, runningState, defineGoals, readValues, definitionPreq,
					renameMap);
			// stmts.get(i).renameMap = renameMap;
		}
	}

	public static List<TestStatement> getSavedStmts(Map<String, TestStatement> allTestStatements,
			List<TestStatement> path) {
		List<TestStatement> savedStmts = new LinkedList<TestStatement>();

		Set<String> pathSet = new HashSet<String>();

		for (TestStatement stmt : path) {
			pathSet.add(stmt.getName());
		}

		for (TestStatement stmt : allTestStatements.values()) {
			if (!pathSet.contains(stmt.getName())) {
				savedStmts.add(stmt);
			}
		}

		return savedStmts;

	}

	public static Set<String> getAllAssertions(Map<String, TestStatement> allTestStatements) {
		Set<String> assertions = new HashSet<String>();
		for (Entry<String, TestStatement> entry : allTestStatements.entrySet()) {
			TestStatement stmt = entry.getValue();
			if (isAssertion(stmt))
				assertions.add(entry.getKey());
		}

		return assertions;
	}

	public static boolean isAssertion(TestStatement statement) {

		// TODO check for indirect assertion checking
		if (statement.statement == null)
			return false;
		String str = statement.statement.toString();
		if (str.toLowerCase().contains("assert") || str.toLowerCase().contains("check"))
			return true;
		return false;
	}

	public static Triple<TestStatement, RunningState, Map<String, String>> dijkstra(TestStatement root,
			Map<String, TestState> graph, RunningState runningState, Map<String, Map<String, String>> readValues,
			Map<String, List<String>> connectedComponentsMap, Map<String, TestStatement> testStatementMap,
			Set<String> assertions, Map<String, Set<VarDefinitionPreq>> definitionPreq)
			throws CloneNotSupportedException {

		Set<TestStatement> visited = new HashSet<TestStatement>();
		root.curStart = root;
		root.distFrom.put(root, (long) 0);
		PriorityQueue<Triple<TestStatement, RunningState, Map<String, String>>> queue = new PriorityQueue<Triple<TestStatement, RunningState, Map<String, String>>>();

		queue.add(new Triple<TestStatement, RunningState, Map<String, String>>(root, runningState,
				new HashMap<String, String>()));

		while (queue.size() != 0) {
			Triple<TestStatement, RunningState, Map<String, String>> pair = queue.poll();
			TestStatement parent = pair.getFirst();
			runningState = pair.getSecond();
			Map<String, String> batchRename = pair.getThird();
			if (visited.contains(parent))
				continue;

			visited.add(parent);

			if (connectedComponentsMap.containsKey(parent.getName()) || assertions.contains(parent.getName())) {
				Map<String, String> renameClone = new HashMap<String, String>();
				renameClone.putAll(batchRename);
				return new Triple<TestStatement, RunningState, Map<String, String>>(parent, runningState.clone(),
						renameClone);
			}

			TestCaseComposer.updateRunningState(parent, runningState, readValues, definitionPreq, batchRename);
			List<Pair<Integer, TestStatement>> comps = getAllPairCompatibleTestStatements(testStatementMap, readValues,
					runningState, visited, definitionPreq);

			Collections.sort(comps, Collections.reverseOrder());
			for (Pair<Integer, TestStatement> stmtPair : comps) {
				TestStatement stmt = stmtPair.getSecond();
				if (!visited.contains(stmt)) {

					relaxChild(root, queue, parent, stmt, runningState,
							stmtPair.getFirst() + readValues.get(stmt.getName()).size(), batchRename);
				}

			}
		}

		return null;
	}

	private static void relaxChild(TestStatement root,
			PriorityQueue<Triple<TestStatement, RunningState, Map<String, String>>> queue, TestStatement parent,
			TestStatement stmt, RunningState runningState, int bonus, Map<String, String> batchRename)
			throws CloneNotSupportedException {
		long newD = parent.distFrom.get(root) + stmt.time - bonus + stmt.getSideEffects().size() * 1000000
				+ TestCaseComposer.getTestStatementNumber(stmt.getName());
		Long childDist = stmt.distFrom.get(root);
		if (childDist == null || newD < childDist) {
			stmt.parent.put(root, parent);
			stmt.distFrom.put(root, newD);
			stmt.curStart = root;
			// queue.remove(stmt);
			Map<String, String> renameClone = new HashMap<String, String>();
			renameClone.putAll(batchRename);
			queue.add(new Triple<TestStatement, RunningState, Map<String, String>>(stmt, runningState.clone(),
					renameClone));
			// queue.add(child.clone());
		}
	}

	public static List<Pair<Integer, TestStatement>> getAllPairCompatibleTestStatements(
			Map<String, TestStatement> allTestStatements, Map<String, Map<String, String>> readValues,
			RunningState runningState, Set<TestStatement> visited, Map<String, Set<VarDefinitionPreq>> definitionPreq) {
		List<Pair<Integer, TestStatement>> comps = new ArrayList<Pair<Integer, TestStatement>>();
		for (TestStatement stmt : allTestStatements.values()) {
			if (stmt == null || (visited != null && visited.contains(stmt)))
				continue;
			Map<String, String> readVals = readValues.get(stmt.getName());
			if (readVals == null)
				continue;
			// if (stmt.equals("Array2DRowRealMatrixTest.testSetRow-1.xml"))
			// System.out.println();
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
				comps.add(new Pair<Integer, TestStatement>(readVals.size(), stmt));

		}

		return comps;
	}

	public static Map<String, Map<String, String>> getAllReadValues(List<String> testCases,
			Map<String, Set<VarDefinitionPreq>> definitionPreq, Set<String> corruptedTestCases,
			Map<String, Statement> allASTStatements) throws IOException, SAXParseException {

		Map<String, Map<String, String>> readValues = new HashMap<String, Map<String, String>>();
		for (String testCase : testCases) {
			// state1 -> <a, b, c>
			String path = Utils.getTestCaseFile(testCase);
			if (path == null) {
				Settings.consoleLogger.error("path to " + testCase + " is missing");
				corruptedTestCases.add(testCase);
				continue;
			}
			Map<String, Set<SimpleName>> readVars = ReadVariableDetector.populateReadVarsForTestCaseOfFile(path,
					testCase, definitionPreq, allASTStatements);
			// System.out.println(readVars);
			// ReadVariableDetector.accumulateReadVars(readVars);

			// state1 ->
			// <object1(a), field 1, field 2, ... >
			// <object2(a), field 1, field 2, ... >
			// <object3(a), field 1, field 2, ... >
			ReadVariableDetector.getReadValues(readVars, readValues, corruptedTestCases); //TODO
		}
		for (Iterator it = readValues.entrySet().iterator(); it.hasNext();) {
			Entry<String, Map<String, String>> entry = (Entry<String, Map<String, String>>) it.next();
			String stateName = entry.getKey();
			if (corruptedTestCases.contains(Utils.getTestCaseNameFromTestStatement(stateName))) {
				it.remove();
			}
		}
		return readValues;
	}

}
