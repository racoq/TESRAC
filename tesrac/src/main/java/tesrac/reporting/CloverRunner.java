package tesrac.reporting;

import java.io.IOException;

import tesrac.utils.CommandRunner;
import tesrac.utils.Logger;

/**
 * This class contains the default command to run OpenClover on some project and the method to run it
 * @author JBecho
 *
 */
public class CloverRunner {
	
	private static final Logger logger = Logger.getInstance(CloverRunner.class.getName());
	
	/*
	 * The default command to run OpenClover on some project
	 */
	private final static String COMMAND = "cmd /c mvn clean clover:setup test -Dmaven.test.failure.ignore=true clover:aggregate clover:clover";

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
		logger.info("Ran OpenClover on " + projectPath + " in " + (end-start)/1000 + "s");
		return val;
	}
}
