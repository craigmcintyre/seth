// Copyright (c) 2017 Boray Data Co. Ltd.  All rights reserved.

package com.rapidsdata.seth.plan;

import com.rapidsdata.seth.contexts.ExecutionContext;
import com.rapidsdata.seth.exceptions.FailureException;
import com.rapidsdata.seth.exceptions.ValidationException;
import com.rapidsdata.seth.logging.TestLogger;
import com.rapidsdata.seth.plan.expectedResults.ExpectedResult;

import java.util.List;

/**
 * An operation that runs sub-operations in a loop for a certain number of iterations.
 */
public class CountedLoopOp extends Operation
{
  /** The number of loop iterations to run. -1 means forever. */
  private final long loopCount;

  /** The list of operations to run. */
  private final List<Operation> operations;

  /**
   * Constructor
   * @param metadata The metadata about where this operation came from in the test file.
   * @param loopCount The number of loop iterations to run. null means forever.
   * @param operations The list of operations to run.
   */
  public CountedLoopOp(OperationMetadata metadata, ExpectedResult expectedResult, Long loopCount, List<Operation> operations)
  {
    super(metadata, expectedResult);
    this.loopCount = (loopCount == null ? -1 : loopCount);
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
    return new CountedLoopOp(this.metadata, expectedResult, this.loopCount, this.operations);
  }

  /**
   * Validates the operation.
   * This does not execute the operation, but it ensures that the operation is semantically correct.
   * e.g., the statement has the correct expected result, etc.
   * @param xContext The execution context, which encapsulates any necessary parameters.
   * @throws ValidationException if the validation fails.
   */
  @Override
  public void validate(ExecutionContext xContext) throws ValidationException
  {
    // TODO: Validate the expected result here.
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
    long count = 0;

    while (loopCount == -1 || count++ < loopCount) {
      for (Operation op : operations) {
        // Check if a failure occurred in another thread and we have to stop running the test.
        if (!xContext.continueTesting()) {
          return;
        }

        logger.testStepExecuting(op.getTestFile(), op.toString(), op.getLine());

        try {
          op.execute(xContext);

        } catch (FailureException e) {
          xContext.markAsFailed(e);
          return;
        }
      }
    }

    expectedResult.assertActualAsSuccess();
  }
}
