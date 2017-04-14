// Copyright (c) 2017 Boray Data Co. Ltd.  All rights reserved.

package com.rapidsdata.seth.exceptions;

import java.io.File;

public class OperationException extends FailureException
{
  /** The error message encountered. */
  protected final String error;

  /**
   * Creates a TestSetupException from a general error message.
   * @param msg the error message.
   * @param testFile the file being tested.
   */
  public OperationException(String msg, File testFile)
  {
    super(msg, testFile, -1, null);
    this.error = msg;
  }

  /**
   * Creates a TestSetupException from a general error message.
   * @param msg the error message.
   * @param testFile the file being tested.
   * @param line the line number where the error occurred.
   * @param command the command being executed at the time of the error.
   */
  public OperationException(String msg, File testFile, long line, String command)
  {
    super(msg, testFile, line, command);
    this.error = msg;
  }

  /**
   * Creates a TestSetupException from a general error message.
   * @param msg the error message.
   * @param t a chained exception.
   * @param testFile the file being tested.
   */
  public OperationException(String msg, Throwable t, File testFile)
  {
    super(msg, t, testFile, -1, null);
    this.error = msg;
  }

  /**
   * Creates a TestSetupException from a general error message.
   * @param msg the error message.
   * @param t a chained exception.
   * @param testFile the file being tested.
   * @param line the line number where the error occurred.
   * @param command the command being executed at the time of the error.
   */
  public OperationException(String msg, Throwable t, File testFile, long line, String command)
  {
    super(msg, t, testFile, line, command);
    this.error = msg;
  }

  /**
   * Return a description of the failure, with option descriptions of where it failed and what
   * was being executed.
   * @return a description of the failure.
   */
  @Override
  public String getMessage()
  {
    return getMessage(null);
  }

  /**
   * Return a description of the failure, with option descriptions of where it failed and what
   * was being executed.
   * @param outerTestFile the path of the outer-most test file. If this equals the test file that
   *                      had the error then we won't reprint the test file path.
   * @return a description of the failure.
   */
  @Override
  public String getMessage(File outerTestFile)
  {
    StringBuilder sb = formatMessage(outerTestFile);

    if (error != null) {
      if (sb.length() > 0) {
        sb.append(System.lineSeparator());
      }

      sb.append(ERROR_HEADING)
        .append(error);

    }

    if (getCause() != null) {
      if (sb.length() > 0) {
        sb.append(System.lineSeparator());
      }

      sb.append(STACK_HEADING)
        .append(indent(getStackTrace(getCause())));
    }

    return sb.toString();
  }
}
