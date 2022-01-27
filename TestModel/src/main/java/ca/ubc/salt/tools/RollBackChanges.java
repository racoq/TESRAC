package ca.ubc.salt.tools;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;

import ca.ubc.salt.model.composer.TestCaseComposer;
import ca.ubc.salt.model.instrumenter.ClassModel;
import ca.ubc.salt.model.merger.MergingResult;
import ca.ubc.salt.model.utils.Settings;
import ca.ubc.salt.model.utils.Utils;

public class RollBackChanges
{

    static final String mergedInfoPath = Settings.SUBJECT + "-" + "mergingResult.xml";

    public static void main(String[] args) throws FileNotFoundException, IOException, ClassNotFoundException
    {
	ObjectInputStream in = new ObjectInputStream(new FileInputStream(mergedInfoPath));
	List<MergingResult> mergingResults = (List<MergingResult>) in.readObject();
	in.close();

	Scanner sc = new Scanner(System.in);

	Set<String> testsToKeep = new HashSet<String>();

	while (sc.hasNextLine())
	    testsToKeep.add(sc.nextLine());

	System.out.println("starting");
	for (MergingResult mr : mergingResults)
	{
	    if (testsToKeep.contains(mr.getMergedClassName() + "." + mr.getMergedTestCaseName()))
		applyChanges(mr);
	    else
		rollBackChanges(mr);
	}

	System.out.println("done!");

    }

    public static void rollBackChanges(MergingResult mergingResult) throws IOException
    {
	Map<String, Set<String>> testClasses = Utils.getTestClassMapFromTestCases(mergingResult.getMergedTestCases());

	for (String testClassName : testClasses.keySet())
	{

	    Document document = TestCaseComposer.getDocumentForClassName(testClassName);
	    String testClassPath = Utils.getClassFileForProjectPath(testClassName, Settings.PROJECT_MERGED_PATH);
	    List<ClassModel> classes = ClassModel.getClasses(document.get(), true, testClassPath,
		    new String[] { Settings.PROJECT_PATH }, new String[] { Settings.LIBRARY_JAVA });

	    Set<String> testCasesOfClass = testClasses.get(testClassName);

	    ASTRewrite rewriter = ASTRewrite.create(classes.get(0).getCu().getAST());

	    for (ClassModel clazz : classes)
	    {

		if (clazz.name.equals(testClassName))
		{
		    TestCaseComposer.reAddTestCasesFromTestClass(clazz, testCasesOfClass, rewriter, false);
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

	    Utils.writebackSourceCode(document,
		    Utils.getClassFileForProjectPath(testClassName, Settings.PROJECT_MERGED_PATH));
	}

	String mergedTestCaseClass = mergingResult.getMergedClassName();
	Set<String> mergedTestCase = new HashSet<String>();
	mergedTestCase.add(mergedTestCaseClass + "." + mergingResult.getMergedTestCaseName());

	Document document = TestCaseComposer.getDocumentForClassName(mergedTestCaseClass);
	String testClassPath = Utils.getClassFileForProjectPath(mergedTestCaseClass, Settings.PROJECT_MERGED_PATH);
	List<ClassModel> classes = ClassModel.getClasses(document.get(), true, testClassPath,
		new String[] { Settings.PROJECT_PATH }, new String[] { Settings.LIBRARY_JAVA });

	ASTRewrite rewriter = ASTRewrite.create(classes.get(0).getCu().getAST());

	for (ClassModel clazz : classes)
	{

	    if (clazz.name.equals(mergedTestCaseClass))
	    {
		TestCaseComposer.reAddTestCasesFromTestClass(clazz, mergedTestCase, rewriter, false);
		TestCaseComposer.removeTestCasesFromTestClass(clazz,
		mergedTestCase, rewriter);
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

	Utils.writebackSourceCode(document,
		Utils.getClassFileForProjectPath(mergedTestCaseClass, Settings.PROJECT_MERGED_PATH));

    }
    public static void applyChanges(MergingResult mergingResult) throws IOException
    {
	Map<String, Set<String>> testClasses = Utils.getTestClassMapFromTestCases(mergingResult.getMergedTestCases());
	
	for (String testClassName : testClasses.keySet())
	{
	    
	    Document document = TestCaseComposer.getDocumentForClassName(testClassName);
	    String testClassPath = Utils.getClassFileForProjectPath(testClassName, Settings.PROJECT_MERGED_PATH);
	    List<ClassModel> classes = ClassModel.getClasses(document.get(), true, testClassPath,
		    new String[] { Settings.PROJECT_PATH }, new String[] { Settings.LIBRARY_JAVA });
	    
	    Set<String> testCasesOfClass = testClasses.get(testClassName);
	    
	    ASTRewrite rewriter = ASTRewrite.create(classes.get(0).getCu().getAST());
	    
	    for (ClassModel clazz : classes)
	    {
		
		if (clazz.name.equals(testClassName))
		{
		    TestCaseComposer.reAddTestCasesFromTestClass(clazz, testCasesOfClass, rewriter, false);
		    TestCaseComposer.removeTestCasesFromTestClass(clazz, testCasesOfClass, rewriter);
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
	    
	    Utils.writebackSourceCode(document,
		    Utils.getClassFileForProjectPath(testClassName, Settings.PROJECT_MERGED_PATH));
	}
	
	String mergedTestCaseClass = mergingResult.getMergedClassName();
	Set<String> mergedTestCase = new HashSet<String>();
	mergedTestCase.add(mergedTestCaseClass + "." + mergingResult.getMergedTestCaseName());
	
	Document document = TestCaseComposer.getDocumentForClassName(mergedTestCaseClass);
	String testClassPath = Utils.getClassFileForProjectPath(mergedTestCaseClass, Settings.PROJECT_MERGED_PATH);
	List<ClassModel> classes = ClassModel.getClasses(document.get(), true, testClassPath,
		new String[] { Settings.PROJECT_PATH }, new String[] { Settings.LIBRARY_JAVA });
	
	ASTRewrite rewriter = ASTRewrite.create(classes.get(0).getCu().getAST());
	
	for (ClassModel clazz : classes)
	{
	    
	    if (clazz.name.equals(mergedTestCaseClass))
	    {
		TestCaseComposer.reAddTestCasesFromTestClass(clazz, mergedTestCase, rewriter, true);
		// TestCaseComposer.removeTestCasesFromTestClass(clazz,
		// mergedTestCase, rewriter);
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
	
	Utils.writebackSourceCode(document,
		Utils.getClassFileForProjectPath(mergedTestCaseClass, Settings.PROJECT_MERGED_PATH));
	
    }

}
