// Copyright (c) 2017 Boray Data Co. Ltd.  All rights reserved.

package com.rapidsdata.seth.exceptions;

import com.rapidsdata.seth.plan.OperationMetadata;
import com.rapidsdata.seth.plan.expectedResults.ExpectedResult;

import java.io.File;

/** A test failure resulting from the fact that the actual result didn't match the expected result. */
public class ExpectedResultFailureException extends FailureException
{
  /** A description of the actual result encountered. */
  protected final String actualResultDesc;

  /** The expected result we should have got. */
  protected final ExpectedResult expectedResult;

  public ExpectedResultFailureException(OperationMetadata opMetadata,
                                        String actualResultDesc,
                                        ExpectedResult expectedResult)
  {
    super("Actual result does not compare to the expected result.", opMetadata.getTestFile(),
          opMetadata.getLine(), opMetadata.getDescription());

    this.actualResultDesc = actualResultDesc;
    this.expectedResult = expectedResult;
  }

  public ExpectedResultFailureException(OperationMetadata opMetadata,
                                        String actualResultDesc,
                                        ExpectedResult expectedResult,
                                        Throwable throwable)
  {
    super("Actual result does not compare to the expected result.", throwable,
        opMetadata.getTestFile(), opMetadata.getLine(), opMetadata.getDescription());

    this.actualResultDesc = actualResultDesc;
    this.expectedResult = expectedResult;
  }

  /**
   * Return a description of the failure, with option descriptions of where it failed and what
   * was being executed.
   * @return a description of the failure.
   */
  @Override
  public String getMessage()
  {
    return getMessage(null);
  }

  /**
   * Return a description of the failure, with option descriptions of where it failed and what
   * was being executed.
   * @param outerTestFile the path of the outer-most test file. If this equals the test file that
   *                      had the error then we won't reprint the test file path.
   * @return a description of the failure.
   */
  @Override
  public String getMessage(File outerTestFile)
  {
    StringBuilder sb = formatMessage(outerTestFile);

    if (sb.length() > 0) {
      sb.append(System.lineSeparator());
    }

    sb.append(EXPECTED_HEADING);
    sb.append(expectedResult.describe());

    sb.append(System.lineSeparator());
    sb.append(ACTUAL_HEADING);
    sb.append(actualResultDesc);


    if (getCause() != null) {
      if (sb.length() > 0) {
        sb.append(System.lineSeparator());
      }

      sb.append(STACK_HEADING)
        .append(indent(getStackTrace(getCause())));
    }

    return sb.toString();
  }
}
