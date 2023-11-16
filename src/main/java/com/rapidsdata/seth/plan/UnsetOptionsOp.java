// Copyright (c) 2017 Boray Data Co. Ltd.  All rights reserved.

package com.rapidsdata.seth.plan;

import com.rapidsdata.seth.Options;
import com.rapidsdata.seth.contexts.ExecutionContext;
import com.rapidsdata.seth.exceptions.FailureException;
import com.rapidsdata.seth.plan.expectedResults.ExpectedResult;

import java.util.List;

public class UnsetOptionsOp extends Operation
{
  private final List<String> keys;

  /**
   * Constructor
   * @param metadata The metadata about where this operation came from in the test file.
   * @param keys the name of the option to be removed from the testOptions.
   */
  public UnsetOptionsOp(OperationMetadata metadata, ExpectedResult expectedResult, List<String> keys)
  {
    super(metadata, expectedResult);

    this.keys = keys;
  }

  /**
   * Constructor
   * @param metadata The metadata about where this operation came from in the test file.
   * @param keys the name of the option to be removed from the testOptions.
   * @param executeImmediately if true then this operation will be executed immediately after parsing
   */
  public UnsetOptionsOp(OperationMetadata metadata, ExpectedResult expectedResult, List<String> keys, boolean executeImmediately)
  {
    super(metadata, expectedResult, executeImmediately);

    this.keys = keys;
  }

  /**
   * Rewrites the current operation with the given expected result.
   * @param expectedResult the expected result to compare to.
   * @return the newly rewritten, immutable Operation with the new expected result.
   */
  @Override
  public Operation rewriteWith(ExpectedResult expectedResult)
  {
    return new UnsetOptionsOp(this.metadata, expectedResult, this.keys, this.executeImmediately);
  }

  /**
   * Executes the operation.
   *
   * @param xContext The execution context, which encapsulates any necessary parameters.
   * @throws FailureException if an error occurs during the execution. e.g., if the
   *                          expected response does not meet the actual response.
   */
  @Override
  public void execute(ExecutionContext xContext) throws FailureException
  {

    Options testOptions = xContext.getTestOptions();

    for (String key : keys) {
      if (testOptions.containsKey(key)) {
        testOptions.remove(key);

      } else {
        expectedResult.assertActualAsFailure(xContext, "No test options set with the key \"" + key + "\".");
      }
    }

    expectedResult.assertActualAsSuccess(xContext, null);
  }
}
