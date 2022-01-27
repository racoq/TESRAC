package ca.ubc.salt.model.composer;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.SimpleName;

public class StatementSimpleNameVisitor extends ASTVisitor
{
    Set<SimpleName> vars = new HashSet<SimpleName>();

    public boolean visit(SimpleName node)
    {
	vars.add(node);
	return true;
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