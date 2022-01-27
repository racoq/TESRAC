package tesrac.utils;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;

public class CommandRunner {
	
	private static final Logger logger = Logger.getInstance(CommandRunner.class.getName());
	
	/**
	 * Executes a given command 
	 * @param commands - command to run
	 * @param path - where to execute the command
	 * @param output - whether to show the command output (true) or not (false)
	 * @return command exit value
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public static int run(String command, String path, boolean output) throws IOException, InterruptedException {
		logger.info("Running command: " + command);
		String[] commands = command.split(" ");
		ProcessBuilder builder = new ProcessBuilder(commands);
		builder.redirectErrorStream(true);
		builder.directory(new File(path));
		Process process = builder.start();
		Scanner scInput = new Scanner(process.getInputStream());
		while (scInput.hasNextLine()) {
			String line = scInput.nextLine();
			if(output)System.out.println(line);
		}
		process.waitFor();
		int val = process.exitValue();
		scInput.close(); 
		return val;
	}
	
}
