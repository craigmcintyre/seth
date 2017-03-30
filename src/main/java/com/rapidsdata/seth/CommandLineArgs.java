// Copyright (c) 2017 Boray Data Co. Ltd.  All rights reserved.

package com.rapidsdata.seth;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import java.io.File;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;

public class CommandLineArgs
{

  // --validate
  // -u <connection_url>
  // -f <path_to_listfile>
  // --includes_relative_to_cwd
  // --includes_relative_to_test
  // --resultdir <path>
  // --resultformat <type>
  // --resultname <filename>
  // --clean
  // --logsteps
  // <list_of_test_files_to_run>


  @Option(name      = "--validate",
          required  = false,
          usage     = "Validates each test file can be parsed and prints its actions but does not run the tests.")
  public boolean doValidate = false;

  @Option(name      = "-u",
          required  = false,
          usage     = "The JDBC connection URL to use to connect to the system under test. The default is \"jdbc:rapidsse://localhost:4335\".")
  public String url = "jdbc:rapidsse://localhost:4335";

  @Option(name      = "-f",
          required  = false,
          usage     = "The path to the text file containing the list of test files to be run.")
  public File listFile = null;

  @Option(name      = "--includes_relative_to_cwd",
          required  = false,
          usage     = "When a test file includes another test with a relative path, that path will be considered " +
                      "relative to the current working directory. The default is to use paths relative to the location " +
                      "of the test file running the INCLUDE command.")
  public boolean includesAreRelativeToTest;

  @Option(name      = "--resultdir",
          required  = false,
          usage     = "The path where the results are to be written. The default is \"./results\".")
  public File resultDir = new File("./results");

  @Option(name      = "--resultformat",
          required  = false,
          usage     = "The format that the result file will be written in. Valid values are junit.")
  public String resultFormat = "junit";

  @Option(name      = "--resultname",
          required  = false,
          usage     = "The name of the file containing the results. Default is \"results.xml\".")
  public String resultName = "results.xml";

  @Option(name      = "--clean",
          required  = false,
          usage     = "Removes all files and subdirectories from the resultdir before running the test. Default is not to do this.")
  public boolean doClean = false;

  @Option(name      = "--logtests",
          required  = false,
          usage     = "Log the overall execution of each test to a log file in the resultdir. Default is not to log anything to file.")
  public boolean logTests = false;

  @Option(name      = "--logsteps",
      required  = false,
      usage     = "Log the execution of each step to a log file in the resultdir. This logs in greater detail than --logtests. Default is not to log anything to file.")
  public boolean logSteps = false;

  @Argument
  public List<String> testFiles = null;


  /**
   * Validates the semantics of the options, after the parser has previously validated the syntax.
   * @param parser
   * @throws CmdLineException
   */
  public void validateSemantics(CmdLineParser parser) throws CmdLineException
  {
    // Validate that the JDBC driver can be found and loaded.
    try {
      Driver driver = DriverManager.getDriver(url);

    } catch (SQLException e) {
      final String msg = "Could not load the JDBC driver for the url \"" + url + "\". Please check that this url is " +
                         "correct and that the appropriate JDBC driver is included in the class path.";
      throw new CmdLineException(parser, msg, e);
    }

    // Cannot specify -f and a set of test files. Must be one or the other.
    if (listFile != null && testFiles != null && !testFiles.isEmpty()) {
      final String msg = "Cannot specify both -f <listFile> as well as a set of test files to run.";
      throw new CmdLineException(parser, msg, null);
    }

    // Check if no test files and no listfile has been specified.
    if (listFile == null && testFiles == null) {
      final String msg = "No test files have been specified.";
      throw new CmdLineException(parser, msg, null);
    }

    // --resultdir must be created if it doesn't exist.
    if (!resultDir.exists() && !resultDir.mkdirs()) {
      final String msg = "Unable to create the resultdir directory at " + resultDir.getPath() + ".";
      throw new CmdLineException(parser, msg, null);
    }

    // TODO: --resultformat must be valid

  }

}
