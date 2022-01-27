package ca.ubc.salt.model.instrumenter;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.MethodDeclaration;

public class ProductionInstrumenterVisitor extends ASTVisitor {
	
	public boolean visit(MethodDeclaration method) {
		return false;
	}
}
