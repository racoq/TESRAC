package ca.ubc.salt.model.state;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AssertStatement;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SwitchCase;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.WhileStatement;

import ca.ubc.salt.model.composer.RunningState;
import ca.ubc.salt.model.utils.Pair;
import ca.ubc.salt.model.utils.Utils;

public class ReadVariableVisitor extends ASTVisitor
{

    Map<String, Set<SimpleName>> readVars;
    Map<String, Set<VarDefinitionPreq>> needToBeDefinedVars;
    Map<String, Statement> allASTStatements;
    String methodName;
    int counter = -1;

    public ReadVariableVisitor(String methodName)
    {
	this.methodName = methodName;
    }

    public boolean visit(ExpressionStatement exp)
    {
	// System.out.println(node.toString());

	counter++;
	allASTStatements.put(methodName+"-"+counter+".xml", exp);
	Expression e = exp.getExpression();
	if (e instanceof Assignment)
	{
	    Assignment a = (Assignment) e;
	    if (a.getLeftHandSide().getNodeType() == ASTNode.SIMPLE_NAME)
	    {
		SimpleName left = (SimpleName) a.getLeftHandSide();
		IBinding binding = left.resolveTypeBinding();
		if (binding != null)
		{
		    String type = binding.getName();
//		    type = RunningState.addFinalLiteral(left, type);
		    Utils.addToTheSetInMap(needToBeDefinedVars, methodName + "-" + counter + ".xml",
			    new VarDefinitionPreq(left, type));
		}
		else
		{
//		    System.out.println("binding is null for " exp.toString() + left);
		}
	    }
	    getReadVars(a.getRightHandSide());
//	    getReadVars(a);

	} else
	    getReadVars(exp);
	return false; // do not continue
    }

    public boolean visit(VariableDeclarationStatement node)
    {
	counter++;
	allASTStatements.put(methodName+"-"+counter+".xml", node);
	return true;
    }

    public boolean visit(VariableDeclarationFragment node)
    {
	// System.out.println(node.toString());
	// System.out.println(varDecs);

	getReadVars(node.getInitializer());
	return false;
    }

    public void getReadVars(ASTNode node)
    {
	if (node == null)
	{
	    Utils.addAllTheSetInMap(readVars, methodName + "-" + counter + ".xml", null);
	    return;
	}
	StatementReadVariableVisitor srvv = new StatementReadVariableVisitor();
	node.accept(srvv);
	Utils.addAllTheSetInMap(readVars, methodName + "-" + counter + ".xml", srvv.readVars);

    }

    public boolean visit(IfStatement node)
    {
	counter++;
	allASTStatements.put(methodName+"-"+counter+".xml", node);
	getReadVars(node);
	return false;
    }

    public boolean visit(WhileStatement node)
    {
	counter++;
	allASTStatements.put(methodName+"-"+counter+".xml", node);
	getReadVars(node);
	return false;
    }

    public boolean visit(EnhancedForStatement node)
    {
	counter++;
	allASTStatements.put(methodName+"-"+counter+".xml", node);
	getReadVars(node);
	return false;
    }

    public boolean visit(ForStatement node)
    {
	counter++;
	allASTStatements.put(methodName+"-"+counter+".xml", node);
	getReadVars(node);
	return false;
    }

    public boolean visit(AssertStatement node)
    {
	counter++;
	allASTStatements.put(methodName+"-"+counter+".xml", node);
	getReadVars(node);
	return false;
    }
    
    public boolean visit(TryStatement node)
    {
	counter++;
	allASTStatements.put(methodName+"-"+counter+".xml", node);
	getReadVars(node);
	return false;
    }

    public boolean visit(SwitchStatement node)
    {
	counter++;
	allASTStatements.put(methodName+"-"+counter+".xml", node);
	getReadVars(node);
	return false;
    }

    public boolean visit(SwitchCase node)
    {
	counter++;
	allASTStatements.put(methodName+"-"+counter+".xml", node);
	getReadVars(node);
	return false;
    }

    public boolean visit(DoStatement node)
    {
	counter++;
	allASTStatements.put(methodName+"-"+counter+".xml", node);
	getReadVars(node);
	return false;
    }

    public Map<String, Set<SimpleName>> getReadVars()
    {
	return readVars;
    }

    public void setReadVars(Map<String, Set<SimpleName>> readVars)
    {
	this.readVars = readVars;
    }

    public Map<String, Set<VarDefinitionPreq>> getNeedToBeDefinedVars()
    {
        return needToBeDefinedVars;
    }

    public void setNeedToBeDefinedVars(Map<String, Set<VarDefinitionPreq>> needToBeDefinedVars)
    {
        this.needToBeDefinedVars = needToBeDefinedVars;
    }

    public Map<String, Statement> getAllASTStatements()
    {
        return allASTStatements;
    }

    public void setAllASTStatements(Map<String, Statement> allASTStatements)
    {
        this.allASTStatements = allASTStatements;
    }
    
    

}