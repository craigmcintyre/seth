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

public interface AppContext
{
  /**
   * Returns the time that the application started, in milliseconds since the epoch.
   * @return the time that the application started, in milliseconds since the epoch.
   */
  public long getAppStartTime();

  /**
   * Returns the list of test files to be executed.
   * @return the list of test files to be executed.
   */
  public List<TestableFile> getTestableFiles();

  /**
   * Returns the url the JDBC driver should use to communicate to the system being tested.
   * @return the url the JDBC driver should use to communicate to the system being tested.
   */
  public String getUrl();

  /**
   * Returns how we are to treat relative paths.
   * @return how we are to treat relative paths.
   */
  public PathRelativity getPathRelativity();

  /**
   * Returns the logger we are to use to write to the console and to file.
   * @return the logger we are to use to write to the console and to file.
   */
  public TestLogger getLogger();

  /**
   * Returns the thread pool executor service, by which new threads can be created.
   * @return the thread pool executor service, by which new threads can be created.
   */
  public ExecutorService getThreadPool();

  /**
   * Returns the command line arguments used to run the program.
   * @return the command line arguments used to run the program.
   */
  public CommandLineArgs getCommandLineArgs();

  /**
   * Returns the options that are applied to the whole application.
   * @return the options that are applied to the whole application.
   */
  public Options getAppOptions();

  /**
   * Returns the variables that are set for the whole application (all tests).
   * @return the variables that are set for the whole application (all tests).
   */
  public Map<String,String> getAppVariables();
}
