package ca.ubc.salt.model.composer;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

public class StatementDefineVariableVisitor extends ASTVisitor
{
    Set<SimpleName> vars = new HashSet<SimpleName>();

    public boolean visit(VariableDeclarationFragment node)
    {
	vars.add(node.getName());
	return false; // do not continue
    }

    public Set<SimpleName> getVars()
    {
	return vars;
    }

    public void setVars(Set<SimpleName> readVars)
    {
	this.vars = readVars;
    }

}