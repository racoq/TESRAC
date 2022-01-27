package tesrac.tools;

import java.io.File;
import java.io.IOException;

import org.xml.sax.SAXParseException;

import tesrac.utils.Configuration;
import tesrac.utils.Logger;
import tesrac.utils.TestlerSuiteHelper;

/**
 * Implements the Testler behaviour
 * @author JBecho
 *
 */
public class Testler extends TSRTool {

	private static final Logger logger = Logger.getInstance(Testler.class.getName());

	/**
	 * Constructor
	 * Just calls the constructor from the super class
	 * @param name
	 * @param path
	 */
	public Testler(String name, String path) {
		super(name, path);
	}

	/**
	 * Executes Testler
	 */
	@Override
	public long execute() {
		long start = System.currentTimeMillis();
		String projectPath = getSubject().getProjectPath();
		String subject = getSubject().getProjectName();
		String reducedPath = Configuration.getReducedProjectsPaths() + "/" + subject + "/" + getName() + "/";

		String name = new File(projectPath).getName();
		String subjectPath = getSubject().getProjectPath().substring(0, projectPath.indexOf(name));

		logger.info("Running Testler");
			try {
				ca.ubc.salt.Main.main(new String[]{subject, subjectPath, reducedPath});
				TestlerSuiteHelper.convert(new File(reducedPath + "src\\test\\java"));
			} catch (ClassNotFoundException | SAXParseException | NoSuchFieldException | IllegalAccessException
					| IOException | CloneNotSupportedException | InterruptedException e) {
				e.printStackTrace();
				return -1;
			}
			
		long end = System.currentTimeMillis();
		return end-start;
	}
}
