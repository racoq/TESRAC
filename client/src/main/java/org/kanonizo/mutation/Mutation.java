package org.kanonizo.mutation;

import com.scythe.instrumenter.InstrumentationProperties.Parameter;
import com.scythe.instrumenter.analysis.ClassAnalyzer;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.kanonizo.framework.objects.TestCase;
import org.kanonizo.framework.objects.TestSuite;
import org.kanonizo.util.Util;

public class Mutation {
  private static List<Mutant> mutants = new ArrayList<Mutant>();
  private static Map<TestCase, List<Mutant>> killMap = new HashMap<TestCase, List<Mutant>>();
  @Parameter(key = "kill_map", description = "This points to a file containing the kill map information generated by performing mutation analysis on the project. This is used to calculate the FEP score for each test case", category = "TCP")
  public static String KILL_MAP = "";

  @Parameter(key = "mutant_log", description = "This points to a file containing the mutation log information generated by running major on some source code. This helps to identify all mutants generated by major", category = "TCP")
  public static String MUTANT_LOG = "";
  private Mutation() {
  }

  public static void initialise(TestSuite tsc) {
    File mutantLog = Util.getFile(MUTANT_LOG);
    File killMap = Util.getFile(KILL_MAP);
    parseMutantLog(mutantLog);
    parseKillMap(killMap, tsc);
  }

  public static List<Mutant> getMutants() {
    return mutants;
  }

  private static void parseMutantLog(File mutantLog) {
    Scanner s = null;
    try {
      s = new Scanner(mutantLog);
      while (s.hasNextLine()) {
        String mutant = s.nextLine();
        String[] mut = mutant.split(":");
        int mutantId = Integer.parseInt(mut[0]);
        String target = mut[4];
        if (target.contains("@")) {
          target = target.substring(0, target.indexOf("@"));
        }
        Class<?> targetClass = Class.forName(target);
        int lineNumber = Integer.parseInt(mut[5]);
        ClassAnalyzer.out.println("Mutant added with id: " + mutantId + ", target class: " + targetClass.getSimpleName()
            + ", lineNumber: " + lineNumber);
        mutants.add(new Mutant(mutantId, targetClass, lineNumber));
      }
    } catch (FileNotFoundException e) {
      // something must have gone really wrong here....
      e.printStackTrace(ClassAnalyzer.out);
    } catch (ClassNotFoundException e) {
      // probably a parsing error
      e.printStackTrace(ClassAnalyzer.out);
    } finally {
      if (s != null) {
        s.close();
      }
    }
  }

  private static void parseKillMap(File kill, TestSuite testSuite) {
    CSVParser parser = null;
    try {
      parser = new CSVParser(new FileReader(kill), CSVFormat.DEFAULT);
      for (CSVRecord record : parser.getRecords()) {
        if (record.getRecordNumber() == 0) {
          continue;
        }
        int testCase = Integer.parseInt(record.get(0));
        int mutantKilled = Integer.parseInt(record.get(1));
        TestCase test = testSuite.getTestCases().get(testCase - 1);
        if (!killMap.containsKey(test)) {
          killMap.put(test, new ArrayList<Mutant>());
        }
        killMap.get(test).addAll(getMutants(mutant -> mutant.getMutantId() == mutantKilled));
      }
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      try {
        parser.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  public static Map<TestCase, List<Mutant>> getKillMap() {
    return killMap;
  }

  public static List<Mutant> getMutants(Predicate<Mutant> pred) {
    return mutants.stream().filter(pred).collect(Collectors.toList());
  }
}
