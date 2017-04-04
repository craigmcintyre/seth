// Copyright (c) 2017 Boray Data Co. Ltd.  All rights reserved.

package com.rapidsdata.seth.plan;

import java.util.Deque;

/** A container of test operations and corresponding cleanup operations. */
public class Plan
{
  /** The test file that this plan represents. */
  private final String testFilePath;

  /** The operations for the test. */
  private final Deque<Operation> testOps;

  /** The cleanup operations to run to undo any changes from the cleanup operations. */
  private final Deque<Operation> cleanupOps;


  /**
   * Constructor
   * @param testFilePath the
   * @param testOps
   * @param cleanupOps
   */
  public Plan(String testFilePath, Deque<Operation> testOps, Deque<Operation> cleanupOps)
  {
    this.testFilePath = testFilePath;
    this.testOps = testOps;
    this.cleanupOps = cleanupOps;
  }

  /**
   * Return the path of the test file that this plan represents.
   * @return the path of the test file that this plan represents.
   */
  public String getTestFilePath()
  {
    return testFilePath;
  }

  /**
   * Returns the queue of operations to execute for this test.
   * @return the queue of operations to execute for this test.
   */
  public Deque<Operation> getTestOperations()
  {
    return testOps;
  }

  /**
   * Returns the queue of operations to cleanup any changes after executing this test.
   * @return the queue of operations to cleanup any changes after executing this test.
   */
  public Deque<Operation> getCleanupOperations()
  {
    return cleanupOps;
  }
}
