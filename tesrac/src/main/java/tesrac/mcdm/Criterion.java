package tesrac.mcdm;

import java.util.List;

/**
 * This class defines a Criterion to use in the AHP
 * @author JBecho
 *
 */
public class Criterion {	

	//this is not used right now
	private String id;

	//The name of the criterion
	private String name;

	//The importances this criterion has related with other criterions (ordered as in the config file)
	private List<Double> importances;

	//The subcriteria of this criterion (can be empty)
	private List<Criterion> subcriteria;

	//The global prioirity of this criterion
	private double globalPriority;	

	/**
	 * Constructor
	 * @param id the id of the criterion
	 * @param name the name of the criterion
	 * @param importances the importances related to other criterion
	 * @param subCriteria the subcriteria
	 */
	public Criterion(String id, String name, List<Double> importances, List<Criterion> subCriteria) {
		this.id = id;
		this.name = name;
		this.importances = importances;
		this.subcriteria = subCriteria;
	}

	/**
	 * Returns the id of this criterion
	 * @return String
	 */
	public String getId() {
		return id;
	}

	/**
	 * Returns the name of this criterion
	 * @return String
	 */
	public String getName() {
		return name;
	}

	/**
	 * Returns a list of doubles that represent the importance of this criterion related to others
	 * @return
	 */
	public List<Double> getImportances() {
		return importances;
	}

	/**
	 * Returns a list of the subcriteria of this criterion
	 * @return
	 */
	public List<Criterion> getSubcriteria(){
		return subcriteria;
	}

	/**
	 * Returns a textual representation of this criterion for logging purposes
	 */
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(name);
		if(!subcriteria.isEmpty()) {
			sb.append("{");
			for(int i = 0; i < subcriteria.size(); i++) {
				sb.append(" " + subcriteria.get(i).toString());
				if(i != subcriteria.size()-1)
					sb.append(",");
				else
					sb.append("}");
			}			
		}		
		return sb.toString();
	}

	/**
	 * Sets the global priority of this criterion
	 * @param priority - the priority of this criterion
	 */
	public void setGlobalPriority(double priority) {
		globalPriority = priority;
	}

	/**
	 * Returns the global priority of this criterion
	 * @return double
	 */
	public double getGlobalPriority() {
		return globalPriority;
	}

}
