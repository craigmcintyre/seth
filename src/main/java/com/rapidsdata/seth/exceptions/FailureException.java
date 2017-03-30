// Copyright (c) 2017 Boray Data Co. Ltd.  All rights reserved.

package com.rapidsdata.seth.exceptions;

import java.io.File;

/**
 * A class that represents a failure while executing a test.
 * Designed to be subclassed so it can accommodate failures from expected results
 * and other general purpose operation failures.
 */
public abstract class FailureException extends SethException
{
  private static final String FILE_HEADING      = "File     : ";
  private static final String LINE_HEADING      = "Line     : ";
  private static final String COMMAND_HEADING   = "Command  : ";
  private static final String ERROR_HEADING     = "Error    : ";
  private static final String EXPECTED_HEADING  = "Expected : ";
  private static final String ACTUAL_HEADING    = "Actual   : ";

  private final File testFile;
  private final long lineNumber;
  private final String command;


  public FailureException(String message, File testFile, long lineNumber, String command)
  {
    super(message);

    this.testFile = testFile;
    this.lineNumber = lineNumber;
    this.command = command;
  }

  public FailureException(String message, Throwable throwable, File testFile, long lineNumber, String command)
  {
    super(message, throwable);

    this.testFile = testFile;
    this.lineNumber = lineNumber;
    this.command = command;
  }

  public FailureException(Throwable t, File testFile, long lineNumber, String command)
  {
    super(t);

    this.testFile = testFile;
    this.lineNumber = lineNumber;
    this.command = command;
  }

  /**
   * Return a description of the failure, showing where it occurred, the command executed
   * and why it failed.
   * @return a description of the failure, showing where it occurred, the command executed
   * and why it failed.
   */
  public abstract String getMessage();

  /**
   * Return a description of the failure, with option descriptions of where it failed and what
   * was being executed.
   * @param showFile if true then it will print the path of the test file being executed.
   * @param showLine if true then it will print the line number of the command being executed.
   * @param showCommand if true then it will print the command being executed.
   * @return a description of the failure.
   */
  public abstract String getMessage(boolean showFile, boolean showLine, boolean showCommand);

  /**
   * Returns a StringBuilder with a partially formatted message, optionally describing where
   * the error occurred and what was being executed.
   * This function should be called first when overriding getMessage(boolean, boolean, boolean) above.
   * @param showFile if true then it will print the path of the test file being executed.
   * @param showLine if true then it will print the line number of the command being executed.
   * @param showCommand if true then it will print the command being executed.
   * @return a partially filled StringBuilder instance with some metadata about the failure.
   */
  protected StringBuilder formatMessage(boolean showFile, boolean showLine, boolean showCommand)
  {
    StringBuilder sb = new StringBuilder(1024);

    if (showFile) {
      sb.append(FILE_HEADING)
        .append(testFile.getPath())
        .append(System.lineSeparator());
    }

    if (showLine) {
      sb.append(LINE_HEADING)
        .append(lineNumber)
        .append(System.lineSeparator());
    }

    if (showCommand) {
      sb.append(COMMAND_HEADING)
        .append(command)
        .append(System.lineSeparator());
    }

    return sb;
  }

}
