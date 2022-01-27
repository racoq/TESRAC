package tesrac.utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * A logger
 * @author JBecho
 *
 */
public class Logger {

	
	private static Logger logger;
	
	private String className;
	
	private Logger(String name) {
		className = name;
	}
	public static Logger getInstance(String name) {
		if(logger == null)
			return new Logger(name);
		logger.setClassName(name);
		return logger;
	}
	
	private void setClassName(String name) {
		className = name;
	}
	
	public void info(String message) {
		File out = new File("out.log");
		PrintWriter pw = null;
		try {
			pw = new PrintWriter(new FileWriter(out, true));
		} catch (IOException e) {
			
		}
		StringBuilder sb = new StringBuilder();
		sb.append(msToHHMMSS(System.currentTimeMillis()));
		sb.append("[INFO]");
		sb.append(className + ": ");
		sb.append(message);
		String msg = sb.toString();
		if(pw != null) pw.println(msg);
		System.out.println(msg);
		pw.close();
	}
	
	public void error(String message) {
		File out = new File("out.log");
		PrintWriter pw = null;
		try {
			pw = new PrintWriter(new FileWriter(out, true));
		} catch (IOException e) {
			
		}
		StringBuilder sb = new StringBuilder();
		sb.append(msToHHMMSS(System.currentTimeMillis()));
		sb.append("[ERROR]");
		sb.append(className + ": ");
		sb.append(message);
		String msg = sb.toString();
		if(pw != null) pw.println(msg);
		System.out.println(msg);
		pw.close();
	}
	
	private String msToHHMMSS(long ms) {
		return new SimpleDateFormat("|dd-MM-yyyy|HH:mm:ss|").format(new Date(ms));
	}

	
}
