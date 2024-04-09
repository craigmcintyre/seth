// Copyright (c) 2017 Boray Data Co. Ltd.  All rights reserved.

// A fake/temporary TestContext to be used when parsing command line variables.

package com.rapidsdata.seth.contexts;

import com.rapidsdata.seth.*;
import com.rapidsdata.seth.exceptions.FailureException;
import com.rapidsdata.seth.logging.TestLogger;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.regex.Pattern;

public class ParserTestContextImpl implements TestContext
{
  /** The application context that this test context uses. */

  private final Options testOptions = new Options();

  /** A case-insensitive map of variable names and values. */
  private final SethVariables variables;

  /**
   * Constructor.
   */
  public ParserTestContextImpl()
  {
    this.variables = new SethVariables(null);
  }


  /**
   * Returns the time that the application started, in milliseconds since the epoch.
   * @return the time that the application started, in milliseconds since the epoch.
   */
  @Override
  public long getAppStartTime()
  {
    return 0;
  }

  /**
   * Returns the list of test files to be executed.
   * @return the list of test files to be executed.
   */
  @Override
  public List<TestableFile> getTestableFiles()
  {
    return null;
  }

  /**
   * Returns the map of variable names and values.
   * @return the map of variable names and values.
   */
  @Override
  public SethVariables getVariables() {
    return variables;
  }

  /**
   * Returns the url that the JDBC driver should use to communicate to the system being tested.
   * @return the url that the JDBC driver should use to communicate to the system being tested.
   */
  @Override
  public String getUrl()
  {
    return null;
  }

  /**
   * Returns how we are to treat relative paths.
   * @return how we are to treat relative paths.
   */
  @Override
  public PathRelativity getPathRelativity()
  {
    return PathRelativity.REFERER;
  }

  /**
   * Returns the logger we are to use to write to the console and to file.
   * @return the logger we are to use to write to the console and to file.
   */
  @Override
  public TestLogger getLogger()
  {
    return null;
  }

  /**
   * Returns the thread pool executor service, by which new threads can be created.
   * @return the thread pool executor service, by which new threads can be created.
   */
  @Override
  public ExecutorService getThreadPool()
  {
    return null;
  }

  /**
   * Returns the command line arguments used to run the program.
   * @return the command line arguments used to run the program.
   */
  @Override
  public CommandLineArgs getCommandLineArgs()
  {
    return null;
  }

  /**
   * Returns the options that are applied to the whole application.
   * @return the options that are applied to the whole application.
   */
  @Override
  public Options getAppOptions()
  {
    return null;
  }

  /**
   * Returns the variables that are set for the whole application (all tests).
   * @return the variables that are set for the whole application (all tests).
   */
  @Override
  public Map<String, String> getAppVariables() {
    return null;
  }

  /**
   * Returns a list of regex patterns for matching commands that are to be ignored.
   * @return a list of regex patterns for matching commands that are to be ignored.
   */
  @Override
  public List<Pattern> getIgnorableCommands() {
    return null;
  }

  /**
   * Compiles and adds a collection of regex strings as Patterns representing
   * commands that are to be ignored.
   * @param regexes the regex strings to be compiled to java Patterns
   */
  @Override
  public void addIgnorableCommand(List<String> regexes) {
    // no-op
  }

  /**
   * Returns the test file currently being executed.
   *
   * @return the test file currently being executed.
   */
  @Override
  public TestableFile getTestFile()
  {
    return null;
  }

  /**
   * A notification flag to child threads whether to continue testing or whether the test is ending
   * and they should stop executing.
   * @return true if the test is continuing or false if it is ending.
   */
  @Override
  public boolean continueTesting()
  {
    return false;
  }

  /**
   * Blocks the current thread until testing operations have completed (either successfully
   * or in failure) and threads should now start executing cleanup operations.
   */
  @Override
  public void waitForCleanup()
  {
    // no-op
  }

  /**
   * Marks the test result as being aborted and causes all the test threads to stop executing and
   * start cleaning up.
   */
  @Override
  public void abortTest()
  {
    // no-op
  }

  /**
   * Marks the test result that the test has started executing.
   */
  @Override
  public void markAsStarted()
  {
    // no-op
  }

  /**
   * Marks the test result as successful and causes the test threads to begin cleaning up.
   */
  @Override
  public void markAsSucceeded()
  {
    // no-op
  }

  /**
   * Marks the test result as a failure and causes the test threads to begin cleaning up.
   * @param failure
   */
  @Override
  public void markAsFailed(FailureException failure)
  {
    // no-op
  }

  /**
   * Returns the result of the test
   */
  @Override
  public TestResult getResult() {
    return null;
  }

  /**
   * Tells any waiting threads on the cleanupPhase condition variable that the testing phase
   * has finished and the cleanup phase has begun. It releases any blocked, waiting threads.
   */
  private void signalEndOfTesting()
  {
    // no-op
  }

  /**
   * Accumulates a count of test steps that occurred in a given testing thread. Each thread should
   * report the number of test steps is executed when the thread completes.
   * @param count the number of test steps that the testing thread executed.
   */
  @Override
  public void accumulateTestSteps(long count)
  {
    // no-op
  }

  /**
   * Increments a count of the number of active threads in the system.
   */
  @Override
  public void incrementActiveThreads()
  {
    // no-op
  }

  /**
   * Decrements a count of the number of active threads in the system.
   */
  @Override
  public void decrementActiveThreads()
  {
    // no-op
  }

  /**
   * Returns the number of active threads in the system.
   * @return the number of active threads in the system.
   */
  @Override
  public int getNumActiveThreads()
  {
    return 0;
  }

  /**
   * Returns the synchronisation barrier associated with a given name for synchronising threads on.
   * Creates the barrier if one does not exist (threadsafe).
   * @param name the name to associate with the synchronisation barrier.
   * @param parties the number of threads to wait on.
   * @return the synchronisation barrier.
   */
  @Override
  public CyclicBarrier getOrCreateSyncObject(String name, int parties)
  {
    return null;
  }

  /**
   * Removes the given synchronisation barrier associated with a given name.
   * @param name  the name of the object to remove.
   * @param barrier the actual synchronisation object to be removed.
   */
  @Override
  public void removeSyncObject(String name, CyclicBarrier barrier)
  {
    // no-op
  }

  @Override
  public Options getTestOptions()
  {
    return testOptions;
  }

}
