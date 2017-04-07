// Copyright (c) 2017 Boray Data Co. Ltd.  All rights reserved.

package com.rapidsdata.seth.plan;

import java.io.File;

/**
 * Holds source metadata about the origin of this operation.
 */
public class OperationMetadata
{
  /** The description of the operation. */
  private final String description;

  /** The file that the operation came from. */
  private final File testFile;

  /** The line in the file where this operation occurs. */
  private final int line;

  /** Whether the statement was defined in a test phase or a cleanup phase. */
  private final TestPhase phase;

  /**
   * Constructor
   * @param description The description of the operation.
   * @param testFile The file that the operation came from.
   * @param line The line in the file where this operation occurs.
   * @param phase Whether this operation was specified in a test or a cleanup phase.
   */
  public OperationMetadata(String description, File testFile, int line, TestPhase phase)
  {
    this.description = description;
    this.testFile = testFile;
    this.line = line;
    this.phase = phase;
  }

  /**
   * Returns the description of the operation.
   * @return the description of the operation.
   */
  public String getDescription()
  {
    return description;
  }

  /**
   * Returns the file that the operation came from.
   * @return the file that the operation came from.
   */
  public File getTestFile()
  {
    return testFile;
  }

  /**
   * Returns the line in the file where this operation occurs.
   * @return the line in the file where this operation occurs.
   */
  public int getLine()
  {
    return line;
  }

  /**
   * Returns whether the statement was defined in a test phase or a cleanup phase.
   * @return whether the statement was defined in a test phase or a cleanup phase.
   */
  public TestPhase getPhase()
  {
    return phase;
  }
}
