// Copyright (c) 2019 Boray Data Co. Ltd.  All rights reserved.

package com.rapidsdata.seth.plan;

import com.rapidsdata.seth.contexts.ExecutionContext;
import com.rapidsdata.seth.exceptions.FailureException;
import com.rapidsdata.seth.logging.TestLogger;
import com.rapidsdata.seth.plan.expectedResults.ExpectedResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * An operation that runs its sub-operations in a randomised order.
 * All operations are run exactly once when this operator is executed.
 */
public class ShuffleOp extends Operation
{
  /** The list of operations to run. */
  private final List<Operation> operations;

  /**
   * Constructor
   * @param metadata The metadata about where this operation came from in the test file.
   * @param operations The list of operations to run.
   */
  public ShuffleOp(OperationMetadata metadata, ExpectedResult expectedResult, List<Operation> operations)
  {
    super(metadata, expectedResult);
    this.operations = operations;
  }

  /**
   * Rewrites the current operation with the given expected result.
   * @param expectedResult the expected result to compare to.
   * @return the newly rewritten, immutable Operation with the new expected result.
   */
  @Override
  public Operation rewriteWith(ExpectedResult expectedResult)
  {
    return new ShuffleOp(this.metadata, expectedResult, this.operations);
  }

  /**
   * Executes the operation.
   * @param xContext The execution context, which encapsulates any necessary parameters.
   * @throws FailureException if an error occurs during the execution. e.g., if the
   *                          expected response does not meet the actual response.
   */
  @Override
  public void execute(ExecutionContext xContext) throws FailureException
  {
    final TestLogger logger = xContext.getLogger();
    long statementCount = 0;

    // Randomise the statements
    List<Operation> randomisedList = new ArrayList<>(operations);
    Collections.shuffle(randomisedList);

    try {
      for (Operation op : randomisedList) {

        // Check if a failure occurred in another thread and we have to stop running the test.
        if (!xContext.continueTesting()) {
          return;
        }

        logger.testStepExecuting(op.getTestFile(), op.toString(), op.getLine());
        ++statementCount;

        try {
          op.execute(xContext);

        } catch (FailureException e) {
          xContext.markAsFailed(e);
          return;
        }
      }

    expectedResult.assertActualAsSuccess(xContext, null);

    } finally {
      // Accumulate the number of operation steps we executed.
      xContext.accumulateTestSteps(statementCount);
    }
  }
}
