package tesrac.utils;

/**
 * This class represents a triple of tool subject and criterion
 * @author JBecho
 *
 */
public class Triple implements Comparable<Triple> {

	//The name of criterion
	private String criterion;

	//The name of the tool
	private String tool;
	
	//The name of the subject
	private String subject;
	
	/**
	 * Constructor
	 * @param tool the name of the tool
	 * @param subj the name of the subject
	 * @param crit the name of the criterion
	 */
	public Triple(String tool, String subj, String crit) {
		this.criterion = crit;
		this.tool = tool;
		this.subject = subj;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((criterion == null) ? 0 : criterion.hashCode());
		result = prime * result + ((subject == null) ? 0 : subject.hashCode());
		result = prime * result + ((tool == null) ? 0 : tool.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof Triple))
			return false;
		Triple other = (Triple) obj;
		if (criterion == null) {
			if (other.criterion != null)
				return false;
		} else if (!criterion.equals(other.criterion))
			return false;
		if (subject == null) {
			if (other.subject != null)
				return false;
		} else if (!subject.equals(other.subject))
			return false;
		if (tool == null) {
			if (other.tool != null)
				return false;
		} else if (!tool.equals(other.tool))
			return false;
		return true;
	}
	
	public String toString() {
		return "(" + subject + ", " + tool + ", " + criterion + ")";
	}
	
	/**
	 * @return the criterion
	 */
	public String getCriterion() {
		return criterion;
	}

	/**
	 * @return the tool
	 */
	public String getTool() {
		return tool;
	}

	/**
	 * @return the subject
	 */
	public String getSubject() {
		return subject;
	}

	@Override
	public int compareTo(Triple o) {
		int project = this.subject.compareTo(o.subject);
		if(project != 0)
			return project;
		int crit = this.criterion.compareTo(o.criterion);
		if(crit != 0)
			return crit;
		return this.tool.compareTo(o.tool);
	}
	
}
