// Copyright (c) 2017 Boray Data Co. Ltd.  All rights reserved.

package com.rapidsdata.seth.contexts;

import com.rapidsdata.seth.PathRelativity;
import com.rapidsdata.seth.logging.TestLogger;

import java.io.File;
import java.sql.Driver;
import java.util.List;
import java.util.concurrent.ExecutorService;

public class AppContextImpl implements AppContext
{
  /** The start time of the application in nanoseconds since the epoch. */
  private final long appStartTime;

  /** The list of test files to be executed. */
  private final List<File> testFiles;

  /** The url that the JDBC driver should use. */
  private final String url;

  /**
   * Are relative paths given to an INCLUDE statement relative to the path of the test file
   * or relative to the current working directory?
   */
  private final PathRelativity pathRelativity;

  /** The logger to use for writing things to the console and to file. */
  private final TestLogger logger;

  /** The service that we create threads from for running tests. */
  private final ExecutorService threadPool;


  public AppContextImpl(long appStartTime,
                        List<File> testFiles,
                        String url,
                        PathRelativity pathRelativity,
                        TestLogger logger,
                        ExecutorService threadPool)
  {
    this.appStartTime = appStartTime;
    this.testFiles = testFiles;
    this.url = url;
    this.pathRelativity = pathRelativity;
    this.logger = logger;
    this.threadPool = threadPool;
  }

  /**
   * Returns the time since the application started, in nanoseconds since the epoch.
   * @return the time since the application started, in nanoseconds since the epoch.
   */
  @Override
  public long getAppStartTime()
  {
    return appStartTime;
  }

  /**
   * Returns the list of test files to be executed.
   * @return the list of test files to be executed.
   */
  @Override
  public List<File> getTestFiles()
  {
    return testFiles;
  }

  /**
   * Returns the url that the JDBC driver should to use to communicate to the system being tested.
   * @return the url that the JDBC driver should to use to communicate to the system being tested.
   */
  @Override
  public String getUrl()
  {
    return url;
  }

  /**
   * Returns how we are to treat relative paths.
   * @return how we are to treat relative paths.
   */
  @Override
  public PathRelativity getPathRelativity()
  {
    return pathRelativity;
  }

  /**
   * Returns the logger we are to use to write to the console and to file.
   * @return
   */
  @Override
  public TestLogger getLogger()
  {
    return logger;
  }

  /**
   * Returns the thread pool executor service, by which new threads can be created.
   * @return the thread pool executor service, by which new threads can be created.
   */
  @Override
  public ExecutorService getThreadPool()
  {
    return threadPool;
  }
}
