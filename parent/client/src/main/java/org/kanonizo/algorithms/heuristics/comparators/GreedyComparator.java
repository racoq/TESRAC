package org.kanonizo.algorithms.heuristics.comparators;

import java.util.Comparator;
import java.util.List;
import org.kanonizo.Framework;
import org.kanonizo.framework.ObjectiveFunction;
import org.kanonizo.framework.Readable;
import org.kanonizo.framework.instrumentation.Instrumenter;
import org.kanonizo.framework.objects.TestCase;

public class GreedyComparator implements ObjectiveFunction, Comparator<TestCase> {

  private Instrumenter inst;
  private Framework fw = Framework.getInstance();

  public GreedyComparator() {
    inst = fw.getInstrumenter();
    fw.addPropertyChangeListener(Framework.INSTRUMENTER_PROPERTY_NAME, (e) -> {
      inst = (Instrumenter) e.getNewValue();
    });
  }

  @Override
  public int compare(TestCase o1, TestCase o2) {
    int fitness1 = inst.getLinesCovered(o1).size();
    int fitness2 = inst.getLinesCovered(o2).size();
    return -Integer.compare(fitness1, fitness2);
  }

  @Override
  public String readableName() {
    return "greedy";
  }

  @Override
  public List<TestCase> sort(List<TestCase> candidates) {
    candidates.sort(this);
    return candidates;
  }
}
