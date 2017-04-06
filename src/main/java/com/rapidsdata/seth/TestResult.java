// Copyright (c) 2017 Boray Data Co. Ltd.  All rights reserved.

package com.rapidsdata.seth;

import com.rapidsdata.seth.exceptions.FailureException;
import com.rapidsdata.seth.exceptions.SethSystemException;
import com.rapidsdata.seth.exceptions.SyntaxException;
import com.rapidsdata.seth.exceptions.TestSetupException;

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
    ABORTED     (true);

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

  /** The test file being executed. */
  private File testFile;

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
  public TestResult(File testFile)
  {
    this.testFile = testFile;
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
   * Marks the test result as having been a failure due to a syntax error encountered.
   * @param e The SyntaxException encountered.
   */
  public void setFailure(SyntaxException e)
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

  private void setStatus(ResultStatus status)
  {
    if (status == ResultStatus.NOT_STARTED) {
      final String msg = "Cannot set ResultStatus to NOT_STARTED.";
      throw new SethSystemException(msg);
    }

    if (status.hasFinished()) {
      final String msg = "Cannot set a ResultStatus to " + status.name() +
                         " when the test already has a completed test status (" +
                         this.status.name() + ").";
      throw new SethSystemException(msg);
    }

    this.status = status;

    if (status == ResultStatus.IN_PROGRESS) {
      // Test is starting, so record the start time.
      this.startTimeNs = System.nanoTime();

    } else {
      // The test has finished, so lets record the execution time.
      this.executionTimeNs = System.nanoTime() - startTimeNs;
    }
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
    return getFailureDescription(true, true, true);
  }

  /**
   * Returns a description of the failure, or an empty string if there is none.
   * @param showFile if true then it will print the path of the test file being executed.
   * @param showLine if true then it will print the line number of the command being executed.
   * @param showCommand if true then it will print the command being executed.
   * @return a description of the failure, or an empty string if there is none.
   */
  public String getFailureDescription(boolean showFile, boolean showLine, boolean showCommand)
  {
    if (status == ResultStatus.ABORTED) {
      return "TEST ABORTED: " + testFile.getPath();
    }

    if (status != ResultStatus.FAILED) {
      return "";
    }

    if (failureException == null) {
      throw new SethSystemException("failureException cannot be null.");
    }

    return failureException.getMessage(showFile, showLine, showCommand);
  }
}
