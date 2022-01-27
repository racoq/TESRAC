/**
 * Copyright (C) 2010-2018 Gordon Fraser, Andrea Arcuri and EvoSuite
 * contributors
 *
 * This file is part of EvoSuite.
 *
 * EvoSuite is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3.0 of the License, or
 * (at your option) any later version.
 *
 * EvoSuite is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with EvoSuite. If not, see <http://www.gnu.org/licenses/>.
 */
package org.evosuite.runtime.vfs;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicInteger;

import org.evosuite.runtime.testdata.EvoSuiteFile;
import org.evosuite.runtime.LeakingResource;
import org.evosuite.runtime.sandbox.MSecurityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A virtual file system (VFS) to handle the files accessed/generated by the
 * test cases. No file is ever written to disk.
 * 
 * <p>
 * The VFS reflects the actual OS in which the JVM is running (eg Windows vs
 * Unix)
 * 
 * <p>
 * This class is also used to keep track of what the test cases have tried to
 * do. This is useful to guide the search.
 * 
 * 
 * @author arcuri
 * 
 */
public final class VirtualFileSystem {

	private static final Logger logger = LoggerFactory.getLogger(VirtualFileSystem.class);

	/**
	 * The only instance of this class
	 */
	private static final VirtualFileSystem singleton = new VirtualFileSystem();

	/**
	 * The root of the VFS
	 * 
	 * <p>
	 * TODO: we might need to simulate more than one root on same FS
	 */
	private VFolder root;

	/**
	 * An atomic counter for generating unique names for tmp files
	 */
	private final AtomicInteger tmpFileCounter;

	/**
	 * Regular files that are accessed during the search. Tmp files are not
	 * considered, as they are not interesting from a point of view of
	 * generating test data.
	 * 
	 * <p>
	 * Ideally we should only keep track of what the SUT reads, and not the
	 * files it generates. However, for testing purposes, if SUT tries to write
	 * to file X, then it would be still important to consider the case in which
	 * X already exists.
	 */
	private final Set<String> accessedFiles;

	/**
	 * Check if all operations in this VFS should throw IOException
	 */
	private volatile boolean shouldAllThrowIOException;

	/**
	 * The classes in this set are marked to throw IOException
	 */
	private final Set<String> classesThatShouldThrowIOException;

	/**
	 * A set of all IO leaking resources (eg streams) created during the search
	 * 
	 */
	private final Set<LeakingResource> leakingResources;
	
	//--------------------------------------------------------------------------

	/**
	 * Hidden, main constructor
	 */
	private VirtualFileSystem() {
		tmpFileCounter = new AtomicInteger(0);
		accessedFiles = new CopyOnWriteArraySet<>(); //we only add during test execution, and read after
		leakingResources =  new CopyOnWriteArraySet<>();
		classesThatShouldThrowIOException = new CopyOnWriteArraySet<>(); //should only contain very few values
	}

	/**
	 * Get the final instance of this singleton
	 * 
	 * @return
	 */
	public static VirtualFileSystem getInstance() {
		return singleton;
	}

	/**
	 * Reset the internal state of this singleton
	 */
	public void resetSingleton() {
		root = null;
		tmpFileCounter.set(0);
		accessedFiles.clear();
		shouldAllThrowIOException = false;
		classesThatShouldThrowIOException.clear();
		
		synchronized(leakingResources){
			for(LeakingResource resource : leakingResources){
				try{
					resource.release();
				} catch(Exception e){
					logger.warn("Failed to release resource: "+e.getMessage(),e);
				}
			}
			leakingResources.clear();
		}
	}

	/**
	 * Add a leaking resource to this VFS.
	 * This is mainly necessary for stream objects, even if they are virtual
	 * 
	 * @param resource
	 */
	public void addLeakingResource(LeakingResource resource){
		synchronized(leakingResources){
			leakingResources.add(resource);
		}
	}
	
	/**
	 * Some file streams open file connections in their constructors. Even if we
	 * use a VFS, those methods have to be called. Therefore, we create a single
	 * tmp file which will be used to deceive those constructors
	 * 
	 * @return
	 */
	public File getRealTmpFile() {
		return MSecurityManager.getRealTmpFile();
	}

	public void throwSimuledIOExceptionIfNeeded(String path) throws IOException {
		if (isClassSupposedToThrowIOException(path)) {
			throw new IOException("Simulated IOException");
		}
	}

	public boolean isClassSupposedToThrowIOException(String path) {
		if (shouldAllThrowIOException || classesThatShouldThrowIOException.contains(path)) {
			return true;
		}
		return false;
	}

	public boolean setShouldThrowIOException(EvoSuiteFile file) {
		String path = file.getPath();
		if (classesThatShouldThrowIOException.contains(path)) {
			return false;
		}
		classesThatShouldThrowIOException.add(path);
		return true;
	}

	/**
	 * All operations in the entire VFS will throw an IOException if that
	 * appears in their method signature
	 */
	public boolean setShouldAllThrowIOExceptions() {
		if (shouldAllThrowIOException) {
			return false;
		}
		shouldAllThrowIOException = true;
		return true;
	}

	/**
	 * Initialize the virtual file system with the current directory the JVM was
	 * started from
	 */
	public void init() {

		root = new VFolder(null, null);

		String workingDir = getWorkingDirPath();
		createFolder(workingDir);
		createFolder(getTmpFolderPath());

		//important to clear, as above code would modify this field
		accessedFiles.clear();
	}

	public static String getWorkingDirPath(){
		//this should be set in the scaffolding file
		return java.lang.System.getProperty("user.dir");
	}

	public static String getDefaultParent(){
		//TODO this need more checking/validation
		return File.separator;
	}

	private void markAccessedFile(String path) {

		if (path.contains("\"")) {
			//shouldn't really have paths with ". Furthermore, " would mess up the writing of JUnit files
			return;
		}

		accessedFiles.add(path);
	}

	/**
	 * For each file that has been accessed during the search, keep track of it
	 * 
	 * @return a set of file paths
	 */
	public Set<String> getAccessedFiles() {
		return new HashSet<String>(accessedFiles);
	}

	/**
	 * Create a tmp file, and return its absolute path
	 * 
	 * @param prefix
	 * @param suffix
	 * @param directory
	 * @return {@code null} if file could not be created
	 * @throws IOException
	 */
	public String createTempFile(String prefix, String suffix, File directory)
	        throws IllegalArgumentException, IOException {

		if (prefix.length() < 3)
			throw new IllegalArgumentException("Prefix string too short");
		if (suffix == null)
			suffix = ".tmp";

		String folder = null;

		if (directory == null) {
			folder = getTmpFolderPath();
		} else {
			folder = directory.getAbsolutePath();
		}

		int counter = tmpFileCounter.getAndIncrement();
		String fileName = prefix + counter + suffix;
		String path = folder + File.separator + fileName;

		boolean created = createFile(path);
		if (!created) {
			throw new IOException();
		}

		return path;
	}

	private String getTmpFolderPath() {
		return java.lang.System.getProperty("java.io.tmpdir");
	}

	public boolean exists(String rawPath) {
		return findFSObject(rawPath) != null;
	}

	/**
	 * Return the VFS object represented by the given {@code rowPath}.
	 * 
	 * @param rawPath
	 * @return {@code null} if the object does not exist in the VFS
	 */
	public FSObject findFSObject(String rawPath) {
		String path = new File(rawPath).getAbsolutePath();
		String[] tokens = tokenize(path);

		markAccessedFile(path);

		VFolder parent = root;
		for (int i = 0; i < tokens.length; i++) {
			String name = tokens[i];
			FSObject child = parent.getChild(name);

			//child might not exist
			if (child == null || child.isDeleted()) {
				return null;
			}

			//we might end up with a file on the path that is not a folder
			if (i < (tokens.length - 1) && !child.isFolder()) {
				return null;
			} else {
				if (i == (tokens.length - 1)) {
					return child;
				}
			}

			parent = (VFolder) child;
		}

		return parent;
	}

	public boolean deleteFSObject(String rawPath) {
		FSObject obj = findFSObject(rawPath);
		if (obj == null || !obj.isWritePermission()) {
			return false;
		}
		return obj.delete();
	}

	public boolean createFile(String rawPath) {
		return createFile(rawPath, false);
	}

	private boolean createFile(String rawPath, boolean tmp) {
		String parent = new File(rawPath).getParent();
		boolean created = createFolder(parent);
		if (!created) {
			return false;
		}

		VFolder folder = (VFolder) findFSObject(parent);
		VFile file = new VFile(rawPath, folder);
		folder.addChild(file);

		if (!tmp) {
			markAccessedFile(file.getPath());
		}

		return true;
	}

	public boolean rename(String source, String destination) {

		String parentSource = new File(source).getParent();
		String parentDest = new File(destination).getParent();

		if ((parentSource == null && parentDest != null)
		        || (!parentSource.equals(parentDest))) {
			//both need to be in the same folder
			return false;
		}

		FSObject src = findFSObject(source);
		if (src == null) {
			//source should exist
			return false;
		}

		FSObject dest = findFSObject(destination);
		if (dest != null) {
			//destination should not exist
			return false;
		}

		return src.rename(destination);
	}

	public boolean createFolder(String rawPath) {
		String[] tokens = tokenize(new File(rawPath).getAbsolutePath());

		VFolder parent = root;
		for (String name : tokens) {

			if (!parent.isReadPermission() || !parent.isWritePermission()
			        || parent.isDeleted()) {
				return false;
			}

			VFolder folder = null;

			if (!parent.hasChild(name)) {
				String path = null;
				if(isUnixStyle() || !parent.isRoot()){
					path = parent.getPath() + File.separator + name;
				} else {
					//this means we are in Windows, and that we are considering a direct child of the root
					path = name;
				}
				folder = new VFolder(path, parent);
			} else {
				FSObject child = parent.getChild(name);
				if (!child.isFolder()) {
					return false;
				}
				folder = (VFolder) child;
			}

			parent.addChild(folder);
			parent = folder;
		}

		markAccessedFile(parent.getPath());

		return true;
	}

	private String[] tokenize(String path){
		return tokenize(path,File.separatorChar);
	}
	
	protected static String[] tokenize(String path, char separator) {
		String[] tokens = path.split(separator == '\\' ? "\\\\" : File.separator);
		List<String> list = new ArrayList<String>(tokens.length);
		for (String token : tokens) {
			if (!token.isEmpty()) {
				list.add(token);
			}
		}
		return list.toArray(new String[0]);
	}

	private boolean isUnixStyle() {
		return File.separator.equals("/");
	}
}
