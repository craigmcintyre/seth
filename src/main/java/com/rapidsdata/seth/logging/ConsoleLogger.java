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
  private static final String FMT = "%-12s:  %s";
  private static final String INDENTATION = "  ";
  private static final String NL_AND_INDENTATION = System.lineSeparator() + INDENTATION;

  private final boolean logTestsPassed;

  public ConsoleLogger(boolean logTestsPassed)
  {
    this.logTestsPassed = logTestsPassed;
  }

  /**
   * Logs that the test is currently being validated.
   *
   * @param testFile the path of the test being validated.
   */
  @Override
  public void testValidating(File testFile)
  {
    final String msg = String.format(FMT, "Validating", testFile.getPath());

    synchronized(this) {
      System.out.println(msg);
    }
  }

  /**
   * Logs that the test is currently being skipped.
   *
   * @param testFile the path of the test being skipped.
   */
  @Override
  public void testSkipping(File testFile)
  {
    final String msg = String.format(FMT, "Skipping", testFile.getPath());
    synchronized(this) {
      System.out.println(msg);
    }
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
    synchronized(this) {
      System.out.println(msg);
    }
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
      final String msg = String.format(FMT, "Test Failed",
          testFile.getPath() + System.lineSeparator() + indent(result.getFailureDescription()));
      synchronized(this) {
        System.out.println(msg);
      }

    } else if (result.getStatus() == TestResult.ResultStatus.ABORTED) {
      final String msg = String.format(FMT, "Test Aborted", testFile.getPath());
      synchronized(this) {
        System.out.println(msg);
      }

    } else if (result.getStatus() == TestResult.ResultStatus.SUCCEEDED && logTestsPassed) {
      final String msg = String.format(FMT, "Test Passed", testFile.getPath());
      synchronized(this) {
        System.out.println(msg);
      }
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
    log(msg, true);
  }

  /**
   * Log a general purpose message.
   *
   * @param msg the message to be logged.
   * @param indent indent the message for easier reading relative to the current test?
   */
  @Override
  public synchronized void log(String msg, boolean indent)
  {
    String loggable = (indent ? indent(msg): msg);

    synchronized(this) {
      System.out.println(loggable);
    }
  }

  /**
   * Log a general purpose error message.
   * @param msg the error message to be logged.
   */
  @Override
  public void warning(String msg)
  {
    synchronized(this) {
      System.out.println("Warning: " + indent(msg));
    }
  }

  /**
   * Log a general purpose error message.
   * @param msg the error message to be logged.
   */
  @Override
  public void error(String msg)
  {
    synchronized(this) {
      System.err.println("Error: " + indent(msg));
    }
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

  /**
   * Nicely indents a string, including any newlines that string may contain.
   * @param oldStr the string to be indented.
   * @return the nicely indented string.
   */
  protected String indent(String oldStr)
  {
    // Replace tabs with spaces
    String newStr = oldStr.replace("\t", INDENTATION);

    // Replace newlines with newlines and some spaces.
    newStr = newStr.replace(System.lineSeparator(), NL_AND_INDENTATION);

    if (!newStr.startsWith(INDENTATION)) {
      newStr = INDENTATION + newStr;
    }

    return newStr;
  }
}
