package tesrac.reporting;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import javax.xml.parsers.ParserConfigurationException;

import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import tesrac.utils.Logger;
import tesrac.utils.XMLUtils;

/**
 * This class contains methods to get the info stored in the OpenClover generated reports
 * @author JBecho
 */
public class ReportReader {

	private static final Logger logger = Logger.getInstance(ReportReader.class.getName());

	/*
	 * the path for the OpenClover reports
	 */
	private static final String CLOVER_REPORT_PATH = "\\target\\site\\clover\\";
	
	/*
	 * the path for the PIT reports
	 */
	private static final String PIT_REPORT_PATH = "\\target\\pit-reports\\";

	/*
	 * the OpenClover XML report name
	 */
	private static final String XML_FILENAME = "clover.xml";

	/*
	 * the OpenClover JSON report parent file name
	 */
	private static final String JSON_PROJECT = "project.js";

	/*
	 * the key associated with the value of the total percentage of the coverage
	 */
	private static final String JSON_TOTALPERCENT_KEY = "TotalPercentageCovered";

	/*
	 * the key associated with the object that contains the metrics to analyze
	 */
	private static final String JSON_STATS_KEY = "stats";

	/**
	 * Returns the project coverage written in the OpenClover report
	 * @param projPath - 
	 * @return
	 * @throws IOException
	 */
	public static double getProjectCoverage(String projPath) throws IOException {
		String cloverPath = projPath + CLOVER_REPORT_PATH;
		JSONObject project = getJSONFromPath(cloverPath+JSON_PROJECT);
		JSONObject projectStats = project.getJSONObject(JSON_STATS_KEY);
		return projectStats.getDouble(JSON_TOTALPERCENT_KEY);
	}
	
	/**
	 * Returns the mutation coverage from the PIT report
	 * @param projPath
	 * @return
	 * @throws IOException
	 */
	public static double getMutationCoverage(String projPath) throws IOException {
		File [] pitReports = new File(projPath + PIT_REPORT_PATH).listFiles();
		String indexPath = pitReports[pitReports.length-1].getAbsolutePath() + "\\index.html";
		File index = new File(indexPath);
		if(index.exists()) {
			org.jsoup.nodes.Document htmlFile = Jsoup.parse(index, "ISO-8859-1"); 
			String text = htmlFile.body().getElementsByTag("td").get(2).text();
			return Integer.parseInt(text.substring(0, text.indexOf('%')));
		}
		return 0;
	}

	/**
	 * Returns a List with the rows retrieved from the OpenClover report 
	 * containing the code metrics related to the source classes
	 * @param projectPath - The path to the project directory
	 * @return List<SrcReportRow>
	 * @throws IOException
	 */
	public static List<SrcReportRow> readSrcRows(String projectPath, boolean reduced) throws IOException{
		String reportPath = projectPath + CLOVER_REPORT_PATH ;
		logger.info("Getting source classes data from " + reportPath);
		JSONObject project = getJSONFromPath(reportPath + JSON_PROJECT);		
		return ReportRowBuilder.getSrcsRow(reportPath, project, reduced);
	}

	/*
	 * Returns the JSONObject related to the json File in the given path
	 */
	private static JSONObject getJSONFromPath(String path) throws IOException {
		String fileContent = new String(Files.readAllBytes(Paths.get(path)));
		String json = fileContent.substring(17, fileContent.length()-5);
		return new JSONObject(json);
	}

	/**
	 * Returns a List with the rows retrieved from the OpenClover report 
	 * containing the code metrics related to the test classes
	 * @param projectPath - the path to the project directory
	 * @return List<TestReportRow>
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 */
	public static List<TestReportRow> readTestRows(String projectPath, boolean reduced) 
			throws ParserConfigurationException, SAXException, IOException {
		String reportPath = projectPath + CLOVER_REPORT_PATH;
		logger.info("Getting test classes data from " + reportPath);
		Document doc = XMLUtils.getXMLDoc(reportPath+XML_FILENAME);
		return ReportRowBuilder.getTestRows(doc, reduced);
	}

	/**
	 * Returns the project coverage from a comparative report
	 * @param report - the file to parse
	 * @param reduced - whether we want the reduced coverage or original
	 * @return double 
	 * @throws FileNotFoundException
	 */
	public static double getProjectCoverage(File report, boolean reduced) throws FileNotFoundException {
		Scanner sc = new Scanner(report);		
		if(reduced) 
			sc.nextLine();
		String coverage = sc.nextLine();
		double cov = Double.parseDouble(coverage.split(";")[1]);
		sc.close();
		return cov;
	}

	/**
	 * Returns the mutation coverage from a comparative report
	 * @param report
	 * @param reduced
	 * @return
	 * @throws FileNotFoundException 
	 * @throws IOException
	 */
	public static int getMutationCoverage(File report, boolean reduced) throws FileNotFoundException  {
		Scanner sc = new Scanner(report);
		sc.nextLine(); sc.nextLine(); //ignore total coverage lines
		if(reduced)
			sc.nextLine(); //ignore original mutation coverage
		String coverage = sc.nextLine();
		sc.close();
		return Integer.parseInt(coverage.split(";")[1]);
	}
	
	/**
	 * Returns a list of the SrcReportRow from a report File
	 * @param report the file 
	 * @param reduced whether we want the original or reduced SrcRows
	 * @return List<SrcReportRow>
	 * @throws FileNotFoundException
	 */
	public static List<SrcReportRow> readSrcRows(File report, boolean reduced) throws FileNotFoundException {
		ArrayList<SrcReportRow> srcRows = new ArrayList<>();
		Scanner sc = new Scanner(report);
		String type = (reduced) ? "Reduced" : "Not Reduced";	
		
		//Jump project coverage and header
		for(int i = 1; i <= 3; i++)
			sc.nextLine();
		String testHeader = TestReportRow.HEADER;
		String line;
		while(!(line = sc.nextLine()).equals(testHeader)) {
			if(line.startsWith(type))
				srcRows.add(new SrcReportRow(line));
		}
		sc.close();
		return srcRows;
	}

	/**
	 * Returns a list of the TestReportRow from a report File
	 * @param report the file 
	 * @param reduced whether we want the original or reduced TestRows
	 * @return List<TestReportRow>
	 * @throws FileNotFoundException
	 */
	public static List<TestReportRow> readTestRows(File report, boolean reduced) throws FileNotFoundException {
		ArrayList<TestReportRow> tstRows = new ArrayList<>();
		Scanner sc = new Scanner(report);	
		String type = (reduced) ? "Reduced" : "Not Reduced";
		String testHeader = TestReportRow.HEADER;
		String line;
		while(!(line = sc.nextLine()).equals(testHeader)) {
		}
		
		while(sc.hasNextLine()) {
			line = sc.nextLine();
			if(line.startsWith(type))
				tstRows.add(new TestReportRow(line));
		}
		sc.close();
		return tstRows;
	}

	/**
	 * Accumulates the values in the given list into a compact SrcRow
	 * @param srcRows the list of SrcRows
	 * @return SrcReportRow
	 */
	public static SrcReportRow compactSrcRow(List<SrcReportRow> srcRows) {
		return ReportRowBuilder.compactSrcRow(srcRows);
	}

	/**
	 * Accumulates the values in the given list into a compact TestRow
	 * @param srcRows the list of TestRows
	 * @return TestReportRow
	 */
	public static TestReportRow compactTstRow(List<TestReportRow> tstRows) {
		return ReportRowBuilder.compactTstRow(tstRows);
	}

}