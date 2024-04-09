// Copyright (c) 2017 Boray Data Co. Ltd.  All rights reserved.

package com.rapidsdata.seth.exceptions;

import com.rapidsdata.seth.TestableFile;

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
   * @param testableFile the file being tested.
   */
  public TestSetupException(String msg, Throwable t, TestableFile testableFile)
  {
    super(msg, t, testableFile, -1, null);
    this.error = msg;
  }

  /**
   * Creates a TestSetupException from a general error message.
   * @param msg the error message.
   * @param testableFile the file being tested.
   * @param lineNo the line that the error occurred on
   */
  public TestSetupException(String msg, TestableFile testableFile, int lineNo)
  {
    super(msg, testableFile, lineNo, null);
    this.error = msg;
  }

  /**
   * Creates a FailureException from a FileNotFoundException.
   * @param e The FileNotFoundException for not finding the test file.
   * @param testableFile The test file we were trying to find.
   */
  public TestSetupException(FileNotFoundException e, TestableFile testableFile)
  {
    super(e.getMessage(), e, testableFile, -1, null);
    this.error = e.getMessage();
  }

  /**
   * Creates a FailureException from a PlanningException.
   * @param e The PlanningException we encountered while parsing the test file.
   */
  public TestSetupException(PlanningException e)
  {
    super(e.getMessage(), null /* don't print stack trace */, e.getTestableFile(), e.getLine(), e.getCommand());
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
   * @param outerTestableFile the path of the outer-most test file. If this equals the test file that
   *                      had the error then we won't reprint the test file path.
   * @return a description of the failure.
   */
  @Override
  public String getMessage(TestableFile outerTestableFile)
  {
    StringBuilder sb = formatMessage(outerTestableFile);

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

  /**
   * Returns a StringBuilder with a partially formatted message, optionally describing where
   * the error occurred and what was being executed.
   * This function should be called first when overriding getMessage(boolean, boolean, boolean) above.
   * @param outerTestableFile the path of the outer-most test file. If this equals the test file that
   *                      had the error then we won't reprint the test file path.
   * @return a partially filled StringBuilder instance with some metadata about the failure.
   */
  @Override
  protected StringBuilder formatMessage(TestableFile outerTestableFile)
  {
    StringBuilder sb = new StringBuilder(1024);

    if (testableFile != null) {
      sb.append(FILE_HEADING)
          .append(testableFile.describePath());
    }

    if (lineNumber > 0) {
      if (sb.length() > 0) {
        sb.append(System.lineSeparator());
      }

      sb.append(LINE_HEADING)
          .append(lineNumber);
    }

    if (command != null) {
      if (sb.length() > 0) {
        sb.append(System.lineSeparator());
      }

      sb.append(COMMAND_HEADING)
          .append(indent(command));
    }

    return sb;
  }
}
