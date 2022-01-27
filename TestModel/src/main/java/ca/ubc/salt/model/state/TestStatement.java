package ca.ubc.salt.model.state;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Statement;

import ca.ubc.salt.model.composer.TestCaseComposer;
import ca.ubc.salt.model.utils.Pair;
import ca.ubc.salt.model.utils.Utils;

public class TestStatement extends TestModelNode {
	Set<TestState> compatibleStates;
	TestState start, end;
	String name;
	public Statement statement;
	public Statement refactoredStatement;
	String methodCall;
	String input;
	Map<String, Pair<String, String>> sideEffects;
	Map<String, String> newVars;
	Map<String, SimpleName> vars;

	public long time = 1000;

	public Map<String, Set<String>> readGoals;
	public Map<String, Set<VarDefinitionPreq>> defineGoals;
	public Map<String, String> renameMap;

	public void initSideEffects(List<String> testCases, Set<VarDefinitionPreq> defPreq) {
		sideEffects = new HashMap<String, Pair<String, String>>();
		newVars = new HashMap<String, String>();
		try {
			Map<String, String> before = TestCaseComposer.nameValuePairs.get(this.name);
			String nextState = Utils.nextOrPrevState(testCases, this.name, true);
			Map<String, String> after = TestCaseComposer.nameValuePairs.get(nextState);

			for (Entry<String, String> entry : after.entrySet()) {
				String varName = entry.getKey();
				String varValAfter = entry.getValue();
				String varValBefore = before.get(varName);
				if (!varValAfter.equals(varValBefore)) {
					if (varValBefore != null)
						sideEffects.put(varName, new Pair<String, String>(varValBefore, varValAfter));
					else
						newVars.put(varName, varValAfter);
				} else {
					if (defPreq != null)
						for (VarDefinitionPreq def : defPreq) {
							if (def.getName().getIdentifier().equals(varName))
								sideEffects.put(varName, new Pair<String, String>(varValBefore, varValAfter));
						}
				}
			}
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public TestStatement(TestState start, TestState end, String name) {
		this.start = start;
		this.end = end;
		this.name = name;
		compatibleStates = new HashSet<TestState>();

	}

	public Set<TestState> getCompatibleStates() {
		return compatibleStates;
	}

	public void setCompatibleStates(Set<TestState> compatibleStates) {
		this.compatibleStates = compatibleStates;
	}

	public TestState getStart() {
		return start;
	}

	public void setStart(TestState start) {
		this.start = start;
	}

	public TestState getEnd() {
		return end;
	}

	public void setEnd(TestState end) {
		this.end = end;
	}

	@Override
	public String toString() {
		// TODO Auto-generated method stub
		if (this.refactoredStatement != null)
			return this.refactoredStatement.toString();
		if (this.statement != null)
			return this.statement.toString();
		else
			return this.name;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof TestStatement) {
			TestStatement tst = (TestStatement) obj;
			if (tst.start.equals(this.start) && tst.end.equals(this.end) // &&
			// tst.methodCall.equals(this.methodCall)
			)
				return true;
		}
		return false;
	}

	@Override
	public TestStatement clone() throws CloneNotSupportedException {
		// TODO Auto-generated method stub

		TestStatement clone = new TestStatement(this.start, this.end, this.name);
		clone.statement = this.statement;
		clone.refactoredStatement = this.refactoredStatement;
		clone.sideEffects = this.sideEffects;
		clone.newVars = this.newVars;
		clone.time = this.time;
		clone.parent.putAll(this.parent);
		clone.distFrom.putAll(this.distFrom);
		clone.renameMap = this.renameMap;
		if (readGoals != null) {
			Map<String, Set<String>> newGoals = new HashMap<String, Set<String>>();
			for (Entry<String, Set<String>> entry : this.readGoals.entrySet()) {
				Utils.addAllTheSetInMap(newGoals, entry.getKey(), entry.getValue());
			}
			clone.readGoals = newGoals;
		}
		if (defineGoals != null) {

			Map<String, Set<VarDefinitionPreq>> newDefGoal = new HashMap<String, Set<VarDefinitionPreq>>();
			for (Entry<String, Set<VarDefinitionPreq>> entry : this.defineGoals.entrySet()) {
				Utils.addAllTheSetInMap(newDefGoal, entry.getKey(), entry.getValue());
			}
			clone.defineGoals = newDefGoal;
		}
		return clone;

	}

	public Map<String, Pair<String, String>> getSideEffects() {
		return sideEffects;
	}

	public Map<String, String> getNewVars() {
		return newVars;
	}

	public void setNewVars(Map<String, String> newVars) {
		this.newVars = newVars;
	}

	// @Override
	// public int hashCode()
	// {
	//
	// return this.start.hashCode() * 13 + this.end.hashCode() * 17 +
	// this.methodCall == null ? 0 : this.methodCall.hashCode();
	//
	// }

	public Map<String, SimpleName> getAllVars() {
		if (this.vars == null)
			this.vars = TestCaseComposer.getAllVars(this.statement);
		return this.vars;
	}

	public String getTypeOfVar(String varName) {
		Map<String, SimpleName> varSims = getAllVars();
		SimpleName sim = varSims.get(varName);
		if (sim == null)
			return "";
		// ITypeBinding typeBind = sname.resolveTypeBinding();
		IBinding bind = sim.resolveBinding();
		IVariableBinding iv = (IVariableBinding) bind;
		ITypeBinding typeBind = (iv != null ? iv.getType() : null);
		if (typeBind != null)
			return typeBind.getName();
		else {
			// Settings.consoleLogger.error("typeBinding is null for " +
			// sim.toString() + " in " + this.toString());
		}

		return "";
	}

}
