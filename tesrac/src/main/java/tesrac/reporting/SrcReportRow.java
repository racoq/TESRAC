package tesrac.reporting;

/**
 * This class represents a row of the report to be written (one for each src class)
 * @author JBecho
 *
 */
public class SrcReportRow {

	/**
	 * The header of the table
	 */
	public static final String HEADER = 
			"R/NR;Class;Complexity;TotalStatements;CoveredStatements;TotalBranches;"
					+ "CoveredBranches;TotalMethods;CoveredMethods;% CoveredStatements;"
					+ "% CoveredBranches;% CoveredMethods;TotalPercentageCovered";

	//Attributes
	private boolean reduced;
	private String className;	
	private int complexity;	
	private int totalStatements;	
	private int coveredStatements;	
	private int totalBranches;	
	private int coveredBranches;	
	private int totalMethods;	
	private int coveredMethods;	
	private double percentageStatements;	
	private double percentageBranches;
	private double percentageMethods;	
	private double totalPercentageCovered;

	/**
	 * Constructor of the class
	 * @param className - name of the src class
	 * @param complexity - complexity of the src class
	 * @param tStatements - total statements in src class
	 * @param cStatements - covered statements in src class
	 * @param tBranches - total brances in src class
	 * @param cBranches - covered statements in src class
	 * @param tMethods - total methods in src class
	 * @param cMethods - covered methods in src class
	 * @param pcBranches - percentage branches covered
	 * @param pcStatements - percentage of statements covered
	 * @param pcMethods - percentage of methods covered
	 * @param totalPercentageCovered - percentage of the code covered
	 */
	public SrcReportRow(boolean reduced, String className, int complexity, int tStatements, int cStatements, 
			int tBranches, int cBranches, int tMethods, int cMethods, double pcBranches, 
			double pcStatements, double pcMethods, double totalPercentageCovered) {
		this.reduced = reduced;
		this.className = className;
		this.complexity = complexity;
		totalStatements = tStatements;
		coveredStatements = cStatements;
		totalBranches = tBranches;
		coveredBranches = cBranches;
		totalMethods = tMethods;
		coveredMethods = cMethods;
		percentageStatements = pcStatements;
		percentageBranches = pcBranches;
		percentageMethods = pcMethods;
		this.totalPercentageCovered = totalPercentageCovered;
	}

	/**
	 * Creates a SrcReportRow by parsing a String
	 * @param line
	 */
	public SrcReportRow(String line) {
		String[] info = line.split(";");
		this.reduced = (info[0].equals("Reduced")) ? true : false;
		this.className = info[1];
		this.complexity = Integer.parseInt(info[2]);
		totalStatements = Integer.parseInt(info[3]);
		coveredStatements = Integer.parseInt(info[4]);
		totalBranches = Integer.parseInt(info[5]);
		coveredBranches = Integer.parseInt(info[6]);
		totalMethods = Integer.parseInt(info[7]);
		coveredMethods = Integer.parseInt(info[8]);
		percentageStatements = Double.parseDouble(info[9].replace(',', '.'));
		percentageBranches = Double.parseDouble(info[10].replace(',', '.'));
		percentageMethods = Double.parseDouble(info[11].replace(',', '.'));
		this.totalPercentageCovered = Double.parseDouble(info[12].replace(',', '.'));
	}

	/**
	 * Returns a textual representation of this object
	 */
	public String toString() {
		return String.format("%s;%s;%d;%d;%d;%d;%d;%d;%d;%.2f;%.2f;%.2f;%.2f", (reduced)?"Reduced":"Not Reduced",
				className, complexity, totalStatements, coveredStatements, totalBranches, coveredBranches,
				totalMethods, coveredMethods, percentageStatements, percentageBranches, percentageMethods, 
				totalPercentageCovered);
	}

	public String getClassName() {
		return className;
	}
	
	/**
	 * Returns the percentage of branch coverage
	 * @return double
	 */
	public double getPcBranches() {
		return percentageBranches;
	}

	/**
	 * Returns the total coverage of the project in percentage
	 * @return double
	 */
	public double getTotalCov() {
		return totalPercentageCovered;
	}

	/**
	 * Returns whether this row is from the reduced or original test suite
	 * @return boolean
	 */
	public boolean isReduced() {
		return reduced;
	}

	/**
	 * Returns the cyclomatic complexity 
	 * @return int
	 */
	public int getComplexity() {
		return complexity;
	}

	/**
	 * Returns the total of statements
	 * @return int
	 */
	public int getTotalStatements() {
		return totalStatements;
	}

	/**
	 * Returns the number of covered statements
	 * @return int
	 */
	public int getCoveredStatements() {
		return coveredStatements;
	}

	/**
	 * Returns the number of total branches
	 * @return int
	 */
	public int getTotalBranches() {
		return totalBranches;
	}

	/**
	 * Returns the number of covered branches
	 * @return int
	 */
	public int getCoveredBranches() {
		return coveredBranches;
	}

	/**
	 * Returns the number of total methods
	 * @return int
	 */
	public int getTotalMethods() {
		return totalMethods;
	}

	/**
	 * Returns the number of covered methods
	 * @return int
	 */
	public int getCoveredMethods() {
		return coveredMethods;
	}

	/**
	 * Returns the percentage of statements covered
	 * @return double
	 */
	public double getPercentageStatements() {
		return percentageStatements;
	}

	/**
	 * Returns the percentage of methods covered
	 * @return double
	 */
	public double getPercentageMethods() {
		return percentageMethods;
	}

}
