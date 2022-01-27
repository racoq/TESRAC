package tesrac.tools;

import java.io.File;

import tesrac.utils.EvoSuiteHelper;

/**
 * Implements the EvoSuite behavior
 * @author JBecho
 *
 */
public class EvoSuite extends TSRTool {
	
	public EvoSuite(String path) {
		super("EvoSuite", path);
	}

	@Override
	public long execute() {	
		long start = System.currentTimeMillis();
		String reducedProject = getSubject().getReducedPath() + getName() + "\\src\\test\\java";
		String projectPath = getSubject().getProjectPath();
		String [] args = {reducedProject, projectPath+"\\src\\test\\java", "-target", projectPath + "\\target\\classes"};
		try{
			org.evosuite.EvoSuite.main(args);
			EvoSuiteHelper.prepareTests(new File(reducedProject));
		} catch(Exception e) {
			return -1;
		}
		long end = System.currentTimeMillis();
		return end-start;
	}

}
