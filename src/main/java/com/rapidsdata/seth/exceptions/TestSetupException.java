// Copyright (c) 2017 Boray Data Co. Ltd.  All rights reserved.

package com.rapidsdata.seth.exceptions;

import java.io.File;
import java.io.FileNotFoundException;

public class TestSetupException extends FailureException
{
  /** The error message encountered. */
  protected final String error;

  /**
   * Creates a TestSetupException from a general error message.
   * @param msg the error message.
   * @param t a chained exception.
   * @param testFile the file being tested.
   */
  public TestSetupException(String msg, Throwable t, File testFile)
  {
    super(msg, t, testFile, -1, null);
    this.error = msg;
  }

  /**
   * Creates a FailureException from a FileNotFoundException.
   * @param e The FileNotFoundException for not finding the test file.
   * @param testFile The test file we were trying to find.
   */
  public TestSetupException(FileNotFoundException e, File testFile)
  {
    super(e.getMessage(), e, testFile, -1, null);
    this.error = e.getMessage();
  }

  /**
   * Creates a FailureException from a SyntaxException.
   * @param e The SyntaxException we encountered while parsing the test file.
   */
  public TestSetupException(SyntaxException e)
  {
    super(e.getMessage(), e, e.getFile(), -1, null);
    this.error = e.getMessage();
  }

  /**
   * Return a description of the failure, showing where it occurred, the command executed
   * and why it failed.
   *
   * @return a description of the failure, showing where it occurred, the command executed
   * and why it failed.
   */
  @Override
  public String getMessage()
  {
    return getMessage(true, (lineNumber >= 0), false);
  }

  /**
   * Return a description of the failure, with option descriptions of where it failed and what
   * was being executed.
   *
   * @param showFile    if true then it will print the path of the test file being executed.
   * @param showLine    if true then it will print the line number of the command being executed.
   * @param showCommand if true then it will print the command being executed.
   * @return a description of the failure.
   */
  @Override
  public String getMessage(boolean showFile, boolean showLine, boolean showCommand)
  {
    StringBuilder sb = formatMessage(showFile, showLine, showCommand);
    sb.append(ERROR_HEADING)
      .append(error)
      .append(System.lineSeparator());

    return sb.toString();
  }
}
