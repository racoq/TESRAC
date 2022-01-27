package ca.ubc.salt.model.composer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.SimpleName;

public class StatementVariableVisitor extends ASTVisitor
{
    Map<String, SimpleName> vars = new HashMap<String, SimpleName>();

    public boolean visit(SimpleName node)
    {
	final IBinding nodeBinding = node.resolveBinding();
	if (nodeBinding == null || nodeBinding instanceof IVariableBinding)
	{
	    IVariableBinding ivb = (IVariableBinding) nodeBinding;
	    vars.put(node.getIdentifier(), node);
	    // System.out.println(ivb.getName());
	    // System.out.println(ivb.getType().getQualifiedName());
	}
	// else
	// {
	// System.out.println(node + " " + nodeBinding);
	// }
	return true;
    }

    public Map<String, SimpleName> getVars()
    {
	return vars;
    }

    public void setVars(Map<String, SimpleName> readVars)
    {
	this.vars = readVars;
    }

}