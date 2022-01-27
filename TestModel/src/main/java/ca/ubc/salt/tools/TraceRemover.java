package ca.ubc.salt.tools;

import java.io.File;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import ca.ubc.salt.model.utils.Settings;
import ca.ubc.salt.model.utils.Utils;

public class TraceRemover
{
    public static void main(String[] args)
    {
	Scanner sc = new Scanner(System.in);
	List<String> statementsToRemove = new LinkedList<String>();

	while (sc.hasNextLine())
	{
	    statementsToRemove.add(sc.nextLine());
	}

	Set<String> testCases = new HashSet<String>();
	for (String stmt : statementsToRemove)
	{
	    String testCase = Utils.getTestCaseNameFromTestStatement(stmt);
	    if (testCase != null)
		testCases.add(testCase);
	}
	System.out.println(testCases);
	removeTestCases(testCases);

    }

    public static void removeTestCases(Set<String> testCases)
    {
	File folder = new File(Settings.tracePaths);
	File[] files = folder.listFiles();
	for (File file : files)
	{
	    if (testCases.contains(Utils.getTestCaseNameFromTestStatement(file.getName())))
	    {
		Settings.consoleLogger.error(String.format("deleting %s", file.getName()));
		file.delete();
	    }
	}

    }
}
