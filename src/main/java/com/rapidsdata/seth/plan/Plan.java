// Copyright (c) 2017 Boray Data Co. Ltd.  All rights reserved.

package com.rapidsdata.seth.plan;

import java.io.File;
import java.util.List;

/** A container of test operations and corresponding cleanup operations. */
public class Plan
{
  /** The test file that this plan represents. */
  private final File testFile;

  /** The operations for the test. */
  private final List<Operation> testOps;

  /** The cleanup operations to run to undo any changes from the cleanup operations. */
  private final List<Operation> cleanupOps;


  /**
   * Constructor
   * @param testFile the file under test.
   * @param testOps the list of operations to be executed for this test.
   * @param cleanupOps the list of operations to cleanup any changes after running the test.
   */
  public Plan(File testFile, List<Operation> testOps, List<Operation> cleanupOps)
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
   * Returns the list of operations to execute for this test.
   * @return the list of operations to execute for this test.
   */
  public List<Operation> getTestOperations()
  {
    return testOps;
  }

  /**
   * Returns the list of operations to cleanup any changes after executing this test.
   * @return the list of operations to cleanup any changes after executing this test.
   */
  public List<Operation> getCleanupOperations()
  {
    return cleanupOps;
  }
}
