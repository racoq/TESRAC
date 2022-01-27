package instrument;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;
import com.thoughtworks.xstream.io.xml.StaxDriver;

public class InstrumentClassGenerator {
	static LinkedList<String> callStack = new LinkedList<String>();
	static String methodName;
	static String fileName;
	// static XStream xstream = new XStream(new StaxDriver());
	static XStream xstream = new XStream(new DomDriver("UTF-16"));
	// static FileWriter fw = null;
	static BufferedWriter fw = null;
	static ObjectOutputStream out;
	static boolean exists = false;

	public static void init(String methodName) {
		InstrumentClassGenerator.methodName = methodName;
		callStack.clear();
	}

	public static void traceMethodCallEntry(String methodName, Object... input) {
		callStack.add(methodName);
		if (callStack.size() == 1 && !exists) {
			try {
				fw.append("<methodCall>");
				fw.append(String.format("<method>%s</method>\n", methodName));
				fw.append("<input>");
				writeObjects(input);
				fw.append("</input>");
				fw.append("</methodCall>\n");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

	public static void traceMethodCallExit() {
		callStack.removeLast();
		close();
	}

	public static void close() {
		try {
			fw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void initTestStatement(int lineNumber) {

		fileName = String.format("traces\\%s-%d.xml", methodName, lineNumber);

//		if (new File(fileName).exists()) {
//			exists = true;
//			return;
//		}
		exists = false;
		try {
			if (fw != null)
				fw.close();
			// fw = new FileWriter(fileName);
			fw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fileName), StandardCharsets.UTF_16));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void traceTestStatementExecution(String... vars) {
		callStack.clear();
//		if (exists)
//			return;
		try {
			fw.append("<vars>");
			for (String var : vars) {
				fw.append(String.format("<var>%s</var>", var));
			}
			fw.append("</vars>\n");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public static void writeObjects(Object... input) {
//		if (exists)
//			return;
		try {
			out = xstream.createObjectOutputStream(fw);
			for (Object obj : input) {
				out.writeObject(obj);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();
		} finally {
			try {
				out.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

}
