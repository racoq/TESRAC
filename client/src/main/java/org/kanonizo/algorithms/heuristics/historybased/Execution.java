package org.kanonizo.algorithms.heuristics.historybased;

class Execution {
  public static Execution NULL_EXECUTION = new Execution(-1, true, null);
  private long executionTime;
  private boolean passed;
  private Throwable failureCause;

  public Execution(long executionTime, boolean passed, Throwable failureCause) {
    this.executionTime = executionTime;
    this.passed = passed;
    this.failureCause = failureCause;
  }

  public long getExecutionTime() {
    return executionTime;
  }

  public boolean isPassed() {
    return passed;
  }

  public Throwable getFailureCause() {
    return failureCause;
  }
}