package ca.ubc.salt.model.instrumenter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.StaxDriver;

import ca.ubc.salt.model.utils.FileUtils;
import ca.ubc.salt.model.utils.Settings;
import ca.ubc.salt.model.utils.Utils;

@RunWith(value = Parameterized.class)
public class Instrumenter {
	
	private static int[] arr;

	private static final String PARENT_CLASS_DEPENDENCY_FILE = String
			.format(Settings.SUBJECT + "-parentClassDependency.txt");

	private static final String PARAMETERIZED_CLASSES_FILE = String
			.format(Settings.SUBJECT + "-parameterizedClasses.txt");

	public static HashMap<String, String> classFileMapping = new HashMap<String, String>();
	public static Map<String, String> classFileMappingShortName = new HashMap<String, String>();
	public static HashMap<String, Set<String>> parentClassDependency = new HashMap<String, Set<String>>();
	public static HashMap<String, String> childClassDependency = new HashMap<String, String>();
	public static Set<String> parameterizedClasses = new HashSet<String>();

	public static void main(String[] args) {

		long startTime = System.currentTimeMillis();

		arr = new int[2];
		// for each test, creates an entry in a map
		classFileMappingShortName = Utils.getClassFileMappingShortName();

		// copies the project in a separate folder for instrumentation
		Utils.copyProject(Settings.PROJECT_PATH, Settings.PROJECT_INSTRUMENTED_PATH);

		try {
			// for each class, creates a class model (heavy)
			initClassFileMapping(Settings.PROJECT_PATH + "\\src");

			// for each subclass, retrieve the superclass (parent)
			initChildClassDependency();

			XStream xstream = new XStream(new StaxDriver());
			FileWriter fw = new FileWriter(Settings.classFileMappingPath);
			fw.write(xstream.toXML(classFileMapping));
			fw.close();

			fw = new FileWriter(Settings.shortClassFileMappingPath);
			fw.write(xstream.toXML(classFileMappingShortName));
			fw.close();

			Settings.consoleLogger.error("initialized class File mapping");
			instrumentClass(Settings.PROJECT_PATH + "\\src"); 
			//TODO
			PrintWriter pw = new PrintWriter(new FileWriter("run.txt", true));
			pw.printf("%s, %d, %d, \n", Settings.PROJECT_PATH.substring(Settings.PROJECT_PATH.lastIndexOf('\\')+1), arr[0], arr[1]);
			pw.close();

			ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(PARAMETERIZED_CLASSES_FILE));
			out.writeObject(parameterizedClasses);
			out.flush();
			out.close();

			out = new ObjectOutputStream(new FileOutputStream(PARENT_CLASS_DEPENDENCY_FILE));
			out.writeObject(parentClassDependency);
			out.flush();
			out.close();

		} catch (IllegalArgumentException | MalformedTreeException | IOException | BadLocationException
				| CoreException e) {
			e.printStackTrace();
		}

		long stopTime = System.currentTimeMillis();
		long elapsedTime = stopTime - startTime;
		System.out.println("Instrumentation time ms : " + elapsedTime);
		System.out.println("Instrumentation time mm:ss : " + msToMin(elapsedTime) + ":" + msToS(elapsedTime));
	}
	
	private static long msToMin(long elapsedTime) {
		return (elapsedTime / 1000) / 60;
	}

	private static long msToS(long elapsedTime) {
        return (elapsedTime / 1000) % 60;
        
	}

	public static void initClassFileMapping(String classPath)
			throws IOException, IllegalArgumentException, MalformedTreeException, BadLocationException, CoreException {

		File fClass = new File(classPath);

		// for each Java class
		if (fClass.isFile() && fClass.getAbsolutePath().endsWith("java")) {

			// if it is a test class
			if (Utils.isTestClass(fClass)) {

				// read the source into a string
				String source = FileUtils.readFileToString(fClass);

				// create a structure which can be parsed
				Document document = new Document(source);

				List<ClassModel> classes = ClassModel.getClasses(document.get(), true, classPath,
						new String[] { Settings.PROJECT_PATH + "\\src" },
						new String[] { Settings.LIBRARY_JAVA, Settings.PROJECT_PATH + "\\target" });

				for (ClassModel clazz : classes) {
					updateStructs(clazz);
					classFileMapping.put(clazz.name, fClass.getAbsolutePath());
					classFileMappingShortName.put(clazz.getTypeDec().getName().getIdentifier(),
							fClass.getAbsolutePath());
				}

			}

			// if it is a directory, go down recursively
		} else if (fClass.isDirectory()) {
			File[] listOfFiles = fClass.listFiles();
			for (int i = 0; i < listOfFiles.length; i++) {
				initClassFileMapping(listOfFiles[i].getAbsolutePath());
			}
		}
	}

	private static void initChildClassDependency() {
		for (Entry<String, Set<String>> entry : parentClassDependency.entrySet()) {
			for (String child : entry.getValue())
				childClassDependency.put(child, entry.getKey());
		}
	}

	public static void loadStructs() {
		try {
			ObjectInputStream in = new ObjectInputStream(new FileInputStream(PARENT_CLASS_DEPENDENCY_FILE));
			parentClassDependency = (HashMap<String, Set<String>>) in.readObject();
			in.close();

			initChildClassDependency();

			in = new ObjectInputStream(new FileInputStream(PARAMETERIZED_CLASSES_FILE));
			parameterizedClasses = (Set<String>) in.readObject();
			in.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void instrumentClass(String classPath)
			throws IOException, IllegalArgumentException, MalformedTreeException, BadLocationException, CoreException {

		File fClass = new File(classPath);

		if (fClass.isFile() && fClass.getAbsolutePath().endsWith("java")) {

			String source = FileUtils.readFileToString(fClass);
			Document document = new Document(source);
			List<ClassModel> classes = ClassModel.getClasses(document.get(), true, classPath,
					new String[] { Settings.PROJECT_PATH + "\\src" },
					new String[] { Settings.LIBRARY_JAVA, Settings.PROJECT_PATH + "\\target" });

			if (!Utils.isTestClass(fClass)) {

				if (!fClass.getAbsolutePath().contains("src\\main"))
					return;

				Settings.consoleLogger.error(String.format("production class: %s", fClass.getName()));
				arr[0]++;
				if (classes.size() > 0) {
					ASTRewrite rewriter = ASTRewrite.create(classes.get(0).getCu().getAST());

					for (ClassModel clazz : classes) {
						// if clazz is an interface this method does not do
						// anything, perhaps add control?
						ProductionClassInstrumenter.instrumentClass(clazz, null, document, rewriter);
					}

					Document newDocument = new Document(document.get());
					TextEdit edits = rewriter.rewriteAST(document, null);
					edits.apply(newDocument);

					ProductionClassInstrumenter.addImports(newDocument);
					document = newDocument;
				}

			} else {
				Settings.consoleLogger.error(String.format("test : %s", classPath));
				arr[1]++;
				if (classes.size() == 0)
					return;

				ASTRewrite rewriter = ASTRewrite.create(classes.get(0).getCu().getAST());
				for (ClassModel clazz : classes) {

					if (clazz.isInstrumentable()) {

						TestClassInstrumenter.instrumentClass(clazz, null, clazz.typeDec.getName().toString(),
								rewriter);
					}
				}

				TextEdit edits = rewriter.rewriteAST(document, null);
				edits.apply(document);

				Utils.addImports(document, Arrays
						.asList(new String[] { "instrument.InstrumentClassGenerator", "instrument.NullValueType" }));
			}

			Utils.writebackSourceCode(document, Settings.getInstrumentedCodePath(classPath));

		} else if (fClass.isDirectory()) {
			File[] listOfFiles = fClass.listFiles();
			for (int i = 0; i < listOfFiles.length; i++) {
				instrumentClass(listOfFiles[i].getAbsolutePath());
			}
		}
	}

	public static void updateStructs(ClassModel clazz) {
		String runWith = "RunWith";
		String runner = "Parameterized";

		// checks if the class uses @RunWith of @Parameters constructs
		if (clazz.isClassIsRunBy(runWith, runner)) {
			parameterizedClasses.add(clazz.name);
		}

		// for each superclass, save the list of its subclasses
		Type parent = clazz.typeDec.getSuperclassType();
		if (parent != null) {
			ITypeBinding binding = parent.resolveBinding();
			String parentName = binding.getQualifiedName();
			Utils.addToTheSetInMap(parentClassDependency, parentName, clazz.name);
		}
	}

}
