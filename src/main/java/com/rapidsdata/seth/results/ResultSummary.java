// Copyright (c) 2017 Boray Data Co. Ltd.  All rights reserved.

package com.rapidsdata.seth.results;

import com.rapidsdata.seth.TestResult;
import com.rapidsdata.seth.exceptions.SethSystemException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class ResultSummary
{
  private final long numTestsExecuted;
  private final long numTestsValidated;
  private final long numTestsPassed;
  private final long numTestsFailed;
  private final long numTestsAborted;
  private final long numTestsSkipped;
  private final long numStepsExecuted;

  private final List<TestResult> failedTests;
  private final List<TestResult> skippedTests;

  private final Map<String,Integer> ignoredCmdCounts;

  private final long totalIgnoredSteps;

  protected ResultSummary(long testsExecuted,
                          long testsValidated,
                          long testsPassed,
                          long testsFailed,
                          long testsAborted,
                          long numTestsSkipped,
                          long stepsExecuted,
                          List<TestResult> failedTests,
                          List<TestResult> skippedTests,
                          Map<String,Integer> ignoredCmdCounts)
  {
    this.numTestsExecuted  = testsExecuted;
    this.numTestsValidated = testsValidated;
    this.numTestsPassed    = testsPassed;
    this.numTestsFailed    = testsFailed;
    this.numTestsAborted   = testsAborted;
    this.numTestsSkipped   = numTestsSkipped;
    this.numStepsExecuted  = stepsExecuted;
    this.failedTests       = failedTests;
    this.skippedTests      = skippedTests;
    this.ignoredCmdCounts  = ignoredCmdCounts;

    long ignoredSteps = 0;
    for (Map.Entry<String,Integer> ignoredEntry : ignoredCmdCounts.entrySet()) {
      ignoredSteps += ignoredEntry.getValue();
    }

    this.totalIgnoredSteps = ignoredSteps;
  }

  /**
   * Creates a summary from the given list of test results.
   * @param results The list of test results to create a summary from.
   * @return a summary from the given list of test results.
   */
  public static ResultSummary summariseFrom(List<TestResult> results)
  {
    List<TestResult> failedTests = new ArrayList<>(results.size());
    List<TestResult> skippedTests = new ArrayList<>(results.size());
    long numTestsExecuted  = 0;
    long numTestsValidated = 0;
    long numTestsPassed    = 0;
    long numTestsFailed    = 0;
    long numTestsAborted   = 0;
    long numTestsSkipped   = 0;
    long numStepsExecuted  = 0;
    Map<String,Integer> ignoredCmdCounts = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);


    for (TestResult result : results) {
      ++numTestsExecuted;
      numStepsExecuted += result.getSteps();

      aggregateIgnoredCommandCounts(ignoredCmdCounts, result.getIgnoredCounts());

      switch (result.getStatus()) {

        case VALIDATED:
          ++numTestsValidated;
          break;

        case SUCCEEDED:
          ++numTestsPassed;
          break;

        case FAILED:
          ++numTestsFailed;
          failedTests.add(result);
          break;

        case ABORTED:
          ++numTestsAborted;
          break;

        case SKIPPED:
          ++numTestsSkipped;
          skippedTests.add(result);
          break;

        case NOT_STARTED:
        case IN_PROGRESS:
        default:
          // Should not happen.
          throw new SethSystemException("Unexpected ResultStatus: " + result.getStatus().name());
      }


    }

    return new ResultSummary(numTestsExecuted, numTestsValidated, numTestsPassed,
                             numTestsFailed, numTestsAborted, numTestsSkipped,
                             numStepsExecuted, failedTests, skippedTests,
                             ignoredCmdCounts);
  }


  /** Aggregate the counts for each ignored command for each test. */
  private static void aggregateIgnoredCommandCounts(Map<String,Integer> aggIgnoredCmdCounts, Map<String,Integer> testIgnoredCmdCounts)
  {
    for (Map.Entry<String,Integer> testEntry : testIgnoredCmdCounts.entrySet()) {
      String cmd = testEntry.getKey();
      Integer testCount = testEntry.getValue();
      Integer aggCount = aggIgnoredCmdCounts.get(cmd);

      aggIgnoredCmdCounts.put(cmd, aggCount == null ? testCount : aggCount + testCount);
    }
  }

  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder(1024);

    sb.append(System.lineSeparator());
    sb.append(System.lineSeparator());
    sb.append("*************************").append(System.lineSeparator());
    sb.append(" Summary Of Test Results ").append(System.lineSeparator());
    sb.append("*************************").append(System.lineSeparator());
    sb.append(System.lineSeparator());

    sb.append(String.format("Tests Executed : %4d", numTestsExecuted)).append(System.lineSeparator());

    if (numTestsValidated > 0) {
      if (numTestsPassed == 0) {
        // Display validated instead of passed.
        sb.append(String.format("Tests Validated: %4d", numTestsValidated)).append(System.lineSeparator());
      } else {
        // Display both validated and passed
        // Display validated instead of passed.
        sb.append(String.format("Tests Validated: %4d", numTestsValidated)).append(System.lineSeparator());
        sb.append(String.format("Tests Passed   : %4d", numTestsPassed)).append(System.lineSeparator());
      }
    } else {
      // Only show passed and not validated
      sb.append(String.format("Tests Passed   : %4d", numTestsPassed)).append(System.lineSeparator());
    }

    sb.append(String.format("Tests Failed   : %4d", numTestsFailed)).append(System.lineSeparator());
    sb.append(String.format("Tests Skipped  : %4d", numTestsSkipped)).append(System.lineSeparator());

    if (numTestsAborted > 0) {
      sb.append(String.format("Tests Aborted  : %4d", numTestsAborted)).append(System.lineSeparator());
    }

    sb.append(System.lineSeparator());
    sb.append(String.format("Steps Executed : %4d", numStepsExecuted)).append(System.lineSeparator());
    sb.append(String.format("Steps Ignored  : %4d", totalIgnoredSteps)).append(System.lineSeparator());

    sb.append(System.lineSeparator());
    sb.append(System.lineSeparator());

    boolean optWs = false;

    if (numTestsSkipped > 0) {
      sb.append(getSkippedTestDesc());
      optWs = true;
    }

    if (numTestsFailed > 0) {
      if (optWs) {
        sb.append(System.lineSeparator());
        sb.append(System.lineSeparator());
      }

      sb.append(getFailedTestDesc());
      optWs = true;
    }

    if (totalIgnoredSteps > 0) {
      if (optWs) {
        sb.append(System.lineSeparator());
        sb.append(System.lineSeparator());
      }

      sb.append(getIgnoredStepsDesc());
      optWs = true;
    }

    return sb.toString();
  }

  /**
   * Returns a string describing the skipped tests.
   * @return a string describing the skipped tests.
   */
  private String getSkippedTestDesc()
  {
    StringBuilder sb = new StringBuilder(1024);

    sb.append("***********************************").append(System.lineSeparator());
    sb.append(String.format(" Summary Of All Skipped Tests (%d)", skippedTests.size())).append(System.lineSeparator());
    sb.append("***********************************").append(System.lineSeparator());

    for (TestResult result : skippedTests) {
      sb.append("Skipped : ").append(result.getTestableFile().describePath()).append(System.lineSeparator());
    }

    return sb.toString();
  }

  /**
   * Returns a string describing the failed tests.
   * @return a string describing the failed tests.
   */
  private String getFailedTestDesc()
  {
    final String indentation = "  ";
    final String nlAndIndentation = System.lineSeparator() + indentation;

    StringBuilder sb = new StringBuilder(1024);

    sb.append("**********************************").append(System.lineSeparator());
    sb.append(" Summary Of All Test Failures (").append(failedTests.size()).append(")").append(System.lineSeparator());
    sb.append("**********************************");

    for (TestResult result : failedTests) {

      sb.append(System.lineSeparator());
      sb.append(System.lineSeparator());
      sb.append("Test : ").append(result.getTestableFile().describePath()).append(System.lineSeparator());

      String desc = indentation + result.getFailureDescription();
      desc = desc.replace(System.lineSeparator(), nlAndIndentation);
      sb.append(desc);
    }

    return sb.toString();
  }

  /**
   * Returns a string describing the ignored commands and how often they occurred.
   * @return a string describing the ignored commands and how often they occurred.
   */
  private String getIgnoredStepsDesc()
  {
    final String indentation = "  ";
    final String nlAndIndentation = System.lineSeparator() + indentation;

    StringBuilder sb = new StringBuilder(1024);

    sb.append("**********************************").append(System.lineSeparator());
    sb.append(" Summary Of All Ignored Steps (").append(totalIgnoredSteps).append(")").append(System.lineSeparator());
    sb.append("**********************************").append(System.lineSeparator());

    sb.append(System.lineSeparator());

    for (Map.Entry<String,Integer> ignoredCmd : ignoredCmdCounts.entrySet()) {
      String cmd = ignoredCmd.getKey();
      int count = ignoredCmd.getValue();

      sb.append("Count : ");
      sb.append(String.format("%-4d  ", count));
      sb.append("Ignored Step : ").append(cmd).append(System.lineSeparator());
    }

    return sb.toString();
  }

  public long getNumTestsExecuted()
  {
    return numTestsExecuted;
  }

  public long getNumTestsValidated()
  {
    return numTestsValidated;
  }

  public long getNumTestsPassed()
  {
    return numTestsPassed;
  }

  public long getNumTestsFailed()
  {
    return numTestsFailed;
  }

  public long getNumTestsAborted()
  {
    return numTestsAborted;
  }

  public long getNumTestsSkipped()
  {
    return numTestsSkipped;
  }

  public long getNumStepsExecuted()
  {
    return numStepsExecuted;
  }

  public List<TestResult> getFailedTests()
  {
    return failedTests;
  }
}
