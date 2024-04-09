// Copyright (c) 2017 Boray Data Co. Ltd.  All rights reserved.

package com.rapidsdata.seth.exceptions;

import com.rapidsdata.seth.TestableFile;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * A class that represents a failure while executing a test.
 * Designed to be subclassed so it can accommodate failures from expected results
 * and other general purpose operation failures.
 */
public abstract class FailureException extends SethException
{
  protected static final String FILE_HEADING      = "File       : ";
  protected static final String LINE_HEADING      = "Line       : ";
  protected static final String COMMAND_HEADING   = "Command    : ";
  protected static final String ERROR_HEADING     = "Error      : ";
  protected static final String COMMENT_HEADING   = "Comment    : ";
  protected static final String EXPECTED_HEADING  = "Expected   : ";
  protected static final String ACTUAL_HEADING    = "Actual     : ";
  protected static final String STACK_HEADING     = "StackTrace : ";

  protected static final String INDENTATION = new String(new char[FILE_HEADING.length()]).replace('\0', ' ');
  protected static final String NL_INDENTATION = System.lineSeparator() + INDENTATION;


  protected final TestableFile testableFile;
  protected final long lineNumber;
  protected final String command;


  public FailureException(String message, TestableFile testableFile, long lineNumber, String command)
  {
    super(message);

    this.testableFile = testableFile;
    this.lineNumber = lineNumber;
    this.command = command;
  }

  public FailureException(String message, Throwable t, TestableFile testableFile, long lineNumber, String command)
  {
    super(message, t);

    this.testableFile = testableFile;
    this.lineNumber = lineNumber;
    this.command = command;
  }

  /**
   * Creates a FailureException from a FileNotFoundException.
   * @param e The FileNotFoundException for not finding the test file.
   * @param testableFile The test file we were trying to find.
   */
  public FailureException(FileNotFoundException e, TestableFile testableFile)
  {
    super(e.getMessage(), e);

    this.testableFile = testableFile;
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

    this.testableFile = e.getTestableFile();
    this.lineNumber = e.getLine();
    this.command = null;
  }

  /**
   * Returns the test file that the error occurred in.
   * @return the test file that the error occurred in.
   */
  public TestableFile getTestableFile()
  {
    return testableFile;
  }

  /**
   * Return a description of the failure, showing where it occurred, the command executed
   * and why it failed.
   * @return a description of the failure, showing where it occurred, the command executed
   * and why it failed.
   */
  public abstract String getMessage();

  /**
   * Return a description of the failure, showing where it occurred, the command executed
   * and why it failed.
   * @param outerTestFile the path of the outer-most test file. If this equals the test file that
   *                      had the error then we won't reprint the test file path.
   * @return a description of the failure, showing where it occurred, the command executed
   * and why it failed.
   */
  public abstract String getMessage(TestableFile outerTestFile);

  /**
   * Returns a StringBuilder with a partially formatted message, optionally describing where
   * the error occurred and what was being executed.
   * This function should be called first when overriding getMessage(boolean, boolean, boolean) above.
   * @param outerTestableFile the path of the outer-most test file. If this equals the test file that
   *                      had the error then we won't reprint the test file path.
   * @return a partially filled StringBuilder instance with some metadata about the failure.
   */
  protected StringBuilder formatMessage(TestableFile outerTestableFile)
  {
    StringBuilder sb = new StringBuilder(1024);

    if ( !(testableFile != null && outerTestableFile != null && testableFile.equals(outerTestableFile)) ) {
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

  /**
   * Returns the stack trace of an exception as a string.
   * @param t the Throwable to get the stack trace of.
   * @return the stack trace of an exception as a string.
   */
  protected String getStackTrace(Throwable t)
  {
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    t.printStackTrace(pw);
    return sw.toString();
  }

  /**
   * Returns a copy of the parameter string, indented to suit the formatting of formatMessage().
   * @param indentable The string to be indented.
   * @return The final indented string.
   */
  protected String indent(String indentable)
  {
    String indented = indentable.replace(System.lineSeparator(), NL_INDENTATION);
    return indented;
  }
}
