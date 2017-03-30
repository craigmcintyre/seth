// Copyright (c) 2017 Boray Data Co. Ltd.  All rights reserved.

package com.rapidsdata.seth.logging;

import com.rapidsdata.seth.TestResult;

import java.io.File;
import java.io.IOException;

/**
 * A logger that logs to the console stdout.
 */
public class ConsoleLogger implements TestLogger
{
  private final String FMT = "%-11s:  %s";

  /**
   * Logs that the test is currently being validated.
   *
   * @param testFile the path of the test being validated.
   */
  @Override
  public void testValidating(File testFile)
  {
    final String msg = String.format(FMT, "Validating", testFile.getPath());
    System.out.println(msg);
  }

  /**
   * Logs that the test is currently being executed.
   *
   * @param testFile the path of the test being executed.
   */
  @Override
  public void testExecuting(File testFile)
  {
    final String msg = String.format(FMT, "Executing", testFile.getPath());
    System.out.println(msg);
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
    if (result.getStatus() == TestResult.ResultStatus.FAILED) {
      final String msg = String.format(FMT, "Failed",
          testFile.getPath() + System.lineSeparator() + System.lineSeparator() + result.getFailureDescription());
      System.out.println(msg);

    } else if (result.getStatus() == TestResult.ResultStatus.ABORTED) {
      final String msg = String.format(FMT, "Aborted", testFile.getPath());
      System.out.println(msg);
    }

    // Not printing other result types.
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
    // We won't print test steps to the console - it would be waaay too noisy.
    // Save this for the log files.
  }

  /**
   * Log a general purpose message.
   *
   * @param msg the message to be logged.
   */
  @Override
  public void log(String msg)
  {
    System.out.println(msg);
  }

  /**
   * Log a general purpose error message.
   * @param msg the error message to be logged.
   */
  @Override
  public void error(String msg)
  {
    System.err.println(msg);
  }

  /**
   * Closes this stream and releases any system resources associated
   * with it. If the stream is already closed then invoking this
   * method has no effect.
   * <p>
   * <p> As noted in {@link AutoCloseable#close()}, cases where the
   * close may fail require careful attention. It is strongly advised
   * to relinquish the underlying resources and to internally
   * <em>mark</em> the {@code Closeable} as closed, prior to throwing
   * the {@code IOException}.
   *
   * @throws IOException if an I/O error occurs
   */
  @Override
  public void close() throws IOException
  {
    // nothing to do here.
  }
}
