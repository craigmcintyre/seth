// Copyright (c) 2017 Boray Data Co. Ltd.  All rights reserved.

package com.rapidsdata.seth.logging;

import com.rapidsdata.seth.TestResult;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * A class that writes to a file log entries about overall tests, as well as the console.
 */
public class TestFileLogger extends FileLogger
{
  /** A string of spaces. */
  private static final String DECORATION_INDENT = System.lineSeparator() + new String(new char[28]).replace('\0', ' ');

  // Get the time that the application started.
  private long jvmStartTime = ManagementFactory.getRuntimeMXBean().getStartTime();

  /**
   * Constructor
   * @param parentDir the directory that will contain the log file.
   */
  public TestFileLogger(File parentDir)
  {
    super(parentDir);
  }

  /**
   * Logs that the test is currently being validated.
   *
   * @param testFile the path of the test being validated.
   */
  @Override
  public void testValidating(File testFile)
  {
    // Ensure we write to the console.
    super.testValidating(testFile);

    String msg = "Validating test file " + testFile.getPath();
    queue.add(decorateMessage(msg));
  }

  /**
   * Logs that the test is currently being executed.
   *
   * @param testFile the path of the test being executed.
   */
  @Override
  public void testExecuting(File testFile)
  {
    // Ensure we write to the console.
    super.testExecuting(testFile);

    String msg = "Executing test file " + testFile.getPath();
    queue.add(decorateMessage(msg));
  }

  /**
   * Logs that the test has completed executing (successfully or not).
   *
   * @param testFile the path of the test that was executed.
   * @param result   the result of the execution.
   */
  @Override
  public void testExecutionFinished(File testFile, TestResult result)
  {
    // Ensure we write to the console.
    super.testExecutionFinished(testFile, result);

    String msg = "Result: " + result.getStatus().name() + ", test file " + testFile.getPath();

    if (result.getStatus() == TestResult.ResultStatus.FAILED) {
      msg += System.lineSeparator() + result.getFailureDescription(false, true, true);
    }

    queue.add(decorateMessage(msg));
  }

  /**
   * Logs that the a given test step is currently being executed.
   *
   * @param testFile the path of the test being executed.
   * @param command  the test step command being executed.
   * @param lineNum  the line number of the command in the test file.
   */
  @Override
  public void testStepExecuting(File testFile, String command, long lineNum)
  {
    // Ensure we write to the console.
    super.testStepExecuting(testFile, command, lineNum);

    // We aren't writing log messages for test steps in this class. You want a StepFileLogger instead.
  }

  /**
   * Log a general purpose message.
   *
   * @param msg the message to be logged.
   */
  @Override
  public void log(String msg)
  {
    super.log(msg);
    queue.add(decorateMessage(msg));
  }

  /**
   * Log a general purpose error message.
   *
   * @param msg the error message to be logged.
   */
  @Override
  public void error(String msg)
  {
    super.log(msg);
    queue.add(decorateMessage("ERROR: " + msg));
  }

  /**
   * Wraps a timestamped header around a log message
   * @param content the log message to be decorated.
   * @return a new string with a timestamp prefixed to the log message.
   */
  protected String decorateMessage(String content)
  {
    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS");
    Calendar cal = Calendar.getInstance();
    long elapsedTime = cal.getTimeInMillis() - jvmStartTime;

    StringBuilder sb = new StringBuilder(1024);
    sb.append(sdf.format(cal.getTime()))
      .append("/+")
      .append(String.format("%06.3f :  ", elapsedTime / 1000.0));

    // Now add the content. Any newlines are replaced by a newline and an indent.
    sb.append(content.replace(System.lineSeparator(), DECORATION_INDENT));

    return sb.toString();
  }
}
