package ca.ubc.salt.model.evaluation;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Arrays;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ca.ubc.salt.model.merger.MergingResult;
import ca.ubc.salt.model.utils.Utils;
import evaluation.TimeResult;

public class CompareRunningTime {

	static final int rep = 100;
	static final int start = 25;
	static final String originalResultsPath = "results\\timeResultsOriginal.txt";
	static final String mergedResultsPath = "results\\timeResultsMerged.txt";
	static final String mergedInfoPath = "results\\mergingResult.txt";

	public static void main(String[] args) throws FileNotFoundException, IOException, ClassNotFoundException {
		ObjectInputStream in = new ObjectInputStream(new FileInputStream(originalResultsPath));
		List<TimeResult> timesOriginal = (List<TimeResult>) in.readObject();
		in.close();

		in = new ObjectInputStream(new FileInputStream(mergedResultsPath));
		List<TimeResult> timesMerged = (List<TimeResult>) in.readObject();
		in.close();

		in = new ObjectInputStream(new FileInputStream(mergedInfoPath));
		List<MergingResult> mergingResults = (List<MergingResult>) in.readObject();
		in.close();

		Formatter fr = new Formatter("timeCompare.csv");

		Map<String, List<TimeResult>> timesMergedMap = getTheMap(timesMerged);
		Map<String, List<TimeResult>> timesOriginalMap = getTheMap(timesOriginal);

		int stmtBefore = 0, stmtAfter = 0, testsBefore = 0, testsAfter = 0;
		outer: for (MergingResult mr : mergingResults) {
			long original = 0;
			for (String testCase : mr.getMergedTestCases()) {
				List<TimeResult> testCaseResult = timesOriginalMap.get(testCase);
				if (testCaseResult == null) {
					System.out.println(testCase + " is not run");
					continue outer;
				} else {
					if (testCaseResult.size() != rep) {
						System.out.println(
								"something's wrong with " + testCase + "it has only run for " + testCaseResult.size());
						continue outer;
					}
					original = sum(testCaseResult, 0.25);
				}
			}

			String mergedTestCaseName = mr.getMergedClassName() + "." + mr.getMergedTestCaseName();
			List<TimeResult> mergedResult = timesMergedMap.get(mergedTestCaseName);
			if (mergedResult != null) {
				long merged = 0;
				if (mergedResult.size() != rep) {
					System.out.println("merged test case " + mr.getMergedTestCaseName() + "it has only run for "
							+ mergedResult.size());
					continue;
				}
				merged = sum(mergedResult, 0.25);
				if (mergedResult.get(0).getStatus().equals("succeeded")) {
					fr.format("%s,%d,%d,%s,%d,%d,%d\n", mr.getMergedTestCases().toString().replace(",", " "),
							mr.getMergedTestCases().size(), mr.getAfter(), mergedTestCaseName, original, merged,
							original - merged);
					testsBefore += mr.getMergedTestCases().size();
					testsAfter++;
					stmtBefore += mr.getBefore();
					stmtAfter += mr.getAfter();
				}
			}
		}

		fr.flush();
		fr.close();
		System.out.println(String.format("stmt before : %d, stmt after : %d, testsBefore : %d, testsAfter : %d\n",
				stmtBefore, stmtAfter, testsBefore, testsAfter));

	}

	private static long outlierSum(List<TimeResult> testCaseResult, double percent) {
		long original = 0;

		long[] times = new long[testCaseResult.size()];
		int i = 0;
		for (TimeResult tr : testCaseResult) {
			times[i] = tr.getTime();
			i++;
		}

		Arrays.sort(times);

		int start = (int) (percent * testCaseResult.size());

		for (i = start; i < testCaseResult.size() - start; i++)
			original += times[i];

		return original;
	}

	private static long sum(List<TimeResult> testCaseResult, double start) {
		long original = 0;
		int startInt = (int) (testCaseResult.size() * start);
		for (int i = startInt; i < testCaseResult.size(); i++) {
			original += testCaseResult.get(i).getTime();
		}
		return original;
	}

	public static Map<String, List<TimeResult>> getTheMap(List<TimeResult> times) {
		Map<String, List<TimeResult>> map = new HashMap<String, List<TimeResult>>();
		for (TimeResult tr : times) {
			// int index = tr.getTestClassName().lastIndexOf('.');
			// String key = tr.getTestClassName().substring(index + 1);
			// key = key + "." + tr.getTestCaseName();
			String key = tr.getTestClassName() + "." + tr.getTestCaseName();

			Utils.addToTheListInMap(map, key, tr);
			// map.put(key, tr);
		}

		return map;
	}

}
