// Copyright (c) 2023 Boray Data Co. Ltd.  All rights reserved.

package com.rapidsdata.seth.parser;

import com.rapidsdata.seth.contexts.TestContext;
import com.rapidsdata.seth.exceptions.FailureException;
import com.rapidsdata.seth.exceptions.SethBrownBagException;
import com.rapidsdata.seth.exceptions.TestSetupException;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SethVariables
{
  private final TestContext testContext;
  private final Map<String,String> variableMap;

  private static final Pattern pattern = Pattern.compile("\\$\\{\\w+\\}");

  private static class MatchedVar
  {
    public int startIdx;
    public int endIdx;
    public String varName;

    public MatchedVar(int startIdx, int endIdx, String varName) {
      this.startIdx = startIdx;
      this.endIdx = endIdx;
      this.varName = varName;
    }
  }

  private static final String VAR_TESTNAME = "testName";
  private static final String VAR_TESTNAME1 = "testName1";

  public SethVariables(TestContext xContext, File testFile, List<File> callStack)
  {
    this.testContext = xContext;
    this.variableMap = xContext.getVariables();

    initTestNameVars(testFile, callStack);
  }

  /**
   * Initialise the variables starting with ${testName}, if applicable
   * @param testFile
   * @param callStack
   */
  private void initTestNameVars(File testFile, List<File> callStack)
  {
    if (variableMap.containsKey(VAR_TESTNAME)) {
      return;
    }

    File rootTestFile = callStack.isEmpty() ? testFile : callStack.get(0);
    rootTestFile = rootTestFile.getAbsoluteFile();

    String testName = rootTestFile.getName();
    int extensionIndex = testName.lastIndexOf(".");
    if (extensionIndex != -1) {
      testName = testName.substring(0, extensionIndex);
    }

    // Create ${testName}
    variableMap.put(VAR_TESTNAME, testName);

    // ${testName1} is the same as ${testName}
    variableMap.put(VAR_TESTNAME1, testName);

    // Create ${testName2}, ${testName3}, ..., ${testName9}
    // which also contain the name of the parent folder(s).
    // e.g. testName2 contains two parts - the name of the test
    // and the name of the parent folder. testName3 also contains
    // the name of the grandparent folder, if it exists.
    File parent = rootTestFile.getParentFile();
    String lastVal = testName;
    int i = 1;

    while (++i < 10) {
      String key = String.format("%s%d", VAR_TESTNAME, i);

      if (parent == null) {
        variableMap.put(key, lastVal);
        continue;
      }

      String val = parent.getName() + "_" + lastVal;
      variableMap.put(key, val);

      lastVal = val;
      parent = parent.getParentFile();
    }
  }

  /**
   * Takes the string value of a token and replaces any variables in it
   * with the variable's value instead. Handles multiple variables in a
   * single token. Variable names are case insensitive.
   * @param tokenStr the contents of the token.
   * @param currentLineNo the line number of the current token
   * @return the updated string, or null if the string does not contain any variables.
   * @throws SethBrownBagException wrapped around a TestSetupException if a variable lookup error occurs.
   */
  public String evaluateToken(String tokenStr, int currentLineNo)
  {
    Matcher matcher = pattern.matcher(tokenStr);
    List<MatchedVar> matchedVars = new ArrayList<>();

    while (matcher.find()) {
      matchedVars.add(new MatchedVar(matcher.start(), matcher.end(), matcher.group()));
    }

    if (matchedVars.isEmpty()) {
      return null;
    }

    StringBuilder sb = new StringBuilder(tokenStr);

    // Replace the variables in reverse order so that the index values
    // we found them at remain correct.
    ListIterator<MatchedVar> iter = matchedVars.listIterator(matchedVars.size());
    while (iter.hasPrevious()) {
      MatchedVar matchedVar = iter.previous();
      String varName = matchedVar.varName.substring(2, matchedVar.varName.length() - 1); // Strip the outer ${} part
      String varValue = variableMap.get(varName);

      if (varValue == null) {
        String errMsg = String.format("Variable not set: ${%s}", varName);
        FailureException e = new TestSetupException(errMsg, testContext.getTestFile(), currentLineNo);
        throw new SethBrownBagException(e);
        // TODO: Could handle this as a warning, or simply replace with an empty string.
//        varValue = "";
      }

      sb.replace(matchedVar.startIdx,
                 matchedVar.endIdx,
                 varValue);
    }

    return sb.toString();
  }



}
