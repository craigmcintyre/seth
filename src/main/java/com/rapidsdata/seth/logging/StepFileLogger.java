// Copyright (c) 2017 Boray Data Co. Ltd.  All rights reserved.

package com.rapidsdata.seth.logging;

import com.rapidsdata.seth.TestableFile;

import java.io.File;

public class StepFileLogger extends TestFileLogger
{
  /**
   * Constructor
   *
   * @param parentDir the directory that will contain the log file.
   */
  public StepFileLogger(File parentDir, boolean logTestsPassed)
  {
    super(parentDir, logTestsPassed);
  }

  /**
   * Logs that the a given test step is currently being executed.
   *
   * @param testFile the path of the test being executed.
   * @param command  the test step command being executed.
   * @param lineNum  the line number of the command in the test file.
   */
  @Override
  public void testStepExecuting(TestableFile testFile, String command, long lineNum)
  {
    // Ensure we write to the console.
    super.testStepExecuting(testFile, command, lineNum);

    String msg = String.format("Executing line #%03d : %s", lineNum, command);
    queue.add(decorateMessage(msg));
  }
}
