// Copyright (c) 2017 Boray Data Co. Ltd.  All rights reserved.

package com.rapidsdata.seth.plan;

import com.rapidsdata.seth.Options;
import com.rapidsdata.seth.contexts.ExecutionContext;
import com.rapidsdata.seth.exceptions.FailureException;
import com.rapidsdata.seth.plan.expectedResults.ExpectedResult;

public class SetOptionsOp extends Operation
{
  private final Options options;

  /**
   * Constructor
   * @param metadata The metadata about where this operation came from in the test file.
   * @param options the keys and values of the options to be set.
   */
  public SetOptionsOp(OperationMetadata metadata, ExpectedResult expectedResult, Options options)
  {
    super(metadata, expectedResult);

    this.options = options;
  }

  /**
   * Rewrites the current operation with the given expected result.
   * @param expectedResult the expected result to compare to.
   * @return the newly rewritten, immutable Operation with the new expected result.
   */
  @Override
  public Operation rewriteWith(ExpectedResult expectedResult)
  {
    return new SetOptionsOp(this.metadata, expectedResult, this.options);
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
    testOptions.putAll(options);

    expectedResult.assertActualAsSuccess(xContext, null);
  }
}
