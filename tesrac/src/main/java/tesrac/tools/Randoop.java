package tesrac.tools;

import java.io.File;

import tesrac.utils.Configuration;

/**
 * Implements the Randoop behaviour
 * @author JBecho
 *
 */
public class Randoop extends TSRTool {
	
	private String testsSourcePath = "\\src\\test\\java";

	public Randoop(String name, String jarPath) {
		super(name, jarPath);
	}

	@Override
	public long execute() {
		long start = System.currentTimeMillis();
		
		String targetPath = getSubject().getProjectPath() + "\\target\\";
		String jar = getJarName(targetPath);
		String completeSuiteClassPath = getClasspath() + targetPath + jar;
		String reducedPathRoot = getSubject().getReducedPath() + "\\" + getName();
		File testsFolder = new File(getSubject().getProjectPath() + testsSourcePath);
		runRandoop(testsFolder, completeSuiteClassPath, reducedPathRoot);
		long time = System.currentTimeMillis() - start;
		return time;
	}

	private void runRandoop(File testsFolder, String completeSuiteClassPath, String reducedPathRoot) {
		for(File file : testsFolder.listFiles()) {
			if(file.isDirectory()) {
				runRandoop(file, completeSuiteClassPath, reducedPathRoot);
			} else {
				if(file.getName().endsWith(".java")) {
					run(file.getAbsolutePath(), completeSuiteClassPath, reducedPathRoot);
				}
					
			}
		}
	}

	/*
	 * Runs randoop in every test suite in the project
	 */
	private void run(String suitePath, String completeSuiteClassPath, String reducedPathRoot) {
		String[] args = {
				"minimize",
				"--suitepath=" + suitePath,
				"--suiteclasspath=" + completeSuiteClassPath,
				reducedPathRoot
			};
			
			randoop.main.Main.main(args);
	}

	/*
	 * retrieves the jarpaths for junit, hamcrest
	 */
	private String getClasspath() {
		ClassLoader classLoader = new Configuration().getClass().getClassLoader();	 
		File [] jars = new File(classLoader.getResource("jar").getFile()).listFiles();
		StringBuilder sb = new StringBuilder();
		for(File f : jars) {
			sb.append(f.getAbsolutePath() + ";");
		}
		return sb.toString();
	}

	/*
	 * Gets the jar file name
	 */
	private String getJarName(String targetPath) {
		String name = null;
		for(String fileName : new File(targetPath).list()) {
			if(fileName.endsWith(".jar"))
				name = fileName;
		}
		return name;
	}

}
