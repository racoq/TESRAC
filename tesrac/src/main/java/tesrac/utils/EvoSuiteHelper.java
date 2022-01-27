package tesrac.utils;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
//import java.util.HashSet;
import java.util.Scanner;

public class EvoSuiteHelper {
	
	private static String[] TO_ERASE = {"org.evosuite.runtime.EvoAssertions", "import static org.mockito.Mockito", 
			"EvoRunner", "System.setCurrent", "Random.set", "EvoSuiteFile", "FileSystemHandling"};
	private static String EXCPTION = "verifyException";
	
	public static void prepareTests(File folder) throws IOException {
		
		for(File f : folder.listFiles()) {
			if(!f.isDirectory()) {
				if(f.getName().endsWith("_scaffolding.java")) {
					f.delete();
				} else {
					convert(f.getPath());
				}
			} else {
				prepareTests(f);
			}
		}
	}

	private static void convert(String pathToTest) throws IOException {
		Scanner sc = new Scanner(new File(pathToTest));	
		ArrayList<String> content = new ArrayList<>();
		String line;
		while(sc.hasNextLine()) {
			line = sc.nextLine();
			handleLine(line, content, sc);
		}
		
		sc.close();
		PrintWriter writer = new PrintWriter(new File(pathToTest));
		for(String string : content) {
			writer.print(string);
		}
		writer.close();
	}
	
	private static void handleLine(String line, ArrayList<String> content, Scanner sc) {	
		switch (operation(line)) {
		case 0: //erase this line
			break;
		case 1: //replace import of mock for a java import
			content.add(line.replace("org.evosuite.runtime.mock.", "").replace("Mock", "") + "\n");
			break;
		case 2: //erase the extends scaffolding
			content.add(line.substring(0, line.indexOf("extends")) + "{\n");
			break;
		case 3: //modifies exception handling
			content.add(getNrSpaces(line) + "boolean b = false;\n");
			handleException(content, sc, line);
			break;
		case 4: //replace Mock
			handleMocks(line, content);
			break;
		case 5: //FileSystemHandling 
			content.add(line.substring(0, line.indexOf("=")) + "= true;\n");
			break;
		case 6: //executor
			handleExecutor(sc, content);
			break;
		case 7: //clone()
			handleClone(line, content);
			break;
		default:
			content.add(line + "\n");
		}	
	}

	private static void handleClone(String line, ArrayList<String> content) {
		String pattern = "\\s*(\\w+)\\s(\\w+)\\s=\\s(\\w+)\\.clone\\(\\);";
		String newLine = line.replaceAll(pattern, "$1 $2 = ($1) $3.clone();");
		content.add(newLine + "\n");
	}

	private static void handleExecutor(Scanner sc, ArrayList<String> content) {
		int lastIdx = content.size()-1;
		content.remove(lastIdx);
		content.remove(lastIdx-1);
		int brackets = 2;
		while(brackets != 0) {
			String line = sc.nextLine();
			brackets += getBracketsCount(line);
		}
	}

	private static void handleMocks(String line, ArrayList<String> content) {
		
		if(line.contains("URI")) {
			String httpURI = "MockURI.aHttpURI";
			String fileURI = "MockURI.aFileURI";
			String ftpURI = "MockURI.aFTPURI";
			if(line.contains(httpURI))
				line = line.replace(httpURI, "URI.create(\"http://foo.bar\")");
			else if(line.contains(fileURI))
				line = line.replace(fileURI, "URI.create(\"file:///tmp/foo.bar\")");
			else if(line.contains(ftpURI))
				line = line.replace(ftpURI, "URI.create(\"ftp://foo.bar\")");
			else if(line.contains("MockURI.URI"))
				line = line.replace("MockURI.URI", "new URI");
			else if(line.matches("(.+)MockURI.normalize\\((.+)\\);")) {
				line = line.replaceAll("(.+)MockURI.normalize\\((.+)\\);", "$1$2.normalize();");
			}
		}
		content.add(line.replace("Mock", "") + "\n");
	}

	private static int operation(String line) {
		if(line.contains("import")) {
			if(line.contains("FileSystemHandling") || 
					line.contains("EvoSuiteFile"))
				return 0;
			if(line.contains("org.evosuite.runtime.mock")){
				return 1;
			}
			if(line.contains("org.evosuite"))
				return 0;
		}
		if(line.contains("public class")) 
			return 2;
		if(containsOneOfToErase(line)) {
			if(line.contains("= FileSystemHandling"))
				return 5;
			return 0;
		}
		if(line.contains("try {"))
			return 3;
		if(line.contains("Mock") && !line.contains("MockRule") 
				&& !line.contains("mockito.Mockito.")){
			if(line.contains("MockThrowable."))
				return 0;
			return 4;
		}
		if(line.contains("Future<?> future = executor.")) {
			return 6;
		}
		if(line.matches("(\\s)*mockThrowable[0-9]+\\..+"))
			return 0;
		if(line.contains(".clone()")) {
			if(!line.contains("="))
				return 0;
			return 7;
		}
		if(line.matches("\\s+path[0-9].isEmpty();"))
				return 0;
		return 10;
	}

	private static String getNrSpaces(String line) {
		int spaces = 0;
		char space = ' ';
		StringBuilder sb = new StringBuilder();
		while(spaces < line.length() && line.charAt(spaces) == space) {
			spaces++;
			sb.append(' ');
		}
		return sb.toString();
	}

	private static void handleException(ArrayList<String> content, Scanner sc, String fstLine) {
		
		String line = sc.nextLine();
		boolean excp = fstLine.contains("try {");
		int brackets = 1;
		content.add(fstLine + "\n");
		handleLine(line, content, sc);
		while(excp) {
			line = sc.nextLine();
			if(line.trim().startsWith("//"))
				continue;
			if(line.contains(EXCPTION))
				content.add(getNrSpaces(line) + "b = true;\n");
			else
				handleLine(line, content, sc);
			brackets += getBracketsCount(line);
			excp = brackets!=0;
		}
		
		content.add(getNrSpaces(line) + "assertTrue(b);\n");

	}


	private static int getBracketsCount(String line) {
		if(line.trim().startsWith("//"))
			return 0;
		int sum = 0;
		boolean str = false;
		boolean chr = false;
		for(int i = 0; i < line.length(); i++) {
			char c = line.charAt(i);
			if(c == '\"' && line.charAt(i-1) != '\\')
				str = !str;
			if(c == '\'' && line.charAt(i-1) != '\\')
				chr = !chr;
			if(c == '{' && !str && !chr)
				sum++;
			if(c == '}' && !str && !chr)
				sum--;
		}
		return sum;
	}

	private static boolean containsOneOfToErase(String line) {
		boolean contains = line.contains(TO_ERASE[0]);
		for(int i = 1; i < TO_ERASE.length; i++)
			contains = contains || line.contains(TO_ERASE[i]);
		return contains;
	}

}
