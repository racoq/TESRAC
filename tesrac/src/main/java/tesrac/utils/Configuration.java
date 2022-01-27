package tesrac.utils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import tesrac.mcdm.Criterion;
import tesrac.tools.TSRTool;

/**
 * The class that holds the configuration of the system
 * loaded from the config.xml file in src/main/resources
 * @author JBecho
 *
 */
public class Configuration {

	public static final String DIMENSION = "Dimension";
	public static final String FILESIZE = "File Size";
	public static final String CASES_QTY = "# of Test Cases";
	public static final String COVERAGE = "Coverage";
	public static final String PC_TOTAL_COVERAGE = "% Total Coverage";
	public static final String PC_BRANCH_COVERAGE = "% Branches Covered";
	public static final String PC_MUTATION_SCORE = "% Mutation Score";
	public static final String TIME = "Time";
	public static final String EXECUTION_TIME = "Tests Execution Time";

	private static final Logger logger = Logger.getInstance(Configuration.class.getName());

	/*
	 * the path to the folder where the reports will be written
	 */
	private static String reportsPath;

	/*
	 * the path to the folder where the reduced projects will be written
	 */
	private static String reducedProjectsPath;

	/*
	 * the path to save the output of tesrac
	 */
	private static String outputPath;

	/*
	 * the List with all the subjects of the experiment (projects)
	 */
	private static ArrayList<Subject> subjects;

	/*
	 * the List with all the TSRTools implemented in the system
	 */
	private static ArrayList<TSRTool> tools;

	/*
	 * flag to set the use of evosuite
	 */
	private static boolean evosuite;

	/*
	 * flag to set the use of kanonizo
	 */
	private static boolean kanonizo;
	private static List<String> kanonizoAlgs;
	private static List<Integer> kanonizoCutOffs;

	/*
	 * path to the OpenClover config file
	 */
	private static String openCloverConfigFile; 

	/*
	 * The mode of execution of the tool
	 */
	private static String mode;

	/*
	 * the package where the implementation of the subclasses of TSRTool are
	 */
	private static final String TOOL_CLASSES_PACKAGE = "tesrac.tools.";

	/*
	 * the list of criteria for the computation of the reduction score
	 */
	private static List<Criterion> criteria;

	static {
		try {
			initialize();
		} catch (ParserConfigurationException | SAXException | IOException e) {
			logger.error("Error initializing the configuration: Getting the XML Document: " + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * Returns the path to the folder where the reports will be written
	 * @return String
	 */
	public static String getReportsPath(){
		return reportsPath;
	}

	/**
	 * Returns the path to the folder where the reduced projects will be written
	 * @return String
	 */
	public static String getReducedProjectsPaths(){
		return reducedProjectsPath;
	}

	/**
	 * Returns the path in which to save tesrac output
	 * @return
	 */
	public static String getOutputPath() {
		return outputPath;
	}

	/**
	 * Returns the List of the projects that will be reduced
	 * @return ArrayList<Subject>
	 */
	public static ArrayList<Subject> getSubjects() {
		return subjects;
	}

	/**
	 * Returns the List of the TSRTool to use in the experiment
	 * @return ArrayList<TSRTool>
	 */
	public static ArrayList<TSRTool> getTools() {
		return tools;
	}	

	/**
	 * Returns if the experiment should be run with evosuite or not
	 * @return true if evosuite is to be used false otherwise
	 */
	public static boolean useEvosuite() {
		return evosuite;
	}

	/**
	 * Returns if the experiment should be run with kanonizo or not
	 * @return true if kanonizo is to be used false otherwise
	 */
	public static boolean useKanonizo() {
		return kanonizo;
	}

	/**
	 * Returns the algorithms that Kanonizo will run
	 * @return List<String> with algorithm names
	 */
	public static List<String> getKanonizoAlgorithms(){
		return kanonizoAlgs;
	}

	/**
	 * Returns the thresholds that Kanonizo will use to reduce the test suites
	 * @return List<Integer> with cutOff values
	 */
	public static List<Integer> getKanonizoCutOffs(){
		return kanonizoCutOffs;
	}

	/**
	 * Returns the path to the OpenClover report configuration file
	 * @return
	 */
	public static String getCloverConfigPath() {
		return openCloverConfigFile;
	}

	/**
	 * Returns the mode of execution
	 * @return
	 */
	public static String getMode() {
		return mode;
	}

	/**
	 * Returns the list of criteria 
	 * @return
	 */
	public static List<Criterion> getCriteria(){
		return criteria;
	}

	/*
	 * loads the attributes from the xml file in the src/main/resources folder
	 */
	private static void initialize() throws ParserConfigurationException, SAXException, IOException {
		ClassLoader classLoader = new Configuration().getClass().getClassLoader();	 
		File configFile = new File(classLoader.getResource("config.xml").getFile());

		Document doc = XMLUtils.getXMLDoc(configFile.getPath());
		setMode(doc);
		setReportsPath(doc);
		setOutputPath(doc);
		setCriteria(doc);
		setSubjects(doc);
		setReducedPath(doc);
		setEvoSuite(doc);
		setKanonizo(doc);
		setCloverReportConfigPath();
		try {
			setTools(doc);
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
				| NoSuchMethodException | SecurityException | ClassNotFoundException e) {
			System.err.println("Couldn't load the tools from the config file: " + e.getMessage() 
			+ "Make sure all entries are correct and all the needed classes are implemented (one for each tool)");
			e.printStackTrace();
		}	

	}

	/*
	 * sets the output path in which to save tesrac output
	 */
	private static void setOutputPath(Document doc) {
		Node outputNode = doc.getElementsByTagName("outputPath").item(0);
		outputPath = outputNode.getTextContent();
		logger.info("Output path: " + outputPath);

	}

	/*
	 * Sets the criteria for this execution of tesrac
	 */
	private static void setCriteria(Document doc) {
		criteria = new ArrayList<Criterion>();
		Node ahpNode = doc.getElementsByTagName("mcdm").item(0);
		NodeList criteriaNodes = ahpNode.getChildNodes();
		StringBuilder sb = new StringBuilder("MCDM Criteria: [");
		for(int i = 0; i < criteriaNodes.getLength(); i++) {
			Node node = criteriaNodes.item(i);
			if(node.getNodeName().equals("criteria")) {
				Criterion c = getCriterionFromNode(node);
				criteria.add(c);
				sb.append(c.toString());
				if(i != criteriaNodes.getLength()-1)
					sb.append(", ");
			}
		}
		sb.append("]");
		logger.info(sb.toString());
	}

	/*
	 * parses the xml file to get a criterion
	 */
	private static Criterion getCriterionFromNode(Node criteriaNode) {
		NodeList criteriaChilds = criteriaNode.getChildNodes();
		String id = "";
		String name = "";
		ArrayList<Double> importances =  new ArrayList<>();
		ArrayList<Criterion> subCriteria = new ArrayList<>();
		for(int i = 0; i < criteriaChilds.getLength(); i++) {
			Node child = criteriaChilds.item(i);
			String childName = child.getNodeName();
			switch(childName) {
			case "id":
				id = child.getTextContent();
				break;
			case "name":
				name = child.getTextContent();
				break;
			case "importance":
				Element importance = (Element) child;
				importances.add(parse(importance.getTextContent()));
				break;
			case "subcriteria":
				NodeList subcriteriaList = child.getChildNodes();
				for(int j = 0; j < subcriteriaList.getLength(); j++) {
					Node subcriteria = subcriteriaList.item(j);
					if(subcriteria.getNodeName().equals("criteria"))
						subCriteria.add(getCriterionFromNode(subcriteria));
				}
				break;
			}
		}
		return new Criterion(id, name, importances, subCriteria);
	}

	/*
	 * parses a fraction to double
	 */
	private static Double parse(String textContent) {
		if(!textContent.contains("/"))
			return Double.parseDouble(textContent);
		String [] div = textContent.split("/");
		return Double.parseDouble(div[0]) / Double.parseDouble(div[1]);
	}

	/*
	 * Sets the execution mode for tesrac
	 */
	private static void setMode(Document doc) {
		Node modeNode = doc.getElementsByTagName("mode").item(0);
		mode = modeNode.getTextContent();
		logger.info("Execution mode: " + mode);
	}

	/*
	 * sets the path to the folder where the reduced projects will be written
	 * with the value from the config.xml file
	 */
	private static void setReducedPath(Document doc) {
		Node rppPath = doc.getElementsByTagName("reducedProjectsPath").item(0);
		reducedProjectsPath = rppPath.getTextContent();
		logger.info("Reduced projects path: " + reducedProjectsPath);
	}

	/*
	 * sets the path to the folder where the reports will be written
	 * with the value from the config.xml file
	 */
	private static void setReportsPath(Document doc) {
		Node rPath = doc.getElementsByTagName("reportsPath").item(0);
		reportsPath = rPath.getTextContent();
		logger.info("Reports path: " + reportsPath);
	}

	/*
	 * sets if the experiment should run using evosuite to generate the test suites
	 */
	private static void setEvoSuite(Document doc) {
		Node evo = doc.getElementsByTagName("evosuite").item(0);
		evosuite = evo.getTextContent().equalsIgnoreCase("true");
		logger.info("EvoSuite: " + (evosuite?"Enabled":"Disabled"));
	}

	/*
	 * sets the available kanonizo attributes: flag and lists with algorithms and cutoffs
	 */
	private static void setKanonizo(Document doc) {
		Node kan = doc.getElementsByTagName("kanonizo").item(0);
		NodeList kanonizoConfig = kan.getChildNodes();
		for(int i = 0; i < kanonizoConfig.getLength(); i++) {
			Node node = kanonizoConfig.item(i);
			if(node.getNodeName().equals("use")) {
				kanonizo = node.getTextContent().equalsIgnoreCase("true");
				logger.info("Kanonizo: " + (kanonizo?"Enabled":"Disabled"));
				if(!kanonizo) break;
			}
			if(node.getNodeName().equals("algorithms"))
				setKanonizoAlgs(node);
			if(node.getNodeName().equals("cutOffs"))
				setKanonizoCutOffs(node);
		}
	}

	/*
	 * Sets the List with the chosen thresholds to reduce the test suites after kanonizo
	 */
	private static void setKanonizoCutOffs(Node node) {
		kanonizoCutOffs = new ArrayList<Integer>();
		NodeList cutOffs = node.getChildNodes();
		StringBuilder sb = new StringBuilder("Kanonizo cut-off values:");
		for(int i = 0; i < cutOffs.getLength(); i++) {
			Node cutOff = cutOffs.item(i);
			if(cutOff.getNodeName().equals("cutOff")) {
				kanonizoCutOffs.add(Integer.parseInt(cutOff.getTextContent()));
				sb.append(" " + cutOff.getTextContent() + "%");
			}
		}
		logger.info(sb.toString());
	}

	/*
	 * Sets the List with the chosen algorithms to use with kanonizo 
	 */
	private static void setKanonizoAlgs(Node node) {
		StringBuilder sb = new StringBuilder("Kanonizo algorithms:");
		kanonizoAlgs = new ArrayList<String>();
		NodeList algorithms = node.getChildNodes();
		for(int i = 0; i < algorithms.getLength(); i++) {
			Node algorithm = algorithms.item(i);
			if(algorithm.getNodeName().equals("algorithm")) {
				Element alg = (Element) algorithm;
				if(alg.getAttribute("use").equalsIgnoreCase("true")) {
					kanonizoAlgs.add(algorithm.getTextContent());
					sb.append(" " + algorithm.getTextContent());
				}
			}
		}
		logger.info(sb.toString());
	}

	/*
	 * loads the values of the name and path of the subjects from the config.xml
	 * and adds them to a list of Subject
	 */
	private static void setSubjects(Document doc) {
		StringBuilder sb = new StringBuilder("Subjects:");
		subjects = new ArrayList<>();
		NodeList subjectsNodes = doc.getElementsByTagName("project");
		for(int i = 0; i < subjectsNodes.getLength(); i++) {
			Node subject = subjectsNodes.item(i);
			NodeList subjectChilds = subject.getChildNodes();
			String name = "";
			String path = "";
			String weightText = "";
			int weight = 1;
			for(int j = 0; j < subjectChilds.getLength(); j++) {  
				Node subjectChild = subjectChilds.item(j);
				String nodeName = subjectChild.getNodeName();
				if(nodeName.equals("name")) {
					name = subjectChild.getTextContent();
					sb.append(" " + name);
				}
				if(nodeName.equals("pathToPom"))
					path = subjectChild.getTextContent();
				if(nodeName.equals("weight")) {
					weightText = subjectChild.getTextContent();
					if(!weightText.equals("equal"))
						weight = Integer.parseInt(weightText);
				}

			}
			subjects.add(new Subject(name, path, weight));
		}
		logger.info(sb.toString());
	}

	/*
	 * loads the necessary classes from the name and path specified in the config.xml files
	 * and adds them in a List of TSRTool
	 */
	private static void setTools(Document doc) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException, ClassNotFoundException {
		StringBuilder sb = new StringBuilder("Tools:");
		tools = new ArrayList<>();
		NodeList toolNodes = doc.getElementsByTagName("tool");
		for(int i = 0; i < toolNodes.getLength(); i++) {
			Node toolNode = toolNodes.item(i);
			NodeList toolChilds = toolNode.getChildNodes();
			String name = "";
			String jarPath = "";
			//ArrayList<String> args = new ArrayList<>();
			for(int j = 0; j < toolChilds.getLength(); j++) {
				Node toolChild = toolChilds.item(j);
				String nodeName = toolChild.getNodeName();
				if(nodeName.equals("name")) {
					name = toolChild.getTextContent();
					sb.append(" " + name);
				}
				if(nodeName.equals("jar"))
					jarPath = toolChild.getTextContent();
			}
			Class<? extends TSRTool> toolClass = Class.forName(TOOL_CLASSES_PACKAGE + name).asSubclass(TSRTool.class);
			Object toolObj = toolClass.getDeclaredConstructor(String.class, String.class).newInstance(name, jarPath);
			TSRTool tool = (TSRTool) toolObj;
			tools.add(tool);
		}

		logger.info(sb.toString());
	}

	/*
	 * Sets the path to the OpenClover config file
	 */
	private static void setCloverReportConfigPath() {
		//Getting clover report configuration file path
		ClassLoader classLoader = new FileUtils().getClass().getClassLoader();	 
		File configFile = new File(classLoader.getResource("clover-report.xml").getFile());
		openCloverConfigFile = configFile.getPath();
		logger.info("OpenClover report configuration file: " + openCloverConfigFile);
	}

}
