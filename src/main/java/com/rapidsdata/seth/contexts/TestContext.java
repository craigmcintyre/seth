// Copyright (c) 2017 Boray Data Co. Ltd.  All rights reserved.

package com.rapidsdata.seth.contexts;

import com.rapidsdata.seth.Options;
import com.rapidsdata.seth.TestResult;
import com.rapidsdata.seth.exceptions.FailureException;

import java.io.File;
import java.util.Map;
import java.util.concurrent.CyclicBarrier;

public interface TestContext extends AppContext
{
  /**
   * Returns the test file currently being executed.
   * @return the test file currently being executed.
   */
  public File getTestFile();

  /**
   * A notification flag to child threads whether to continue testing or whether the test is ending
   * and they should stop executing.
   * @return true if the test is continuing or false if it is ending.
   */
  public boolean continueTesting();

  /**
   * Blocks the current thread until testing operations have completed (either successfully
   * or in failure) and threads should now start executing cleanup operations.
   */
  public void waitForCleanup();

  /**
   * Marks the test result as being aborted and causes all the test threads to stop executing and
   * start cleaning up.
   */
  public void abortTest();

  /**
   * Marks the test result that the test has started executing.
   */
  public void markAsStarted();

  /**
   * Marks the test result as successful and causes the test threads to begin cleaning up.
   */
  public void markAsSucceeded();

  /**
   * Marks the test result as a failure and causes the test threads to begin cleaning up.
   */
  public void markAsFailed(FailureException failure);

  /**
   * Returns the result of the test
   */
  public TestResult getResult();

  /**
   * Accumulates a count of test steps that occurred in a given testing thread. Each thread should
   * report the number of test steps is executed when the thread completes.
   * @param count the number of test steps that the testing thread executed.
   */
  public void accumulateTestSteps(long count);

  /** Increments a count of the number of active threads in the system. */
  public void incrementActiveThreads();

  /** Decrements a count of the number of active threads in the system. */
  public void decrementActiveThreads();

  /**
   * Returns the number of active threads in the system.
   * @return the number of active threads in the system.
   */
  public int getNumActiveThreads();

  /**
   * Returns the synchronisation barrier associated with a given name for synchronising threads on.
   * Creates the barrier if one does not exist (threadsafe).
   * @param name the name to associate with the synchronisation barrier.
   * @param parties the number of threads to wait on.
   * @return the synchronisation barrier.
   */
  public CyclicBarrier getOrCreateSyncObject(String name, int parties);

  /**
   * Removes the given synchronisation barrier associated with a given name.
   * @param name  the name of the object to remove.
   * @param barrier the actual synchronisation object to be removed.
   */
  public void removeSyncObject(String name, CyclicBarrier barrier);

  /**
   * Returns the options object that applies to this test.
   * @return the options object that applies to this test.
   */
  public Options getTestOptions();

}
