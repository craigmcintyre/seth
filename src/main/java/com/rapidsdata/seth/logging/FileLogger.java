// Copyright (c) 2017 Boray Data Co. Ltd.  All rights reserved.

package com.rapidsdata.seth.logging;

import com.rapidsdata.seth.TestResult;
import com.rapidsdata.seth.TestableFile;
import com.rapidsdata.seth.exceptions.SethSystemException;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/** A class that logs to a file and to the console. */
public abstract class FileLogger extends ConsoleLogger implements Closeable
{
  /** The file that we write the log to. */
  private File logfile;

  /** The queue of messages that will be written to file by the writing thread. */
  protected BlockingQueue<String> queue = new LinkedBlockingQueue<>();

  /** The thread that writes to the logfile. */
  private WriterThread writerThread;

  /**
   * Constructor
   * @param parentDir the directory that will contain the log file.
   */
  public FileLogger(File parentDir, String logNamePrefix, boolean logTestsPassed)
  {
    super(logTestsPassed);

    this.logfile = makeLogFile(parentDir, logNamePrefix);

    // Create a buffered writer for writing to the log file.
    BufferedWriter writer;
    try {
      writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(logfile), "utf-8"));

    } catch (IOException e) {
      final String msg = "Could not open the log file for writing: " + logfile.getPath();
      throw new SethSystemException(msg, e);
    }

    writerThread = new WriterThread(writer);
    Thread t = new Thread(writerThread);
    t.setDaemon(true); // So this thread doesn't prevent the application from exiting.
    t.start();
  }

  /**
   * Logs that the test is currently being validated.
   *
   * @param testFile the path of the test being validated.
   */
  @Override
  public void testValidating(TestableFile testFile)
  {
    // Ensure we write to the console.
    super.testValidating(testFile);
  }

  /**
   * Logs that the test is currently being skipped.
   *
   * @param testFile the path of the test being skipped.
   */
  @Override
  public void testSkipping(TestableFile testFile)
  {
    // Ensure we write to the console.
    super.testSkipping(testFile);
  }

  /**
   * Logs that the test is currently being executed.
   *
   * @param testFile the path of the test being executed.
   */
  @Override
  public void testExecuting(TestableFile testFile)
  {
    // Ensure we write to the console.
    super.testExecuting(testFile);
  }

  /**
   * Logs that the test has completed executing (successfully or not).
   *
   * @param testFile the path of the test that was executed.
   * @param result   the result of the execution.
   */
  @Override
  public void testExecutionFinished(TestableFile testFile, TestResult result)
  {
    // Ensure we write to the console.
    super.testExecutionFinished(testFile, result);
  }

  /**
   * Logs that the a given test step is currently being executed.
   *
   * @param testFile the path of the test being executed.
   * @param command  the test step command being executed.
   * @param lineNum  the line number of the command in the test file.
   */
  @Override
  public void testStepExecuting(TestableFile testFile, String command, long lineNum)
  {
    // Ensure we write to the console.
    super.testStepExecuting(testFile, command, lineNum);
  }

  /**
   * Log a general purpose message.
   * @param msg the message to be logged.
   */
  @Override
  public void log(String msg)
  {
    super.log(msg);
  }

  /**
   * Log a general purpose message.
   * @param msg the message to be logged.
   * @param indent indent the message for easier reading relative to the current test?
   */
  @Override
  public void log(String msg, boolean indent)
  {
    super.log(msg, indent);
  }

  /**
   * Log a general purpose warning message about a non-fatal event.
   * @param msg the warning message to be logged.
   */
  @Override
  public void warning(String msg)
  {
    super.warning(msg);
  }

  /**
   * Log a general purpose error message.
   * @param msg the error message to be logged.
   */
  @Override
  public void error(String msg)
  {
    super.error(msg);
  }

  /**
   * Creates a new logfile in the parentDir and returns a File reference to it.
   * @param parentDir the result directory where the logfile will be created.
   * @param logNamePrefix the optional prefix to the name of the log file
   * @return a File reference to the newly created logfile.
   */
  protected File makeLogFile(File parentDir, String logNamePrefix)
  {
    // Get the time that the application started and format this into a filename.
    long jvmStartTime = ManagementFactory.getRuntimeMXBean().getStartTime();

    SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");

    Calendar calendar = Calendar.getInstance();
    calendar.setTimeInMillis(jvmStartTime);

    final String filename;

    if (logNamePrefix != null && !logNamePrefix.isEmpty()) {
      filename = logNamePrefix + "-log-" + sdf.format(calendar.getTime());
    } else {
      filename = "log-" + sdf.format(calendar.getTime());
    }

    File logfile = Paths.get(parentDir.getPath(), filename).toFile();

    if (logfile.exists()) {
      final String msg = "Logfile " + logfile.getPath() + " already exists.";
      throw new SethSystemException(msg);
    }

    // Create the logfile.
    try {
      logfile.createNewFile();

    } catch (IOException | SecurityException e) {
      final String msg = "Unable to create the logfile " + logfile.getPath();
      throw new SethSystemException(msg, e);
    }

    return logfile;
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
    super.close();
    writerThread.close();
  }


  /**
   * The class responsible for writing log messages from the queue to the log file.
   */
  private class WriterThread implements Closeable, Runnable
  {
    /** How long to wait for a log object to be written to the queue. */
    private static final int POLL_TIME_MS = 1000;

    /** The object that we use for writing to the file. */
    protected BufferedWriter writer;

    /** A flag indicating that the file should be closed. */
    protected volatile boolean toBeClosed = false;

    /**
     * Constructor.
     * @param writer The object that we will use for writing to the log file.
     */
    public WriterThread(BufferedWriter writer)
    {
      this.writer = writer;
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
      toBeClosed = true;
    }


    /**
     * When an object implementing interface <code>Runnable</code> is used
     * to create a thread, starting the thread causes the object's
     * <code>run</code> method to be called in that separately executing
     * thread.
     * <p>
     * The general contract of the method <code>run</code> is that it may
     * take any action whatsoever.
     *
     * @see Thread#run()
     */
    @Override
    public void run()
    {
      int factor = 1;

      while (toBeClosed == false) {

        String msg = null;

        try {
          msg = queue.poll(POLL_TIME_MS/factor, TimeUnit.MILLISECONDS);

          if (msg != null) {
            writer.write(msg);
            writer.newLine();
            writer.flush();
          }

        } catch (InterruptedException e) {
          factor = 10;

        } catch (IOException e) {
          final String err = "Could not write log message to file.";
          throw new SethSystemException(err, e);
        }
      }

      try {
        writer.close();
      } catch (Exception e) { /*ignore*/ }
    }
  }
}
