package ca.ubc.salt.model.composer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.SimpleName;

import ca.ubc.salt.model.utils.Utils;

public class StatementFieldVisitor extends ASTVisitor
{
    // class -> fields
    Map<String, Set<String>> vars = new HashMap<String, Set<String>>();

    Map<String, SimpleName> fields = new HashMap<String, SimpleName>();

    String className = null;

    public StatementFieldVisitor(Map<String, Set<String>> vars)
    {
	this.vars = vars;
    }
    public StatementFieldVisitor()
    {
    }

    public boolean visit(SimpleName node)
    {
	final IBinding nodeBinding = node.resolveBinding();
	if (nodeBinding instanceof IVariableBinding)
	{
	    IVariableBinding ivb = (IVariableBinding) nodeBinding;
	    
	    if (ivb.isField())
	    {
		
//		System.out.println(ivb);
//		System.out.println(ivb.getJavaElement());
//		System.out.println(ivb.getDeclaringClass().getName());
//		System.out.println(ivb.getVariableDeclaration());
//		System.out.println(ivb.getVariableDeclaration().getJavaElement());
		
		if (className == null)
		{
		    if (ivb.getDeclaringClass() == null)
			return true;
		    className = ivb.getDeclaringClass().getName();
		}
		
		Utils.addToTheSetInMap(vars, ivb.getDeclaringClass().getName(), node.getIdentifier());
		
		fields.put(node.getIdentifier(), node);
	    }
	    // System.out.println(ivb.getName());
	    // System.out.println(ivb.getType().getQualifiedName());
	}
	// else
	// {
	// System.out.println(node + " " + nodeBinding);
	// }
	return true;
    }

    public Map<String, Set<String>> getVars()
    {
	return vars;
    }

    public void setVars(Map<String, Set<String>> vars)
    {
	this.vars = vars;
    }

}