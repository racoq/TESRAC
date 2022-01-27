//package ca.ubc.salt.model.instrumenter;
//
//import java.io.File;
//import java.io.FileInputStream;
//import java.io.FileOutputStream;
//import java.io.FileWriter;
//import java.io.IOException;
//import java.io.ObjectInputStream;
//import java.io.ObjectOutputStream;
//import java.util.ArrayList;
//import java.util.Formatter;
//import java.util.HashMap;
//import java.util.HashSet;
//import java.util.LinkedList;
//import java.util.List;
//import java.util.Map;
//import java.util.Set;
//
//import org.eclipse.jdt.core.dom.Statement;
//import org.junit.runner.RunWith;
//import org.junit.runners.Parameterized;
//import org.xml.sax.SAXParseException;
//
//import com.thoughtworks.xstream.XStream;
//import com.thoughtworks.xstream.io.xml.StaxDriver;
//
//import ca.ubc.salt.model.merger.BackwardTestMerger;
//import ca.ubc.salt.model.state.ProductionCallingTestStatement;
//import ca.ubc.salt.model.state.VarDefinitionPreq;
//import ca.ubc.salt.model.utils.Settings;
//import ca.ubc.salt.model.utils.Utils;
//
//@RunWith(value = Parameterized.class)
//public class InstrumentedTestSuiteExecutor {
//
//	public static void main(String[] args) throws Exception {
//
////		Settings.consoleLogger.error("Deleting old traces");
////		deleteTraces();
////		Settings.consoleLogger.error("Running instrumented project");
////		long startTime = System.currentTimeMillis();
////		
////		runInstrumentedProject();
////		
////		long stopTime = System.currentTimeMillis();
////		long elapsedTime = stopTime - startTime;
////		System.out.println("Running instrumented test suite in: " + elapsedTime / 1000);
////
////		Settings.consoleLogger.error("Check trace consistency");
//		checkTraceConsistency();
//	}
//
//	public static void checkTraceConsistency() throws IOException, ClassNotFoundException, SAXParseException {
//
//		// delete the unnecessary files
//		Utils.cleanProjectBeforeMerging();
//
//		Instrumenter.loadStructs();
//		Formatter formatter = new Formatter(Settings.SUBJECT + "-mergingStat.csv");
//		formatter.format(
//				"merging test,merged test class,merged test case,before,after,saved,fatal error,warning,couldn't satisfy\n");
//		XStream xstream = new XStream(new StaxDriver());
//
//		File file = new File(Settings.SUBJECT + "-components.xml");
//		List<Set<String>> connectedComponents = null;
//		Map<String, List<String>> connectedComponentsMap = null;
//
//		if (!file.exists()) {
//			long setupCost = 10;
//			List<Map<String, List<String>>> uniqueTestStatementSet = new ArrayList<Map<String, List<String>>>();
//			for (String paramClass : Instrumenter.parameterizedClasses) {
//				uniqueTestStatementSet
//						.add(ProductionCallingTestStatement.getUniqueTestStatementsForTestClass(paramClass));
//			}
//			Map<String, List<String>> uniqueTestStatements = ProductionCallingTestStatement.getUniqueTestStatements();
//			uniqueTestStatementSet.add(uniqueTestStatements);
//			connectedComponents = ProductionCallingTestStatement.getTestCasesThatShareTestStatement(1,
//					uniqueTestStatementSet);
//
//			connectedComponentsMap = ProductionCallingTestStatement.convertTheSetToMap(uniqueTestStatementSet);
//
//			String components = xstream.toXML(connectedComponents);
//
//			FileWriter fw = new FileWriter(Settings.SUBJECT + "-components.xml");
//			fw.write(components);
//			fw.close();
//
//			ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(Settings.SUBJECT + "-unique.xml"));
//			out.writeObject(connectedComponentsMap);
//			out.close();
//
//		} else {
//			connectedComponents = (List<Set<String>>) xstream.fromXML(new File(Settings.SUBJECT + "-components.xml"));
//			ObjectInputStream in = new ObjectInputStream(new FileInputStream(Settings.SUBJECT + "-unique.xml"));
//			connectedComponentsMap = (Map<String, List<String>>) in.readObject();
//			in.close();
//		}
//
//		for (Set<String> connectedComponent : connectedComponents) {
//
//			List<String> testCases = new LinkedList<String>();
//			testCases.addAll(connectedComponent);
//
//			Map<String, Set<VarDefinitionPreq>> definitionPreq = new HashMap<String, Set<VarDefinitionPreq>>();
//			Set<String> corruptedTestCases = new HashSet<String>();
//
//			Map<String, Statement> allASTStatements = new HashMap<String, Statement>();
//			Map<String, Map<String, String>> readValues = BackwardTestMerger.getAllReadValues(testCases, definitionPreq,
//					corruptedTestCases, allASTStatements);
//		}
//
//	}
//
//	private static void deleteTraces() throws Exception {
//		String traceDir = Settings.PROJECT_INSTRUMENTED_PATH + "/traces";
//
//		// clear the traces
//		File f = new File(traceDir);
//
//		if (!f.isDirectory()) {
//			throw new Exception("Traces directory not found");
//		} else {
//			File[] traces = f.listFiles();
//			for (File trace : traces) {
//				if (!trace.getName().equals("init.init-.xml") && !trace.getName().equals("staticLoading.xml")) {
//					if (trace.delete()) {
//						// System.out.println("Deleted trace " +
//						// trace.getName());
//					}
//				}
//
//			}
//		}
//	}
//
//	private static void runInstrumentedProject() {
//		// run the instrumented project
//		String[] cmdRun = new String[] { "/usr/local/Cellar/maven/3.3.9/bin/mvn", "test" };
//		try {
//			System.out.println(Utils.runCommand(cmdRun, Settings.PROJECT_INSTRUMENTED_PATH + "/"));
//		} catch (IOException | InterruptedException e) {
//			e.printStackTrace();
//		}
//	}
//
//}
