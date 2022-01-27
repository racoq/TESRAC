package ca.ubc.salt.model.composer;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SimpleName;

import ca.ubc.salt.model.utils.Settings;

public class TestMethodInvocationVisitor extends ASTVisitor
{
    
    List<MethodInvocation> methodInvocations = new ArrayList<MethodInvocation>();
    
    Set<String> className;
    public TestMethodInvocationVisitor(Set<String> className)
    {
	// TODO Auto-generated constructor stub
	this.className = className;
    }
    public boolean visit(MethodInvocation node)
    {
	
	IMethodBinding nodeBinding = node.resolveMethodBinding();
	if (nodeBinding == null)
	{
	    return true;
	}
	
	ITypeBinding classBinding  = nodeBinding.getDeclaringClass();
	String classNameForTheNode = classBinding.getName();
	if (className.contains(classNameForTheNode))
	{
	    methodInvocations.add(node);
	}
	
	return true;
    }
    public List<MethodInvocation> getMethodInvocations()
    {
        return methodInvocations;
    }
    public void setMethodInvocations(List<MethodInvocation> methodInvocations)
    {
        this.methodInvocations = methodInvocations;
    }
    
    
    
}
