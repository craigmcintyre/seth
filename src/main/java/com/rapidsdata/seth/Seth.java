// Copyright (c) 2017 Boray Data Co. Ltd.  All rights reserved.

package com.rapidsdata.seth;

import com.rapidsdata.seth.exceptions.SethSystemException;
import com.rapidsdata.seth.logging.*;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;

import java.io.File;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * The SE Test Harness.
 */
public class Seth {

  /** The parsed arguments to the application. */
  private CommandLineArgs args;

  /**
   * The main entry point for the SE Test Harness
   * @param arguments The arguments provided to the application.
   */
  public static void main(String[] arguments)
  {
    // Parse the command line arguments
    CommandLineArgs args = new CommandLineArgs();
    CmdLineParser parser  = new CmdLineParser(args);

    try {
      parser.parseArgument(arguments);
      args.validateSemantics(parser);

    } catch (CmdLineException e) {
      System.err.println(e.getMessage());
      System.err.println("Command line arguments are:");
      parser.printUsage(System.err);
      System.exit(1);
    }

    Seth seth = new Seth(args);
    seth.run();
  }

  /**
   * Constructor
   * @param args The parsed command line arguments provided to the application.
   */
  public Seth(CommandLineArgs args)
  {
    this.args = args;
  }

  /**
   * The main execution loop of the application.
   */
  private void run()
  {
    // Build a list of test files to run.
    List<File> testFiles = buildTestList(args);

    // Get the JDBC driver we are using.
    Driver driver;
    try {
      driver = DriverManager.getDriver(args.url);

    } catch (SQLException e) {
      final String msg = "Unable to load the JDBC driver for the url \"" + args.url + "\".";
      throw new SethSystemException(msg, e);
    }

    // Clean the result directory if necessary
    if (args.doClean) {
      cleanResultDir(args.resultDir);
    }

    TestLogger logger;

    if (args.logSteps) {
      // This logger logs all test steps to the console and to a file.
      logger = new StepFileLogger(args.resultDir);

    } else if (args.logTests) {
      // This logger logs only messages about whole tests to the console and to a file.
      logger = new TestFileLogger(args.resultDir);

    } else {
      // This logger only logs to the screen.
      logger = new ConsoleLogger();
    }

    // Create the main run context.
    RunContext runContext = new RunContext(testFiles, driver, args.doValidate, args.includesAreRelativeToTest, logger);

    // Run the test suite.

    // Print the results.

    // Exit.
  }


  private List<File> buildTestList(CommandLineArgs args)
  {
    List<File> testFiles = new ArrayList<>();

    // TODO

    return testFiles;
  }

  private void cleanResultDir(File resultDir)
  {
    // TODO
  }
}
