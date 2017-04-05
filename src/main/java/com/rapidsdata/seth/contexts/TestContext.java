// Copyright (c) 2017 Boray Data Co. Ltd.  All rights reserved.

package com.rapidsdata.seth.contexts;

import com.rapidsdata.seth.exceptions.FailureException;

import java.io.File;

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
   * Marks the test result as being aborted and causes all the test threads to stop executing and
   * start cleaning up.
   */
  public void abortTest();

  /**
   * Marks the test result as successful and causes the test threads to begin cleaning up.
   */
  public void markAsSucceeded();

  /**
   * Marks the test result as a failure and causes the test threads to begin cleaning up.
   */
  public void markAsFailed(FailureException failure);

  /**
   * Accumulates a count of test steps that occurred in a given testing thread. Each thread should
   * report the number of test steps is executed when the thread completes.
   * @param count the number of test steps that the testing thread executed.
   */
  public void accumulateTestSteps(long count);
}
