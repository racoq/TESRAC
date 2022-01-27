package tesrac.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

/**
 * This class contains methods that help with the Testler's reduced suites conversion
 * @author JBecho
 *
 */
public class TestlerSuiteHelper {

	public static void convert(File folder) throws FileNotFoundException {
		for(File f : folder.listFiles()) {
			if(f.isDirectory()) {
				convert(f);
			} else {
				if(f.getName().endsWith(".java")) {
					System.out.println("Converting " + f.getName());
					convertTests(f);
				}			
			}
		}
	}

	private static void convertTests(File f) throws FileNotFoundException {
		ArrayList<String> content = new ArrayList<>();
		Scanner sc = new Scanner(f);
		while(sc.hasNextLine()) {
			String line = sc.nextLine();
			if(line.trim().startsWith("@")) {
				
				if(line.contains("@Test") && line.contains("public void") && line.contains("_"))
					getMethod(line, sc, content);
				else 
					content.add(line);
			} else 
				content.add(line);
		}

		PrintWriter pw = new PrintWriter(f);
		System.out.println("Writing into " + f.getPath());
		for(String s : content) {
			pw.println(s);
		}
			
		pw.close();
		sc.close();
	}

	private static void getMethod(String line, Scanner sc, ArrayList<String> content) {
		content.add("@Test");	
		ArrayList<String> thisContent = getMethodContentWithLineBreaks(line.replace("@Test", ""));
		ArrayList<String> types = new ArrayList<>();
		String pattern = ".*\\s([a-z]+)Array([0-9]).*";
		for(String s : thisContent) {
			if(s.matches(pattern)) {
				String name = s.replaceAll(pattern, "$1Array$2");
				if(s.contains("new")){
					types.add(name);
				}else {
					if(!types.contains(name)) {
						String type = s.replaceAll(pattern, "$1");
						if(type.equals("string"))
							type = "String";
						if(type.equals("object"))
							type = "Object";
						content.add("\t" + type + " [] " + name + " = new " + type + "[10];");
					}
				}
				content.add(s);
			} else {
				content.add(s);
			}			
		}
	}

	private static ArrayList<String> getMethodContentWithLineBreaks(String line) {
		ArrayList<String> methodContent = new ArrayList<>();
		StringBuilder sb = new StringBuilder();
		int tabs = 0;
		boolean newLine = false;
		boolean str = false;
		for(int i = 0; i < line.length(); i++) {
			char c = line.charAt(i);
			if(newLine) {
				if(c == '}')
					tabs--;
				sb.append(getTabs(tabs));
				newLine = false;
			}
			if(c == '\"') {
				if(i>0) {
					if(line.charAt(i-1) != '\\') {
						str = !str;
					} else {
						if(i>2 && line.charAt(i-2) == '\\' && line.charAt(i-3) != '\\')
							str = !str;
					}
				}				
			}

			if(!str) {
				if(c == '{') {
					sb.append("{\n");
					newLine = true;
					tabs++;
				}
				else if(c == '}' || c == ';') {
					sb.append(c + "\n");
					newLine = true;
				}
				else 
					sb.append(c);
			} else 
				sb.append(c);
		}

		methodContent.addAll(Arrays.asList(sb.toString().split("\n")));
		return methodContent;
	}

	private static String getTabs(int tabs) {
		StringBuilder sb = new StringBuilder();
		for(int i = 1; i <= tabs; i++)
			sb.append("    ");
		return sb.toString();
	}

}
