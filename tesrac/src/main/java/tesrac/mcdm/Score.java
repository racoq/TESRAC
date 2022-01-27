package tesrac.mcdm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import tesrac.utils.Pair;
import tesrac.utils.Subject;

/**
 * This class contains methods to help manage the scores of each tool 
 * @author JBecho
 *
 */
public class Score {

	/*
	 * The scores a tool got for each project 
	 */
	private HashMap<String, ArrayList<Pair<String, Double>>> scores;

	private static Score INSTANCE;

	/*
	 * Constructor
	 */
	private Score() {
		scores = new HashMap<>();
	}

	/**
	 * Returns the instance of Score
	 * @return
	 */
	public static Score getInstance() {
		if(INSTANCE == null)
			INSTANCE = new Score();
		return INSTANCE;
	}

	/**
	 * Saves a score
	 * @param toolName - the name of the tool that got the score
	 * @param subjectName - the name of the subject measured
	 * @param score - the value of the score
	 */
	public void put(String toolName, String subjectName, double score) {
		if(scores.get(toolName) == null)
			scores.put(toolName, new ArrayList<>());
		scores.get(toolName).add(new Pair<String, Double>(subjectName, score));
	}

	/**
	 * Gets a score a tool got for a subject
	 * @param toolName the name of the tool
	 * @param subjectName the name of the subject
	 * @return double - the score
	 */
	public double get(String toolName, String subjectName) {
		ArrayList<Pair<String, Double>> toolScores = scores.get(toolName);
		double score = 0;
		for(Pair<String, Double> pair : toolScores) {
			if(pair.getFirst().equals(subjectName)) {
				score = pair.getSecond();
				break;
			}
		}
		return score;
	}

	/**
	 * Returns a list containing pairs tool-finalScore, sorted by descending order of the finalScore
	 * @param subjects - the subjects of the experiment
	 * @return - list of Pair
	 */
	public ArrayList<Pair<String, Double>> getSortedToolsWithScores(List<Subject> subjects) {	
		ArrayList<Pair<String, Double>> finalScores = new ArrayList<>();
		for(String toolName : scores.keySet()) {
			ArrayList<Pair<String, Double>> toolScores = scores.get(toolName);
			double sum = 0;
			for(int i = 0; i < toolScores.size(); i++) {
				Pair<String, Double> pair = toolScores.get(i);
				sum += pair.getSecond() * subjects.get(i).getWeight();
			}
			double avg = sum / getTotalWeights(subjects);
			finalScores.add(new Pair<String, Double>(toolName, avg));
		}
		finalScores.sort((x,y) -> Double.compare(y.getSecond(), x.getSecond()));
		return finalScores;
	}

	/*
	 * Returns the sum of the weights of each project
	 */
	private double getTotalWeights(List<Subject> subjects) {
		int weights = 0;
		for(Subject s : subjects) {
			weights += s.getWeight();
		}
		return weights;
	}
}
