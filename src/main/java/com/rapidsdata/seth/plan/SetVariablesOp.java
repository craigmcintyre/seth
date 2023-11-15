// Copyright (c) 2017 Boray Data Co. Ltd.  All rights reserved.

package com.rapidsdata.seth.plan;

import com.rapidsdata.seth.contexts.ExecutionContext;
import com.rapidsdata.seth.exceptions.FailureException;
import com.rapidsdata.seth.plan.expectedResults.ExpectedResult;

import java.util.Map;

public class SetVariablesOp extends Operation
{
  private final Map<String,String> newVars;

  /**
   * Constructor
   * @param metadata The metadata about where this operation came from in the test file.
   * @param newVars the keys and values of the variables to be set.
   */
  public SetVariablesOp(OperationMetadata metadata, ExpectedResult expectedResult, Map<String,String> newVars)
  {
    super(metadata, expectedResult, true);

    this.newVars = newVars;
  }

  /**
   * Rewrites the current operation with the given expected result.
   * @param expectedResult the expected result to compare to.
   * @return the newly rewritten, immutable Operation with the new expected result.
   */
  @Override
  public Operation rewriteWith(ExpectedResult expectedResult)
  {
    return new SetVariablesOp(this.metadata, expectedResult, this.newVars);
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

    Map<String,String> variablesMap = xContext.getVariables();
    variablesMap.putAll(newVars);

    expectedResult.assertActualAsSuccess(xContext, null);
  }
}
