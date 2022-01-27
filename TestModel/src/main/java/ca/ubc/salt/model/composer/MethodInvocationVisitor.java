package ca.ubc.salt.model.composer;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SimpleName;

public class MethodInvocationVisitor extends ASTVisitor
{

    List<MethodInvocation> methodInvocations = new ArrayList<MethodInvocation>();

    public boolean visit(MethodInvocation node)
    {

	methodInvocations.add(node);

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
