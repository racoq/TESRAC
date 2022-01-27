package ca.ubc.salt.model.instrumenter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.TreeMap;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AssertStatement;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SwitchCase;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.WhileStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

import ca.ubc.salt.model.utils.Utils;

public class InstrumenterVisitor extends ASTVisitor
{
	LinkedList<VariableDeclarationFragment> varDecs = new LinkedList<VariableDeclarationFragment>();
	Stack<Integer> varDecLen = new Stack<Integer>();
	Map<String, VariableDeclarationFragment> unassignedVars = new HashMap<String, VariableDeclarationFragment>();
	ASTRewrite astRewrite;
	int randomNumber;
	int counter = 2;
	String methodName;
	List<ClassModel> clazz;
	//    boolean addedUnInitializedVar = false;

	public InstrumenterVisitor(ASTRewrite astRewrite, int randomNumber, String methodName, List<ClassModel> clazz)
	{
		this.astRewrite = astRewrite;
		this.randomNumber = randomNumber;
		this.methodName = methodName;
		this.clazz = clazz;
	}

	public void addFieldVars(ClassModel clazz)
	{
		for (FieldDeclaration fd : clazz.getAllFields())
		{
			for (Object obj : fd.fragments())
			{
				VariableDeclarationFragment vdf = (VariableDeclarationFragment) obj;
				this.varDecs.add(vdf);
			}
		}
	}

	@Override
	public boolean preVisit2(ASTNode node)
	{
		if (node instanceof Statement && node.getNodeType() != ASTNode.VARIABLE_DECLARATION_STATEMENT
				&& node.getNodeType() != ASTNode.VARIABLE_DECLARATION_EXPRESSION
				&& node.getNodeType() != ASTNode.VARIABLE_DECLARATION_FRAGMENT)
		{
			varDecLen.push(varDecs.size());
		}
		return true;

	}

	// public boolean visit(Statement node)
	// {
	// if (node.getNodeType() == ASTNode.IF_STATEMENT || node.getNodeType() ==
	// ASTNode.WHILE_STATEMENT
	// || node.getNodeType() == ASTNode.ENHANCED_FOR_STATEMENT ||
	// node.getNodeType() == ASTNode.TRY_STATEMENT
	// || node.getNodeType() == ASTNode.SWITCH_STATEMENT || node.getNodeType()
	// == ASTNode.SWITCH_CASE
	// || node.getNodeType() == ASTNode.DO_STATEMENT)
	// {
	// if (node.getNodeType() != ASTNode.RETURN_STATEMENT)
	// addDumpCode(node);
	// return false;
	// } else if (node.getNodeType() == ASTNode.EXPRESSION_STATEMENT)
	// {
	//
	// }
	// return true;
	// }

	public boolean visit(VariableDeclarationFragment node)
	{
		if (node.getInitializer() == null)
		{
			unassignedVars.put(node.getName().toString(), node);
		} 
		varDecs.add(node);

		return false; // do not continue
		// System.out.println(node.toString());
		// System.out.println(varDecs);
	}


	public boolean visit(IfStatement node)
	{
		addDumpCode(node, true);
		return false;
	}

	public boolean visit(WhileStatement node)
	{
		addDumpCode(node, true);
		return false;
	}

	public boolean visit(EnhancedForStatement node)
	{
		addDumpCode(node, true);
		return false;
	}

	public boolean visit(ForStatement node)
	{
		addDumpCode(node, true);
		return false;
	}

	public boolean visit(TryStatement node)
	{
		addDumpCode(node, true);
		return false;
	}

	public boolean visit(SwitchStatement node)
	{
		addDumpCode(node, true);
		return false;
	}

	public boolean visit(SwitchCase node)
	{
		addDumpCode(node, true);
		return false;
	}

	public boolean visit(DoStatement node)
	{
		addDumpCode(node, true);
		return false;
	}

	public boolean visit(AssertStatement node)
	{
		addDumpCode(node, true);
		return false;
	}
	public boolean visit(ExpressionStatement exp)
	{
		Expression e = exp.getExpression();
		if (e instanceof Assignment)
		{
			Assignment a = (Assignment) e;
			if (a.getLeftHandSide().getNodeType() == ASTNode.SIMPLE_NAME)
			{
				SimpleName sn = (SimpleName) a.getLeftHandSide();
				VariableDeclarationFragment vdf = unassignedVars.get(sn.toString());
				if (vdf != null)
				{
					//		    varDecs.add(vdf);
					unassignedVars.remove(sn.toString());
					//		    addedUnInitializedVar = true;
				}
			}

		}
		if (exp.getNodeType() != ASTNode.RETURN_STATEMENT)
			addDumpCode(exp, false);
		// System.out.println(node.toString());
		// System.out.println(varDecs);
		return false;
	}

	private void addDumpCode(ASTNode node, boolean loop)
	{
		ASTNode newCode = TestClassInstrumenter.generateInstrumentationBlock(randomNumber, varDecs, methodName,
				counter++, unassignedVars, this.clazz);

		ASTNode loopCode = Utils.createBlockWithText("InstrumentClassGenerator.traceLoop();");
		ASTNode parent = node.getParent();
		ListRewrite listRewrite;
		if ((parent instanceof Block))
		{
			listRewrite = astRewrite.getListRewrite(parent, Block.STATEMENTS_PROPERTY);
			listRewrite.insertAfter(newCode, node, null);
			if (loop)
				listRewrite.insertBefore(loopCode, node, null);
		} else
		{
			// TODO fill here create a new node and replace it parent list
		}
	}

	@Override
	public void postVisit(ASTNode node)
	{
		if (node instanceof Statement)
		{
			if (node.getNodeType() == ASTNode.VARIABLE_DECLARATION_STATEMENT)
				addDumpCode(node, false);
			if ( node.getNodeType() != ASTNode.VARIABLE_DECLARATION_STATEMENT
					&& node.getNodeType() != ASTNode.VARIABLE_DECLARATION_EXPRESSION
					&& node.getNodeType() != ASTNode.VARIABLE_DECLARATION_FRAGMENT)
			{
				//	    if (addedUnInitializedVar == false)
				//	    {
				int size = varDecLen.pop();
				while (varDecs.size() > size)
					varDecs.removeLast();
				//	    } else
				//		addedUnInitializedVar = false;
			}
		}
	}
	// public boolean visit(SimpleName node)
	// {
	// if (this.names.contains(node.getIdentifier()))
	// {
	// System.out.println("Usage of '" + node + "' at line " +
	// cu.getLineNumber(node.getStartPosition()));
	// }
	// return true;
	// }

	public ASTNode getFirstParentWithTypeOf(ASTNode node, int parentType)
	{
		if (node.getNodeType() == parentType)
			return node;

		ASTNode parent = node.getParent();
		while (parent != null && parent.getNodeType() != parentType)
		{
			node = parent;
			parent = node.getParent();
		}

		return parent;
	}

	public ASTNode getChildOfFirstParentWithTypeOf(ASTNode node, int parentType)
	{
		if (node.getNodeType() == parentType)
			return null;

		ASTNode parent = node.getParent();
		while (parent != null && parent.getNodeType() != parentType)
		{
			node = parent;
			parent = node.getParent();
		}
		if (parent != null)
			return node;
		return null;
	}

}
