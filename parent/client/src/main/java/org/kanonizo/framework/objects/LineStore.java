package org.kanonizo.framework.objects;

import java.util.HashMap;
import java.util.Map;

public class LineStore {
  private static Map<String, Line> lines = new HashMap<>();

  public static void add(Line l){
    String key = l.getParent().getCUT().getName() + ":" + l.getLineNumber();
    lines.put(key, l);
  }

  public static Line with(ClassUnderTest parent, int lineNumber){
    String key = parent.getCUT().getName() + ":" + lineNumber;
    if(lines.containsKey(key)){
      return lines.get(key);
    } else {
      return new Line(parent, lineNumber);
    }
  }
}
