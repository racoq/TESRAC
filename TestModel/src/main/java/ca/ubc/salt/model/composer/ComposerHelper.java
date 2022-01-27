package ca.ubc.salt.model.composer;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.AnnotationTypeMemberDeclaration;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.EnumConstantDeclaration;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IExtendedModifier;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.Initializer;
import org.eclipse.jdt.core.dom.MarkerAnnotation;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Modifier.ModifierKeyword;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;

import ca.ubc.salt.model.instrumenter.ClassModel;
import ca.ubc.salt.model.instrumenter.Method;
import ca.ubc.salt.model.merger.BackwardTestMerger;
import ca.ubc.salt.model.state.TestStatement;
import ca.ubc.salt.model.utils.FileUtils;
import ca.ubc.salt.model.utils.Settings;
import ca.ubc.salt.model.utils.Utils;

public class ComposerHelper
{

    public static void makeParentChainPublic(ClassModel clazz)
    {
	List<String> paths = Utils.getAllParentsPaths(clazz.name);
	for (String path : paths)
	{
	    path = path.replace(Settings.PROJECT_PATH, Settings.PROJECT_MERGED_PATH);
	    String source;
	    List<ClassModel> classes;
	    try
	    {
		source = FileUtils.readFileToString(path);

		Document document = new Document(source);
		classes = ClassModel.getClasses(document.get(), true, Utils.getUnitName(path),
			new String[] { Settings.PROJECT_PATH },
			new String[] { Settings.LIBRARY_JAVA, Settings.PROJECT_PATH + "\\target" });

		ASTRewrite rewriter = ASTRewrite.create(classes.get(0).getCu().getAST());

		for (ClassModel pclazz : classes)
		{
		    makeEverythingPublic(pclazz, rewriter);
		}

		TextEdit edits = rewriter.rewriteAST(document, null);
		try
		{
		    edits.apply(document);
		} catch (MalformedTreeException | BadLocationException e)
		{
		    e.printStackTrace();
		}

		Utils.writebackSourceCode(document, path);
	    } catch (IOException e)
	    {
		// TODO Auto-generated catch block
		e.printStackTrace();
	    }
	}
    }

    public static void makeEverythingPublic(ClassModel clazz, ASTRewrite rewriter)
    {
	List<BodyDeclaration> decs = clazz.getTypeDec().bodyDeclarations();
	Modifier oldModifier = null;
	for (BodyDeclaration bd : decs)
	{
	    List<IExtendedModifier> mods = bd.modifiers();
	    ListRewrite listRewrite = null;

	    listRewrite = rewriter.getListRewrite(bd, bd.getModifiersProperty());

	    for (Iterator<IExtendedModifier> iterator = mods.iterator(); iterator.hasNext();)
	    {
		IExtendedModifier obj = iterator.next();
		if (obj instanceof Modifier)
		{
		    Modifier mod = (Modifier) obj;
		    if (mod.isPrivate() || mod.isPublic() || mod.isProtected())
		    {
			listRewrite.remove(mod, null);
		    }

		}

	    }
	    AST ast = clazz.getTypeDec().getAST();
	    listRewrite.insertFirst(ast.newModifier(ModifierKeyword.PUBLIC_KEYWORD), null);
	}
    }

    public static ASTNode getDeclarationHeader(Set<String> classes, String mainTestClass)
    {

	StringBuilder sb = new StringBuilder();
	for (String clazz : classes)
	{
	    if (!clazz.equals(mainTestClass))
	    {
		String setup = hasSetup(clazz);
		if (setup != null)
		{
		    sb.append(String.format("%s.%s();", Utils.getTestCaseName(clazz).toLowerCase(), setup));
		}
		sb.append(String.format("%s %s = new %s();", clazz, Utils.getTestCaseName(clazz.toLowerCase()), clazz));
	    }
	}

	return Utils.createBlockWithText(sb.toString());

    }

    public static void renameMethodCalls(List<TestStatement> originalStatements, String mainClass)
    {
	for (TestStatement statement : originalStatements)
	{
	    if (statement.statement == null)
		continue;
	    if (!mainClass.equals(Utils.getTestClassNameFromTestStatement(statement.getName())))
		statement.refactoredStatement = renameMethodInvocs(statement);
	}
    }

    public static Statement renameMethodInvocs(TestStatement stmt)
    {
	Statement cpyStmt = (Statement) ASTNode.copySubtree(stmt.refactoredStatement.getAST(),
		stmt.refactoredStatement);
	List<MethodInvocation> methodInvocs = TestCaseComposer.getMethodInvocations(cpyStmt);
	List<MethodInvocation> testMethodInvocation = TestCaseComposer.getTestMethodInvocations(stmt);
	Set<String> testMethodNames = Utils.getNames(testMethodInvocation);
	for (MethodInvocation methodCall : methodInvocs)
	{
	    if (testMethodNames.contains(methodCall.getName().getIdentifier()))
	    {
		// String renamedVar = methodCall.getName().getIdentifier() +
		// "_"
		// + Utils.getTestClassNameFromTestStatement(stmt.getName());
		// methodCall.setName(methodCall.getAST().newSimpleName(renamedVar));
		methodCall.setExpression(methodCall.getAST().newSimpleName(
			Utils.getTestCaseName(Utils.getTestClassNameFromTestStatement(stmt.getName())).toLowerCase()));
	    }
	}
	return cpyStmt;
    }

    public static List<TypeDeclaration> getAllClassDefs(ClassModel clazz)
    {
	return null;
    }

    public static String hasSetup(String className)
    {
	Document document;
	try
	{
	    ClassModel clazz = Utils.classes.get(className);

	    List<ClassModel> parents = clazz.getAllSuperModelsAndThis();
	    for (ClassModel parent : parents)
	    {
		String setup = hasSetup(parent);
		if (setup != null)
		    return setup;
	    }
	} catch (ExecutionException e)
	{
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}

	return null;
    }

    public static String hasSetup(ClassModel clazz)
    {
	List<Method> methods = clazz.getMethods();
	for (Method m : methods)
	{
	    List modifs = m.getMethodDec().modifiers();
	    for (Iterator it = modifs.listIterator(); it.hasNext();)
	    {
		Object obj = it.next();
		if (obj instanceof MarkerAnnotation)
		{
		    MarkerAnnotation ma = (MarkerAnnotation) obj;
		    if (ma.getTypeName().toString().contains("Before"))
		    {
			return m.getMethodDec().getName().getIdentifier();
		    }
		}
	    }
	}

	return null;
    }

    public static void writeBackMergedTestCases(List<TestStatement> originalStatements, Set<String> testCases,
	    String name, Map<String, Set<String>> testClasses, String mainClassName) throws IOException
    {
	if (BackwardTestMerger.testCasesToRemove.size() > 0)
	    return;

	// class -> testCases
	renameMethodCalls(originalStatements, mainClassName);

	List<ASTNode> path = TestCaseComposer.getPathFromStatements(originalStatements);

	addClassHeaderInstantiation(testClasses, mainClassName, path);

	Map<String, ImportDeclaration> imports = new HashMap<String, ImportDeclaration>();

	while (!testClasses.isEmpty())
	{

	    String testClassName = Utils.getTestClassWithMaxNumberOfTestCases(testClasses);

	    Document document = TestCaseComposer.getDocumentForClassName(testClassName);
	    String testClassPath = Utils.getClassFileForProjectPath(testClassName, Settings.PROJECT_MERGED_PATH);
	    List<ClassModel> classes = ClassModel.getClasses(document.get(), true, testClassPath,
		    new String[] { Settings.PROJECT_PATH}, new String[] { Settings.LIBRARY_JAVA });

	    Set<String> testCasesOfClass = testClasses.get(testClassName);

	    ASTRewrite rewriter = ASTRewrite.create(classes.get(0).getCu().getAST());

	    for (ClassModel clazz : classes)
	    {

		if (clazz.name.equals(testClassName))
		{

		    ListRewrite listRewrite = rewriter.getListRewrite(clazz.getTypeDec(),
			    TypeDeclaration.BODY_DECLARATIONS_PROPERTY);

		     TestCaseComposer.removeTestCasesFromTestClass(clazz,
		     testCasesOfClass, rewriter);

		    if (testClassName.equals(mainClassName))
		    {
			Settings.consoleLogger
				.error(String.format("adding tests to %s", Utils.getClassFile(mainClassName)));
			TestCaseComposer.addMergedTestCase(path, name, clazz, listRewrite);
			// System.out.println(getMergedMethod(path, name,
			// clazz.getTypeDec().getAST()).toString());

		    } else
		    {
			TestCaseComposer.getAllImports(imports, clazz);
			makeEverythingPublic(clazz, rewriter);

		    }
		    makeParentChainPublic(clazz);

		}
	    }
	    TextEdit edits = rewriter.rewriteAST(document, null);
	    try
	    {
		edits.apply(document);
	    } catch (MalformedTreeException | BadLocationException e)
	    {
		e.printStackTrace();
	    }

	    Utils.addImports(document, Arrays.asList(new String[] { "org.junit.Ignore" }));

	    Utils.writebackSourceCode(document,
		    Utils.getClassFileForProjectPath(testClassName, Settings.PROJECT_MERGED_PATH));
	    // System.out.println(document.get());

	    testClasses.remove(testClassName);
	    // parse each test class
	    // remove and replace
	    // if they have variables var with isField() add TestClass x = new
	    // TestClass(); var -> x.var
	}

	TestCaseComposer.addImportsAndNonTestMethodsToMainClass(null, mainClassName, imports);
    }

    private static void addClassHeaderInstantiation(Map<String, Set<String>> testClasses, String mainClassName,
	    List<ASTNode> path)
    {
	Block block = (Block) getDeclarationHeader(testClasses.keySet(), mainClassName);

	List<Statement> stmts = block.statements();

	for (Statement stmt : stmts)
	{
	    path.add(0, stmt);
	}
    }
}
