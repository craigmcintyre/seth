// Copyright (c) 2017 Boray Data Co. Ltd.  All rights reserved.

package com.rapidsdata.seth;

import com.rapidsdata.seth.contexts.AppContext;
import com.rapidsdata.seth.contexts.AppContextImpl;
import com.rapidsdata.seth.exceptions.InvalidResultFormatException;
import com.rapidsdata.seth.exceptions.PlanningException;
import com.rapidsdata.seth.exceptions.SethBrownBagException;
import com.rapidsdata.seth.exceptions.SethSystemException;
import com.rapidsdata.seth.logging.*;
import com.rapidsdata.seth.parser.SethLexer;
import com.rapidsdata.seth.parser.SethParser;
import com.rapidsdata.seth.plan.TestPlanGenerator;
import com.rapidsdata.seth.plan.TestPlanner;
import com.rapidsdata.seth.results.ResultWriter;
import com.rapidsdata.seth.results.ResultWriterFactory;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.antlr.v4.runtime.tree.ParseTree;
import org.apache.commons.io.FileUtils;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.ParserProperties;

import java.io.*;
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

/**
 * The SE Test Harness.
 */
public class Seth {

  /** The parsed arguments to the application. */
  private CommandLineArgs args;

  /** Time the application was started, in nanoseconds. */
  private long jvmStartTime = System.currentTimeMillis();

  /** The thing to log messages to. */
  private TestLogger logger;

  private static final int minimumJavaVer = 11;

  /**
   * The main entry point for the SE Test Harness
   * @param arguments The arguments provided to the application.
   */
  public static void main(String[] arguments)
  {
    // Check Java version meets minimum
    int currentJavaVer = getCurrentJavaVer();
    if (currentJavaVer < minimumJavaVer) {
      System.err.println("Seth requires Java >= " + minimumJavaVer +
                         ". The current Java version is " + currentJavaVer);
      System.exit(1);
    }

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

    boolean logTestsPassed = (args.parallelTests > 1);

    if (args.logSteps) {
      // This logger logs all test steps to the console and to a file.
      logger = new StepFileLogger(args.resultDir, logTestsPassed);

    } else if (args.logTests) {
      // This logger logs only messages about whole tests to the console and to a file.
      logger = new TestFileLogger(args.resultDir, logTestsPassed);

    } else {
      // This logger only logs to the screen.
      logger = new ConsoleLogger(logTestsPassed);
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

    // Build a list of test files to run and test files to be skipped.
    List<TestableFile> testableFiles = getTestableFiles(args);

    if (testableFiles.isEmpty()) {
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

    // Extract any command line arguments.
    Options appOptions = new Options();

    if (args.opts != null) {
      extractAppOptions(appOptions, args.opts);
    }

    // Create the main run context.
    AppContext appContext = new AppContextImpl(jvmStartTime,
                                               args,
                                               testableFiles,
                                               args.url,
                                               args.relativity,
                                               logger,
                                               threadPool,
                                               appOptions);

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
  private List<TestableFile> getTestableFiles(CommandLineArgs args)
  {
    List<TestableFile> testableFiles;

    if (args.listFile != null) {
      // We need to read a file that then contains the
      testableFiles = getTestableFilesFromListFile(args.listFile, args.relativity, TestableFile.Instruction.EXECUTE);

    } else {
      // We should have a set of test files specified on the command line.
      testableFiles = TestableFile.listOf(args.testFiles, TestableFile.Instruction.EXECUTE);
    }

    // Validate that each of these files exist. Remove those that do not exist.
    Iterator<TestableFile> iterator = testableFiles.iterator();

    while (iterator.hasNext()) {
      TestableFile testableFile = iterator.next();

      if (testableFile.getInstruction() == TestableFile.Instruction.EXECUTE &&
          !testableFile.getFile().exists()) {
        final String msg = "Test file \"" + testableFile.getFile().getPath() + "\" does not exist and will not be executed.";
        logger.error(msg);
        testableFile.setInstruction(TestableFile.Instruction.SKIP);
      }
    }

    return testableFiles;
  }

  /**
   * Read the list file, which may contain a test file to execute on each line.
   * @param listFile the list file to get the set of test files from.
   * @param relativity how we deal with relative paths.
   * @param defaultInstruction what instruction to use by default for each test or testlist encountered.
   * @return a list of File objects representing the test files to be executed.
   */
  private List<TestableFile> getTestableFilesFromListFile(File listFile, PathRelativity relativity, TestableFile.Instruction defaultInstruction)
  {
    List<String> lines;

    try {
      lines = Files.readAllLines(listFile.toPath());

    } catch (NoSuchFileException e) {
      System.err.println("No such testlist file: " + listFile.toString());
      return new ArrayList<TestableFile>();

    } catch (IOException | SecurityException e) {
      final String msg = "Could not read from the listFile at " + listFile.getPath() + ".";
      throw new SethSystemException(msg, e);
    }

    List<TestableFile> files = new ArrayList<TestableFile>();

    final String[] lineComments = new String[] {"#", "--", "//"};

    for (String line : lines) {
      TestableFile.Instruction instruction = defaultInstruction;

      line = line.trim();

      // Strip out any rest-of-line-comments from the line.
      for (int i = 0; i < lineComments.length; i++) {
        String comment = lineComments[i];
        int index = line.indexOf(comment);

        if (index == 0) {
          // A test file (or testlist file) that is commented out is treated as a skipped test
          // (or a skipped set of tests in the case of a testlist file).
          instruction = TestableFile.Instruction.SKIP;

          // Remove the comment and keep processing.
          line = line.substring(comment.length()).trim();

          // Check again for any more of these comments on the line.
          --i;

        } else if (index > 0) {
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
        if (instruction == TestableFile.Instruction.SKIP && !f.exists()) {
          // Treat it as a general comment in the file and ignore it.
          continue;
        }

        if (isTestFile(f)) {
          // No globbing. Simply add this file.
          files.add(new TestableFile(f, instruction));

        } else {
          // Must be a list file. Recurse and process this list file.
          files.addAll(getTestableFilesFromListFile(f, relativity, instruction));
        }

        continue;
      }

      addGlobbedFiles(f, files, relativity, instruction);
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
   * @param defaultInstruction what instruction to use by default for each test or testlist encountered.
   */
  private void addGlobbedFiles(File f, List<TestableFile> files, PathRelativity relativity, TestableFile.Instruction defaultInstruction)
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

            if (isTestFile(file.toFile())) {
              files.add(new TestableFile(file.toFile(), defaultInstruction));

            } else {
              // Must be a list file. Recurse and process this list file.
              files.addAll(getTestableFilesFromListFile(file.toFile(), relativity, defaultInstruction));
            }
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
   * Returns true if the specified file is a test file, otherwise false.
   * @param file the file to test.
   * @return true if the specified file is a test file, otherwise false.
   */
  private boolean isTestFile(File file)
  {
    // TODO: this could be done better, perhaps by inspecting the contents of the file.
    return file.getName().toLowerCase().trim().endsWith(".test");
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

  /**
   * Use ANTLR to parse and extract all of the options specified on the command line.
   * @param appOptions the Options object to add all of the options to.
   * @param cmdLineOpts the string containing the command line options.
   * @throws PlanningException
   */
  private void extractAppOptions(Options appOptions, String cmdLineOpts)
  {
    if (cmdLineOpts.trim().isEmpty()) {
      return;
    }

    SethLexer lexer = new SethLexer(new ANTLRInputStream(cmdLineOpts));
    SethParser parser = new SethParser(new CommonTokenStream(lexer));

    parser.setErrorHandler(new DefaultErrorStrategy() {
      @Override public void recover(Parser recognizer, RecognitionException e) { bail(); }
      @Override public void sync(Parser recognizer) { }

      @Override public Token recoverInline(Parser recognizer) {
        InputMismatchException e = new InputMismatchException(recognizer);
        Token t = e.getOffendingToken();
        String near = getTokenErrorDisplay(t);
        String expected = e.getExpectedTokens().toString(recognizer.getTokenNames());

        String msg = String.format("Syntax error in option arguments near %s; expected %s", near, expected);
        throw new IllegalArgumentException(msg);
      }

      public void bail() { throw new IllegalArgumentException("Invalid options specified."); }
    });

    ParseTree tree;
    Options options;

    try {
      tree = parser.optionList();

      TestPlanGenerator generator = new TestPlanGenerator(parser, null, null, null, null);
      options = generator.generateOptionsFor(tree);

    } catch (SethBrownBagException e) {
      if (e.getCause() instanceof PlanningException) {
        throw new IllegalArgumentException("Invalid options specified", e.getCause());

      } else {
        throw new SethSystemException("Unhandled exception " + e.getClass().getSimpleName(), e.getCause());
      }
    }

    if (options != null) {
      appOptions.putAll(options);
    }
  }

  private static int getCurrentJavaVer()
  {
    String version = System.getProperty("java.version");

    if(version.startsWith("1.")) {
      version = version.substring(2, 3);

    } else {
      int dot = version.indexOf(".");

      if(dot != -1) {
        version = version.substring(0, dot);
      }
    }

    return Integer.parseInt(version);
  }
}
