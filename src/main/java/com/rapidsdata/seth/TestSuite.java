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
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/** The class that runs the batch of test files. */
public class TestSuite
{
  /** The application context, which contains various common bits of information applicable to all tests. */
  private final AppContext appContext;

  /** The object responsible for writing the final test results. */
  private final ResultWriter resultWriter;

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

    TestPlanner planner = new TestPlanner(appContext);
    Plan plan = null;

    ExecutorService threadPool = appContext.getThreadPool();
    TestLogger logger = appContext.getLogger();

    TestContext testContext = null;

    List<TestAnnotationInfo> testsToAnnotate = null;

    if (appContext.getCommandLineArgs().recordResults) {
      testsToAnnotate = new ArrayList<>();
    }

    try {

      // Iterate each test file
      for (File testFile : appContext.getTestFiles()) {
        logger.testExecuting(testFile);

        String testName = testFile.getName() + appContext.getCommandLineArgs().testSuffix;

        // Make a TestResult to hold the result of the test.
        TestResult testResult = new TestResult(testFile, testName);

        // Save it in the list of results. It will get updated as the test executes.
        resultList.add(testResult);

        // Parse each test file
        try {
          plan = planner.newPlanFor(testFile, new ArrayList<File>(), testsToAnnotate);

        } catch (FileNotFoundException e) {
          testResult.setFailure(e);
          logger.error(testResult.getFailureDescription());
          continue;

        } catch (PlanningException e) {
          testResult.setFailure(e);
          logger.error(testResult.getFailureDescription());
          continue;

        } catch (Exception e) {
          // Remove the last result as it is probably incomplete.
          if (resultList.size() > 0) {
            resultList.remove(resultList.size() - 1);
          }
          throw e;
        }

        // Make a new test context for executing this test.
        testContext = new TestContextImpl(appContext, testFile, testResult);

        // Make a new TestRunner to run the plan
        TestRunner testRunner = new TestRunner(plan, testContext, true);

        // Run each test file asynchronously.
        Future<?> future = threadPool.submit(testRunner);

        // Wait until the test finishes
        future.get();

        // Log the result of each test file
        logger.testExecutionFinished(testFile, testResult);

        testContext = null;
      }

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

    } catch (ExecutionException e) {
      // Thrown if the execution of a sub-task throws an exception.
      String stackTrace = "Unexpected internal exception encountered -" + System.lineSeparator() +
                          getStackTrace(e);
      logger.error(stackTrace);
      resultList.remove(resultList.size() - 1);

    } catch (InterruptedException e) {
      if (testContext != null) {
        testContext.abortTest();
      }

    } finally {
      // Write out the results
      if (!resultList.isEmpty()) {
        resultWriter.writeResults(resultList);
      }
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
}
