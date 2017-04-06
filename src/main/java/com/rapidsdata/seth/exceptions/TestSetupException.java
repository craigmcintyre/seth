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
      sb.append(System.lineSeparator())
        .append(ERROR_HEADING)
        .append(error);

    }

    if (getCause() != null) {
      sb.append(System.lineSeparator())
        .append(STACK_HEADING)
        .append(getStackTrace(getCause()));
    }

    return sb.toString();
  }
}
