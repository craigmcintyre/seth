// Copyright (c) 2017 Boray Data Co. Ltd.  All rights reserved.

package com.rapidsdata.seth.contexts;

import com.rapidsdata.seth.PathRelativity;
import com.rapidsdata.seth.TestResult;
import com.rapidsdata.seth.exceptions.FailureException;
import com.rapidsdata.seth.logging.TestLogger;

import java.io.File;
import java.sql.Driver;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class TestContextImpl implements TestContext
{
  /** The application context that this test context uses. */
  private final AppContext appContext;

  /** The test file being executed. */
  private final File testFile;

  /** The object that will hold the result of the test. */
  private final TestResult testResult;

  /** A flag that indicates to test threads whether to keep running or to exit the test early. */
  private volatile boolean continueTesting = true;

  /** A lock used for the cleanupPhase. */
  private final Lock lock = new ReentrantLock();

  /** A condition to wait on for cleanup to start. */
  private final Condition cleanupPhase = lock.newCondition();

  /**
   * Constructor.
   * @param appContext The application context.
   */
  public TestContextImpl(AppContext appContext, File testFile, TestResult testResult)
  {
    this.appContext = appContext;
    this.testFile = testFile;
    this.testResult = testResult;
  }


  /**
   * Returns the time since the application started, in nanoseconds since the epoch.
   * @return the time since the application started, in nanoseconds since the epoch.
   */
  @Override
  public long getAppStartTime()
  {
    return appContext.getAppStartTime();
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
   * Returns the url that the JDBC driver should use to communicate to the system being tested.
   * @return the url that the JDBC driver should use to communicate to the system being tested.
   */
  @Override
  public String getUrl()
  {
    return appContext.getUrl();
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
   * Returns the test file currently being executed.
   * @return the test file currently being executed.
   */
  @Override
  public File getTestFile()
  {
    return testFile;
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
   * Blocks the current thread until testing operations have completed (either successfully
   * or in failure) and threads should now start executing cleanup operations.
   */
  @Override
  public void waitForCleanup()
  {
    lock.lock();

    try {
      while (continueTesting) {
        cleanupPhase.awaitUninterruptibly();
      }

    } finally {
      lock.unlock();
    }
  }

  /**
   * Marks the test result as being aborted and causes all the test threads to stop executing and
   * start cleaning up.
   */
  @Override
  public void abortTest()
  {
    lock.lock();

    try {
      if (continueTesting) {
        testResult.setAbort();
        signalEndOfTesting();
      }
    } finally {
      lock.unlock();
    }
  }

  /**
   * Marks the test result that the test has started executing.
   */
  @Override
  public void markAsStarted()
  {
    testResult.setStarted();
  }

  /**
   * Marks the test result as successful and causes the test threads to begin cleaning up.
   */
  @Override
  public void markAsSucceeded()
  {
    lock.lock();

    try {
      if (continueTesting) {
        testResult.setSuccess();
        signalEndOfTesting();
      }
    } finally {
      lock.unlock();
    }
  }

  /**
   * Marks the test result as a failure and causes the test threads to begin cleaning up.
   * @param failure
   */
  @Override
  public void markAsFailed(FailureException failure)
  {
    lock.lock();

    try {
      if (continueTesting) {
        testResult.setFailure(failure);
        signalEndOfTesting();
      }
    } finally {
      lock.unlock();
    }
  }

  /**
   * Tells any waiting threads on the cleanupPhase condition variable that the testing phase
   * has finished and the cleanup phase has begun. It releases any blocked, waiting threads.
   */
  private void signalEndOfTesting()
  {
    lock.lock();

    try {
      continueTesting = false;
      cleanupPhase.signalAll();

    } finally {
      lock.unlock();
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
