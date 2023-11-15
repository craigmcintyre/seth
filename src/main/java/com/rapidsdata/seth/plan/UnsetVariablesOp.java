// Copyright (c) 2017 Boray Data Co. Ltd.  All rights reserved.

package com.rapidsdata.seth.plan;

import com.rapidsdata.seth.Options;
import com.rapidsdata.seth.contexts.ExecutionContext;
import com.rapidsdata.seth.exceptions.FailureException;
import com.rapidsdata.seth.plan.expectedResults.ExpectedResult;

import java.util.List;
import java.util.Map;

public class UnsetVariablesOp extends Operation
{
  private final List<String> varNames;

  /**
   * Constructor
   * @param metadata The metadata about where this operation came from in the test file.
   * @param varNames the name of the variables to be removed from the variables map.
   */
  public UnsetVariablesOp(OperationMetadata metadata, ExpectedResult expectedResult, List<String> varNames)
  {
    super(metadata, expectedResult, true);

    this.varNames = varNames;
  }

  /**
   * Rewrites the current operation with the given expected result.
   * @param expectedResult the expected result to compare to.
   * @return the newly rewritten, immutable Operation with the new expected result.
   */
  @Override
  public Operation rewriteWith(ExpectedResult expectedResult)
  {
    return new UnsetVariablesOp(this.metadata, expectedResult, this.varNames);
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

    for (String varName : varNames) {
      String result = variablesMap.remove(varName);

      if (result == null) {
        expectedResult.assertActualAsFailure(xContext, "No test variable with the name \"" + varName + "\".");
      }
    }

    expectedResult.assertActualAsSuccess(xContext, null);
  }
}
