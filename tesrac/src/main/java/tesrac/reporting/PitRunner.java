package tesrac.reporting;

import java.io.IOException;

import tesrac.utils.CommandRunner;
import tesrac.utils.Logger;

/**
 * This class contains the default command to run PIT on some project and the method to run it
 * @author JBecho
 *
 */
public class PitRunner {
	
	private static final Logger logger = Logger.getInstance(PitRunner.class.getName());
	
	/*
	 * The default command to run PIT on some project
	 */
	private final static String COMMAND = "cmd /c mvn org.pitest:pitest-maven:mutationCoverage";

	/**
	 * Runs OpenClover on the project denoted by the given path
	 * @param projectPath - the path to the project
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public static int run(String projectPath) throws IOException, InterruptedException {
		long start = System.currentTimeMillis();
		int val = CommandRunner.run(COMMAND, projectPath, true);
		long end = System.currentTimeMillis();
		logger.info("Ran PIT on " + projectPath + " in " + (end-start)/1000 + "s");
		return val;
	}
}
