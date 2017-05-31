// Copyright (c) 2017 Boray Data Co. Ltd.  All rights reserved.

package com.rapidsdata.seth;

import com.rapidsdata.seth.contexts.AppContext;
import com.rapidsdata.seth.contexts.AppContextImpl;
import com.rapidsdata.seth.exceptions.InvalidResultFormatException;
import com.rapidsdata.seth.exceptions.SethBrownBagException;
import com.rapidsdata.seth.exceptions.SethSystemException;
import com.rapidsdata.seth.logging.*;
import com.rapidsdata.seth.results.ResultWriter;
import com.rapidsdata.seth.results.ResultWriterFactory;
import org.apache.commons.io.FileUtils;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.ParserProperties;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.management.ManagementFactory;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * The SE Test Harness.
 */
public class Seth {

  /** The parsed arguments to the application. */
  private CommandLineArgs args;

  /** Time the application was started. */
  private long jvmStartTime = ManagementFactory.getRuntimeMXBean().getStartTime();

  /** The thing to log messages to. */
  private TestLogger logger;

  /**
   * The main entry point for the SE Test Harness
   * @param arguments The arguments provided to the application.
   */
  public static void main(String[] arguments)
  {
    // Parse the command line arguments
    CommandLineArgs args = new CommandLineArgs();
    ParserProperties parserProperties = ParserProperties.defaults().withShowDefaults(false).withOptionSorter(null);
    CmdLineParser parser  = new CmdLineParser(args, parserProperties);

    try {
      parser.parseArgument(arguments);
      args.validateSemantics(parser);

    } catch (CmdLineException e) {
      System.err.println(e.getMessage());
      System.err.println("Usage: ./seth.sh <options> [files...]");
      System.err.println("Command line options are:");
      parser.printUsage(System.err);
      System.exit(1);
    }

    // Clean the result directory if necessary.
    // Must do this before creating the logger because otherwise it may create a log file
    // which would duly then be cleaned up.
    if (args.doClean) {
      cleanResultDir(args.resultDir);
    }

    // Create an appropriate logger.
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

    Seth seth = new Seth(args, logger);

    try {
      seth.run();

    } catch (SethBrownBagException e) {
      String msg = getStackTraceFrom(e);
      seth.logger.log(msg, false);
    }
  }

  /**
   * Constructor
   * @param args The parsed command line arguments provided to the application.
   * @param logger the thing to log messages to.
   */
  public Seth(CommandLineArgs args, TestLogger logger)
  {
    this.args = args;
    this.logger = logger;
  }

  /**
   * The main execution loop of the application.
   */
  private void run()
  {
    logStartTime();

    // Build a list of test files to run.
    List<File> testFiles = buildTestList(args);

    if (testFiles.isEmpty()) {
      logger.log("There are no test files to execute!", false);
      return;
    }

    // Get the JDBC driver we are using.
    Driver driver;
    try {
      driver = DriverManager.getDriver(args.url);

    } catch (SQLException e) {
      final String msg = "Unable to load the JDBC driver for the url \"" + args.url + "\".";
      throw new SethSystemException(msg, e);
    }

    ExecutorService threadPool = Executors.newCachedThreadPool();

    // Create the main run context.
    AppContext appContext = new AppContextImpl(jvmStartTime,
                                               testFiles,
                                               args.url,
                                               args.relativity,
                                               logger,
                                               threadPool);

    // Create the ResultWriter
    ResultWriter resultWriter;
    try {
      resultWriter = ResultWriterFactory.get(args, appContext);

    } catch (InvalidResultFormatException e) {
      logger.error(e.getMessage());
      return;
    }

    // Run the test suite.
    TestSuite testSuite = new TestSuite(appContext, resultWriter);
    testSuite.run();

    // Shut down the thread pool we created for running the tests.
    // Wait 5 seconds for it to complete and then force it to shutdown.
    threadPool.shutdown();

    try {
      threadPool.awaitTermination(5, TimeUnit.SECONDS);
    } catch (InterruptedException e) { /*ignore*/ }

    if (!threadPool.isTerminated()) {
      threadPool.shutdownNow();
    }


    // Close the logger.
    try {
      logger.close();
    } catch (IOException e) { /*ignore*/ }

    // All done.
  }


  /**
   * Logs the time the application started.
   */
  private void logStartTime()
  {
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    Calendar cal = Calendar.getInstance();
    cal.setTimeInMillis(jvmStartTime);

    final String msg = "Application started at " + sdf.format(cal.getTime()) + ".";
    logger.log(msg, false);
  }


  /**
   * Builds a list of test files to run and validates that they all exist.
   * @param args
   * @return
   */
  private List<File> buildTestList(CommandLineArgs args)
  {
    List<File> testFiles;

    if (args.listFile != null) {
      // We need to read a file that then contains the
      testFiles = getTestFileListFromListFile(args.listFile, args.relativity);

    } else {
      // We should have a set of test files specified on the command line.
      testFiles = new ArrayList<>(args.testFiles);
    }

    // Validate that each of these files exist. Remove those that do not exist.
    Iterator<File> iterator = testFiles.iterator();

    while (iterator.hasNext()) {
      File testFile = iterator.next();

      if (!testFile.exists()) {
        final String msg = "Test file \"" + testFile.getPath() + "\" does not exist and will not be executed.";
        logger.error(msg);
        iterator.remove();
      }
    }

    return testFiles;
  }

  /**
   * Read the list file, which may contain a test file to execute on each line.
   * @param listFile the list file to get the set of test files from.
   * @param relativity how we deal with relative paths.
   * @return a list of File objects representing the test files to be executed.
   */
  private List<File> getTestFileListFromListFile(File listFile, PathRelativity relativity)
  {
    List<String> lines;

    try {
      lines = Files.readAllLines(listFile.toPath());

    } catch (IOException | SecurityException e) {
      final String msg = "Could not read from the listFile at " + listFile.getPath() + ".";
      throw new SethSystemException(msg, e);
    }

    List<File> files = new ArrayList<File>();

    final String[] lineComments = new String[] {"#", "--", "//"};

    for (String line : lines) {
      line = line.trim();

      // Strip out any rest-of-line-comments from the line.
      for (String comment : lineComments) {

        int index = line.indexOf(comment);

        if (index >= 0) {
          // Remove the rest of the line.
          line = line.substring(0, index);
        }
      }

      line = line.trim();

      if (line.isEmpty()) {
        continue;
      }

      File f = new File(line);

      // If the filename is relative and our path relativity is REFERER then
      // we need to make this filename relative to the path of the listFile
      // that is referring to it.
      if (!f.toPath().isAbsolute() && relativity == PathRelativity.REFERER) {
        String parent = listFile.getParent();

        if (parent == null) {
          parent = "";
        }

        f = Paths.get(parent, f.getPath()).toFile();
      }

      // Is there any file globbing in the file name? Look for globbing characters: * ? { } [ ]

      if (!hasGlobbing(f)) {
        // No globbing. Simply add this file.
        files.add(f);
        continue;
      }

      addGlobbedFiles(f, files);
    }

    return files;
  }

  /**
   * Returns true if the given File path contains globbing characters.
   * @param f the file path to check.
   * @return true if the given File path contains globbing characters.
   */
  private boolean hasGlobbing(File f)
  {
    return f.getPath().matches("^.*[*?{}\\[\\]]+.*$");
  }

  /**
   * Globs the given file path and adds File instances that match the globbing pattern
   * to the list of Files provided.
   * @param f the file with the path that contains globbing characters.
   * @param files the list we wish to add matching files to.
   */
  private void addGlobbedFiles(File f, List<File> files)
  {
    // Expand any file globbing.
    PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + f.getPath());

    Path startPath;

    if (f.getParent() == null) {
      startPath = Paths.get("");
    } else {
      startPath = Paths.get(f.getParent());
    }

    try {
      Files.walkFileTree(startPath, new SimpleFileVisitor<Path>() {
        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
          if (matcher.matches(file)) {
            files.add(file.toFile());
          }
          return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
          return FileVisitResult.CONTINUE;
        }
      });
    } catch (IOException e) {
      throw new SethSystemException(e);
    }
  }

  /**
   * Removes all files and subdirectories from the results directory.
   * @param resultDir the results directory to be cleaned of all contents.
   */
  private static void cleanResultDir(File resultDir)
  {
    try {
      FileUtils.cleanDirectory(resultDir);

    } catch (IOException e) {
      final String msg = "Could not clean the result directory, " + resultDir.getPath() + ".";
      throw new SethSystemException(msg, e);
    }
  }

  /**
   * Returns the stack trace of an exception as a string.
   * @param t the Throwable to get the stack trace of.
   * @return the stack trace of an exception as a string.
   */
  protected static String getStackTraceFrom(Throwable t)
  {
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    t.printStackTrace(pw);
    return sw.toString();
  }
}
