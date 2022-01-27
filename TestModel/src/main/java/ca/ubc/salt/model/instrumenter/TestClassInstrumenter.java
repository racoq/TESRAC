package ca.ubc.salt.model.instrumenter;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;

import ca.ubc.salt.model.utils.FileUtils;
import ca.ubc.salt.model.utils.Settings;
import ca.ubc.salt.model.utils.Utils;

public class TestClassInstrumenter {
	
	public static void instrumentClass(String testClassPath)
			throws IOException, IllegalArgumentException, MalformedTreeException, BadLocationException, CoreException {

		File testClass = new File(testClassPath);
		
		if (testClass.isFile()) {
			
			if (!Utils.isTestClass(testClass))
				return;

			String source = FileUtils.readFileToString(testClass);
			Document document = new Document(source);
			
			List<ClassModel> classes = ClassModel.getClasses(document.get(), true, testClassPath,
					new String[] { Settings.PROJECT_PATH }, new String[] { Settings.LIBRARY_JAVA });
			
			if (classes.size() == 0) return;
			
			ASTRewrite rewriter = ASTRewrite.create(classes.get(0).cu.getAST());
			
			for (ClassModel clazz : classes)
				instrumentClass(clazz, null, clazz.typeDec.getName().toString(), rewriter);

			TextEdit edits = rewriter.rewriteAST(document, null);
			edits.apply(document);
			System.out.println(document.get());

		} else if (testClass.isDirectory()) {
			File[] listOfFiles = testClass.listFiles();
			for (int i = 0; i < listOfFiles.length; i++) {
				instrumentClass(listOfFiles[i].getAbsolutePath());
			}
		}

	}

	String thisTestClassName = "something";

	public static void addClassStringToClass(ClassModel clazz, ASTRewrite rewriter) {
		ListRewrite listRewrite = rewriter.getListRewrite(clazz.getTypeDec(),
				TypeDeclaration.BODY_DECLARATIONS_PROPERTY);

		AST ast = clazz.getTypeDec().getAST();

		VariableDeclarationFragment vdf = ast.newVariableDeclarationFragment();
		vdf.setName(ast.newSimpleName("thisTestClassName"));
		StringLiteral sl = ast.newStringLiteral();
		sl.setLiteralValue(clazz.name);
		vdf.setInitializer(sl);
		FieldDeclaration fd = ast.newFieldDeclaration(vdf);
		fd.setType(ast.newSimpleType(ast.newName("String")));
		listRewrite.insertFirst(fd, null);
	}

	public static void instrumentClass(ClassModel srcClass, List<ClassModel> loadedClasses, String fileName,
			ASTRewrite rewriter)
			throws IllegalArgumentException, MalformedTreeException, BadLocationException, CoreException {

		List<Method> methods = srcClass.getMethods();

		for (Method method : methods) {
			if (method.isTestMethod() && !method.isIgnored()
					&& !Settings.blackListSet.contains(method.getFullMethodName()))
				method.instrumentTestMethod(rewriter, null, fileName, !method.getMethodDec().isConstructor());
		}

		if (srcClass.isAbstract() == false) {
			addClassStringToClass(srcClass, rewriter);
		}

	}

	// public static void instrumentClass2(String testClassPath) throws
	// IOException
	// {
	// File testClass = new File(testClassPath);
	// if (testClass.isFile())
	// {
	//
	// String extention = FilenameUtils.getExtension(testClass.getName());
	// // if (extention.equals("java") &&
	// // testClass.getName().toLowerCase().contains("test"))
	// {
	// Settings.consoleLogger.info(String.format("instrumenting %s\n",
	// testClass.getName()));
	//
	// ASTParser parser = ASTParser.newParser(AST.JLS8);
	// parser.setKind(ASTParser.K_COMPILATION_UNIT);
	// Map pOptions = JavaCore.getOptions();
	// pOptions.put(JavaCore.COMPILER_COMPLIANCE, JavaCore.VERSION_1_8);
	// pOptions.put(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM,
	// JavaCore.VERSION_1_8);
	// pOptions.put(JavaCore.COMPILER_SOURCE, JavaCore.VERSION_1_8);
	// parser.setCompilerOptions(pOptions);
	//
	// String source = FileUtils.readFileToString(testClassPath);
	// parser.setSource(source.toCharArray());
	// CompilationUnit cu = (CompilationUnit) parser.createAST(null);
	//
	// List typeDeclarationList = cu.types();
	//
	// for (Object type : typeDeclarationList)
	// {
	// // get methods list
	// // System.out.println(((AbstractTypeDeclaration)type).getName());
	// if (type instanceof TypeDeclaration)
	// {
	// TypeDeclaration typeDec = (TypeDeclaration) type;
	// MethodDeclaration[] methodList = typeDec.getMethods();
	//
	// for (MethodDeclaration method : methodList)
	// {
	// System.out.println(method.getName());
	//// Visitor visitor = new Visitor();
	//// method.accept(visitor);
	//// for (VariableDeclarationFragment var : visitor.varDecs)
	//// System.out.println(" " + var.getName());
	//
	//
	// Block block = method.getBody();
	// for (Object obj : block.statements())
	// {
	// Statement stmt = (Statement) obj;
	//// System.out.println("*");
	//// System.out.println(stmt);
	// }
	//
	// }
	//
	// FieldDeclaration[] fields = typeDec.getFields();
	//
	// for (FieldDeclaration field : fields)
	// {
	// if (Modifier.isStatic(field.getModifiers()))
	// System.out.println("static");
	// for (Object o : field.fragments())
	// {
	// if (o instanceof VariableDeclarationFragment)
	// {
	// VariableDeclarationFragment var = ((VariableDeclarationFragment) o);
	// String s = var.getName().toString();
	// System.out.println("-------------field: " + s);
	// } else
	// System.out.println("hehe");
	//
	// }
	// }
	//
	// }
	// }
	//
	// }
	//
	// } else if (testClass.isDirectory())
	// {
	// File[] listOfFiles = testClass.listFiles();
	// for (int i = 0; i < listOfFiles.length; i++)
	// {
	// instrumentClass(listOfFiles[i].getAbsolutePath());
	// }
	// }
	//
	// }

	public static void main(String[] args)
			throws IOException, IllegalArgumentException, MalformedTreeException, BadLocationException, CoreException {
		instrumentClass(
				"/Users/arash/Research/repos/commons-math/src/test/java/org/apache/commons/math4/stat/regression/GLSMultipleLinearRegressionTest.java");
		// instrumentClass(
		// "/Users/arash/Library/Mobile
		// Documents/com~apple~CloudDocs/Research/Calculator/src/calc/CalculatorTest.java");
		// instrumentClass(Settings.TEST_CLASS);
	}

	public static ASTNode generateInstrumentationHeader(List<ClassModel> clazz, int randomNumber, String fileName,
			String methodName) {

		StringBuilder sb = new StringBuilder();
		sb.append(String.format(
				"InstrumentClassGenerator.init(%s+\".%s\");InstrumentClassGenerator.initTestStatement(0);",
				"this.thisTestClassName", methodName));
		sb.append(getTextForInstrumentation(new LinkedList<VariableDeclarationFragment>(), 1, null, clazz));
		return Utils.createBlockWithText(sb.toString());

	}

	public static ASTNode generateFooterBlock(int randomNumber) {
		return Utils.createBlockWithText("InstrumentClassGenerator.traceLoop();");
	}

	public static ASTNode generateInstrumentationBlock(int randomNumber,
			LinkedList<VariableDeclarationFragment> varDecs, String methodName, int counter,
			Map<String, VariableDeclarationFragment> unassignedVars, List<ClassModel> clazz) {

		String text = getTextForInstrumentation(varDecs, counter, unassignedVars, clazz);

		return Utils.createBlockWithText(text);

	}
	// public static ASTNode generateInstrumentationHeader(int randomNumber,
	// String methodName)
	// {
	// StringBuilder sb = new StringBuilder();
	// sb.append(String.format("XStream xstream_%d = new XStream(new
	// StaxDriver());", randomNumber));
	// sb.append(String.format("try{FileWriter fw_%d = new
	// FileWriter(\"traces/%s-%d.xml\");", randomNumber,
	// methodName, 1));
	// sb.append(String.format("fw_%d.append(\"<vars></vars>\\n\");",
	// randomNumber));
	// sb.append(String.format("ObjectOutputStream out_%d =
	// xstream_%d.createObjectOutputStream(fw_%d);", randomNumber,
	// randomNumber, randomNumber));
	// sb.append(String.format("out_%d.close();}catch (IOException
	// e){e.printStackTrace();}", randomNumber));
	// return Utils.createBlockWithText(sb.toString());
	//
	// }
	//
	// public static ASTNode generateFooterBlock(int randomNumber)
	// {
	// String str = String.format("out_%d.close();", randomNumber);
	// return Utils.createBlockWithText(str);
	// }
	//
	// public static ASTNode generateInstrumentationBlock(int randomNumber,
	// LinkedList<VariableDeclarationFragment> varDecs, String methodName, int
	// counter)
	// {
	//
	// StringBuilder sb = new StringBuilder();
	// sb.append(String.format("try{FileWriter fw_%d = new
	// FileWriter(\"traces/%s-%d.xml\");", randomNumber,
	// methodName, counter));
	// sb.append(String.format("fw_%d.append(\"<vars>\");", randomNumber));
	// for (VariableDeclarationFragment var : varDecs)
	// sb.append(String.format("fw_%d.append(\"<var>%s</var>\");", randomNumber,
	// var.getName()));
	// sb.append(String.format("fw_%d.append(\"</vars>\\n\");", randomNumber));
	//
	// sb.append(String.format("ObjectOutputStream out_%d =
	// xstream_%d.createObjectOutputStream(fw_%d);", randomNumber,
	// randomNumber, randomNumber));
	// for (VariableDeclarationFragment var : varDecs)
	// sb.append(String.format("out_%d.writeObject(%s);", randomNumber,
	// var.getName()));
	// sb.append(String.format("out_%d.close();}catch (IOException
	// e){e.printStackTrace();}", randomNumber));
	// return Utils.createBlockWithText(sb.toString());
	//
	// }

	public static String getTextForInstrumentation(List<VariableDeclarationFragment> list, int counter,
			Map<String, VariableDeclarationFragment> unassignedVars, List<ClassModel> clazzes) {
		StringBuilder sb = new StringBuilder();
		sb.append(String.format("InstrumentClassGenerator.traceTestStatementExecution("));
		boolean remove = false;
		// if (clazzes.size() > 1)
		// System.out.println();
		for (ClassModel clazz : clazzes) {
			for (FieldDeclaration fd : clazz.getStaticFields()) {
				for (Object obj : fd.fragments()) {
					VariableDeclarationFragment vdf = (VariableDeclarationFragment) obj;
					sb.append("\"");
					sb.append(vdf.getName());
					sb.append("\"");
					sb.append(',');
					remove = true;
				}
			}
			for (FieldDeclaration fd : clazz.getFields()) {
				for (Object obj : fd.fragments()) {
					VariableDeclarationFragment vdf = (VariableDeclarationFragment) obj;
					sb.append("\"");
					sb.append(vdf.getName());
					sb.append("\"");
					sb.append(',');
					remove = true;
				}
			}
		}
		for (VariableDeclarationFragment var : list) {
			sb.append("\"");
			sb.append(var.getName());
			sb.append("\"");
			sb.append(',');
			remove = true;
		}
		if (remove)
			sb.setLength(sb.length() - 1);
		sb.append(");");

		remove = false;
		sb.append("InstrumentClassGenerator.writeObjects(");
		for (ClassModel clazz : clazzes) {
			for (FieldDeclaration fd : clazz.getStaticFields()) {
				for (Object obj : fd.fragments()) {
					VariableDeclarationFragment vdf = (VariableDeclarationFragment) obj;
					String varName = clazz.getTypeDec().getName() + "." + vdf.getName().getIdentifier();
					addTextForWriteObject(unassignedVars, sb, vdf, varName);
					remove = true;
				}
			}
			for (FieldDeclaration fd : clazz.getFields()) {
				for (Object obj : fd.fragments()) {
					VariableDeclarationFragment vdf = (VariableDeclarationFragment) obj;
					String varName = "this." + vdf.getName().getIdentifier();
					addTextForWriteObject(unassignedVars, sb, vdf, varName);
					remove = true;
				}
			}
		}

		for (VariableDeclarationFragment var : list) {
			String varName = var.getName().getIdentifier();
			addTextForWriteObject(unassignedVars, sb, var, varName);
			remove = true;
		}
		if (remove)
			sb.setLength(sb.length() - 1);
		sb.append(");");
		sb.append(String.format("InstrumentClassGenerator.initTestStatement(%d);", counter));

		String text = sb.toString();
		return text;
	}

	private static void addTextForWriteObject(Map<String, VariableDeclarationFragment> unassignedVars, StringBuilder sb,
			VariableDeclarationFragment var, String varName) {
		if (unassignedVars == null || !unassignedVars.containsKey(var.getName().toString())) {
			IBinding bind = var.resolveBinding();
			IVariableBinding iv = (IVariableBinding) bind;
			if (iv != null) {
				ITypeBinding typeBind = iv.getType();
				if (typeBind != null)
					if (typeBind.getName().toString().contains("[][]"))
						sb.append("(Object[])");
			}
			sb.append(varName);
		} else
			sb.append("new NullValueType(\"" + var.getName().resolveTypeBinding().getName() + "\", \""
					+ var.getName().getIdentifier() + "\")");
		sb.append(',');
	}

}
