package evaluation;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.AssumptionViolatedException;
import org.junit.rules.Stopwatch;
import org.junit.runner.Description;

public class MyJUnitStopWatch extends Stopwatch {

	public static Formatter formatter;
	public static ObjectOutputStream out;
	public static List<TimeResult> times = new ArrayList<TimeResult>(7000);
	

    
	static {
		try {
			formatter = new Formatter("times.csv");
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				MyJUnitStopWatch.formatter.close();
				try {
					out = new ObjectOutputStream(new FileOutputStream("timeResults.txt"));
					MyJUnitStopWatch.out.writeObject(times);
					MyJUnitStopWatch.out.flush();
					MyJUnitStopWatch.out.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
		});
	}

	private static void logInfo(Description description, String status, long nanos) {

		String testName = description.getClassName() + "." + description.getMethodName();
		formatter.format("%s,%s,%d\n", testName, status, nanos);
		times.add(new TimeResult(description.getClassName(), description.getMethodName(), status, nanos));
		// System.out.println(String.format("Test %s %s, spent %d microseconds",
		// testName, status,
		// TimeUnit.NANOSECONDS.toMicros(nanos)));
	}

	@Override
	protected void succeeded(long nanos, Description description) {
		 logInfo(description, "succeeded", nanos);
	}

	@Override
	protected void failed(long nanos, Throwable e, Description description) {
		 logInfo(description, "failed", nanos);
	}

	@Override
	protected void skipped(long nanos, AssumptionViolatedException e, Description description) {
		logInfo(description, "skipped", nanos);
	}

	@Override
	protected void finished(long nanos, Description description) {
//		logInfo(description, "finished", nanos);
	}
}
