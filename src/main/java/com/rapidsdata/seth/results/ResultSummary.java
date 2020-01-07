// Copyright (c) 2017 Boray Data Co. Ltd.  All rights reserved.

package com.rapidsdata.seth.results;

import com.rapidsdata.seth.TestResult;
import com.rapidsdata.seth.exceptions.SethSystemException;

import java.util.ArrayList;
import java.util.List;

public class ResultSummary
{
  private final long numTestsExecuted;
  private final long numTestsValidated;
  private final long numTestsPassed;
  private final long numTestsFailed;
  private final long numTestsAborted;
  private final long numStepsExecuted;

  private final List<TestResult> failedTests;


  protected ResultSummary(long testsExecuted,
                          long testsValidated,
                          long testsPassed,
                          long testsFailed,
                          long testsAborted,
                          long stepsExecuted,
                          List<TestResult> failedTests)
  {
    this.numTestsExecuted  = testsExecuted;
    this.numTestsValidated = testsValidated;
    this.numTestsPassed    = testsPassed;
    this.numTestsFailed    = testsFailed;
    this.numTestsAborted   = testsAborted;
    this.numStepsExecuted  = stepsExecuted;
    this.failedTests       = failedTests;
  }

  /**
   * Creates a summary from the given list of test results.
   * @param results The list of test results to create a summary from.
   * @return a summary from the given list of test results.
   */
  public static ResultSummary summariseFrom(List<TestResult> results)
  {
    List<TestResult> failedTests = new ArrayList<>(results.size());
    long numTestsExecuted  = 0;
    long numTestsValidated = 0;
    long numTestsPassed    = 0;
    long numTestsFailed    = 0;
    long numTestsAborted   = 0;
    long numStepsExecuted  = 0;

    for (TestResult result : results) {
      ++numTestsExecuted;
      numStepsExecuted += result.getSteps();

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

        case NOT_STARTED:
        case IN_PROGRESS:
        default:
          // Should not happen.
          throw new SethSystemException("Unexpected ResultStatus: " + result.getStatus().name());
      }
    }

    return new ResultSummary(numTestsExecuted, numTestsValidated, numTestsPassed, numTestsFailed,
                             numTestsAborted, numStepsExecuted, failedTests);
  }

  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder(1024);

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

    if (numTestsAborted > 0) {
      sb.append(String.format("Tests Aborted  : %4d", numTestsAborted)).append(System.lineSeparator());
    }

    sb.append(System.lineSeparator());
    sb.append(String.format("Steps Executed : %4d", numStepsExecuted)).append(System.lineSeparator());

    sb.append(System.lineSeparator());

    if (numTestsFailed > 0) {
      sb.append(getFailedTestDesc());
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
      sb.append("Test : ").append(result.getTestFile().getPath()).append(System.lineSeparator());

      String desc = indentation + result.getFailureDescription();
      desc = desc.replace(System.lineSeparator(), nlAndIndentation);
      sb.append(desc);
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

  public long getNumStepsExecuted()
  {
    return numStepsExecuted;
  }

  public List<TestResult> getFailedTests()
  {
    return failedTests;
  }
}
