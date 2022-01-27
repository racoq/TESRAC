package tesrac.reporting;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import tesrac.utils.XMLUtils;

/**
 * This class contains methods to parse the OpenClover generated XML and JSON reports
 * @author JBecho
 *
 */
public class ReportRowBuilder {

	// The key, tags and attributes names found in the XML and JSON reports
	private static final String JSON_PACKAGE = "package.js";
	private static final String JSON_EXTENSION = ".js";
	private static final String JSON_CHILDREN_KEY = "children";
	private static final String JSON_STATS_KEY = "stats";
	private static final String JSON_COMPLEXITY_KEY = "Complexity";
	private static final String JSON_CBRANCHES_KEY = "CoveredBranches";
	private static final String JSON_TBRANCHES_KEY = "TotalBranches";
	private static final String JSON_CSTATEMENTS_KEY = "CoveredStatements";
	private static final String JSON_TSTATEMENTS_KEY = "TotalStatements";
	private static final String JSON_CELEMENTS_KEY = "CoveredElements";
	private static final String JSON_TELEMENTS_KEY = "TotalElements";
	private static final String JSON_TOTALPERCENT_KEY = "TotalPercentageCovered";
	private static final String XML_PROJECT_TAG = "testproject";
	private static final String XML_PACKAGE_TAG = "package";
	private static final String XML_FILE_TAG = "file";
	private static final String XML_CLASS_TAG = "class";
	private static final String XML_METRICS_TAG = "metrics";
	private static final String XML_NAME_ATTR = "name";
	private static final String XML_PATH_ATTR = "path";
	private static final String XML_NCLOC_ATTR = "ncloc";
	private static final String XML_TIME_ATTR = "testduration";
	private static final String XML_PASSED_ATTR = "testpasses";
	private static final String XML_FAILED_ATTR = "testfailures";
	private static final String XML_TOTAL_ATTR = "testruns";

	/**
	 * Parses the JSON report to build a List of SrcReportRow containing the info for each src class
	 * @param path - the path to the report file
	 * @param project - the JSONObject
	 * @return List<SrcReportRow>
	 * @throws IOException
	 */
	public static List<SrcReportRow> getSrcsRow(String path, JSONObject project, boolean reduced) throws IOException {
		ArrayList<SrcReportRow> rows = new ArrayList<SrcReportRow>();
		JSONArray packages = project.getJSONArray(JSON_CHILDREN_KEY);
		for(int i = 0; i < packages.length(); i++) {
			String pathToPkg = path+packages.getString(i) /*.replace('/', '\\')*/;
			JSONObject pkg = getJSONFromPath(pathToPkg + JSON_PACKAGE);
			JSONArray classes = pkg.getJSONArray(JSON_CHILDREN_KEY);
			for(int j = 0; j < classes.length(); j++) {
				String className = classes.getString(j);
				String pathToClass = pathToPkg+className+JSON_EXTENSION;
				JSONObject aClass = getJSONFromPath(pathToClass);
				SrcReportRow r = getSrcRowFromJSONObject(aClass, className, reduced);
				rows.add(r);
			}
		}
		return rows;
	}

	/*
	 * Returns a SrcReportRow containing the metrics from a specific class
	 */
	private static SrcReportRow getSrcRowFromJSONObject(JSONObject aClass, String className, boolean reduced) {
		JSONObject metrics = aClass.getJSONObject(JSON_STATS_KEY);
		int complexity = metrics.has(JSON_COMPLEXITY_KEY) ? metrics.getInt(JSON_COMPLEXITY_KEY) : -1;
		int totalStatements = metrics.has(JSON_TSTATEMENTS_KEY) ? metrics.getInt(JSON_TSTATEMENTS_KEY) : 0;
		int coveredStatements = metrics.has(JSON_CSTATEMENTS_KEY) ? metrics.getInt(JSON_CSTATEMENTS_KEY) : 0;
		int totalBranches = metrics.has(JSON_TBRANCHES_KEY) ? metrics.getInt(JSON_TBRANCHES_KEY) : 0;
		int coveredBranches = metrics.has(JSON_CBRANCHES_KEY) ? metrics.getInt(JSON_CBRANCHES_KEY) : 0;
		int totalElements = metrics.has(JSON_TELEMENTS_KEY) ? metrics.getInt(JSON_TELEMENTS_KEY) : 0;
		int coveredElements = metrics.has(JSON_CELEMENTS_KEY) ? metrics.getInt(JSON_CELEMENTS_KEY) : 0;
		double totalPercentageCoverage = metrics.has(JSON_TOTALPERCENT_KEY) ? metrics.getDouble(JSON_TOTALPERCENT_KEY) : 0; 
		int totalMethods = totalElements - (totalBranches + totalStatements);
		int coveredMethods = coveredElements - (coveredBranches + coveredStatements);
		double pcCoveredBranches = totalBranches!=0 ? (((double)coveredBranches/totalBranches)*100) : 0;
		double pcCoveredStatements = totalStatements != 0 ? (((double)coveredStatements/totalStatements)*100) : 0;
		double pcCoveredMethods = totalMethods != 0 ? (((double)coveredMethods/totalMethods)*100) : 0;
		return new SrcReportRow(reduced, className, complexity, totalStatements, coveredStatements,
				totalBranches, coveredBranches, totalMethods, coveredMethods, pcCoveredBranches, 
				pcCoveredStatements, pcCoveredMethods, totalPercentageCoverage);
	}

	/*
	 * Returns the JSONObject related to the json file denoted by the given path
	 */
	private static JSONObject getJSONFromPath(String path) throws IOException {
		String fileContent = new String(Files.readAllBytes(Paths.get(path)));
		String json = fileContent.substring(17, fileContent.length()-5);
		return new JSONObject(json);
	}

	/**
	 * Returns List of TestReportRow containing the relevant info from the OpenCloverReport
	 * @param doc
	 * @return
	 */
	public static List<TestReportRow> getTestRows(Document doc, boolean reduced){
		ArrayList<TestReportRow> testRows = new ArrayList<TestReportRow>();
		NodeList testProjectChilds = doc.getElementsByTagName(XML_PROJECT_TAG).item(0).getChildNodes();
		for(int i = 0; i < testProjectChilds.getLength(); i++) {
			Node child = testProjectChilds.item(i);
			if(child.getNodeName().equals(XML_PACKAGE_TAG)) {
				testRows.addAll(getTestRowsFromPackage(child, reduced));
			}
		}
		return testRows;
	}

	/*
	 * Returns an ArrayList of TestReportRow with the info from each test class
	 */
	private static ArrayList<TestReportRow> getTestRowsFromPackage(Node pkg, boolean reduced){
		ArrayList<TestReportRow> testRows = new ArrayList<TestReportRow>();
		NodeList files = pkg.getChildNodes();
		for(int i = 0; i < files.getLength(); i++) {
			Node child = files.item(i);
			if(child.getNodeName().equals(XML_FILE_TAG))
				testRows.addAll(getTestRowsFromFile(child, reduced));
		}
		return testRows;
	}

	/*
	 * Returns an ArrayList of TestReportRow with the info from each file
	 */
	private static ArrayList<TestReportRow> getTestRowsFromFile(Node fileNode, boolean reduced) {
		Element fileElem = (Element) fileNode;
		Node metricsNode = XMLUtils.getUniqueChild(fileNode, XML_METRICS_TAG);
		Element metrics = (Element) metricsNode;
		String filePath = fileElem.getAttribute(XML_PATH_ATTR);
		String ncloc = metrics.getAttribute(XML_NCLOC_ATTR);
		File file = new File(filePath);
		String fileSize = Long.toString(file.length());
		ArrayList<TestReportRow> testRows = new ArrayList<TestReportRow>();
		NodeList fileChilds = fileNode.getChildNodes();
		for(int i = 0; i < fileChilds.getLength(); i++) {
			Node child = fileChilds.item(i);
			if(child.getNodeName().equals(XML_CLASS_TAG))
				testRows.add(getTestRow(child, fileSize, ncloc, reduced));
		}
		return testRows;
	}

	/*
	 * Returns a TestReportRow with the info from a <metrics> tag 
	 */
	private static TestReportRow getTestRow(Node classNode, String fileSize, String ncloc, boolean reduced) {
		Node metricsNode = XMLUtils.getUniqueChild(classNode, XML_METRICS_TAG);
		Element metrics = (Element) metricsNode;
		Element classElem = (Element) classNode;
		return new TestReportRow(
				reduced, 
				classElem.getAttribute(XML_NAME_ATTR),
				Integer.parseInt(ncloc),
				Double.parseDouble(metrics.getAttribute(XML_TIME_ATTR)),
				Integer.parseInt(metrics.getAttribute(XML_PASSED_ATTR)),
				Integer.parseInt(metrics.getAttribute(XML_FAILED_ATTR)),
				Integer.parseInt(metrics.getAttribute(XML_TOTAL_ATTR)),
				Integer.parseInt(fileSize)
				);
	}

	/**
	 * Accumulates the values in the given list into a compact TestRow
	 * (only the needed metrics for the reduction score computation)
	 * @param srcRows the list of TestRows
	 * @return TestReportRow
	 */
	public static TestReportRow compactTstRow(List<TestReportRow> tstRows) {

		double time = 0;
		int fileSize = 0;
		int nrTests = 0;	
		for(TestReportRow r : tstRows) {
			if(r.getNrTestCases() == 0)
				continue;
			fileSize += r.getFileSize();
			nrTests += r.getNrTestCases();
			time += r.getTime();
		}
		return new TestReportRow(tstRows.get(0).isReduced(), "Compact", -1,
				time, -1, -1, nrTests, fileSize);
	}

	/**
	 * Accumulates the values in the given list into a compact SrcRow
	 * (only the needed metrics for the reduction score computation)
	 * @param srcRows the list of SrcRows
	 * @return SrcReportRow
	 */
	public static SrcReportRow compactSrcRow(List<SrcReportRow> srcRows) {
		double pcBranches = 0;
		double pcTotCov = 0;
		for(SrcReportRow r : srcRows) {
			pcBranches += r.getPcBranches();
			pcTotCov += r.getTotalCov();
		}
		pcBranches /= srcRows.size();
		pcTotCov /= srcRows.size();
		return new SrcReportRow(srcRows.get(0).isReduced(), "All", -1, 
				-1, -1, -1, -1, -1, -1, pcBranches, -1, -1, pcTotCov);
	}

}
