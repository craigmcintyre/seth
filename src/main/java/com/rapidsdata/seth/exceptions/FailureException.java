// Copyright (c) 2017 Boray Data Co. Ltd.  All rights reserved.

package com.rapidsdata.seth.exceptions;

import java.io.File;
import java.io.FileNotFoundException;

/**
 * A class that represents a failure while executing a test.
 * Designed to be subclassed so it can accommodate failures from expected results
 * and other general purpose operation failures.
 */
public abstract class FailureException extends SethException
{
  protected static final String FILE_HEADING      = "File     : ";
  protected static final String LINE_HEADING      = "Line     : ";
  protected static final String COMMAND_HEADING   = "Command  : ";
  protected static final String ERROR_HEADING     = "Error    : ";
  protected static final String EXPECTED_HEADING  = "Expected : ";
  protected static final String ACTUAL_HEADING    = "Actual   : ";

  protected final File testFile;
  protected final long lineNumber;
  protected final String command;


  public FailureException(String message, File testFile, long lineNumber, String command)
  {
    super(message);

    this.testFile = testFile;
    this.lineNumber = lineNumber;
    this.command = command;
  }

  public FailureException(String message, Throwable e, File testFile, long lineNumber, String command)
  {
    super(message);

    this.testFile = testFile;
    this.lineNumber = lineNumber;
    this.command = command;
  }

  /**
   * Creates a FailureException from a FileNotFoundException.
   * @param e The FileNotFoundException for not finding the test file.
   * @param testFile The test file we were trying to find.
   */
  public FailureException(FileNotFoundException e, File testFile)
  {
    super(e.getMessage(), e);

    this.testFile = testFile;
    this.lineNumber = -1;
    this.command = null;
  }

  /**
   * Creates a FailureException from a SyntaxException.
   * @param e The SyntaxException we encountered when we were parsing the test file.
   */
  public FailureException(SyntaxException e)
  {
    super(e.getMessage(), e);

    this.testFile = e.getFile();
    this.lineNumber = e.getLine();
    this.command = null;
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
        .append(lineNumber >= 0 ? "(none)" : lineNumber)
        .append(System.lineSeparator());
    }

    if (showCommand) {
      sb.append(COMMAND_HEADING)
        .append(command == null ? "(none)" : command)
        .append(System.lineSeparator());
    }

    return sb;
  }

}
