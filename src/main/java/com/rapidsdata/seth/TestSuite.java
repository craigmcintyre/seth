// Copyright (c) 2017 Boray Data Co. Ltd.  All rights reserved.

package com.rapidsdata.seth;

import com.rapidsdata.seth.contexts.AppContext;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

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
    List<TestResult> resultList = new LinkedList<>();

    try {

      // Iterate each test file
      for (File testFile : appContext.getTestFiles()) {

        // Parse each test file

        // Run each test file

        // Save the result

        // Log the result of each test file

      }
    } catch (InterruptedException e) {

    } finally {
      // TODO: Write out the results

    }
  }
}
