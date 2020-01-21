// Copyright (c) 2017 Boray Data Co. Ltd.  All rights reserved.

package com.rapidsdata.seth;

import com.rapidsdata.seth.exceptions.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.concurrent.atomic.AtomicLong;

public class TestResult
{
  public enum ResultStatus {
    NOT_STARTED (false),
    IN_PROGRESS (false),
    VALIDATED   (true),
    SUCCEEDED   (true),
    FAILED      (true),
    ABORTED     (true),
    SKIPPED     (true);

    private final boolean finished;

    ResultStatus(boolean finished)
    {
      this.finished = finished;
    }

    /**
     * Returns true if the enum value indicates that the test has a result.
     * @return true if the enum value indicates that the test has a result.
     */
    public boolean hasFinished()
    {
      return this.finished;
    }
  }

  private static final String FAILURE_INDENTING = System.lineSeparator() + "  ";

  /** The test file being executed. */
  private File testFile;

  /** The shortened name being given to the test. May simply be the name of the file. */
  private String testName;

  /** The result status of running the test. */
  private ResultStatus status;

  /** The details of any failure, or null if there is none. */
  private FailureException failureException;

  /** Total number of test steps executed for this test. */
  private AtomicLong numStepsExecuted;

  /** The total time it took to run the test, in nanoseconds. */
  private long executionTimeNs;

  /** The system time when the test was started. */
  private long startTimeNs;

  /**
   * Constructor.
   * @param testFile The test file being executed.
   */
  public TestResult(File testFile, String testName)
  {
    this.testFile = testFile;
    this.testName = testName;
    this.status = ResultStatus.NOT_STARTED;
    this.failureException = null;
    this.numStepsExecuted = new AtomicLong(0);
    this.executionTimeNs = 0;
    this.startTimeNs = System.nanoTime();
  }

  /**
   * Returns the test file being executed.
   * @return the test file being executed.
   */
  public File getTestFile()
  {
    return testFile;
  }

  /**
   * Returns the name of the test being executed.
   * @return the name of the test being executed.
   */
  public String getTestName()
  {
    return testName;
  }

  /**
   * Marks the test as having started executing.
   */
  public void setStarted()
  {
    setStatus(ResultStatus.IN_PROGRESS);
  }

  /**
   * Marks the test as having executed successfully.
   */
  public void setSuccess()
  {
    setStatus(ResultStatus.SUCCEEDED);
  }

  /**
   * Marks the test result as having failed, explained in the given FailureException.
   * @param e The reason why the test failed.
   */
  public void setFailure(FailureException e)
  {
    setStatus(ResultStatus.FAILED);
    failureException = e;
  }

  /**
   * Marks the test result as having been a failure due to a test file not being found.
   * @param e The FileNotFoundException encountered.
   */
  public void setFailure(FileNotFoundException e)
  {
    setStatus(ResultStatus.FAILED);
    failureException = new TestSetupException(e, testFile);
  }

  /**
   * Marks the test result as having been a failure due to a planning error encountered.
   * @param e The PlanningException encountered.
   */
  public void setFailure(PlanningException e)
  {
    setStatus(ResultStatus.FAILED);
    failureException = new TestSetupException(e);
  }

  /**
   * Marks the test result as having been aborted by the user.
   */
  public void setAbort()
  {
    setStatus(ResultStatus.ABORTED);
  }

  private void setStatus(ResultStatus newStatus)
  {
    if (newStatus == ResultStatus.NOT_STARTED) {
      final String msg = "Cannot set ResultStatus to NOT_STARTED.";
      throw new SethSystemException(msg);
    }

    if (this.status.hasFinished()) {
      if (newStatus == ResultStatus.ABORTED) {
        return;
      }

      final String msg = "Cannot set a ResultStatus to " + newStatus.name() +
                         " when the test already has a completed test status (" +
                         this.status.name() + ").";
      throw new SethSystemException(msg);
    }

    this.status = newStatus;

    if (newStatus == ResultStatus.IN_PROGRESS) {
      // Test is starting, so record the start time.
      this.startTimeNs = System.nanoTime();

    } else {
      // The test has finished, so lets record the execution time.
      this.executionTimeNs = System.nanoTime() - startTimeNs;
    }
  }

  /**
   * Marks the test as having been skipped on user request.
   */
  public void setSkipped()
  {
    setStatus(ResultStatus.SKIPPED);
  }

  /**
   * Returns the status of executing this test.
   * @return the status of executing this test.
   */
  public ResultStatus getStatus()
  {
    return status;
  }

  /**
   * Returns the failure exception that describes why the test failed.
   * @return the failure exception that describes why the test failed.
   */
  public FailureException getFailureException()
  {
    return failureException;
  }

  /**
   * Increments the number of test steps executed by this test file.
   * @param count the number of test steps to add to the counter.
   */
  public void accumulateSteps(long count)
  {
    numStepsExecuted.addAndGet(count);
  }

  /**
   * Returns the number of test steps executed by this test file.
   * @return the number of test steps executed by this test file.
   */
  public long getSteps()
  {
    return numStepsExecuted.get();
  }

  /**
   * Returns a counter (in nanoseconds) of when the this test was started.
   * @return a counter (in nanoseconds) of when the this test was started.
   */
  public long getStartTimeNs()
  {
    return startTimeNs;
  }

  /**
   * Returns how long the test took to execute, in nanoseconds. A value of 0 indicates it hasn't finished yet.
   * @return Returns how long the test took to execute, in nanoseconds.
   */
  public long getExecutionTimeNs()
  {
    return (executionTimeNs >= 0 ? executionTimeNs : 0);
  }

  /**
   * Returns a description of the failure, or an empty string if there is none.
   * @return a description of the failure, or an empty string if there is none.
   */
  public String getFailureDescription()
  {
    if (failureException != null) {
      return failureException.getMessage(testFile);
    }

    return "";
  }

  @Override
  public String toString()
  {
    String msg = String.format("Test %-11s :  ", status.name().toUpperCase());

    if (status == ResultStatus.FAILED) {
      if (!testFile.equals(failureException.getTestFile())) {
        msg += testFile.getPath();
      }

      if (failureException != null) {
        msg += failureException.getMessage().replace(System.lineSeparator(), FAILURE_INDENTING);
      }

      return msg;
    }

    msg += testFile.getPath();
    return msg;
  }

  /** @returns a TestResult indicating that the given test file was skipped. */
  public static TestResult skipped(File testFile, String testName)
  {
    TestResult result = new TestResult(testFile, testName);
    result.setSkipped();
    return result;
  }
}
