package tesrac.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.maven.model.Build;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.model.ReportPlugin;
import org.apache.maven.model.Reporting;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.xml.sax.SAXException;

import tesrac.reporting.SrcReportRow;
import tesrac.reporting.TestReportRow;

/**
 * Contains some methods that will be useful to handle files
 * @author JBecho
 *
 */
public class FileUtils {
	
	private static final Logger logger = Logger.getInstance(FileUtils.class.getName());

	private static List<Pair<String, String>> buildPluginConfig = 
			Arrays.asList(new Pair<>("reportDescriptor", ""),
					new Pair<>("resolveReportDescriptor", "false"),
					new Pair<>("generateJson", "true"),
					new Pair<>("generateXml", "true"),
					new Pair<>("generatePdf", "false"),
					new Pair<>("generateHtml", "false"),
					new Pair<>("setTestFailureIgnore", "true"),
					new Pair<>("jdk", "1.6"));

	private static List<Pair<String, String>> reportingPluginConfig = 
			Arrays.asList(new Pair<>("reportDescriptor", ""),
					new Pair<>("resolveReportDescriptor", "false"),
					new Pair<>("jdk", "1.6"));

	/**
	 * Checks if the folder in the given path exists and creates it if not
	 * @param reductionReportsPath
	 * @param reducedProjectsPath
	 */
	public static void checkFolder(String path) {
		File folder = new File(path); 	
		if(!folder.exists()) {
			folder.mkdirs();
			logger.info("Folder " + path + " created");
		}
	}  

	/**
	 * Writes the report into a file with the given path
	 * @param reportPath - the path to write the report
	 * @param nrProjCov - not reduced project coverage
	 * @param rProjCov - reduced project coverage
	 * @param nrSrcRows - list of not reduced project src files info
	 * @param rSrcRows - list of reduced project src files
	 * @param nrTestRows - list of not reduced test files info
	 * @param rTestRows - list of reduced test files info
	 * @throws IOException
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 */
	public static void writeReport(String reportPath, double nrProjCov, double rProjCov, 
			double nrMutCov, double rMutCov,
			List<SrcReportRow> nrSrcRows, List<SrcReportRow> rSrcRows, 
			List<TestReportRow> nrTestRows, List<TestReportRow> rTestRows)
					throws IOException, ParserConfigurationException, SAXException {
		PrintWriter writer = new PrintWriter(new File(reportPath));
		writer.println("Original Project Coverage (%);" + nrProjCov);
		writer.println("Reduced Project Coverage (%);" + rProjCov);
		writer.println("Original Mutation Coverage (%);" + nrMutCov);
		writer.println("Reduced Project Coverage(%);" + rMutCov);
		writer.println(SrcReportRow.HEADER);

		for(int i = 0; i < nrSrcRows.size(); i++) {
			writer.println(nrSrcRows.get(i).toString());
			writer.println(rSrcRows.get(i).toString());
		}

		writer.println(TestReportRow.HEADER);

		for(int i = 0; i < nrTestRows.size(); i++) {
			writer.println(nrTestRows.get(i).toString());
			writer.println(rTestRows.get(i).toString());
		}
		writer.close();
	}

	/**
	 * Reads a pom.xml file, adds the dependencies and plugins necessary to run the experiment
	 * and writes the new file
	 * @param pathToPom - the path to the pom.xml file
	 */
	public static void preparePom(String pathToPom) {
		String configFilePath = Configuration.getCloverConfigPath();
		Model model = readPom(pathToPom);
		if(Configuration.useEvosuite()) {
			addEvoDependency(model);
		}
		addCloverReportingPlugin(model, configFilePath);
		addCloverBuildPlugin(model, configFilePath);
		addPitPlugin(model);
		writePom(pathToPom, model);
	}

	/*
	 * Adds the pit plugin to the project pom
	 */
	private static void addPitPlugin(Model model) {
		Plugin plugin = new Plugin();
		plugin.setGroupId("org.pitest");
		plugin.setArtifactId("pitest-maven");
		plugin.setVersion("1.5.2");
		model.getBuild().addPlugin(plugin);
	}

	/*
	 * Adds the open clover build plugin to the given model
	 */
	private static void addCloverBuildPlugin(Model model, String configFilePath) {
		//builds the plugin
		Plugin cloverPluginBuild = new Plugin();
		cloverPluginBuild.setGroupId("org.openclover");
		cloverPluginBuild.setArtifactId("clover-maven-plugin");
		cloverPluginBuild.setVersion("4.3.1");

		//builds the plugin configuration
		Xpp3Dom config = new Xpp3Dom("configuration");
		for(Pair<String, String> pair : buildPluginConfig) {
			addChildElement(config, pair.getFirst(), 
					pair.getFirst().equals("reportDescriptor") ? configFilePath : pair.getSecond());
		}
		cloverPluginBuild.setConfiguration(config);

		//builds the plugin execution
		PluginExecution execution = new PluginExecution();
		execution.setPhase("pre-site");
		execution.setGoals(Arrays.asList("instrument", "check", "save-history"));	
		cloverPluginBuild.setExecutions(Arrays.asList(execution));

		//gets the model's build and adds the new plugin
		Build build = model.getBuild();
		if(build == null) {
			build = new Build();
			model.setBuild(build);
		}
		if(!build.getPlugins().contains(cloverPluginBuild))
			build.addPlugin(cloverPluginBuild);
	}

	/*
	 * Adds the open clover reporting plugin to the given model 
	 */
	private static void addCloverReportingPlugin(Model model, String configPath) {
		//Builds the ReportPlugin
		ReportPlugin cloverPluginReport = new ReportPlugin();
		cloverPluginReport.setGroupId("org.openclover");
		cloverPluginReport.setArtifactId("clover-maven-plugin");
		cloverPluginReport.setVersion("@project.version@");

		//Builds the plugin configuration
		Xpp3Dom config = new Xpp3Dom("configuration");
		for(Pair<String, String> pair : reportingPluginConfig) {
			addChildElement(config, pair.getFirst(), 
					pair.getFirst().equals("reportDescriptor") ? configPath : pair.getSecond());
		}
		cloverPluginReport.setConfiguration(config);

		//Gets the reporting from the model and adds the new plugin
		Reporting reporting = model.getReporting();
		if(reporting == null) {
			reporting = new Reporting();
			model.setReporting(reporting);
		}
		if(!reporting.getPlugins().contains(cloverPluginReport))
			reporting.addPlugin(cloverPluginReport);
	}

	/*
	 * Adds the evosuite dependency to the given model
	 */
	private static void addEvoDependency(Model model) {
		//Builds the dependency
		Dependency evoDependency = new Dependency();
		evoDependency.setGroupId("org.evosuite");
		evoDependency.setArtifactId("evosuite-standalone-runtime");
		evoDependency.setVersion("1.0.6");
		evoDependency.setScope("test");

		//Checks if this dependency is already in the model
		boolean present = false;
		for (int i = 0; i < model.getDependencies().size(); i++) {
			Dependency d = (Dependency) model.getDependencies().get(i);
			if (d.getArtifactId().equals(evoDependency.getArtifactId())) {
				present = true;
				break;
			}
		}

		//Adds the dependency
		if (!present) {
			model.addDependency(evoDependency);
		}
	}

	/*
	 * Adds a child element to the given Xpp3Dom
	 */
	private static void addChildElement(Xpp3Dom newTag, String name, String value) {
		Xpp3Dom subTag = new Xpp3Dom(name);
		subTag.setValue(value);
		newTag.addChild(subTag);
	}

	/**
	 * Rewrites the test class in the file denoted by the given path deleting the 
	 * methods with the given names
	 * @param pathClass - the path to the java file
	 * @param methodNames - the list of method names to erase 
	 * @throws FileNotFoundException
	 */
	public static void eraseTestMethods(File clazz, List<String> methodNames) throws FileNotFoundException {	
		if(!(methodNames == null || methodNames.size() == 0)) {
			Scanner sc = new Scanner(clazz);
			int nrBrackets = 0;
			boolean method = false;
			StringBuilder newClassString = new StringBuilder();
			String test = "";
			while(sc.hasNextLine()) {
				String line = sc.nextLine();
				
				if(line.trim().equals("@SuppressWarnings(\"unchecked\")"))
					continue;
				if(line.contains("@Test")) {
					test = line;
					line = sc.nextLine();
				}
				if(containsOneOf(line, methodNames)) {	
					method = true;
					if(line.contains("{"))
						nrBrackets++;
				} else {
					if(method) {
						if(line.contains("{"))
							nrBrackets++;
						if(line.contains("}"))
							nrBrackets--;
						method = nrBrackets!=0;
						if(!method)
							test = "";
					} else {
						if(test.length() != 0) {
							newClassString.append(test + "\n");
							test = "";
						}
						newClassString.append(line + "\n");
					}
				}
			}
			sc.close();
			PrintWriter pw = new PrintWriter(clazz);
			pw.print(newClassString.toString());
			pw.close();
		}
	}

	/*
	 * Checks if the given String contains one of the Strings given in the list
	 */
	private static boolean containsOneOf(String line, List<String> methodNames) {
		for(String method : methodNames) {
			line.indexOf(method);
			if(hasExactWord(line, method))
				return true;
		}
		return false;
	}

	/*
	 * Checks if the given String contains the whole word given in the second String
	 */
	private static boolean hasExactWord(String line, String methodName) {
		String pattern = "\\b"+methodName+"\\b";
		Pattern p=Pattern.compile(pattern);
		Matcher m=p.matcher(line);
		return m.find();
	}

	private static Model readPom(String path) {
		// Reading pom
		MavenXpp3Reader reader = new MavenXpp3Reader();
		Model model = null;
		try {
			model = reader.read(new FileReader(new File(path)));
		} catch (IOException | XmlPullParserException e1) {
			e1.printStackTrace();
		}
		return model;
	}
	
	private static void writePom(String path, Model model) {
		// Writing pom
		MavenXpp3Writer writer = new MavenXpp3Writer();
		try {
			writer.write(new FileWriter(new File(path)), model);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void copyProject(File from, File to) throws IOException {
		org.apache.commons.io.FileUtils.copyDirectory(from, to);
	}
}
