package tesrac;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import tesrac.reporting.ReportReader;
import tesrac.mcdm.Mcdm;
import tesrac.mcdm.Score;
import tesrac.reporting.CloverRunner;
import tesrac.reporting.PitRunner;
import tesrac.reporting.SrcReportRow;
import tesrac.reporting.TestReportRow;
import tesrac.tools.EvoSuite;
import tesrac.tools.Kanonizo;
import tesrac.tools.TSRTool;
import tesrac.utils.Configuration;
import tesrac.utils.FileUtils;
import tesrac.utils.Logger;
import tesrac.utils.Pair;
import tesrac.utils.Subject;

/**
 * The Main class
 * 
 * @author JBecho
 *
 */
public class Main {

	private static final Logger logger = Logger.getInstance(Main.class.getName());

	// template message for tool error
	private static final String toolErrorTemplate = "%s failed to run on %s";

	//The map in which the execution times of the tools are saved
	private static Map<String, ArrayList<Pair<String, Long>>> times;

	private static Mcdm mcdm;

	public static void main( String[] args )  {

		long start = System.currentTimeMillis();
		
		//load the criteria weights
		mcdm = new Mcdm(Configuration.getCriteria());

		try {
			if(Configuration.getMode().equals("Analyze")) {
				analyzeOnly();
			} else if(Configuration.getMode().equals("Full")){
				fullMode();
			} 
			long duration = System.currentTimeMillis() - start;		
			
			writeOutput(duration/1000, Configuration.getSubjects());
			
			
		} catch (IOException | InterruptedException | ParserConfigurationException | SAXException e) {
			logger.error("There was a problem executing the tool: " + e.getMessage());
			e.printStackTrace();
		}

	}

	/*
	 * Analyze only execution
	 * Using the already generated comparative reports with the data gathered by OpenClover
	 * process their data and rank the tools according to it
	 */
	private static void analyzeOnly() throws FileNotFoundException, InterruptedException {
		String reportsPath = Configuration.getReportsPath();
		File reportsFolder = new File(reportsPath);
		File[] reports = reportsFolder.listFiles();
		logger.info("Found " + reports.length + " reports");
		ArrayList<Subject> subjects = Configuration.getSubjects();
		ArrayList<String> tools = getDistinguishedTools();
		
		//For each OpenClover comparative report found
		for(File report : reports) {
			String name = report.getName().substring(0, report.getName().indexOf('.'));
			String[] info = name.split("_");
			String subject = info[0];
			if(subjects.stream().filter(x -> x.getProjectName().equals(subject)).count() == 0L)
				continue;
			String tool = info[1];
			if(tools.stream().filter(x -> x.equals(tool)).count() == 0L)
				continue;
			
			double origCoverage;
			double redCoverage;
			int origMutScore;
			int redMutScore;
			List<SrcReportRow> origSrcRows;
			List<TestReportRow> origTstRows;
			List<SrcReportRow> redSrcRows;
			List<TestReportRow> redTstRows;
			try {
				
				//Get the data gathered by OpenClover for the original and reduced test suites
				origCoverage = ReportReader.getProjectCoverage(report, false);
				origSrcRows = ReportReader.readSrcRows(report, false);
				origTstRows = ReportReader.readTestRows(report, false);
				origMutScore = ReportReader.getMutationCoverage(report, false);

				redCoverage = ReportReader.getProjectCoverage(report, true);	
				redSrcRows = ReportReader.readSrcRows(report, true);
				redTstRows = ReportReader.readTestRows(report, true);
				redMutScore = ReportReader.getMutationCoverage(report, true); 
				
				//Accumulate the data gathered into one row for the original and reduced suites
				SrcReportRow rSrcRow = ReportReader.compactSrcRow(redSrcRows);
				SrcReportRow nrSrcRow = ReportReader.compactSrcRow(origSrcRows);
				TestReportRow rTstRow = ReportReader.compactTstRow(redTstRows);
				TestReportRow nrTstRow = ReportReader.compactTstRow(origTstRows);
				
				//Saving the values for this project-tool
				mcdm.saveValues(tool, subject, rSrcRow, nrSrcRow, rTstRow, nrTstRow, redCoverage, origCoverage, origMutScore, redMutScore);

			} catch (IOException e) {
				//This will never happen since the files were obtained through .listFiles()
			}		
		}	
		//After all files are processed, set the final scores for each tool
		mcdm.setScores(subjects, tools);
	}

	/*
	 * Full mode execution: (Generating tests) -> Reducing -> Analyze -> Rank tools
	 */
	private static void fullMode() throws IOException, InterruptedException, ParserConfigurationException, SAXException {

		//Checks if we should run the experiment with evosuite and kanonizo
		boolean useEvo = Configuration.useEvosuite();
		boolean useKan = Configuration.useKanonizo();

		//This map will save the execution times of each tool so we can present them in the end
		times = initTimes(useEvo, useKan);

		//Gets the reports path and reduced projects path from config.xml
		String reductionReportsPath = Configuration.getReportsPath();
		String reducedProjectsPath = Configuration.getReducedProjectsPaths();

		//Checks if those folders exist and creates them if not
		FileUtils.checkFolder(reductionReportsPath);
		FileUtils.checkFolder(reducedProjectsPath);
		FileUtils.checkFolder(Configuration.getOutputPath());

		//Gets the subjects and the TSRTools from config.xml to use in the experiment
		ArrayList<Subject> subjects = Configuration.getSubjects();   	
		ArrayList<TSRTool> tools = Configuration.getTools();

		//Goes through every subject (java project)
		for(Subject subject : subjects) {

			//gets the subject name and subject path
			String projectPath = subject.getProjectPath();
			String projectName = subject.getProjectName();

			logger.info("Working on: " + projectName + " project");

			subject.prepareProject();

			if(useEvo) {
				//Loads EvoSuite (to generate tests)
				EvoSuite evo = new EvoSuite("");

				//generates tests for the subject
				logger.info("Generating tests using EvoSuite");
				evo.setSubject(subject);
				evo.prepareProject();
				long time = evo.execute();
				if(time == -1) {
					logger.error(String.format(toolErrorTemplate, evo.getName(), projectName));
					logger.error("Skipping to next subject...");
					continue;
				}

				//add evo execution time
				times.get(evo.getName()).add(new Pair<String, Long>(subject.getProjectName(), time));
			}

			//runs subject with OpenClover to generate the not reduced report
			logger.info("Running OpenClover on " + projectName);
			if(CloverRunner.run(projectPath) == -1) {
				logger.error(String.format(toolErrorTemplate, "OpenClover", projectName));
				logger.error("Skipping to next subject...");
				continue;
			}
			
			//runs subject with PIT
			logger.info("Running PIT on " + projectName);
			if(PitRunner.run(projectPath) == -1) {
				logger.error(String.format(toolErrorTemplate, "PIT", projectName));
				logger.error("Skipping to next subject...");
				continue;
			}

			//gets data from the original project report
			logger.info("Getting the data from the OpenClover reports");
			List<SrcReportRow> originalSrcRows;
			List<TestReportRow> originalTstRows;
			double originalProjectCoverage;
			double originalMutCoverage;
			try {
				originalSrcRows = ReportReader.readSrcRows(projectPath, false);
				originalTstRows = ReportReader.readTestRows(projectPath, false);
				originalProjectCoverage = ReportReader.getProjectCoverage(projectPath);
				originalMutCoverage = ReportReader.getMutationCoverage(projectPath);
			} catch (IOException e) {
				logger.error("OpenClover or PIT did not generate reports for " + projectName);
				logger.error("Check if its tests do not contain compilation errors");
				logger.error(e.getMessage());
				logger.error("Skipping to next subject...");
				continue;
			}
			if(useEvo) {
				//We can write the report for the reduced evosuite test suite right now	
				int val = runReduced("EvoSuite", reducedProjectsPath+"\\"+projectName+"\\"+"EvoSuite", 
						reductionReportsPath, projectName, originalSrcRows, originalTstRows, 
						originalProjectCoverage, originalMutCoverage);
				if(val == -1) {
					logger.error("Failed for EvoSuite");
				}
			}

			//goes through every TSRTool
			for(TSRTool tool : tools) {

				String toolName = tool.getName();

				logger.info("Reducing " + projectName + " using " + toolName);
				String reducedPath = reducedProjectsPath + "/" + projectName + "/" + toolName;

				//prepares project (copies it from the original path to the folder of reduced projects)
				tool.setSubject(subject);
				tool.prepareProject();

				//executes the tool, reducing the test suite and writing it to the specified path
				logger.info("Running " + toolName);
				long time = tool.execute();
				if(time == -1) {
					logger.error(String.format(toolErrorTemplate, tool.getName(), projectName));
					logger.error("Skipping to next tool...");
				} else {
					times.get(tool.getName()).add(new Pair<String, Long>(subject.getProjectName(), time));
					logger.info("Done");			

					//runs the reduced project with OpenClover and generate the report
					int val = runReduced(toolName, reducedPath, reductionReportsPath, projectName, originalSrcRows, 
							originalTstRows, originalProjectCoverage, originalMutCoverage);
					if(val == -1) {
						logger.error("Failed for " + toolName);
					}
				}
			}

			//Run kanonizo if it is set
			if(useKan) {
				runKanonizo(times, reductionReportsPath, subject, projectName, originalSrcRows, originalTstRows,
						originalProjectCoverage, originalMutCoverage);
			}  		
		}
		
		//After every tool and project -> set the final scores for each tool
		mcdm.setScores(subjects, getDistinguishedTools());

	}

	/*
	 * Returns a list with the subjects names
	 */
	private static List<String> getSubjectNames(ArrayList<Subject> subjects) {
		ArrayList<String> subjectsNames = new ArrayList<>();
		for(Subject subject : subjects)
			subjectsNames.add(subject.getProjectName());
		return subjectsNames;
	}

	/*
	 * Runs Kanonizo
	 * Different from the other tools as there are many combinations of algorithm-cutOff
	 */
	private static void runKanonizo(Map<String, ArrayList<Pair<String, Long>>> times, String reductionReportsPath,
			Subject subject, String projectName, List<SrcReportRow> originalSrcRows,
			List<TestReportRow> originalTstRows, double originalProjectCoverage, double originalMutCoverage)
					throws IOException, InterruptedException, ParserConfigurationException, SAXException {

		Kanonizo kanonizo = new Kanonizo("", Configuration.getKanonizoAlgorithms(), 
				Configuration.getKanonizoCutOffs());
		kanonizo.setSubject(subject);
		kanonizo.prepareProject();
		logger.info("Reducing " + projectName + " using " + kanonizo.getName());
		long result = kanonizo.execute();
		logger.info("Done");
		if(result != -1 || result != -2) {
			times.get(kanonizo.getName()).add(new Pair<String, Long>(subject.getProjectName(), result));
			//Kanonizo will generate algorithms.size()*cutOffs.size() reduced projects
			for(String alg : Configuration.getKanonizoAlgorithms()) {
				if(!kanonizo.getFailedAlgs().contains(alg)) {
					for(int cut : Configuration.getKanonizoCutOffs()){
						String toolName = kanonizo.getName() + "-" + alg + "-" + cut;
						String reducedPath = Configuration.getReducedProjectsPaths() +"\\"+ 
								subject.getProjectName() + "\\"+ kanonizo.getName() + "\\" + alg + "\\" + cut;
						int val = runReduced(toolName, reducedPath, reductionReportsPath, projectName, originalSrcRows, 
								originalTstRows, originalProjectCoverage, originalMutCoverage);
						if(val == -1) {
							logger.error("Skipping to next cutOff...");
							continue;
						}
					}
				} else {
					logger.error("Skipping kanonizo algorithm " + alg + " because it didn't run");
				}
			}
		}
	}

	/*
	 * writes the information on execution times for each tool
	 */
	private static void writeOutput(long totalTime, ArrayList<Subject> subjects) throws FileNotFoundException {
		writeTimeInfo(totalTime);
		writeScoreInfo(subjects);
	}

	/*
	 * Writes a file containing the score each tools got for each project and their final score
	 */
	private static void writeScoreInfo(ArrayList<Subject> subjects) throws FileNotFoundException {
		StringBuilder sb = new StringBuilder(" ");
		List<String> projects = getSubjectNames(subjects);
		
		//Writes header
		for(String projectName : projects) {
			sb.append(", " + projectName);
		}
		sb.append(", Total\n");
		
		//Writes 2nd row
		sb.append("Weights");
		for(Subject subject : subjects) {
			sb.append(", " + Integer.toString(subject.getWeight()));
		}
		sb.append(", \n");
		
		//Writes the rows (tools and its scores) sorted by descending order of final score
		ArrayList<Pair<String, Double>> sortedTools = Score.getInstance().getSortedToolsWithScores(subjects);
		for(int i = 0; i < sortedTools.size(); i++) {
			Pair<String, Double> pair = sortedTools.get(i);
			sb.append(pair.getFirst());
			for(String projectName : projects) {
				sb.append(", " + Double.toString(Score.getInstance().get(pair.getFirst(), projectName)));
			}
			sb.append(", " + Double.toString(pair.getSecond()) + "\n");
		}

		//Gets output file name
		String output = Configuration.getOutputPath() + "\\0-ReductionScores.csv";
		if(Files.exists(Paths.get(output)))
			output = output.replace("0-", getNextNumber(new File(Configuration.getOutputPath()).list()) + "-");

		PrintWriter pw = new PrintWriter(new File(output)); 
		pw.print(sb.toString());
		pw.close();
		logger.info("Table with scores saved into: " + output);
	}

	/*
	 * Returns the number of the next report to save
	 */
	private static String getNextNumber(String[] filePaths) {
		int max = 0;
		for(String path : filePaths) {
			if(path.contains("-ReductionScores")) {
				File f = new File(path);
				String name = f.getName();
				int idx = name.indexOf('-');
				int number = Integer.parseInt(name.substring(0, idx));
				if(max < number)
					max = number;
			}
		}
		return Integer.toString(max+1);
	}

	/*
	 * Writes the execution times each tool took to reduce the test suites for each project
	 * And the total time of execution of this framework
	 */
	private static void writeTimeInfo(long totalTime) {
		StringBuilder sb = new StringBuilder();
		if(!Configuration.getMode().equals("Analyze")) {
			sb.append("Execution times: \n");
			for(String toolName : times.keySet()) {
				for(Pair<String, Long> pair : times.get(toolName)) {
					sb.append(toolName + " -> " + pair.getFirst() + ": " + (pair.getSecond()/1000) + "s\n");
				}
			}
		}
		sb.append("\nTotal time: " + totalTime + "s\n");
		logger.info(sb.toString());
	}


	/*
	 * Prepares a Map to save the execution times of each tool for each project
	 */
	private static Map<String, ArrayList<Pair<String, Long>>> initTimes(boolean useEvo, boolean useKan) {
		Map<String, ArrayList<Pair<String, Long>>> times = new HashMap<>();
		int size = Configuration.getSubjects().size();
		for(TSRTool tool : Configuration.getTools()) 
			times.put(tool.getName(), new ArrayList<Pair<String, Long>>(size));
		if(useEvo)
			times.put("EvoSuite", new ArrayList<Pair<String, Long>>(size));
		if(useKan)
			times.put("Kanonizo", new ArrayList<Pair<String, Long>>(size));
		return times;
	}

	/*
	 * Runs OpenClover on the given project, gets the data from its report and writes 
	 * the comparative report
	 */
	private static int runReduced(String toolName, String reducedPath, String reductionReportsPath, 
			String projectName,	List<SrcReportRow> originalSrcRows, 
			List<TestReportRow> originalTstRows, double originalProjectCoverage, double originalMutCoverage)
					throws IOException, InterruptedException, ParserConfigurationException, SAXException {

		logger.info("Running OpenClover on " + projectName + "-reduced");
		if(CloverRunner.run(reducedPath) == -1) {
			logger.error(String.format(toolErrorTemplate, "OpenClover", projectName+"-reduced"));
			logger.error("Skipping to next subject...");
			return -1;
		}
		logger.info("Done");
		
		logger.info("Running PIT on " + projectName + "-reduced");
		if(PitRunner.run(reducedPath) == -1) {
			logger.error(String.format(toolErrorTemplate, "PIT", projectName+"-reduced"));
			logger.error("Skipping to next subject...");
			return -1;
		}

		//defines the name of the report for this subject-tool combination
		String fileName = String.format("%s\\%s-%s-report.csv", reductionReportsPath, projectName, toolName);

		//gets the rows with the data related to the reduced project
		logger.info("Getting the OpenClover data from the reduced project");
		List<SrcReportRow> reducedSrcRows;
		List<TestReportRow> reducedTstRows;
		double reducedProjectCoverage;
		double reducedMutCoverage;
		try{
			reducedSrcRows = ReportReader.readSrcRows(reducedPath, true);
			reducedTstRows = ReportReader.readTestRows(reducedPath, true);
			reducedProjectCoverage = ReportReader.getProjectCoverage(reducedPath);
			reducedMutCoverage = ReportReader.getMutationCoverage(reducedPath);
		} catch (IOException e) {
			logger.error("OpenClover or PIT did not generate reports for " + projectName + "-reduced");
			logger.error("Check if its tests contain compilation errors");
			logger.error(e.getMessage());
			return -1;
		}

		//writes the report for this subject-tool combination
		logger.info("Writing report into " + fileName);
		FileUtils.writeReport(fileName, originalProjectCoverage, reducedProjectCoverage, 
				originalMutCoverage, reducedMutCoverage,
				originalSrcRows, reducedSrcRows, 
				originalTstRows, reducedTstRows);

		SrcReportRow rSrcRow = ReportReader.compactSrcRow(reducedSrcRows);
		SrcReportRow nrSrcRow = ReportReader.compactSrcRow(originalSrcRows);
		TestReportRow rTstRow = ReportReader.compactTstRow(reducedTstRows);
		TestReportRow nrTstRow = ReportReader.compactTstRow(originalTstRows);

		mcdm.saveValues(toolName, projectName, rSrcRow, nrSrcRow, rTstRow, nrTstRow, 
				reducedProjectCoverage, originalProjectCoverage, originalMutCoverage, reducedMutCoverage);
		return 0;
	}
	
	/*
	 * Gets a list with the tools' name and divides Kanonizo into each of its combinations
	 */
	private static ArrayList<String> getDistinguishedTools() {
		ArrayList<String> toolNames = new ArrayList<>();
		for(TSRTool t : Configuration.getTools())
			toolNames.add(t.getName());
		if(Configuration.useEvosuite())
			toolNames.add("EvoSuite");
		if(Configuration.useKanonizo()) {
			for(String alg : Configuration.getKanonizoAlgorithms()) {
				if(alg.equals("random")) {
					for(int cutOff : Configuration.getKanonizoCutOffs()) {
						for(int i = 1; i <= 30; i++)
							toolNames.add("kanonizo#" + alg + "#" + Integer.toString(cutOff) + "#" + Integer.toString(i));
					}
				} else {
					for(int cutOff : Configuration.getKanonizoCutOffs()) {
						toolNames.add("kanonizo#" + alg + "#" + Integer.toString(cutOff));
					}
				}
			}
		}
		return toolNames;
	}

}
