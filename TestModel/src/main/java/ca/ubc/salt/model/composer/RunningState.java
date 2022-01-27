package ca.ubc.salt.model.composer;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IExtendedModifier;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

import java.util.Map.Entry;

import Comparator.NaturalOrderComparator;
import ca.ubc.salt.model.state.TestStatement;
import ca.ubc.salt.model.state.VarDefinitionPreq;
import ca.ubc.salt.model.utils.FileUtils;
import ca.ubc.salt.model.utils.Pair;
import ca.ubc.salt.model.utils.Settings;
import ca.ubc.salt.model.utils.Utils;

public class RunningState {
	Map<String, String> nameValuePairForCurrentState;
	Map<String, Set<String>> valueNamePairForCurrentState;
	Map<String, Set<String>> typeNamePairForCurrentState;
	Map<String, String> nameTypePairForCurrentState;

	public RunningState() {
		nameValuePairForCurrentState = new HashMap<String, String>();
		valueNamePairForCurrentState = new HashMap<String, Set<String>>();
		typeNamePairForCurrentState = new HashMap<String, Set<String>>();
		nameTypePairForCurrentState = new HashMap<String, String>();
	}

	public String getValue(String name) {
		return nameValuePairForCurrentState.get(name);
	}

	public Set<String> getName(String value) {
		return valueNamePairForCurrentState.get(value);
	}

	public String getType(String name) {
		return nameTypePairForCurrentState.get(name);
	}

	public Set<String> getNameForType(String type) {
		return typeNamePairForCurrentState.get(type);
	}

	public void put(String name, String value, String type) {
		String prevVal = this.getValue(name);
		if (prevVal != null) {
			// valueNamePairForCurrentState.remove(prevVal);
			Utils.removeFromTheSetInMap(valueNamePairForCurrentState, prevVal, name);
		}
		nameValuePairForCurrentState.put(name, value);
		Utils.addToTheSetInMap(valueNamePairForCurrentState, value, name);

		nameTypePairForCurrentState.put(name, type);
		Utils.addToTheSetInMap(typeNamePairForCurrentState, type, name);

		// valueNamePairForCurrentState.put(value, name);
	}

	public void update(TestStatement stmt, Map<String, String> renameMap,
			Map<String, Set<VarDefinitionPreq>> defPreqs) {
		Map<String, SimpleName> vars = stmt.getAllVars();
		if (vars == null)
			return;
		Set<String> varsName = Utils.getNameSet(vars.values());

		for (Entry<String, Pair<String, String>> entry : stmt.getSideEffects().entrySet()) {
			String name = entry.getKey();
			String value = entry.getValue().getSecond();
			String oldValue = entry.getValue().getFirst();
			putTheVar(stmt, renameMap, vars, varsName, name, value, oldValue);
		}

		for (Entry<String, String> entry : stmt.getNewVars().entrySet()) {
			String name = entry.getKey();
			String value = entry.getValue();
			putTheVar(stmt, renameMap, vars, varsName, name, value, null);
		}

		if (defPreqs.get(stmt.getName()) != null)
			for (VarDefinitionPreq preq : defPreqs.get(stmt.getName())) {
				String name = preq.getName().getIdentifier();
				Pair<String, String> sideEffect = stmt.getSideEffects().get(name);
				if (sideEffect != null) {
					String value = sideEffect.getSecond();
					putTheVar(stmt, renameMap, vars, varsName, name, value, null);
				}
			}

	}

	public void putTheVar(TestStatement stmt, Map<String, String> renameMap, Map<String, SimpleName> vars,
			Set<String> varsName, String name, String value, String oldValue) {
		if (varsName.contains(name)) {

			String renamedName = renameMap.get(name);

			SimpleName sname = vars.get(name);
			// String type = getTypeOfValue(value);
			String type = "";
			if (sname != null) {
				// ITypeBinding typeBind = sname.resolveTypeBinding();
				IBinding bind = sname.resolveBinding();
				IVariableBinding iv = (IVariableBinding) bind;

				ITypeBinding typeBind = (iv != null ? iv.getType() : null);
				if (typeBind != null) {
					type = typeBind.getName();
					// type = addFinalLiteral(sname, type);

				} else {
					// Settings.consoleLogger
					// .error("typeBinding is null for " + sname.toString() + "
					// in " + stmt.toString());
				}
			}
			if (renamedName != null)
				name = renamedName;

			this.put(name, value, type);
		} else

		{
			Set<String> names = this.getName(oldValue);
			if (names != null && !names.isEmpty()) {
				String inStateName = names.contains(name) ? name : names.iterator().next();
				this.put(inStateName, value, this.getType(inStateName));
			} else
				Settings.consoleLogger.error("Stmt changes a var's value that is not in the state");
		}
	}

	public static String addFinalLiteral(SimpleName sname, String type) {
		ASTNode parent = sname.getParent();
		if (parent != null) {
			ASTNode grandParent = parent.getParent();
			if (grandParent != null && grandParent.getNodeType() == ASTNode.VARIABLE_DECLARATION_STATEMENT) {
				VariableDeclarationStatement vds = (VariableDeclarationStatement) grandParent;
				List<IExtendedModifier> mods = vds.modifiers();
				for (IExtendedModifier imod : mods) {
					if (imod instanceof Modifier) {
						Modifier mod = (Modifier) imod;
						if (mod.isFinal()) {
							type = "final " + type;
							break;
						}
					}
				}
			}
		}
		return type;
	}

	@Override
	public RunningState clone() throws CloneNotSupportedException {
		// TODO Auto-generated method stub
		RunningState rs = new RunningState();
		rs.nameValuePairForCurrentState.putAll(this.nameValuePairForCurrentState);
		for (Entry<String, Set<String>> entry : this.valueNamePairForCurrentState.entrySet()) {
			Set<String> newSet = new HashSet<String>();
			newSet.addAll(entry.getValue());
			rs.valueNamePairForCurrentState.put(entry.getKey(), newSet);

		}
		rs.nameTypePairForCurrentState.putAll(this.nameTypePairForCurrentState);
		for (Entry<String, Set<String>> entry : this.typeNamePairForCurrentState.entrySet()) {
			Set<String> newSet = new HashSet<String>();
			newSet.addAll(entry.getValue());
			rs.typeNamePairForCurrentState.put(entry.getKey(), newSet);

		}
		return rs;
	}

	public RunningState(Collection<String> testCases) {
		nameValuePairForCurrentState = new HashMap<String, String>();
		valueNamePairForCurrentState = new HashMap<String, Set<String>>();
		typeNamePairForCurrentState = new HashMap<String, Set<String>>();
		nameTypePairForCurrentState = new HashMap<String, String>();

		for (String testCase : testCases) {
			Map<String, String> nameValuePair = FileUtils.getNameValuePairs(testCase + "-0.xml");
			for (Entry<String, String> entry : nameValuePair.entrySet())
				this.put(entry.getKey(), entry.getValue(), getTypeOfValue(entry.getValue()));
		}

	}

	public static String getTypeOfValue(String origVal) {
		
		int end = origVal.indexOf('>');
		String val = origVal.substring(0, end);
		int begin = val.lastIndexOf('.');
		if (begin == -1)
			begin = 0;
		val = val.substring(begin + 1);
		if (val.equals("NullValueType")) {
			begin = origVal.indexOf("<type>");
			begin += 7;
			end = origVal.indexOf("</type>");
			val = origVal.substring(begin, end);
		}

		val = val.replaceAll("-array", "[]");
		return val;
	}

	public RunningState(Collection<String> testCases, String mainTestClass) {
		
		nameValuePairForCurrentState = new HashMap<String, String>();
		valueNamePairForCurrentState = new HashMap<String, Set<String>>();
		typeNamePairForCurrentState = new HashMap<String, Set<String>>();
		nameTypePairForCurrentState = new HashMap<String, String>();

		Set<String> done = new HashSet<String>();
		for (String testCase : testCases) {
			
			String testClass = Utils.getTestClassNameFromTestCase(testCase);
			
			if (done.contains(testClass)) continue;
			
			Map<String, String> nameValuePair = FileUtils.getNameValuePairs(testCase + "-0.xml");
			for (Entry<String, String> entry : nameValuePair.entrySet()) {
				
				String name = entry.getKey();
				
				if (!testClass.equals(mainTestClass))
					name = Utils.getTestCaseName(testClass.toLowerCase()) + "." + name;
				
				String value = entry.getValue();
				if(value.equals("")) {
					this.put(name, "", "");
				} else {
					this.put(name, entry.getValue(), getTypeOfValue(entry.getValue()));
				}
				
					
			}
			done.add(testClass);
		}
	}

	// public RunningState(Collection<String> testCases, String mainTestClass)
	// {
	// nameValuePairForCurrentState = new HashMap<String, String>();
	// valueNamePairForCurrentState = new HashMap<String, Set<String>>();
	// typeNamePairForCurrentState = new HashMap<String, Set<String>>();
	// nameTypePairForCurrentState = new HashMap<String, String>();
	//
	// Set<String> done = new HashSet<String>();
	// for (String testCase : testCases)
	// {
	// String testClass = Utils.getTestClassNameFromTestCase(testCase);
	// if (done.contains(testClass))
	// continue;
	// Map<String, String> nameValuePair = FileUtils.getNameValuePairs(testCase
	// + "-0.xml");
	// for (Entry<String, String> entry : nameValuePair.entrySet())
	// {
	// String name = entry.getKey();
	// if (!testClass.equals(mainTestClass))
	// name = name + "_" + testClass;
	// this.put(name, entry.getValue(), getTypeOfValue(entry.getValue()));
	// }
	// done.add(testClass);
	// }
	// }
	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return nameTypePairForCurrentState.toString();
	}
}