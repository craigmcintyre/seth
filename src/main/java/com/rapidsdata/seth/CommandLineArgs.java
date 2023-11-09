// Copyright (c) 2017 Boray Data Co. Ltd.  All rights reserved.

package com.rapidsdata.seth;

import com.rapidsdata.seth.exceptions.InvalidResultFormatException;
import com.rapidsdata.seth.results.ResultWriterFactory;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CommandLineArgs
{

  // --validate
  // -u <connection_url>
  // -f <path_to_listfile>
  // --resultdir <path>
  // --resultformat <type>
  // --resultname <filename>
  // --clean
  // --logsteps
  // <list_of_test_files_to_run>


  @Option(name      = "-u",
          required  = false,
          usage     = "The JDBC connection URL to use to connect to the system under test. The default is \"jdbc:se://localhost:9123\".")
  public String url = "jdbc:se://localhost:9123";

  @Option(name      = "-f",
          required  = false,
          usage     = "The path to the text file containing the list of test files to be run.")
  public File listFile = null;

  @Option(name      = "--opt",
          aliases   = {"--options"},
          required  = false,
          usage     = "Options that will apply to all test files executed. Specified as \"key=value\" or simply just \"key\".")
  public String opts = null;

  @Option(name      = "--unordered",
          required  = false,
          usage     = "Ignore row order in expected result even when \"ordered rows\" is specified.")
  public boolean unordered = false;

  @Option(name      = "--nostop",
          required  = false,
          usage     = "Just show message and continue on wrong result or exception failure.")
  public boolean nostop = false;

  @Option(name      = "--record",
      required      = false,
      usage         = "For each operation, record the results and write the test and results to a new test file.")
  public boolean recordResults = false;

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

  @Option(name      = "--relativity",
          required  = false,
          usage     = "Determines how relative paths are interpreted. Set to \"referer\" for " +
                      "relative paths to be relative to the thing referring to it (usually another " +
                      "file). Set to \"cwd\" for relative paths to be relative to the current " +
                      "working directory of the application. The default is \"referer\".")
  public PathRelativity relativity = PathRelativity.REFERER;

  @Option(name      = "--resultdir",
          required  = false,
          usage     = "The path where the results are to be written. The default is \"./results\".")
  public File resultDir = new File("./results");

  @Option(name      = "--resultformat",
          required  = false,
          usage     = "The format that the result file will be written in. Valid values are log, " +
                      "junit. Default is log.")
  public String resultFormat = "log";

  @Option(name      = "--resultname",
          required  = false,
          usage     = "The name of the file containing the results (when the resultformat is not \"log\"). " +
                      "Default is \"results.xml\".")
  public String resultName = "results.xml";

  @Option(name      = "--testsuffix",
      required  = false,
      usage     = "A suffix to append to a test name when the resultformat is junit. Can be used to " +
          "differentiate test names when executed against a debug and then a release version.")
  public String testSuffix = "";


  @Option(name      = "--parallel",
          aliases   = { "-p" },
          required  = false,
          usage     = "Set to a value > 1 to run a maximum of this number of tests in parallel.")
  public int parallelTests = 1;

  @Option(name      = "--ignore",
          aliases   = { "--ignoreCmd" },
          required  = false,
          usage     = "Server commands to be ignored. The syntax must match exactly but case is insensitive.")
  public List<String> ignoreCommands = new ArrayList<>();

  @Argument(usage  = "The list of test files to be executed.",
            hidden = true)
  public List<File> testFiles = null;

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
      final String msg = "Could not load the JDBC driver for the url \"" + url +"\"." +
                         System.lineSeparator() + "Please check that this url is correct, " +
                         "that the appropriate JDBC driver is included in the classpath " +
                         "and that the Java environment meets minimum requirement for the driver." +
                         System.lineSeparator() + "The current classpath is: " +
                         ManagementFactory.getRuntimeMXBean().getClassPath() + System.lineSeparator() +
                         "The Java VM is: " +
                         ManagementFactory.getRuntimeMXBean().getVmVendor() + " " +
                         ManagementFactory.getRuntimeMXBean().getVmName() + " " +
                         ManagementFactory.getRuntimeMXBean().getVmVersion() + System.lineSeparator()
                         ;
      throw new CmdLineException(parser, msg, e);
    }

    // Cannot specify -f and a set of test files. Must be one or the other.
    if (listFile != null && testFiles != null && !testFiles.isEmpty()) {
      final String msg = "Cannot specify both -f <listFile> as well as a set of test files to run." +
                         System.lineSeparator();
      throw new CmdLineException(parser, msg, null);
    }

    // Check if no test files and no listfile has been specified.
    if (listFile == null && testFiles == null) {
      final String msg = "No test files have been specified." +
                         System.lineSeparator();
      throw new CmdLineException(parser, msg, null);
    }

    // --resultdir must be created if it doesn't exist.
    if (!resultDir.exists() && !resultDir.mkdirs()) {
      final String msg = "Unable to create the resultdir directory at " + resultDir.getPath() +
                         "." + System.lineSeparator();
      throw new CmdLineException(parser, msg, null);
    }

    // --resultformat must be valid
    try {
      ResultWriterFactory.validate(resultFormat);
    } catch (InvalidResultFormatException e) {
      throw new CmdLineException(parser, e);
    }

    // --parallelTests must be >= 1
    if (parallelTests < 1) {
      final String msg = "The \"--parallel\" parameter must have a value >= 1." +
              System.lineSeparator();
      throw new CmdLineException(parser, msg, null);
    }
  }

}
