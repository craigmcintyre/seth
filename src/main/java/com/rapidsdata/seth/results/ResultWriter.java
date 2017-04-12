// Copyright (c) 2017 Boray Data Co. Ltd.  All rights reserved.

package com.rapidsdata.seth.results;

import com.rapidsdata.seth.TestResult;

import java.util.List;

public interface ResultWriter
{
  /**
   * Write out the complete test results to file and the test log.
   * @param results the list of results of each individual test.
   */
  public void writeResults(List<TestResult> results);
}
