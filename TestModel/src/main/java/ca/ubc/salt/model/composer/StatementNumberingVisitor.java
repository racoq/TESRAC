package ca.ubc.salt.model.composer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SwitchCase;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.WhileStatement;

public class StatementNumberingVisitor extends ASTVisitor
{

    ArrayList<Statement> statements = new ArrayList<Statement>();
    
    public boolean visit(ExpressionStatement node)
    {
	// System.out.println(node.toString());
	setStatement(node);
	return false; // do not continue
    }

    public boolean visit(VariableDeclarationFragment node)
    {
	// System.out.println(node.toString());
	// System.out.println(varDecs);

	setStatement(node.getParent());

	return false;
    }

    public void setStatement(ASTNode node)
    {
	statements.add((Statement) node);
    }
    
    
    public boolean visit(IfStatement node)
    {
	setStatement(node.getParent());
	return false;
    }
    public boolean visit(WhileStatement node)
    {
	setStatement(node.getParent());
	return false;
    }
    public boolean visit(EnhancedForStatement node)
    {
	setStatement(node.getParent());
	return false;
    }
    public boolean visit(ForStatement node)
    {
	setStatement(node.getParent());
	return false;
    }
    public boolean visit(TryStatement node)
    {
	setStatement(node.getParent());
	return false;
    }
    public boolean visit(SwitchStatement node)
    {
	setStatement(node.getParent());
	return false;
    }
    public boolean visit(SwitchCase node)
    {
	setStatement(node.getParent());
	return false;
    }
    public boolean visit(DoStatement node)
    {
	setStatement(node.getParent());
	return false;
    }

}