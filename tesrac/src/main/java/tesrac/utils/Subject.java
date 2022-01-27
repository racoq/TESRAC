package tesrac.utils;

/**
 * This class represents a subject of the experiment
 * Essentially a Java maven project
 * @author JBecho
 *
 */
public class Subject {

	/*
	 * The name of the project
	 */
	private String projectName;
	
	/*
	 * The path to the root folder of the project
	 */
	private String projectPath;
	
	/*
	 * The corresponding reduced base path to this project
	 */
	private String reducedPath;
	
	/*
	 * The weight of this project on the computation for the final reduction score of a tool 
	 */
	private int weight;
	
	/**
	 * Constructor
	 * @param name of the project
	 * @param path to the root folder of the project
	 */
	public Subject(String name, String path, int weight) {
		projectName = name;
		projectPath = path;
		reducedPath = Configuration.getReducedProjectsPaths() + "\\" + projectName + "\\";
		this.weight = weight;
	}

	/**
	 * Returns the project name
	 * @return String
	 */
	public String getProjectName() {
		return projectName;
	}

	/**
	 * Return the path to the root of the project
	 * @return String
	 */
	public String getProjectPath() {
		return projectPath;
	}
	
	/**
	 * prepares the pom.xml file of this project, adding the necessary dependencies
	 * and plugins to its build so we can run the experiment
	 */
	public void prepareProject() {
		FileUtils.preparePom(projectPath + "\\pom.xml");
	}
	
	/**
	 * Returns the reduced path for this project
	 * @return
	 */
	public String getReducedPath() {
		return reducedPath;
	}
	
	/**
	 * Returns the weight of this project
	 * @return
	 */
	public int getWeight() {
		return weight;
	}
	
}
