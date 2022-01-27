package ca.ubc.salt.model.evaluation;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;
import org.junit.internal.Classes;

import ca.ubc.salt.model.instrumenter.ClassModel;
import ca.ubc.salt.model.instrumenter.ProductionClassInstrumenter;
import ca.ubc.salt.model.instrumenter.TestClassInstrumenter;
import ca.ubc.salt.model.utils.FileUtils;
import ca.ubc.salt.model.utils.Settings;
import ca.ubc.salt.model.utils.Utils;

public class StopWatchAdder
{

    
    public static void main(String[] args) throws IllegalArgumentException, MalformedTreeException, IOException, BadLocationException, CoreException
    {
	Utils.copyProject(Settings.PROJECT_PATH, Settings.PROJECT_STOPWATCH_PATH);
	instrumentClass(Settings.PROJECT_PATH);
    }
    public static void instrumentClass(String classPath)
	    throws IOException, IllegalArgumentException, MalformedTreeException, BadLocationException, CoreException
    {
	File fClass = new File(classPath);
	if (fClass.isFile() && fClass.getAbsolutePath().endsWith("java"))
	{
	    if (Utils.isTestClass(fClass))
	    {
		Settings.consoleLogger.error(classPath);
		String source = FileUtils.readFileToString(fClass);
		Document document = new Document(source);
		List<ClassModel> classes = ClassModel.getClasses(source);
		if (classes.size() == 0)
		    return;
		
		CompilationUnit cu = classes.get(0).getCu(); 
		cu.recordModifications();
		Utils.addImport(cu, "evaluation.StopWatchParentTestClass");
		for (ClassModel clazz : classes)
		{
		    if (clazz.typeDec.getSuperclassType() == null)
		    {
			AST ast = clazz.typeDec.getAST();
			clazz.typeDec.setSuperclassType(ast.newSimpleType(ast.newName("StopWatchParentTestClass")));
		    }
		}
		
		TextEdit edits = cu.rewrite(document, null);
		edits.apply(document);
		Utils.writebackSourceCode(document, Settings.getTimedCodePath(classPath));
	    }

	} else if (fClass.isDirectory())
	{
	    File[] listOfFiles = fClass.listFiles();
	    for (int i = 0; i < listOfFiles.length; i++)
	    {
		instrumentClass(listOfFiles[i].getAbsolutePath());
	    }
	}

    }
}
