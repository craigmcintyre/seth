// Copyright (c) 2023 Boray Data Co. Ltd.  All rights reserved.

package com.rapidsdata.seth.parser;

import java.util.ArrayList;
import java.util.List;

public class SethVariables
{

  /**
   * Return a new string with all copies of "${testName}" replaced by the name
   * of the current file name. The variable is case insensitive.
   * @param origStr The original string which may contain the test name variable
   * @param replacementValue the value to replace the variable with
   * @return a new string if a variable was found, otherwise a null string.
   */
  public static String replaceVarTestName(String origStr, String replacementValue)
  {
    final String testNameVar = "${testname}";

    String lowerStr = origStr.toLowerCase();
    String newStr   = origStr;
    Boolean foundVar = false;

    List<Integer> indexes = new ArrayList<>();
    int fromIndex = -1;
    int idx;

    while ((idx = lowerStr.indexOf(testNameVar, fromIndex + 1)) != -1) {
      indexes.add(idx);
      fromIndex = idx;
      foundVar = true;
    }

    if (!foundVar) {
      return null;
    }

    StringBuilder sb = new StringBuilder(origStr);

    for (int startIdx : indexes) {
      int endIdx = startIdx + testNameVar.length();
      sb.replace(startIdx, endIdx, replacementValue);
    }

    return sb.toString();
  }

}
