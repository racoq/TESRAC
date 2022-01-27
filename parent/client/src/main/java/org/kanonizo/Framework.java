package org.kanonizo;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.Expose;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.scythe.instrumenter.InstrumentationProperties.Parameter;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.JavaClass;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.runner.Description;
import org.junit.runners.Parameterized.Parameters;
import org.kanonizo.algorithms.MutationSearchAlgorithm;
import org.kanonizo.algorithms.SearchAlgorithm;
import org.kanonizo.algorithms.metaheuristics.fitness.APBCFunction;
import org.kanonizo.algorithms.metaheuristics.fitness.APFDFunction;
import org.kanonizo.algorithms.metaheuristics.fitness.APLCFunction;
import org.kanonizo.algorithms.metaheuristics.fitness.FitnessFunction;
import org.kanonizo.annotations.Algorithm;
import org.kanonizo.annotations.Prerequisite;
import org.kanonizo.display.Display;
import org.kanonizo.display.NullDisplay;
import org.kanonizo.framework.TestCaseStore;
import org.kanonizo.framework.instrumentation.Instrumenter;
import org.kanonizo.framework.objects.ClassUnderTest;
import org.kanonizo.framework.objects.ParameterisedTestCase;
import org.kanonizo.framework.objects.SystemUnderTest;
import org.kanonizo.framework.objects.TestCase;
import org.kanonizo.instrumenters.NullInstrumenter;
import org.kanonizo.junit.TestingUtils;
import org.kanonizo.listeners.TestCaseSelectionListener;
import org.kanonizo.reporting.CsvWriter;
import org.kanonizo.reporting.MiscStatsWriter;
import org.kanonizo.reporting.TestCaseOrderingWriter;
import org.kanonizo.util.Util;
import org.reflections.Reflections;

public class Framework implements Serializable {

  @Parameter(key = "forbidden_classnames", description = "Some projects have classes that deliberately should not load (e.g. Apache Commons Lang has some enum classes that are designed to fail to load). Use this parameter to prevent these classes from loading", category = "Framework")
  public static String FORBIDDEN_CLASSNAMES = "";
  @Parameter(key = "use_suite_methods", description = "Many JUnit3 style test cases come with a \"suite\" method, that determines which test classes/test cases should be run. This flag tells Kanonizo to use the suite methods to find test cases instead of using reflection", category = "Framework")
  public static boolean USE_SUITE_METHODS = false;

  private List<TestCaseSelectionListener> listeners = new ArrayList<>();

  @Expose
  private File sourceFolder;
  @Expose
  private File testFolder;
  @Expose
  private List<File> libraries = new ArrayList<>();
  private List<CsvWriter> writers = new ArrayList<>();
  private static final Logger logger = LogManager.getLogger(Framework.class);
  private SystemUnderTest sut;
  @Expose
  private SearchAlgorithm algorithm;
  @Expose
  private Instrumenter inst = new NullInstrumenter();
  private static Framework instance;
  private Display display = new NullDisplay();
  @Expose
  private File rootFolder;
  private PropertyChangeSupport changeSupport = new PropertyChangeSupport(this);

  public static final String SOURCE_FOLDER_PROPERTY_NAME = "sourceFolder";
  public static final String TEST_FOLDER_PROPERTY_NAME = "testFolder";
  public static final String ROOT_FOLDER_PROPERTY_NAME = "rootFolder";
  public static final String INSTRUMENTER_PROPERTY_NAME = "instrumenter";
  public static final String ALGORITHM_PROPERTY_NAME = "algorithm";
  public static final String LIBS_PROPERTY_NAME = "libraries";

  private Framework() {
    addPropertyChangeListener(ROOT_FOLDER_PROPERTY_NAME, (e) -> {
      if (MavenAnalyser.isMavenProject((File) e.getNewValue())) {
        int response = display
            .ask("Maven project detected - would you like to import dependencies from maven?");
        if (response == Display.RESPONSE_YES) {
          MavenAnalyser.addMavenDependencies((File) e.getNewValue());
        }
      }
    });
    addPropertyChangeListener((e) -> {
      if (e.getPropertyName().equals(SOURCE_FOLDER_PROPERTY_NAME) || e.getPropertyName()
          .equals(TEST_FOLDER_PROPERTY_NAME)) {
        Util.removeFromClassPath((File) e.getOldValue());
        Util.addToClassPath((File) e.getNewValue());
      }
    });
  }

  public static Framework getInstance() {
    if (instance == null) {
      instance = new Framework();
      Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
        Util.resumeOutput();
        e.printStackTrace();
        Util.suppressOutput();
      });
    }
    return instance;
  }

  public void setAlgorithm(SearchAlgorithm algorithm) {
    changeSupport.firePropertyChange(ALGORITHM_PROPERTY_NAME, this.algorithm, algorithm);
    this.algorithm = algorithm;
  }

  public SearchAlgorithm getAlgorithm() {
    return algorithm;
  }

  public Instrumenter getInstrumenter() {
    return inst;
  }

  public void setInstrumenter(Instrumenter inst) {
    changeSupport.firePropertyChange(INSTRUMENTER_PROPERTY_NAME, this.inst, inst);
    this.inst = inst;
  }

  public void addWriter(CsvWriter writer) {
    writers.add(writer);
  }

  public void setSourceFolder(File sourceFolder) {
    changeSupport.firePropertyChange(SOURCE_FOLDER_PROPERTY_NAME, this.sourceFolder, sourceFolder);
    this.sourceFolder = sourceFolder;
  }

  public void setTestFolder(File testFolder) {
    changeSupport.firePropertyChange(TEST_FOLDER_PROPERTY_NAME, this.testFolder, testFolder);
    this.testFolder = testFolder;
  }

  public void setRootFolder(File rootFolder) {
    changeSupport.firePropertyChange(ROOT_FOLDER_PROPERTY_NAME, this.rootFolder, rootFolder);
    this.rootFolder = rootFolder;
  }

  public File getRootFolder() {
    if (rootFolder == null) {
      rootFolder = new File(System.getProperty("user.home"));
    }
    return rootFolder;
  }

  public List<File> getLibraries() {
    return libraries;
  }

  /**
   * Add a library folder containing jar files to the framework. This is particularly important for
   * java applications that aren't built on Maven. Maven applications will automatically add their
   * dependencies through using maven as a system tool
   *
   * @param lib - a library folder containing JAR files that are required for the test cases to
   *            execute properly
   */
  public void addLibrary(File lib) {
    if (lib.isDirectory()) {
      Arrays.asList(lib.listFiles(file -> file.getName().endsWith(".jar")))
          .forEach(jar -> {
            changeSupport.firePropertyChange(LIBS_PROPERTY_NAME, libraries, jar);
            libraries.add(jar);
            Util.addToClassPath(jar);
          });
    } else if (lib.getName().endsWith(".jar")) {
      Util.addToClassPath(lib);
      changeSupport.firePropertyChange(LIBS_PROPERTY_NAME, libraries, lib);
      libraries.add(lib);
    }

  }


  protected List<File> findClasses(File folder) throws ClassNotFoundException {
    List<File> classes = new ArrayList<>();
    if (folder.isDirectory()) {
      File[] files = folder
          .listFiles(file -> (file.getName().endsWith(".class") && !file.getName().contains("CLR4")) || file.isDirectory());
      for (File file : files) {
        if (file.isDirectory()) {
          classes.addAll(findClasses(file));
        } else {
          classes.add(file);
        }
      }
    } else if (folder.isFile()) {
      classes.add(folder);
    }
    /* the default behaviour of listFiles will return files as ordered according to File#compareTo,
     * which in turn delegates to String#compareTo. This returns internal classes before the class
     * that defines them, because (int) '.' < (int) '$' - to solve this we sort the files using our
     * own comparator */
    classes.sort((o1, o2) -> compareFileNames(o1.getPath(), o2.getPath()));
    return classes;
  }

  /**
   * method is loosely based on String#compareto, but with the addition of stripping the filenames
   * to ensure classes with $ in the name (i.e. internal classes) are placed *after* te classes that
   * define them
   */
  private static int compareFileNames(String fileName1, String fileName2) {
    // strip the file extension to ensure we are only comparing the actual file name characters
    int length1 = fileName1.length() - ".class".length();
    int length2 = fileName2.length() - ".class".length();
    int minLength = Math.min(length1, length2);
    char[] chars1 = new char[length1];
    char[] chars2 = new char[length2];
    // grab chars from filename
    fileName1.getChars(0, length1, chars1, 0);
    fileName2.getChars(0, length2, chars2, 0);
    for (int i = 0; i < minLength; i++) {
      char one = chars1[i];
      char two = chars2[i];
      // alphabetically orders classes
      if (one != two) {
        return one - two;
      }
    }
    // the *shorter* fileName should be placed first
    return length1 - length2;
  }


  public void loadClasses() throws ClassNotFoundException {
    sut = new SystemUnderTest();
    List<File> sourceFiles = findClasses(sourceFolder);
    Premain.instrument = true;
    for (File file : sourceFiles) {
      Class<?> cl = loadClassFromFile(file);
      sut.addClass(new ClassUnderTest(cl));
      logger.info("Added class " + cl.getName());
    }
    Premain.instrument = false;
    List<File> testFiles = findClasses(testFolder);
    List<TestSuite> testSuites = new ArrayList<>();
    for (File file : testFiles) {
      Class<?> cl = loadClassFromFile(file);
      if (cl != null) {
        if (Util.isTestClass(cl)) {
          TestSuite ts = TestingUtils.getTestSuite(cl);
          if (ts != null) {
            if (TestingUtils.isSuiteContainer(ts)) {
              logger.info("Found test suite in class " + cl.getSimpleName());
              testSuites.add(ts);
            }
          }
          List<Method> testMethods = TestingUtils.getTestMethods(cl);
          logger.info("Adding " + testMethods.size() + " test methods from " + cl.getName());
          for (Method m : testMethods) {
            if (TestingUtils.isParameterizedTest(cl, m)) {
              Optional<Method> parameterMethod = Arrays.asList(cl.getMethods()).stream()
                  .filter(method -> method.getAnnotation(Parameters.class) != null).findFirst();
              if (parameterMethod.isPresent()) {
                try {
                	Object obj = parameterMethod.get().invoke(null, new Object[] {});
                	if(obj instanceof Object[][]) {
                		Object[][] parameters = (Object[][]) obj;
                		for (Object[] inst : parameters) {
                            ParameterisedTestCase ptc = new ParameterisedTestCase(cl, m, inst);
                            sut.addTestCase(ptc);
                          }
                	} else {
                		Iterable<Object[]> parameters = (Iterable<Object[]>) obj;
                		for (Object[] inst : parameters) {
                            ParameterisedTestCase ptc = new ParameterisedTestCase(cl, m, inst);
                            sut.addTestCase(ptc);
                          }
                	}
                 
                } catch (IllegalAccessException e) {
                  logger.error(e);
                } catch (InvocationTargetException e) {
                  logger.error(e);
                }
              } else {
                logger
                    .error(
                        "Trying to create parameterized test case that has no parameter method");
              }
            } else {
              TestCase t = new TestCase(cl, m);
              sut.addTestCase(t);
            }
          }
        } else {
          sut.addExtraClass(cl);
          logger.info("Adding supporting test class " + cl.getName());
        }
      }
    }
    if(testSuites.size() > 0){
      sut.getTestSuite().clear();
      for(TestSuite ts : testSuites){
        List<TestCase> testCases = collectTestCases(ts);
        for(TestCase tc : testCases){
          if(!sut.getTestSuite().contains(tc)) {
            sut.getTestSuite().addTestCase(tc);
          }
        }
      }
    }
    ClassUnderTest.resetCount();
    logger.info("Finished adding source and test files. Total " + sut.getClassesUnderTest().size() + " classes and " + sut.getTestSuite().size() + " test cases");
  }

  private List<TestCase> collectTestCases(TestSuite suite) {
    List<TestCase> retTests = new ArrayList<>();
    Enumeration<Test> tests = suite.tests();
    if (TestingUtils.isSuiteContainer(suite)) {
      while (tests.hasMoreElements()) {
        junit.framework.Test next = tests.nextElement();
        retTests.addAll(collectTestCases((TestSuite) next));
      }
    } else {
      logger.info("Adding " + suite.testCount() + " test cases from " + suite.getName());
      while (tests.hasMoreElements()) {
        junit.framework.Test next = tests.nextElement();
        if (next instanceof junit.framework.TestCase) {
          junit.framework.TestCase nextCase = (junit.framework.TestCase) next;
          Class<? extends Test> testClass = next.getClass();
          try {
            Method m = testClass.getMethod(nextCase.getName());
            String desc = Description.createTestDescription(testClass, m.getName()).toString();
            TestCase tc = TestCaseStore.with(desc);
            // some test cases never get added to the test case store because kanonizo doesn't recognise them
            // as test cases. To prevent NPEs later on we only at test cases that are registered in the store
            if(tc != null){
              retTests.add(tc);
            }
          } catch (NoSuchMethodException e) {

          }

        }
      }
    }
    return retTests;
  }

  private Class<?> loadClassFromFile(File file) {
    Class<?> cl = null;
    try {
      ClassParser parser = new ClassParser(file.getAbsolutePath());
      
      JavaClass jcl = parser.parse();  
      List<String> forbiddenClasses = Arrays.asList(FORBIDDEN_CLASSNAMES.split(","));
      forbiddenClasses = forbiddenClasses.stream().filter(name -> !name.isEmpty())
          .collect(Collectors.toList());
      if (forbiddenClasses.size() > 0 && forbiddenClasses.stream().anyMatch(
          f -> jcl.getClassName().substring(jcl.getPackageName().length() + 1).startsWith(f))) {
        logger.info("Ignoring class " + jcl.getClassName() + " because it is forbidden");
        return null;
      } else {
        cl = Class.
        		forName(jcl.getClassName(), false, Thread.currentThread().getContextClassLoader());
      }
    } catch (ClassNotFoundException | NoClassDefFoundError e) {
      logger.error(e);
    } catch (IOException e) {
      logger.error(e);
    } catch (ExceptionInInitializerError e) {
      logger.error(e);
    }
    return cl;
  }

  /**
   * Create the instance of the fitness function that will be used to guide a metaheuristic search.
   * It is assumed that the Fitness Function has been set by the main method and is contained in the
   * {@link Properties#FITNESS_FUNC} object. Default case is an {@link APFDFunction}, and other
   * options include {@link APLCFunction} at present. More will be added soon
   */
  protected void setupFitnessFunction() {
    FitnessFunction<SystemUnderTest> func;
    switch (Properties.COVERAGE_APPROACH) {
      case LINE:
        func = new APLCFunction(sut);
        break;
      case BRANCH:
        func = new APBCFunction(sut);
        break;
      default:
        func = new APLCFunction(sut);
    }
    if (algorithm instanceof MutationSearchAlgorithm) {
      func = ((MutationSearchAlgorithm) algorithm).getFitnessFunction();
    }
    sut.getTestSuite().setFitnessFunction(func);
  }

  /**
   * Method that can decide how to display results once the algorithm has finished executing.
   *
   * @param algorithm - the Search Algorithm that was used to find a solution
   */
  protected void reportResults(SearchAlgorithm algorithm) {
    StringBuilder sb = new StringBuilder();
    if (algorithm != null) {
      sb.append("The solution found by the ");
      String algorithmName = algorithm.getClass().getName();
      algorithmName = algorithmName.substring(algorithmName.lastIndexOf(".") + 1);
      String[] algorithmSplit = algorithmName.split("(?=\\p{Upper})");
      for (String name : algorithmSplit) {
        sb.append(name + " ");
      }
      sb.append("for the problem ");
      sb.append("is:\n");
      sb.append(algorithm.getCurrentOptimal());

    }
    logger.info(sb.toString());
    for (CsvWriter writer : writers) {
      writer.write();
    }
    logger.info("Results complete");
  }

  public void run() throws ClassNotFoundException {
    loadClasses();

    if (Properties.PRIORITISE) {
      algorithm.setSearchProblem(sut);
    }

    // collect (or read) coverage
    Instrumenter inst = getInstrumenter();
    inst.setTestSuite(sut.getTestSuite());
    inst.collectCoverage();

    if (algorithm.needsFitnessFunction()) {
      setupFitnessFunction();
    }

    TestCaseOrderingWriter writer = new TestCaseOrderingWriter(algorithm);
    addWriter(writer);
    //addWriter(new CoverageWriter(sut));
    addWriter(new MiscStatsWriter(algorithm));
    if (Properties.PRIORITISE) {
      algorithm.start();
    }
    reportResults(algorithm);

  }

  public static List<SearchAlgorithm> getAvailableAlgorithms()
      throws InstantiationException, IllegalAccessException {
    Reflections r = Util.getReflections();
    Set<Class<?>> algorithms = r.getTypesAnnotatedWith(Algorithm.class);
    List<SearchAlgorithm> algorithmsInst = algorithms.stream().map(cl -> {
      try {
        return (SearchAlgorithm) cl.newInstance();
      } catch (InstantiationException e) {
      } catch (IllegalAccessException e) {
      }
      throw new RuntimeException("Could not instantiate one of more search algorithms");
    }).collect(Collectors.toList());
    return algorithmsInst;
  }

  public static List<Instrumenter> getAvailableInstrumenters()
      throws InstantiationException, IllegalAccessException {
    Reflections r = Util.getReflections();
    Set<Class<?>> algorithms = r.getTypesAnnotatedWith(org.kanonizo.annotations.Instrumenter.class);
    List<Instrumenter> algorithmsInst = algorithms.stream().map(cl -> {
      try {
        return (Instrumenter) cl.newInstance();
      } catch (InstantiationException e) {
      } catch (IllegalAccessException e) {
      }
      throw new RuntimeException("Could not instantiate one of more instrumenters");
    }).collect(Collectors.toList());
    return algorithmsInst;
  }

  public static List<Method> getPrerequisites(SearchAlgorithm algorithm) {
    List<Method> requirements = Arrays.asList(algorithm.getClass().getMethods()).stream()
        .filter(m -> m.isAnnotationPresent(Prerequisite.class)).collect(Collectors.toList());
    Iterator<Method> it = requirements.iterator();
    while (it.hasNext()) {
      Method requirement = it.next();
      if (Modifier.isStatic(requirement.getModifiers())) {
        if (requirement.getReturnType().equals(Boolean.class) || requirement.getReturnType()
            .equals(boolean.class)) {
          if (!requirement.isAccessible()) {
            requirement.setAccessible(true);
          }
        } else {
          logger.info("Ignoring requirement " + requirement.getName()
              + " because the return type is not boolean");
          it.remove();
        }

      } else {
        logger.info("Ignoring requirement " + requirement.getName() + " because it is not static");
        it.remove();
      }
    }
    return requirements;
  }

  public void write(File out) throws IOException {
    Gson gson = new GsonBuilder()
        .excludeFieldsWithoutExposeAnnotation()
        .registerTypeAdapter(File.class, new FileTypeAdapter())
        .registerTypeAdapter(SearchAlgorithm.class, new AlgorithmAdapter())
        .registerTypeAdapter(Instrumenter.class, new InstrumenterAdapter())
        .create();
    FileOutputStream w = new FileOutputStream(out);
    w.write(gson.toJson(this).getBytes());
    w.flush();
  }

  public Framework read(File in) throws FileNotFoundException {
    Gson gson = new GsonBuilder()
        .registerTypeAdapter(File.class, new FileTypeAdapter())
        .registerTypeAdapter(SearchAlgorithm.class, new AlgorithmAdapter())
        .registerTypeAdapter(Instrumenter.class, new InstrumenterAdapter())
        .excludeFieldsWithoutExposeAnnotation()
        .create();
    Framework fw = gson.fromJson(new FileReader(in), Framework.class);
    return fw;
  }

  public Display getDisplay() {
    return display;
  }

  public void setDisplay(Display display) {
    this.display = display;
  }

  public File getSourceFolder() {
    return sourceFolder;
  }

  public File getTestFolder() {
    return testFolder;
  }

  public void addSelectionListener(TestCaseSelectionListener list) {
    listeners.add(list);
  }

  public void notifyTestCaseSelection(TestCase tc) {
    for (TestCaseSelectionListener list : listeners) {
      list.testCaseSelected(tc);
    }
  }

  public void addPropertyChangeListener(PropertyChangeListener pcl) {
    changeSupport.addPropertyChangeListener(pcl);
  }

  public void addPropertyChangeListener(String propertyName, PropertyChangeListener pcl) {
    changeSupport.addPropertyChangeListener(propertyName, pcl);
  }

  public void removePropertyChangeListener(PropertyChangeListener pcl) {
    changeSupport.removePropertyChangeListener(pcl);
  }

  public void removePropertyChangeListener(String propertyName, PropertyChangeListener pcl) {
    changeSupport.removePropertyChangeListener(propertyName, pcl);
  }

  private class FileTypeAdapter extends TypeAdapter<File> {

    @Override
    public void write(JsonWriter out, File file) throws IOException {
      out.beginObject();
      out.name("path");
      if (file == null) {
        out.value((String) null);
      } else {
        out.value(file.getAbsolutePath());
      }
      out.endObject();
    }

    @Override
    public File read(JsonReader in) throws IOException {
      in.beginObject();
      String name = in.nextName();
      File f = null;
      if (name.equals("path")) {
        String fileName = in.nextString();
        if (fileName == null) {
          f = null;
        } else {
          f = new File(fileName);
        }
      }
      in.endObject();
      return f;
    }
  }

  private class AlgorithmAdapter extends TypeAdapter<SearchAlgorithm> {

    @Override
    public void write(JsonWriter out, SearchAlgorithm searchAlgorithm) throws IOException {
      out.beginObject();
      out.name("name");
      out.value(searchAlgorithm.getClass().getName());
      out.endObject();
    }

    @Override
    public SearchAlgorithm read(JsonReader in) throws IOException {
      in.beginObject();
      String name = in.nextName();
      SearchAlgorithm sa = null;
      if (name.equals("name")) {
        String className = in.nextString();
        try {
          sa = (SearchAlgorithm) Class.forName(className).newInstance();
        } catch (InstantiationException e) {
          e.printStackTrace();
        } catch (IllegalAccessException e) {
          e.printStackTrace();
        } catch (ClassNotFoundException e) {
          e.printStackTrace();
        }
      }
      in.endObject();
      return sa;
    }
  }

  private class InstrumenterAdapter extends TypeAdapter<Instrumenter> {

    @Override
    public void write(JsonWriter out, Instrumenter instrumenter) throws IOException {
      out.beginObject();
      out.name("class");
      out.value(instrumenter.getClass().getName());
      out.endObject();
    }

    @Override
    public Instrumenter read(JsonReader in) throws IOException {
      in.beginObject();
      String cl = in.nextName();
      Instrumenter inst = new NullInstrumenter();
      if (cl.equals("class")) {
        String clName = in.nextString();
        try {
          inst = (Instrumenter) Class.forName(clName).newInstance();
        } catch (IllegalAccessException e) {
          e.printStackTrace();
        } catch (InstantiationException e) {
          e.printStackTrace();
        } catch (ClassNotFoundException e) {
          e.printStackTrace();
        }
      }
      in.endObject();
      return inst;
    }
  }
}
