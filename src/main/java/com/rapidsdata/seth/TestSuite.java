// Copyright (c) 2017 Boray Data Co. Ltd.  All rights reserved.

package com.rapidsdata.seth;

import com.rapidsdata.seth.contexts.AppContext;
import com.rapidsdata.seth.contexts.TestContext;
import com.rapidsdata.seth.contexts.TestContextImpl;
import com.rapidsdata.seth.exceptions.*;
import com.rapidsdata.seth.logging.TestLogger;
import com.rapidsdata.seth.plan.Plan;
import com.rapidsdata.seth.plan.TestPlanner;
import com.rapidsdata.seth.plan.annotated.TestAnnotationInfo;
import com.rapidsdata.seth.results.ResultWriter;

import java.io.*;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static com.rapidsdata.seth.TestResult.ResultStatus.NOT_STARTED;

/** The class that runs the batch of test files. */
public class TestSuite
{
  /** The application context, which contains various common bits of information applicable to all tests. */
  private final AppContext appContext;

  /** The object responsible for writing the final test results. */
  private final ResultWriter resultWriter;

  /** Commands to be ignored when running tests in parallel. */
  private static final String[] PARALLEL_IGNORE_CMDS_REGEX = { "trackmemory\\s*;\\s*", "trackmemory\\s+force\\s*;\\s*", "memoryleaks\\s*;\\s*" };

  /**
   * Constructor
   * @param appContext contains various common bits of information applicable to all tests.
   */
  public TestSuite(AppContext appContext , ResultWriter resultWriter )
  {
    this.appContext = appContext;
    this.resultWriter = resultWriter;
  }

  /**
   * Runs all the tests in the test suite.
   */
  public void run()
  {
    // A list of results from running all the tests.
    List<TestResult> resultList = new LinkedList<>();
    List<FutureContext>  futureContextList = new ArrayList<>();

    Plan plan = null;

    TestLogger logger = appContext.getLogger();
    TestContext testContext = null;
    List<TestAnnotationInfo> testsToAnnotate = null;

    if (appContext.getCommandLineArgs().recordResults) {
      testsToAnnotate = new ArrayList<>();
    }

    // Create an executor service with a fixed limit for parallelising the tests.
    int numParallelTests = appContext.getCommandLineArgs().parallelTests;
    ExecutorService threadPool = Executors.newFixedThreadPool(numParallelTests);

    // if we are running tests in parallel then ignore the memoryleaks and trackmemory commands
    if (numParallelTests > 1) {
      Collections.addAll(appContext.getCommandLineArgs().ignoreCommands, PARALLEL_IGNORE_CMDS_REGEX);
      appContext.addIgnorableCommand(Arrays.asList(PARALLEL_IGNORE_CMDS_REGEX));
    }

    try {

      // Iterate each test file
      for (TestableFile testableFile : appContext.getTestableFiles()) {

        String testName = testableFile.getFile().getName();

        if (testableFile.getInstruction() == TestableFile.Instruction.SKIP) {
          logger.testSkipping(testableFile.getFile());
          resultList.add(TestResult.skipped(testableFile.getFile(), testName));
          continue;
        }

        // We need to execute this test file.
        // logger.testExecuting(testableFile.getFile());

        // Make a TestResult to hold the result of the test.
        TestResult testResult = new TestResult(testableFile.getFile(), testName);

        // Save it in the list of results. It will get updated as the test executes.
        resultList.add(testResult);

        // Make a new test context for executing this test.
        testContext = new TestContextImpl(appContext, testableFile.getFile(), testResult);

        // Parse each test file
        try {
          TestPlanner planner = new TestPlanner(testContext);
          plan = planner.newPlanFor(testableFile.getFile(), new ArrayList<File>(), testsToAnnotate);

        } catch (FailureException e) {
          if (testContext.getResult().getStatus() == NOT_STARTED) {
            // This can happen with a failure during parsing
            logger.testExecuting(testContext.getTestFile());
            testContext.markAsStarted();
          }

          testResult.setFailure(e);
          logger.error("\n" + testResult.getFailureDescription());
          continue;

        } catch (FileNotFoundException e) {
          testResult.setFailure(e);
          logger.error(testResult.getFailureDescription());
          continue;

        } catch (PlanningException e) {
          testResult.setFailure(e);
          logger.error(testResult.getFailureDescription());
          continue;

        } catch (Exception e) {
          // Remove this result as it is probably incomplete.
          resultList.remove(testResult);
          throw e;
        }


        // Make a new TestRunner to run the plan
        TestRunner testRunner = new TestRunner(plan, testContext, true);

        // Run each test file asynchronously.
        Future<?> future = threadPool.submit(testRunner);
        FutureContext futureContext = new FutureContext(future, testContext, testResult);
        futureContextList.add(futureContext);

        testContext = null;
      }

      // Wait for all tests to complete
      for (FutureContext futureContext : futureContextList) {
        try {
          futureContext.future.get();

        } catch (ExecutionException e) {
          // Thrown if the execution of a sub-task throws an exception.
          String stackTrace = "Unexpected internal exception encountered -" + System.lineSeparator() +
                  getStackTrace(e);
          logger.error(stackTrace);
          resultList.remove(futureContext.testResult);

        } catch (InterruptedException e) {
          if (futureContext.testContext != null) {
            futureContext.testContext.abortTest();
          }
        }
      }

      // All tests have now finished.

      if (appContext.getCommandLineArgs().recordResults) {
        for (TestAnnotationInfo testToAnnotate : testsToAnnotate) {

          try {
            testToAnnotate.annotate();
          } catch (IOException e) {
            String msg = "Could not annotate test: " + testToAnnotate.getOriginalTestFile().toString() +
                    System.lineSeparator() + getStackTrace(e);
            logger.error(msg);
          }
        }
      }
    } finally {
      // Write out the results
      if (!resultList.isEmpty()) {
        resultWriter.writeResults(resultList);
      }

      threadPool.shutdownNow();
    }
  }


  /**
   * Returns the stack trace of an exception as a string.
   * @param t the Throwable to get the stack trace of.
   * @return the stack trace of an exception as a string.
   */
  protected String getStackTrace(Throwable t)
  {
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    t.printStackTrace(pw);
    return sw.toString();
  }


  private static class FutureContext {
    public Future<?> future;
    public TestContext testContext;
    public TestResult testResult;

    public FutureContext(Future<?> future, TestContext testContext, TestResult testResult) {
      this.future = future;
      this.testContext = testContext;
      this.testResult = testResult;
    }
  }
}
