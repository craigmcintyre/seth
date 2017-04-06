// Copyright (c) 2017 Boray Data Co. Ltd.  All rights reserved.

package com.rapidsdata.seth.plan;

import java.io.File;
import java.util.Deque;

/** A container of test operations and corresponding cleanup operations. */
public class Plan
{
  /** The test file that this plan represents. */
  private final File testFile;

  /** The operations for the test. */
  private final Deque<Operation> testOps;

  /** The cleanup operations to run to undo any changes from the cleanup operations. */
  private final Deque<Operation> cleanupOps;


  /**
   * Constructor
   * @param testFile the file under test.
   * @param testOps
   * @param cleanupOps
   */
  public Plan(File testFile, Deque<Operation> testOps, Deque<Operation> cleanupOps)
  {
    this.testFile = testFile;
    this.testOps = testOps;
    this.cleanupOps = cleanupOps;
  }

  /**
   * Returns the test file that this plan represents.
   * @return the test file that this plan represents.
   */
  public File getTestFile()
  {
    return testFile;
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
