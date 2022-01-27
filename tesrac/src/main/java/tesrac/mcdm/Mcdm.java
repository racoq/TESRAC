package tesrac.mcdm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import tesrac.reporting.SrcReportRow;
import tesrac.reporting.TestReportRow;
import tesrac.utils.Configuration;
import tesrac.utils.Pair;
import tesrac.utils.Subject;
import tesrac.utils.Triple;

/**
 * This class contains methods to help with the management of the values measured by OpenClover
 * @author JBecho
 *
 */
public class Mcdm {

	/*
	 * The criteria considered
	 */
	private List<Criterion> criteria;

	/*
	 * The values measured for a triple (Project, Tool, Criterion)
	 */
	private Map<Triple, Double> values;

	/**
	 * Constructor
	 * Receives a list of criterion and computes their priority 
	 * @param list
	 */
	public Mcdm(List<Criterion> list) {
		criteria = list;
		setAllPriorities();
		values = new HashMap<>();
	}

	/*
	 * Computes the priorities for all criterion according to the AHP
	 */
	private void setAllPriorities() {
		//Sets priorities for the top level criterion
		setPriorities(criteria);

		//Sets priorities for the subcriteria 
		for(int i = 0; i < criteria.size(); i++) {
			Criterion crit = criteria.get(i);
			List<Criterion> subCriteria = crit.getSubcriteria();
			setPriorities(subCriteria);
			//Sets the global priority for the subcriteria
			for(Criterion sub : subCriteria) {
				double localP = sub.getGlobalPriority();
				sub.setGlobalPriority(localP * crit.getGlobalPriority());
			}
		}
	}

	/*
	 * Computes the priorities for a given list of criterion
	 */
	private void setPriorities(List<Criterion> criteria) {
		double[] priorities = new PairwiseComparisonMatrix(criteria).getPriorities();
		for(int i = 0; i < priorities.length; i++)
			criteria.get(i).setGlobalPriority(priorities[i]);
	}

	/**
	 * Returns the criteria considered
	 * @return
	 */
	public List<Criterion> getCriteria(){
		return criteria;
	}

	/**
	 * Saves the values measured for each criterion for a given Tool and Subject
	 * @param tool - the tool name
	 * @param subject - the subject name
	 * @param rSrcRow - the cumulative reduced src row
	 * @param oSrcRow - the cumulative original src row
	 * @param rTstRow - the cumulative reduced test row
	 * @param oTstRow - the cumulative original test row
	 * @param rTotCov - the reduced total coverage
	 * @param oTotCov - the original total coverage
	 * @param rMutCov 
	 * @param oMutCov 
	 */
	public void saveValues(String tool, String subject, SrcReportRow rSrcRow, SrcReportRow oSrcRow,
			TestReportRow rTstRow, TestReportRow oTstRow, double rTotCov, double oTotCov, double oMutCov, double rMutCov) {	
		
		
		//Function to apply for the cost criteria
		BiFunction<Double, Double, Double> f1 = (x,y) ->  y == 0 ? (x == 0 ? 0 : -10) : ((1-(x/y))*100);

		//Function to apply for the benefit criteria
		BiFunction<Double, Double, Double> f2 = (x,y) ->  y == 0 ? x : ((x/y)*100);

		//Dimension	==============  COST
		//FileSize
		double rFileSize = (double)rTstRow.getFileSize();
		double nrFileSize = (double)oTstRow.getFileSize();
		Triple filesize = new Triple(tool, subject, Configuration.FILESIZE);	
		values.put(filesize, f1.apply(rFileSize,nrFileSize));

		//TestCasesQty
		double rTestQty = (double)rTstRow.getNrTestCases();
		double nrTestQty = (double)oTstRow.getNrTestCases();
		Triple testQty = new Triple(tool, subject, Configuration.CASES_QTY);
		values.put(testQty, f1.apply(rTestQty, nrTestQty));

		//Coverage =============== BENEFIT
		//PcBranchCovered
		double rPcBranches = rSrcRow.getPcBranches();
		double nrPcBranches = oSrcRow.getPcBranches() == 0 ? 0.0001 : oSrcRow.getPcBranches();
		Triple pcBranches = new Triple(tool, subject, Configuration.PC_BRANCH_COVERAGE);
		values.put(pcBranches, f2.apply(rPcBranches, nrPcBranches));

		//PcTotalCovered
		Triple pcTotalCovered = new Triple(tool, subject, Configuration.PC_TOTAL_COVERAGE);
		values.put(pcTotalCovered, f2.apply(rTotCov, oTotCov));
		
		//Mutation Score
		Triple mutScore = new Triple(tool, subject, Configuration.PC_MUTATION_SCORE);
		values.put(mutScore,  f2.apply(rMutCov, oMutCov));

		//Time ===================  COST
		//TestsExecutionTime
		double rTime = rTstRow.getTime();
		double nrTime = oTstRow.getTime();
		Triple time = new Triple(tool, subject, Configuration.EXECUTION_TIME);
		values.put(time, f1.apply(rTime,nrTime));
	}
	
	/**
	 * Method to get a baseline score 
	 * @return
	 */
	public double getBaseLine() {
		
		ArrayList<Pair<String, Double>> scoresBySubCriterion = new ArrayList<>();
		
		//Dimension	==============  COST
		//FileSize   0
		scoresBySubCriterion.add(new Pair<>(Configuration.FILESIZE, 0.0));

		//TestCasesQty   0
		scoresBySubCriterion.add(new Pair<>(Configuration.CASES_QTY, 0.0));

		//Coverage =============== BENEFIT
		//PcBranchCovered    100
		scoresBySubCriterion.add(new Pair<>(Configuration.PC_BRANCH_COVERAGE, 1.0));

		//PcTotalCovered    100
		scoresBySubCriterion.add(new Pair<>(Configuration.PC_TOTAL_COVERAGE, 1.0));
		
		//PcTotalCovered    100
		scoresBySubCriterion.add(new Pair<>(Configuration.PC_MUTATION_SCORE, 1.0));

		//Time ===================  COST
		//TestsExecutionTime   0;
		scoresBySubCriterion.add(new Pair<>(Configuration.EXECUTION_TIME, 0.0));
		
		double baseline = 0;
		for(Pair<String, Double> pair : scoresBySubCriterion) 
			baseline += getSubCriterionPrioirty(pair.getFirst()) * pair.getSecond();
		
		return baseline;
	}

	private double getSubCriterionPrioirty(String criterion) {
		List<Criterion> criteria = getBaseLevelCriteria();
		for(Criterion c : criteria) {
			if(c.getName().equals(criterion))
				return c.getGlobalPriority();
		}
		return -1;
	}

	/**
	 * Returns the base level criteria 
	 * @return A list of Criterion
	 */
	public List<Criterion> getBaseLevelCriteria(){
		ArrayList<Criterion> baseLevelCriteria = new ArrayList<>();
		for(Criterion c : criteria)
			getBaseLevelCriteriaFrom(c, baseLevelCriteria);
		return baseLevelCriteria;
	}

	/*
	 * Returns the base level criteria from a criterion
	 */
	private void getBaseLevelCriteriaFrom(Criterion c, List<Criterion> baseLevel) {
		List<Criterion> sub = c.getSubcriteria();
		if(sub != null && sub.size() == 0)
			baseLevel.add(c);
		for(Criterion subC : sub)
			getBaseLevelCriteriaFrom(subC, baseLevel);	
	}

	/**
	 * Sets the scores for each tool
	 * @param subjects the list of Subject
	 * @param tools the list of tools' names
	 */
	public void setScores(List<Subject> subjects, List<String> tools) {
		//Normalizes the values
		normalizeValues(subjects, tools);
		Score scores = Score.getInstance();
		for(Subject s : subjects) {
			String subject = s.getProjectName();
			List<Criterion> baseCriteria = getBaseLevelCriteria();
			for(String tool : tools) {
				double score = 0;
				//For each criterion gets the value measured and adds it to the score
				//multiplied by that criterion priority
				for(Criterion c : baseCriteria) {
					Triple triple = new Triple(tool, subject, c.getName());
					score += (c.getGlobalPriority() * values.get(triple));
				}
				scores.put(tool, subject, score);
			}
		}	
	}

	/*
	 * Normalize the values measured for a given criterion for the same project with all the tools
	 */
	private void normalizeValues(List<Subject> subjects, List<String> tools) {
		for(Subject s : subjects) {
			String subject = s.getProjectName();
			for(Criterion c : getBaseLevelCriteria()) {
				List<Double> vals = getValuesForProjectCriterion(subject, c.getName(), tools);
				if(anyNegative(vals)) 
					vals = changeToPositive(vals);
				double max = getMax(vals);
				if(max != 0) {
					for(int i = 0; i < tools.size(); i++) {
						Triple triple = new Triple(tools.get(i), subject, c.getName());
						double oldValue = vals.get(i);
						values.put(triple, oldValue/max);
					}
				}
			}		
		}
	}

	//Checks if there's any negative value in the given list
	private boolean anyNegative(List<Double> vals) {
		for(double d : vals) {
			if(d < 0)
				return true;
		}
		return false;
	}

	/*
	 * Returns the max value in a list of doubles
	 */
	public double getMax(List<Double> vals) {
		double max = vals.get(0);
		for(int i = 1; i < vals.size(); i++) {
			if(max < vals.get(i))
				max = vals.get(i);
		}
		return max;
	}

	/*
	 * Shifts the values so that the minimum value will be 0
	 */
	public List<Double> changeToPositive(List<Double> vals) {
		List<Double> newList = new ArrayList<>(vals.size());
		double min = vals.get(0);
		for(double d : vals) {
			if(min > d)
				min = d;
		}
		min = Math.abs(min);
		for(int i = 0; i < vals.size(); i++) {
			newList.add(vals.get(i) + min);
		}
		return newList;
	}

	/*
	 * Returns the different values measured by the tools for the same project and criterion 
	 */
	private List<Double> getValuesForProjectCriterion(String subject, String criterion, List<String> tools) {
		List<Double> doubles = new ArrayList<>();
		for(String tool : tools) {
			Triple triple = new Triple(tool, subject, criterion);
			doubles.add(values.get(triple));
		}
		return doubles;
	}
}
