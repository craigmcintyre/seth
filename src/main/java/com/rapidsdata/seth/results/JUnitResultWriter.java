// Copyright (c) 2017 Boray Data Co. Ltd.  All rights reserved.

package com.rapidsdata.seth.results;

import com.rapidsdata.seth.TestResult;
import com.rapidsdata.seth.contexts.AppContext;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/** A class that writes JUnit-parsable XML results of the tests to a file. */
public class JUnitResultWriter extends LoggableResultWriter
{
  protected final Path resultPath;

  /**
   * Constructor
   * @param context Common information and settings of the application.
   */
  public JUnitResultWriter(AppContext context, File resultDir, String resultName)
  {
    super(context);
    resultPath = Paths.get(resultDir.getPath(), resultName);
  }

  /**
   * Write out the complete test results to file and the test log.
   * @param results the list of results of each individual test.
   */
  @Override
  public void writeResults(List<TestResult> results)
  {
    ResultSummary summary = summmariseResults(results);

    writeSummary(summary);
  }

  /**
   * Write the test summary to the test log.
   * @param summary the summary of the results.
   */
  protected void writeSummary(ResultSummary summary)
  {
    super.writeSummary(summary);

    // TODO: Write to file.
  }
}
