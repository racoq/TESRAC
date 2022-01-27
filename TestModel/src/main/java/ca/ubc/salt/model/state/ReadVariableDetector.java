package ca.ubc.salt.model.state;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jface.text.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXParseException;

import ca.ubc.salt.model.instrumenter.ClassModel;
import ca.ubc.salt.model.instrumenter.Method;
import ca.ubc.salt.model.utils.FileUtils;
import ca.ubc.salt.model.utils.Settings;
import ca.ubc.salt.model.utils.Utils;
import ca.ubc.salt.model.utils.XMLUtils;

public class ReadVariableDetector {

	// public static void main(String[] args) throws IOException
	// {
	// populateReadVarsForFile(
	// "/Users/Arash/Research/repos/commons-math/src/test/java/org/apache/commons/math4/fraction/FractionTest.java");
	// }

	public static Map<String, Set<SimpleName>> populateReadVarsForFile(String path,
			Map<String, Set<VarDefinitionPreq>> definitionPreq, Map<String, Statement> allASTStatements)
			throws IOException {
		
		File testClass = new File(path);
		if (testClass.isFile()) {
			if (!Utils.isTestClass(testClass))
				return null;

			String source = FileUtils.readFileToString(testClass);
			Document document = new Document(source);
			List<ClassModel> classes = ClassModel.getClasses(document.get(), true, path,
					new String[] { Settings.PROJECT_PATH }, new String[] { Settings.LIBRARY_JAVA });

			Map<String, Set<SimpleName>> readVars = new HashMap<String, Set<SimpleName>>();

			for (ClassModel clazz : classes)
				populateReadVarsForClass(clazz, null, document, readVars, definitionPreq, allASTStatements);
			return readVars;

		}
		return null;
		// TODO Do I need to handle it ?!
		// else if (testClass.isDirectory())
		// {
		// File[] listOfFiles = testClass.listFiles();
		// for (int i = 0; i < listOfFiles.length; i++)
		// {
		// populateReadVarsForFile(listOfFiles[i].getAbsolutePath());
		// }
		// }
	}

	public static void accumulateReadVars(Map<String, Set<SimpleName>> readVars) {
		List<String> statements = new ArrayList(readVars.keySet());
		Collections.sort(statements);

		for (int i = statements.size() - 2; i >= 0; i--) {
			String statement = statements.get(i);
			// String testCase =
			// Utils.getTestCaseNameFromTestStatement(statement);

			String childStmt = statements.get(i + 1);
			// String childStmtTestCase =
			// Utils.getTestCaseNameFromTestStatement(childStmt);

			// if (testCase.equals(childStmtTestCase))
			readVars.get(statement).addAll(readVars.get(childStmt));
		}

	}

	public static String[] getSources(String path, String testCase) {
		String className = Utils.getTestClassNameFromTestCase(testCase);
		List<String> parentPaths = Utils.getAllParentsPaths(className);
		String[] arr = new String[parentPaths.size() + 1];
		arr[0] = path;
		for (int i = 1; i < arr.length; i++) {
			arr[i] = parentPaths.get(i - 1);
		}
		return arr;
	}

	public static Map<String, Set<SimpleName>> populateReadVarsForTestCaseOfFile(String path, String testcase,
			Map<String, Set<VarDefinitionPreq>> definitionPreq, Map<String, Statement> allASTStatements)
			throws IOException {
		
		File testClass = new File(path);
		if (testClass.isFile()) {
			if (!Utils.isTestClass(testClass))
				return null;

			String source = FileUtils.readFileToString(testClass);
			Document document = new Document(source);
			List<ClassModel> classes = ClassModel.getClasses(document.get(), true, Utils.getUnitName(path),
					new String[] { Settings.PROJECT_PATH },
					new String[] { Settings.LIBRARY_JAVA, Settings.PROJECT_PATH + "\\target" });

			Map<String, Set<SimpleName>> readVars = new HashMap<String, Set<SimpleName>>();

			for (ClassModel clazz : classes)
				populateReadVarsForTestCaseOfClass(clazz, null, document, readVars, testcase, definitionPreq,
						allASTStatements);
			return readVars;

		}
		return null;
		// TODO Do I need to handle it ?!
		// else if (testClass.isDirectory())
		// {
		// File[] listOfFiles = testClass.listFiles();
		// for (int i = 0; i < listOfFiles.length; i++)
		// {
		// populateReadVarsForFile(listOfFiles[i].getAbsolutePath());
		// }
		// }
	}

	public static void populateReadVarsForClass(ClassModel srcClass, List<String> loadedClasses, Document document,
			Map<String, Set<SimpleName>> readVars, Map<String, Set<VarDefinitionPreq>> definitionPreq,
			Map<String, Statement> allASTStatements) {
		if (readVars == null)
			readVars = new HashMap<String, Set<SimpleName>>();
		List<Method> methods = srcClass.getMethods();

		for (Method m : methods) {
			m.populateReadVars(document, loadedClasses, readVars, definitionPreq, allASTStatements);
		}

	}

	public static void populateReadVarsForTestCaseOfClass(ClassModel srcClass, List<String> loadedClasses,
			Document document, Map<String, Set<SimpleName>> readVars, String testcase,
			Map<String, Set<VarDefinitionPreq>> definitionPreq, Map<String, Statement> allASTStatements) {
		if (readVars == null)
			readVars = new HashMap<String, Set<SimpleName>>();
		List<Method> methods = srcClass.getMethods();

		for (Method m : methods) {
			if (m.getMethodDec().getName().toString().equals(Utils.getTestCaseName(testcase))) {
				m.populateReadVars(document, loadedClasses, readVars, definitionPreq, allASTStatements);
				break;
			}
		}

	}

	public static void getReadValues(Map<String, Set<SimpleName>> readVars, Map<String, Map<String, String>> readValues,
			Set<String> corruptedTestCases) throws SAXParseException {
		for (Entry<String, Set<SimpleName>> entry : readVars.entrySet()) {
			String stateName = entry.getKey();
			Map<String, String> readValuesForState = getReadValuesOfState(stateName, entry.getValue());
			if (readValuesForState == null) {
				corruptedTestCases.add(Utils.getTestCaseNameFromTestStatement(stateName));
				return;
			} else {
				readValues.put(stateName, readValuesForState);
			}
		}
	}

	public static Map<String, String> getReadValuesOfState(String stateName, Set<SimpleName> readVars) 
		throws SAXParseException {
		Map<String, String> varStateSet = new HashMap<String, String>();
		String varXML = FileUtils.getVars(stateName);
		// TODO double check what to return
		if (varXML == null)
			return varStateSet;
		Set<String> readVarNames = Utils.getNameSet(readVars);
		List<String> stateVarNames = XMLUtils.getVars(varXML);
		int index = 0;
		NodeList nodeList = XMLUtils.getNodeList(stateName);
		LinkedList<String> key = new LinkedList<String>();
		if (nodeList != null) {
			for (String stateVar : stateVarNames) {
				if (readVarNames.contains(stateVar)) {
					varStateSet.put(stateVar, XMLUtils.getXMLString(nodeList.item(index)));
					// processObjectValues(nodeList.item(index), varStateSet,
					// key,
					// stateName);
				}
				index++;
			}
		} else {
			if (stateVarNames.size() > 0)
				return null;
		}
		if (stateVarNames.size() != index) {
			Settings.consoleLogger.error("XStream error with " + stateName);
		}
		return varStateSet;
	}

	public static void processObjectValues(Node object, Set<List<String>> varStateSet, LinkedList<String> key,
			String stateName) {

		key.add(object.getNodeName());

		NodeList children = object.getChildNodes();
		if (children.getLength() == 0) {
			key.add(object.getNodeValue());
			varStateSet.add((List) key.clone());
			key.removeLast();
		} else {
			for (int i = 0; i < children.getLength(); i++) {
				processObjectValues(children.item(i), varStateSet, key, stateName);
			}
		}

		key.removeLast();
	}

}
