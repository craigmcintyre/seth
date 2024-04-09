// Copyright (c) 2017 Boray Data Co. Ltd.  All rights reserved.

package com.rapidsdata.seth.logging;

import com.rapidsdata.seth.TestResult;
import com.rapidsdata.seth.TestableFile;

import java.io.Closeable;
import java.io.File;

public interface TestLogger extends Closeable
{
  /**
   * Logs that the test is currently being validated.
   * @param testFile the path of the test being validated.
   */
  public void testValidating(TestableFile testFile);

  /**
   * Logs that the test is currently being skipped.
   * @param testFile the path of the test being skipped.
   */
  public void testSkipping(TestableFile testFile);

  /**
   * Logs that the test is currently being executed.
   * @param testFile the path of the test being executed.
   */
  public void testExecuting(TestableFile testFile);

  /**
   * Logs that the test has completed executing (successfully or not).
   * @param testFile the path of the test that was executed.
   * @param result the result of the execution.
   */
  public void testExecutionFinished(TestableFile testFile, TestResult result);

  /**
   * Logs that the a given test step is currently being executed.
   * @param testFile the path of the test being executed.
   * @param command the test step command being executed.
   * @param lineNum the line number of the command in the test file.
   */
  public void testStepExecuting(TestableFile testFile, String command, long lineNum);

  /**
   * Log a general purpose message.
   * @param msg the message to be logged.
   */
  public void log(String msg);

  /**
   * Log a general purpose message.
   * @param msg the message to be logged.
   * @param indent indent the message for easier reading relative to the current test?
   */
  public void log(String msg, boolean indent);

  /**
   * Log a general purpose warning message about a non-fatal event.
   * @param msg the warning message to be logged.
   */
  public void warning(String msg);

  /**
   * Log a general purpose error message.
   * @param msg the message to be logged.
   */
  public void error(String msg);
}
