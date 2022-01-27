package ca.ubc.salt.model.merger;

import java.io.Serializable;
import java.util.Set;

public class MergingResult implements Serializable {

	private static final long serialVersionUID = 1720396600141708542L;
	public boolean fatalError = false;
	public boolean warning = false;
	public boolean couldntsatisfy = false;
	String mergedClassName;
	String mergedTestCaseName;
	Set<String> mergedTestCases;
	int before, after;

	public MergingResult(String mergedClassName, Set<String> mergedTestCases) {
		this.mergedClassName = mergedClassName;
		this.mergedTestCases = mergedTestCases;
	}

	public boolean isFatalError() {
		return fatalError;
	}

	public void setFatalError(boolean fatalError) {
		this.fatalError = fatalError;
	}

	public boolean isWarning() {
		return warning;
	}

	public void setWarning(boolean warning) {
		this.warning = warning;
	}

	public boolean isCouldntsatisfy() {
		return couldntsatisfy;
	}

	public void setCouldntsatisfy(boolean couldntsatisfy) {
		this.couldntsatisfy = couldntsatisfy;
	}

	public String getMergedClassName() {
		return mergedClassName;
	}

	public void setMergedClassName(String mergedClassName) {
		this.mergedClassName = mergedClassName;
	}

	public String getMergedTestCaseName() {
		return mergedTestCaseName;
	}

	public void setMergedTestCaseName(String mergedTestCaseName) {
		this.mergedTestCaseName = mergedTestCaseName;
	}

	public Set<String> getMergedTestCases() {
		return mergedTestCases;
	}

	public void setMergedTestCases(Set<String> mergedTestCases) {
		this.mergedTestCases = mergedTestCases;
	}

	public int getBefore() {
		return before;
	}

	public void setBefore(int before) {
		this.before = before;
	}

	public int getAfter() {
		return after;
	}

	public void setAfter(int after) {
		this.after = after;
	}

}
