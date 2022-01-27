package evaluation;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;

public class Utils
{


    public static String getNamePrecision(String className, int precision)
    {
	for (int i = className.length() - 1; i >= 0; i--)
	{
	    if (className.charAt(i) == '.')
	    {
		precision--;
		if (precision == 0)
		    return className.substring(i + 1);
	    }
	}
	return "";
    }

    public static <T> Set intersection(List<Set<T>> sets)
    {
	Set common = new HashSet();
	if (sets.size() != 0)
	{
	    ListIterator<Set<T>> iterator = sets.listIterator();
	    common.addAll(iterator.next());
	    while (iterator.hasNext())
	    {
		common.retainAll(iterator.next());
	    }
	}
	return common;
    }

    public static boolean isTestClass(File classFile)
    {
	if (classFile.getAbsolutePath().contains("/test/"))
	    return true;
	else
	    return false;
    }

    public static void copyProject(String from, String to)
    {

	// String[] cmdRM = new String[]{"rm", "-r",
	// Settings.PROJECT_INSTRUMENTED_PATH};
	String[] cmdCP = new String[] { "cp", "-r", from, to };
	try
	{
	    System.out.println(runCommand(cmdCP, "/"));
	} catch (IOException | InterruptedException e)
	{
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
    }


    public static String runCommand(String[] commands, String path) throws IOException, InterruptedException
    {
	StringBuffer result = new StringBuffer();

	ProcessBuilder builder = new ProcessBuilder(commands);
	builder.redirectErrorStream(true);
	builder.directory(new File(path));
	Process process = builder.start();

	Scanner scInput = new Scanner(process.getInputStream());
	while (scInput.hasNext())
	{
	    String line = scInput.nextLine();
	    result.append(line);
	    result.append("\n");
	}
	process.waitFor();
	return result.toString();

    }

    public static String getTestClassNameFromTestCase(String testCase)
    {
	int index = testCase.lastIndexOf('.');
	String className = testCase.substring(0, index);
	return className;
    }

    public static <K, V> void addToTheListInMap(Map<K, List<V>> map, K key, V value)
    {
	List<V> list = map.get(key);
	if (list == null)
	{
	    list = new LinkedList<V>();
	    map.put(key, list);
	}
	list.add(value);
    }

    public static <K, V> void addToTheSetInMap(Map<K, Set<V>> map, K key, V value)
    {
	Set<V> list = map.get(key);
	if (list == null)
	{
	    list = new HashSet<V>();
	    map.put(key, list);
	}
	list.add(value);
    }

    public static <K, V> void addAllTheSetInMap(Map<K, Set<V>> map, K key, Set<V> value)
    {
	Set<V> list = map.get(key);
	if (value == null)
	    return;
	if (list == null)
	{
	    list = new HashSet<V>();
	    map.put(key, list);
	}
	list.addAll(value);
    }

    public static <K, V> void addAllTheListInMap(Map<K, List<V>> map, K key, List<V> value)
    {
	List<V> list = map.get(key);
	if (value == null)
	    return;
	if (list == null)
	{
	    list = new ArrayList<V>();
	    map.put(key, list);
	}
	list.addAll(value);
    }

    public static <K, V> boolean containsInSetInMap(Map<K, Set<V>> map, K key, V value)
    {
	Set<V> set = map.get(key);
	if (set == null)
	{
	    return false;
	}
	return set.contains(value);
    }

    public static <K, V> void removeFromTheSetInMap(Map<K, Set<V>> map, K key, V value)
    {
	Set<V> list = map.get(key);
	if (list != null)
	{
	    list.remove(value);
	} else
	    map.remove(key);
    }

    public static String getTestCaseName(String testCase)
    {
	int index = testCase.lastIndexOf('.');
	return testCase.substring(index + 1);
    }

    public static String getTestCaseNameFromTestStatementWithoutClassName(String testStatement)
    {
	String testCase = getTestCaseNameFromTestStatement(testStatement);
	return testCase.substring(testCase.lastIndexOf('.') + 1);
    }

    public static String getTestCaseNameFromTestStatement(String testStatement)
    {
	int index = testStatement.lastIndexOf('-');
	if (index == -1)
	    return null;
	return testStatement.substring(0, index);
    }

    public static Map<String, Set<String>> getTestClassMapFromTestCases(Set<String> testCases)
    {
	Map<String, Set<String>> map = new HashMap<String, Set<String>>();
	for (String testCase : testCases)
	{
	    String testClass = Utils.getTestClassNameFromTestCase(testCase);
	    Utils.addToTheSetInMap(map, testClass, testCase);
	}
	return map;
    }


    public static String nextOrPrevState(String state, ArrayList<String> sortedTestStatements, boolean next)
    {
	// int index = Collections.binarySearch(sortedTestStatements, state, new
	// NaturalOrderComparator());
	int index = sortedTestStatements.indexOf(state);
	if (index == -1 || (next && index == sortedTestStatements.size() - 1) || (!next && index == 0))
	    return "";

	String[] split = Utils.splitState(state);

	if (split.length != 2)
	    return "";

	String nextState = sortedTestStatements.get(index + (next ? 1 : -1));
	String[] splitNext = Utils.splitState(nextState);

	if (splitNext.length != 2)
	    return "";

	if (split[0].equals(splitNext[0]))
	    return nextState;
	else
	    return "";
    }

    public static String[] splitState(String state)
    {
	state = state.substring(0, state.lastIndexOf('.'));
	String[] split = state.split("-");
	return split;
    }





    // could be improved later !
    public static String getTestClassWithMaxNumberOfTestCases(Map<String, Set<String>> map)
    {
	int max = Integer.MIN_VALUE;
	String maxTestClass = null;
	for (Entry<String, Set<String>> entry : map.entrySet())
	{
	    if (max < entry.getValue().size())
	    {
		max = entry.getValue().size();
		maxTestClass = entry.getKey();
	    }
	}

	return maxTestClass;
    }


}
