// Copyright (c) 2017 Boray Data Co. Ltd.  All rights reserved.

package com.rapidsdata.seth;

import com.rapidsdata.seth.logging.TestLogger;

import java.io.File;
import java.sql.Driver;
import java.util.List;

public class RunContext
{
  /** The list of test files to be executed. */
  private final List<File> testFiles;

  /** The JDBC driver to use. */
  private final Driver driver;

  /** If true then we don't run the test, just validate them. If false, we run the tests. */
  private final boolean onlyValidate;

  /**
   * Are relative paths given to an INCLUDE statement relative to the path of the test file
   * or relative to the current working directory?
   */
  private final PathRelativity pathRelativity;

  /** The logger to use for writing things to the console and to file. */
  private final TestLogger logger;


  public RunContext(List<File> testFiles,
                    Driver driver,
                    boolean onlyValidate,
                    PathRelativity pathRelativity,
                    TestLogger logger)
  {
    this.testFiles = testFiles;
    this.driver = driver;
    this.onlyValidate = onlyValidate;
    this.pathRelativity = pathRelativity;
    this.logger = logger;
  }

  /**
   * Returns the list of test files to be executed.
   * @return the list of test files to be executed.
   */
  public List<File> getTestFiles()
  {
    return testFiles;
  }

  /**
   * Returns the JDBC driver to use to communicate to the system being tested.
   * @return the JDBC driver to use to communicate to the system being tested.
   */
  public Driver getDriver()
  {
    return driver;
  }

  /**
   * If true then the test files will only be validated and not executed.
   * If false then the test files will be executed in full.
   * @return whether to only validate or to fully execute the test files.
   */
  public boolean onlyValidateTestFiles()
  {
    return onlyValidate;
  }

  /**
   * Returns how we are to treat relative paths.
   * @return how we are to treat relative paths.
   */
  public PathRelativity getPathRelativity()
  {
    return pathRelativity;
  }

  /**
   * Returns the logger we are to use to write to the console and to file.
   * @return
   */
  public TestLogger getLogger()
  {
    return logger;
  }
}
