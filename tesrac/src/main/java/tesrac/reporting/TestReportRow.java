package tesrac.reporting;

/**
 * This class represents a row of the report to be written (one for each test class)
 * @author JBecho
 *
 */
public class TestReportRow {

	/**
	 * The header of the table
	 */
	public static final String HEADER = 
			"R/NR;TestClass;ncLineCount;ExecutionTime(s);PassingTests;FailingTests;TotalTests;Size(bytes)";

	//Attributes
	private boolean reduced;
	private String className;
	private int ncLoc;
	private double executionTime;
	private int passedTests;
	private int failedTests;
	private int totalTests;
	private int fileSize;

	/**
	 * The constructor of the class
	 * @param name - name of the test class
	 * @param loc - number of lines of code of the test class
	 * @param time - time it took to run the class
	 * @param passed - how many tests passed
	 * @param failed - how many tests failed
	 * @param total - how many tests in total
	 * @param size - the file size in bytes
	 */
	public TestReportRow(boolean reduced, String name, int loc, double time, int passed, 
			int failed, int total, int size) {
		this.reduced = reduced;
		className = name;
		ncLoc = loc;
		executionTime = time;
		passedTests = passed;
		failedTests = failed;
		totalTests = total;
		fileSize = size;
	}

	/**
	 * Creates a TestReportRow by parsing a String
	 * @param line
	 */
	public TestReportRow(String line) {
		String[] info = line.split(";");
		this.reduced = (info[0].equals("Reduced")) ? true : false;
		this.className = info[1];
		ncLoc = Integer.parseInt(info[2]);
		executionTime = Double.parseDouble(info[3]);
		passedTests = Integer.parseInt(info[4]);
		failedTests = Integer.parseInt(info[5]);
		totalTests = Integer.parseInt(info[6]);
		fileSize = Integer.parseInt(info[7]);
	}

	/**
	 * Returns a textual representation of this object
	 */
	public String toString() {
		return String.format("%s;%s;%s;%s;%s;%s;%s;%s", (reduced?"Reduced":"Not Reduced"),
				className, ncLoc, executionTime, passedTests, 
				failedTests, totalTests, fileSize);
	}

	/**
	 * Returns the file size 
	 * @return int
	 */
	public int getFileSize() {
		return fileSize;
	}

	/**
	 * Returns the number of test cases
	 * @return int
	 */
	public int getNrTestCases() {
		return totalTests;
	}

	/**
	 * Returns the tests execution time
	 * @return double
	 */
	public double getTime() {
		return executionTime;
	}

	/**
	 * @return the reduced
	 */
	public boolean isReduced() {
		return reduced;
	}

	/**
	 * @return the className
	 */
	public String getClassName() {
		return className;
	}

	/**
	 * @return the ncLoc
	 */
	public int getNcLoc() {
		return ncLoc;
	}

	/**
	 * @return the passedTests
	 */
	public int getPassedTests() {
		return passedTests;
	}

	/**
	 * @return the failedTests
	 */
	public int getFailedTests() {
		return failedTests;
	}
}
