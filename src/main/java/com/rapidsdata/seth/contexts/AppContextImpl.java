// Copyright (c) 2017 Boray Data Co. Ltd.  All rights reserved.

package com.rapidsdata.seth.contexts;

import com.rapidsdata.seth.CommandLineArgs;
import com.rapidsdata.seth.Options;
import com.rapidsdata.seth.PathRelativity;
import com.rapidsdata.seth.logging.TestLogger;
import com.rapidsdata.seth.TestableFile;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

public class AppContextImpl implements AppContext
{
  /** The start time of the application in milliseconds since the epoch. */
  private final long appStartTime;

  /** The list of test files to be executed. */
  private final List<TestableFile> testableFiles;

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

  /** The command line arguments. */
  private final CommandLineArgs args;

  /** The options that are applicable to the whole application. */
  private final Options appOptions;

  /** The variables that are applied to all tests that run. */
  private final Map<String, String> appVariables;


  public AppContextImpl(long appStartTime,
                        CommandLineArgs args,
                        List<TestableFile> testableFiles,
                        String url,
                        PathRelativity pathRelativity,
                        TestLogger logger,
                        ExecutorService threadPool,
                        Options appOptions,
                        Map<String,String> appVariables)
  {
    this.appStartTime = appStartTime;
    this.args = args;
    this.testableFiles = testableFiles;
    this.url = url;
    this.pathRelativity = pathRelativity;
    this.logger = logger;
    this.threadPool = threadPool;
    this.appOptions = appOptions;
    this.appVariables = appVariables;
  }

  /**
   * Returns the time since the application started, in milliseconds since the epoch.
   * @return the time since the application started, in milliseconds since the epoch.
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
  public List<TestableFile> getTestableFiles()
  {
    return testableFiles;
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

  /**
   * Returns the command line arguments used to run the program.
   * @return the command line arguments used to run the program.
   */
  @Override
  public CommandLineArgs getCommandLineArgs()
  {
    return args;
  }

  /**
   * Returns the options that are applied to the whole application.
   * @return the options that are applied to the whole application.
   */
  @Override
  public Options getAppOptions()
  {
    return appOptions;
  }

  @Override
  public Map<String, String> getAppVariables() {
    return appVariables;
  }
}
