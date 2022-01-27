package tesrac.mcdm;

import java.util.List;

/**
 * This class contains the implementation of the pairwise comparison matrix used in the AHP
 * @author JBecho
 *
 */
public class PairwiseComparisonMatrix {

	/*
	 * The comparison matrix
	 */
	private double [][] matrix;

	/*
	 * The number of criterions
	 */
	private final int criteriaNumber;

	/*
	 * The array with the priorities
	 */
	private double[] priorities;

	/**
	 * Constructor
	 * @param criteria
	 */
	public PairwiseComparisonMatrix(List<Criterion> criteria) {
		criteriaNumber = criteria.size();
		if(criteriaNumber == 1) {
			priorities = new double[]{1};
		} else {
			matrix = new double[criteriaNumber][criteriaNumber];
			initMatrix(criteria);
			priorities = new double[criteriaNumber];
			definePriorities();
		}
	}

	/**
	 * Returns the priorities for the criteria
	 * @return array of double
	 */
	public double[] getPriorities() {
		return priorities;
	}

	/*
	 * Initialize the matrix
	 */
	private void initMatrix(List<Criterion> criteria){
		int getIdx = 0;
		for(int i = 0; i < matrix.length; i++) {
			for(int j = 0; j < matrix[0].length; j++) {
				if(i == j) {
					matrix[i][j] = 1;
				} else {
					matrix[i][j] = criteria.get(i).getImportances().get(getIdx);
					getIdx++;
				}
			}
			getIdx = 0;
		}
	}

	/*
	 * Computes the priorities for each criterion
	 */
	private void definePriorities() {
		double[] sum = getSum();
		double[] sumForAvg = new double[criteriaNumber];	
		//Normalize
		for(int i = 0; i < sum.length; i++) {
			for(int j = 0; j < matrix.length; j++) {
				matrix[j][i] = matrix[j][i]/sum[i];
			}
		}
		//Define priorities
		for(int i = 0; i < matrix.length; i++) {
			for(int j = 0; j < matrix.length; j++) {
				sumForAvg[i] += matrix[i][j];
			}
			priorities[i] = sumForAvg[i]/criteriaNumber;
		}
	}

	/*
	 * Returns an array with the sums for each column of the matrix
	 */
	private double[] getSum() {
		double[] sum = new double[criteriaNumber];
		for(int i = 0; i < sum.length; i++) {
			for(int j = 0; j < matrix.length; j++)
				sum[i] += matrix[j][i];
		}
		return sum;
	}

}
