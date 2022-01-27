package tesrac.tools;

import java.io.File;
import java.io.IOException;

import tesrac.utils.FileUtils;
import tesrac.utils.Logger;
import tesrac.utils.Subject;

/**
 * Abstract class that represent a TSR Tool
 * @author JBecho
 *
 */
public abstract class TSRTool {

	private static final Logger logger = Logger.getInstance(TSRTool.class.getName());

	/*
	 * The name of the tool
	 */
	private String name;

	/*
	 * The path to the jar 
	 */
	private String path;

	/*
	 * The current subject of the experiment
	 */
	private Subject subject;

	/**
	 * Constructor
	 * @param name of the Tool
	 * @param path
	 */
	public TSRTool(String name, String path) {
		this.name = name;
		this.path = path;
	}

	/**
	 * Copies a project from the originPath to the reduced reports folder
	 * erasing its test suite
	 * @throws IOException
	 */
	public void prepareProject() throws IOException {
		File projectRoot = new File(subject.getProjectPath());
		File clone = new File(getSubject().getReducedPath() + name + "\\");
		clone.mkdirs();
		FileUtils.copyProject(projectRoot, clone);
		logger.info("Copied project to " + clone.getAbsolutePath());
	}

	/**
	 * Executes the tool and returns the time of executions
	 * @return the time of execution
	 */
	public abstract long execute();

	/**
	 * Returns the name of the tool
	 * @return String name of the tool
	 */
	public String getName() {
		return name;
	}

	/**
	 * Returns the path to the jar
	 * @return String path to the jar
	 */
	public String getJarPath() {
		return path;
	}

	/**
	 * Returns the current subject
	 * @return Subject
	 */
	public Subject getSubject() {
		return subject;
	}

	/**
	 * Sets the new subject that this tool will run on
	 * @param subject - the new subject
	 */
	public void setSubject(Subject subject) {
		this.subject = subject;
	}
}
