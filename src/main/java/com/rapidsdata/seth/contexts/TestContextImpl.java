// Copyright (c) 2017 Boray Data Co. Ltd.  All rights reserved.

package com.rapidsdata.seth.contexts;

import com.rapidsdata.seth.PathRelativity;
import com.rapidsdata.seth.TestResult;
import com.rapidsdata.seth.exceptions.FailureException;
import com.rapidsdata.seth.exceptions.SethSystemException;
import com.rapidsdata.seth.logging.TestLogger;

import java.io.File;
import java.sql.Driver;
import java.util.List;
import java.util.concurrent.ExecutorService;

public class TestContextImpl implements TestContext
{
  /** The application context that this test context uses. */
  private final AppContext appContext;

  /** The test file being executed. */
  private final String testFilePath;

  /** The object that will hold the result of the test. */
  private final TestResult testResult;

  /** A flag that indicates to test threads whether to keep running or to exit the test early. */
  private volatile boolean continueTesting = true;

  /**
   * Constructor.
   * @param appContext The application context.
   */
  public TestContextImpl(AppContext appContext, String testFilePath, TestResult testResult)
  {
    this.appContext = appContext;
    this.testFilePath = testFilePath;
    this.testResult = testResult;
  }


  /**
   * Returns the list of test files to be executed.
   * @return the list of test files to be executed.
   */
  @Override
  public List<File> getTestFiles()
  {
    return appContext.getTestFiles();
  }

  /**
   * Returns the JDBC driver to use to communicate to the system being tested.
   * @return the JDBC driver to use to communicate to the system being tested.
   */
  @Override
  public Driver getDriver()
  {
    return appContext.getDriver();
  }

  /**
   * If true then the test files will only be validated and not executed.
   * If false then the test files will be executed in full.
   * @return whether to only validate or to fully execute the test files.
   */
  @Override
  public boolean onlyValidateTestFiles()
  {
    return appContext.onlyValidateTestFiles();
  }

  /**
   * Returns how we are to treat relative paths.
   * @return how we are to treat relative paths.
   */
  @Override
  public PathRelativity getPathRelativity()
  {
    return appContext.getPathRelativity();
  }

  /**
   * Returns the logger we are to use to write to the console and to file.
   * @return the logger we are to use to write to the console and to file.
   */
  @Override
  public TestLogger getLogger()
  {
    return appContext.getLogger();
  }

  /**
   * Returns the thread pool executor service, by which new threads can be created.
   * @return the thread pool executor service, by which new threads can be created.
   */
  @Override
  public ExecutorService getThreadPool()
  {
    return appContext.getThreadPool();
  }

  /**
   * Returns the path of the test file currently being executed.
   * @return the path of the test file currently being executed.
   */
  @Override
  public String getTestFilePath()
  {
    return testFilePath;
  }

  /**
   * A notification flag to child threads whether to continue testing or whether the test is ending
   * and they should stop executing.
   * @return true if the test is continuing or false if it is ending.
   */
  @Override
  public boolean continueTesting()
  {
    return continueTesting;
  }

  /**
   * Marks the test result as being aborted and causes all the test threads to stop executing and
   * start cleaning up.
   */
  @Override
  public synchronized void abortTest()
  {
    if (continueTesting) {
      testResult.setStatus(TestResult.ResultStatus.ABORTED);
      continueTesting = false;
    }
  }

  /**
   * Marks the test result as successful and causes the test threads to begin cleaning up.
   */
  @Override
  public synchronized void markAsSucceeded()
  {
    if (continueTesting) {
      testResult.setStatus(TestResult.ResultStatus.SUCCEEDED);
      continueTesting = false;
    }
  }

  /**
   * Marks the test result as a failure and causes the test threads to begin cleaning up.
   * @param failure
   */
  @Override
  public synchronized void markAsFailed(FailureException failure)
  {
    if (continueTesting) {
      testResult.setStatus(TestResult.ResultStatus.FAILED);
      continueTesting = false;
    }
  }

  /**
   * Accumulates a count of test steps that occurred in a given testing thread. Each thread should
   * report the number of test steps is executed when the thread completes.
   * @param count the number of test steps that the testing thread executed.
   */
  @Override
  public void accumulateTestSteps(long count)
  {
    testResult.accumulateSteps(count);
  }
}
