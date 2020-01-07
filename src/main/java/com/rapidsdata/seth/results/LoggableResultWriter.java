// Copyright (c) 2017 Boray Data Co. Ltd.  All rights reserved.

package com.rapidsdata.seth.results;

import com.rapidsdata.seth.TestResult;
import com.rapidsdata.seth.contexts.AppContext;

import java.util.List;

/** A class that writes human readable test results to the log. */
public class LoggableResultWriter implements ResultWriter
{
  /** Common information and settings of the application. */
  protected final AppContext context;

  /**
   * Constructor
   * @param context Common information and settings of the application.
   */
  public LoggableResultWriter(AppContext context)
  {
    this.context = context;
  }


  /**
   * Write out the complete test results to file and the test log.
   *
   * @param results the list of results of each individual test.
   */
  @Override
  public void writeResults(List<TestResult> results)
  {
    ResultSummary summary = summmariseResults(results);

    writeSummary(summary);
  }

  /**
   * Summarise the results into high level metrics.
   * @param results the list of test results to summarise.
   * @return the result summary.
   */
  protected ResultSummary summmariseResults(List<TestResult> results)
  {
    return ResultSummary.summariseFrom(results);
  }

  /**
   * Write the test summary to the test log.
   * @param summary the summary of the results.
   */
  protected void writeSummary(ResultSummary summary)
  {
    // Write them to the logger.
    context.getLogger().log(summary.toString(), false);
  }
}
