package tesrac.tools;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import tesrac.utils.Configuration;
import tesrac.utils.FileUtils;
import tesrac.utils.Logger;

/**
 * Implements the Kanonizo behaviour
 * @author JBecho
 *
 */
public class Kanonizo extends TSRTool {

	/*
	 * The path to the directory where kanonizo will generate the logs
	 */
	private String logsDir;
	
	/*
	 * The list of algorithms that kanonizo will run
	 */
	private List<String> algorithms;
	
	/*
	 * The list of thresholds that kanonizo will use to reduce test suites
	 */
	private List<Integer> cutOffs;
	
	/*
	 * THe list of algorithms that failed
	 */
	private ArrayList<String> algsErrors;
	
	private static final Logger logger = Logger.getInstance(Kanonizo.class.getName());
	
	/**
	 * Constructor
	 * @param path to the jar
	 * @param algorithms List of algorithm names
	 * @param cutOffs List of cutOffs values
	 */
	public Kanonizo(String path, List<String> algorithms, List<Integer> cutOffs) {
		super("Kanonizo", path);
		this.logsDir = System.getProperty("user.dir") + "\\logs\\";
		this.algorithms = algorithms;
		this.cutOffs = cutOffs;
		this.algsErrors = new ArrayList<>();
	}

	@Override
	public long execute() {
		long start = System.currentTimeMillis();
		String subjectName = getSubject().getProjectName();
		
		//Run kanonizo with variousalgorithms
		for(String a : algorithms) {
			String logDirPath = logsDir + subjectName +"\\"+ a;
			File logPath = new File(logDirPath);
			logPath.mkdirs();
			
			String[] args = {"-s", getSubject().getProjectPath()+"\\target\\classes", "-t", 
					getSubject().getProjectPath()+"\\target\\test-classes", "-a", a, "-Dlog_dir=" + logDirPath};
					
			org.kanonizo.Main.main(args);

			for(int cutOff : cutOffs) {
				String projectTestsFolder = Configuration.getReducedProjectsPaths() + "\\" + 
						subjectName + "\\Kanonizo\\" + a + "\\" + cutOff + "\\src\\test\\java";
				try {
					reduce(logDirPath+"\\ordering", projectTestsFolder, cutOff);
				} catch (FileNotFoundException e) {
					logger.error("Couldn't reduce " + getSubject().getProjectName());
					logger.error(e.getMessage());
					e.printStackTrace();
				}
			}
		}
		long end = System.currentTimeMillis();
		return end-start;
	}

	/*
	 * Uses a log generated by kanonizo and a cutOff value to reduce the test suites 
	 * present in the given path
	 */
	private void reduce(String logDirPath, String projectTestsFolder, int cutOff) throws FileNotFoundException {
		File log = new File(logDirPath).listFiles()[0];
		Map<String, ArrayList<String>> map = processKanonizoLog(log);
		for(Map.Entry<String, ArrayList<String>> entry : map.entrySet()) {
			File clazz = new File(projectTestsFolder + "/" + entry.getKey().replace('.', '/') + ".java");
			logger.info("Reducing " + clazz.getName());
			tesrac.utils.FileUtils.eraseTestMethods(clazz, 
					getLastXNames(entry.getValue(), cutOff));
		}
	}

	/*
	 * Returns a subList of the given one with just the last elements defined by the cutOff
	 */
	private List<String> getLastXNames(ArrayList<String> methodNames, int cutOff) {
		int fromIndex = methodNames.size( ) * cutOff / 100;
		return fromIndex == 0 ? null : methodNames.subList(methodNames.size()-1-fromIndex, methodNames.size());
	}

	/*
	 * Reads the kanonizo log file and returns a Map<String, ArrayList<String>> that organizes the methods into
	 * each class
	 */
	private Map<String, ArrayList<String>> processKanonizoLog(File log) throws FileNotFoundException {
		Map<String, ArrayList<String>> map = new HashMap<>();
		Scanner sc = new Scanner(log);
		sc.nextLine(); //Header is not important
		while(sc.hasNextLine()) {
			String methodClass = sc.nextLine().split(",")[0];
			String method = methodClass.substring(0, methodClass.indexOf('('));
			String fromClass = methodClass.substring(methodClass.indexOf('(') + 1, methodClass.indexOf(')'));
			ArrayList<String> methods = map.get(fromClass);
			if(methods == null)
				methods = new ArrayList<>();
			methods.add(method);
			map.put(fromClass, methods);
		}
		sc.close();
		return map;
	}

	@Override
	public void prepareProject() throws IOException {
		//Kanonizo overrides this method because it can potentially generate more than one reduced project
		File projectRoot = new File(getSubject().getProjectPath());
		for(String alg : algorithms) {
			for(int cutOff : cutOffs) {
				String newFolderName = getSubject().getProjectName() + "\\Kanonizo\\" + alg + "\\" + cutOff;
				File newProject = new File(Configuration.getReducedProjectsPaths() + "\\" + newFolderName + "\\");
				newProject.mkdirs();
				FileUtils.copyProject(projectRoot, newProject);
			}	
		}
	}
	
	public List<String> getFailedAlgs(){
		return algsErrors;
	}
}
