package org.kanonizo.display;

import org.kanonizo.framework.objects.TestSuite;

public interface Display {
  int RESPONSE_YES=0;
  int RESPONSE_NO=1;
  int RESPONSE_INVALID=-1;
  void initialise();
  void fireTestSuiteChange(TestSuite ts);
  void reportProgress(double current, double max);
  int ask(String question);
  void notifyTaskStart(String name, boolean progress);
}
