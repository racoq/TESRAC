package ca.ubc.salt.model.instrumenter;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.MarkerAnnotation;
import org.eclipse.jdt.core.dom.MemberValuePair;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;

import ca.ubc.salt.model.state.ReadVariableVisitor;
import ca.ubc.salt.model.state.VarDefinitionPreq;
import ca.ubc.salt.model.utils.Settings;
import ca.ubc.salt.model.utils.Utils;

public class Method {
	MethodDeclaration methodDec = null;
	String className;
	ClassModel clazz;

	public Method(MethodDeclaration methodDec, String className, ClassModel clazz) {
		this.methodDec = methodDec;
		this.className = className;
		this.clazz = clazz;
	}

	public boolean isIgnored() {
		List modifs = this.getMethodDec().modifiers();
		for (Iterator it = modifs.listIterator(); it.hasNext();) {
			Object obj = it.next();
			if (obj instanceof MarkerAnnotation) {
				MarkerAnnotation ma = (MarkerAnnotation) obj;
				if (ma.getTypeName().toString().contains("Ignore")
						|| ma.getTypeName().toString().contains("Deprecated")) {
					return true;
				}
			}
		}
		return false;
	}

	public void instrumentTestMethod(ASTRewrite rewriter, List<String> loadedClassVars, String fileName, boolean start)
			throws JavaModelException, IllegalArgumentException, MalformedTreeException, BadLocationException {

		addTimeOut(rewriter);

		int randomNumber = (int) (Math.random() * (Integer.MAX_VALUE - 1));
		Block block = methodDec.getBody();
		if (block == null)
			return;
		ListRewrite listRewrite = rewriter.getListRewrite(block, Block.STATEMENTS_PROPERTY);

		List<ClassModel> parentModels = this.clazz.getAllSuperModelsAndThis();

		Block header = (Block) TestClassInstrumenter.generateInstrumentationHeader(parentModels, randomNumber, fileName,
				methodDec.getName().toString());
		List<Statement> stmts = header.statements();
		if (start)
			for (int i = stmts.size() - 1; i >= 0; i--)
				listRewrite.insertFirst(stmts.get(i), null);
		else
			for (int i = 0; i < stmts.size(); i++)
				listRewrite.insertLast(stmts.get(i), null);

		// Block footer =
		// (Block)TestClassInstrumenter.generateFooterBlock(randomNumber);
		// stmts = footer.statements();
		// for (int i = stmts.size() - 1; i >= 0; i--)
		// listRewrite.insertLast(stmts.get(i), null);
		// listRewrite.insertLast(footer, null);

		InstrumenterVisitor visitor = new InstrumenterVisitor(rewriter, randomNumber, methodDec.getName().toString(),
				parentModels);
		// visitor.addFieldVars(this.clazz);
		this.methodDec.accept(visitor);

		ASTNode loopCode = Utils.createBlockWithText("InstrumentClassGenerator.traceLoop();");
		if (block.statements().size() > 0) {

			Statement lastStmt = (Statement) block.statements().get(block.statements().size() - 1);
			if (lastStmt instanceof ReturnStatement)
				listRewrite.insertBefore(loopCode, lastStmt, null);
			else
				listRewrite.insertLast(loopCode, null);
		}

		// apply the text edits to the compilation unit

		// edits.apply(document);
		//
		// // this is the code for adding statements
		// System.out.println(document.get());

	}

	public void instrumentProductionMethod(ASTRewrite rewriter, Document document, List<String> loadedClassVars,
			boolean start)
			throws JavaModelException, IllegalArgumentException, MalformedTreeException, BadLocationException {
		Block block = methodDec.getBody();
		if (block == null)
			return;
		ListRewrite listRewrite = rewriter.getListRewrite(block, Block.STATEMENTS_PROPERTY);

		List params = this.methodDec.parameters();

		List<String> paramStrs = new LinkedList<String>();
		if (!Modifier.isStatic(methodDec.getModifiers()) && !methodDec.isConstructor())
			paramStrs.add("this");
		for (Object paramObj : params) {
			if (paramObj instanceof VariableDeclaration) {
				VariableDeclaration param = (VariableDeclaration) paramObj;
				String name = param.getName().toString();
				paramStrs.add(name);
			}
		}
		AST ast = methodDec.getAST();

		Block header = (Block) ASTNode.copySubtree(ast, ProductionClassInstrumenter
				.generateInstrumentationHeader(className + "." + methodDec.getName().toString(), paramStrs));
		List<Statement> stmts = header.statements();

		TryStatement trystmt = ast.newTryStatement();
		// Block newBlock = ast.newBlock();
		Block blk = (Block) ASTNode.copySubtree(ast, block);

		trystmt.setBody(blk);
		Block footer = (Block) ASTNode.copySubtree(ast, ProductionClassInstrumenter.generateFooterBlock());
		trystmt.setFinally(footer);

		removeAllFromBlock(block, listRewrite);
		listRewrite.insertFirst(trystmt, null);
		if (!start && block.statements().size() > 0) {
			ASTNode firstStmt = (ASTNode) blk.statements().remove(0);
			listRewrite.insertFirst(firstStmt, null);
		}

		ListRewrite tryList = rewriter.getListRewrite(trystmt.getBody(), Block.STATEMENTS_PROPERTY);

		for (int i = stmts.size() - 1; i >= 0; i--)
			tryList.insertFirst(stmts.get(i), null);
	}

	private void removeAllFromBlock(Block block, ListRewrite listRewrite) {
		for (Object obj : block.statements())
			listRewrite.remove((ASTNode) obj, null);
	}

	public void populateReadVars(Document document, List<String> loadedClassVars, Map<String, Set<SimpleName>> readVars,
			Map<String, Set<VarDefinitionPreq>> definitionPreq, Map<String, Statement> allASTStatements) {
		ReadVariableVisitor visitor = new ReadVariableVisitor(className + "." + methodDec.getName().toString());
		visitor.setReadVars(readVars);
		visitor.setNeedToBeDefinedVars(definitionPreq);
		visitor.setAllASTStatements(allASTStatements);
		this.methodDec.accept(visitor);
		// System.out.println(visitor.getReadVars());
	}

	public MethodDeclaration getMethodDec() {
		return methodDec;
	}

	public void setMethodDec(MethodDeclaration methodDec) {
		this.methodDec = methodDec;
	}

	public void addTimeOut(ASTRewrite rewrite) {

		// if
		// (method.methodDec.getName().toString().toLowerCase().contains("test"))
		// return true;

		ListRewrite listRewrite = rewrite.getListRewrite(this.methodDec, MethodDeclaration.MODIFIERS2_PROPERTY);
		List<NormalAnnotation> modifs = this.methodDec.modifiers();
		AST ast = this.methodDec.getAST();
		for (Object obj : modifs) {
			if (obj instanceof MarkerAnnotation) {
				MarkerAnnotation ma = (MarkerAnnotation) obj;
				if (ma.getTypeName().getFullyQualifiedName().contains("Test")) {
					listRewrite.remove(ma, null);
					// ma =
					// (MarkerAnnotation)ASTNode.copySubtree(method.methodDec.getAST(),
					// ma);
					NormalAnnotation na = ast.newNormalAnnotation();
					Name name = ast.newName(ma.getTypeName().getFullyQualifiedName());
					na.setTypeName(name);
					MemberValuePair pair = ast.newMemberValuePair();
					pair.setName(ast.newSimpleName("timeout"));
					pair.setValue(ast.newNumberLiteral(Settings.TIMEOUT));
					na.values().add(pair);
					listRewrite.insertFirst(na, null);
				}
			}
		}

	}

	public String getFullMethodName() {
		return this.className + "." + this.methodDec.getName().toString();
	}

	public boolean isTestMethod() {

		List<NormalAnnotation> modifs = this.methodDec.modifiers();
		AST ast = this.methodDec.getAST();
		for (Object obj : modifs) {
			if (obj instanceof MarkerAnnotation) {
				MarkerAnnotation ma = (MarkerAnnotation) obj;
				if (ma.getTypeName().getFullyQualifiedName().contains("Test")) {
					return true;
				}
			}
			if (obj instanceof NormalAnnotation) {
				NormalAnnotation ma = (NormalAnnotation) obj;
				if (ma.getTypeName().getFullyQualifiedName().contains("Test")) {
					List values = ma.values();
					for (Object objV : values) {
						if (objV instanceof MemberValuePair) {
							MemberValuePair mvp = (MemberValuePair) objV;
							if (mvp.getName().getIdentifier().contains("expected"))
								return false;
						}
					}
					return true;
				}
			}
		}

		// if
		// (this.methodDec.getName().toString().toLowerCase().startsWith("test")
		// && (this.methodDec.parameters() == null ||
		// this.methodDec.parameters().size() == 0))
		// return true;

		return false;
	}

	public String getClassName() {
		return className;
	}

	public void setClassName(String className) {
		this.className = className;
	}

}
