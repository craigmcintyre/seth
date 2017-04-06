// Copyright (c) 2017 Boray Data Co. Ltd.  All rights reserved.

package com.rapidsdata.seth;

import com.rapidsdata.seth.contexts.AppContext;
import com.rapidsdata.seth.contexts.TestContext;
import com.rapidsdata.seth.contexts.TestContextImpl;
import com.rapidsdata.seth.exceptions.SyntaxException;
import com.rapidsdata.seth.logging.TestLogger;
import com.rapidsdata.seth.plan.Plan;
import com.rapidsdata.seth.plan.TestPlanner;

import java.io.File;
import java.io.FileNotFoundException;
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

  /** The object responsible for writing
  //private final ResultWriter resultWriter;

  /**
   * Constructor
   * @param appContext contains various common bits of information applicable to all tests.
   */
  public TestSuite(AppContext appContext /*, ResultWriter resultWriter */)
  {
    this.appContext = appContext;
    // this.resultWriter = resultWriter;
  }

  /**
   * Runs all the tests in the test suite.
   */
  public void run()
  {
    // A list of results from running all the tests.
    List<TestResult> resultList = new LinkedList<>();

    TestPlanner planner = new TestPlanner();
    Plan plan = null;

    ExecutorService threadPool = appContext.getThreadPool();
    TestLogger logger = appContext.getLogger();

    try {

      // Iterate each test file
      for (File testFile : appContext.getTestFiles()) {
        logger.testExecuting(testFile);

        // Make a TestResult to hold the result of the test.
        TestResult testResult = new TestResult(testFile);

        // Save it in the list of results. It will get updated as the test executes.
        resultList.add(testResult);

        // Parse each test file
        try {
          plan = planner.newPlanFor(testFile);

        } catch (FileNotFoundException e) {
          testResult.setFailure(e);
          logger.error(testResult.getFailureDescription());
          continue;

        } catch (SyntaxException e) {
          testResult.setFailure(e);
          logger.error(testResult.getFailureDescription());
          continue;
        }

        // Make a new test context for executing this test.
        TestContext testContext = new TestContextImpl(appContext, testFile, testResult);

        // Make a new TestRunner to run the plan
        TestRunner testRunner = new TestRunner(plan, testContext, true);

        // Run each test file and wait until it finishes.
        Future<?> future = threadPool.submit(testRunner);
        future.get();

        // Log the result of each test file if it didn't succeed.
        if (testResult.getStatus() != TestResult.ResultStatus.SUCCEEDED) {
          logger.error(testResult.getFailureDescription());
        }

      }
    } catch (ExecutionException e) {
      // TODO: Thrown if the execution of a sub-task throws an exception.
      // Can we catch a SethSystemException with this?

    } catch (InterruptedException e) {
      // TODO: cause test to shutdown. Abort remaining tests.

    } finally {
      // TODO: Write out the results

    }
  }
}
