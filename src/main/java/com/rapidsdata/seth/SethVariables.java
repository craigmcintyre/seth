// Copyright (c) 2023 Boray Data Co. Ltd.  All rights reserved.

package com.rapidsdata.seth;

import com.rapidsdata.seth.exceptions.FailureException;
import com.rapidsdata.seth.exceptions.TestSetupException;

import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SethVariables
{
  private final Map<String,String> variableMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

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

  /**
   * Constructor
   * @param testFile
   */
  public SethVariables(File testFile)
  {
    initTestNameVars(testFile);
  }

  /**
   * Initialise the variables starting with ${testName}, if applicable
   * @param testFile
   */
  private void initTestNameVars(File testFile)
  {
    File rootTestFile = testFile.getAbsoluteFile();

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
   * @return the updated string, or the original string if it did not contain any variable references.
   * @throws FailureException if a variable lookup error occurs.
   */
  public String evaluateVarRefs(String tokenStr, Options.BadVarRefHandler errorHandler,
                                File currentFile, int currentLineNo) throws FailureException
  {
    Matcher matcher = pattern.matcher(tokenStr);
    List<MatchedVar> matchedVars = new ArrayList<>();

    while (matcher.find()) {
      matchedVars.add(new MatchedVar(matcher.start(), matcher.end(), matcher.group()));
    }

    if (matchedVars.isEmpty()) {
      return tokenStr;
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

        switch (errorHandler) {

          case ERROR:
            String errMsg = String.format("Variable not set: ${%s}", varName);
            throw new TestSetupException(errMsg, currentFile, currentLineNo);

          case EMPTY:       // Use an empty string instead
            varValue = "";
            break;

          case NO_EVAL:     // Don't evaluate. Leave the reference as it is.
            continue;
        }
      }

      sb.replace(matchedVar.startIdx,
                 matchedVar.endIdx,
                 varValue);
    }

    return sb.toString();
  }

  /**
   * Adds new variables to the map.
   * @param newVars the set of variables and values to be added.
   */
  public void putAll(Map<String,String> newVars)
  {
    this.variableMap.putAll(newVars);
  }

  /**
   * Removes a variable from the map and returns its value, or null if it
   * wasn't in the collection.
   * @param varName the name of the variable to be removed, without the "${}" adornments.
   * @return the value of the variable, or null if a variable of that name didn't exist.
   */
  public String remove(String varName)
  {
    return this.variableMap.remove(varName);
  }

}
