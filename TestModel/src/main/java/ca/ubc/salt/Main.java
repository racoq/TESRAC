package ca.ubc.salt;

import static org.junit.Assume.assumeNoException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;

import org.xml.sax.SAXParseException;

import ca.ubc.salt.model.instrumenter.Instrumenter;
import ca.ubc.salt.model.merger.BackwardTestMerger;
import ca.ubc.salt.model.merger.TestMerger;
import ca.ubc.salt.model.utils.Settings;
import ca.ubc.salt.model.utils.SuperArray;

public class Main {

	public static final String COMMAND = "mvn test -Drat.skip=true -Dmaven.test.failure.ignore=true -Denforcer.skip=true -Dpmd.skip=true -Dmaven.antrun.skip=true -Dcheckstyle.skip";

	public static void main(String[] args) throws FileNotFoundException, ClassNotFoundException, SAXParseException, IOException, CloneNotSupportedException, InterruptedException, NoSuchFieldException, IllegalAccessException {
		
		run(args);
	}

	private static void run(String[] args) throws IOException, InterruptedException, ClassNotFoundException, SAXParseException, CloneNotSupportedException, NoSuchFieldException, IllegalAccessException {
		
		// RUN INSTRUMENTER
		System.out.println("- Running instrumenter on project " + Settings.PROJECT_PATH);
		Instrumenter.main(null); //args nao eh usado	
		System.out.println("- Instrumenter done ");

		// MVN TEST INSTRUMENTED PROJECT
		System.out.println("- mvn test " + Settings.PROJECT_INSTRUMENTED_PATH);
		runCommand(new String[] {"cmd", "/c", COMMAND}, Settings.PROJECT_INSTRUMENTED_PATH);
		System.out.println("- mvn test done.");

		// MERGING USECASE
		System.out.println("- testMerger init");
		BackwardTestMerger.main(null);
		System.out.println("- testMerger done.");


	}

	private static long msToMin(long elapsedTime) {
		return (elapsedTime / 1000) / 60;
	}

	private static long msToS(long elapsedTime) {
		return (elapsedTime / 1000) % 60;     
	}

	private static void runCommand(String[] commands, String path) throws IOException, InterruptedException {
		//StringBuffer result = new StringBuffer();

		System.out.print("Running command: ");
		for(String s : commands) {
			System.out.print(s + " ");
		}
		System.out.println();
		ProcessBuilder builder = new ProcessBuilder(commands);
		builder.redirectErrorStream(true);
		builder.directory(new File(path));
		Process process = builder.start();
		Scanner scInput = new Scanner(process.getInputStream());

		while (scInput.hasNextLine()) {
			String line = scInput.nextLine();
			System.out.println(line);
			//result.append(line);
			//result.append("\n");

		}
		process.waitFor();
		scInput.close();
		//return result.toString();

	}

}
