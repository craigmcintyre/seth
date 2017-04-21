// Copyright (c) 2017 Boray Data Co. Ltd.  All rights reserved.

package com.rapidsdata.seth.plan;

import com.rapidsdata.seth.contexts.ExecutionContext;
import com.rapidsdata.seth.exceptions.FailureException;
import com.rapidsdata.seth.exceptions.ValidationException;
import com.rapidsdata.seth.logging.TestLogger;
import com.rapidsdata.seth.plan.expectedResults.ExpectedResult;

import java.time.Duration;
import java.util.List;

/**
 * An operation that runs sub-operations in a loop for a given period of time.
 */
public class TimedLoopOp extends Operation
{
  /** How long to run the loop for, in milliseconds. */
  private final long durationMs;

  /** The list of operations to run. */
  private final List<Operation> operations;

  /**
   * Constructor
   * @param metadata The metadata about where this operation came from in the test file.
   * @param durationMs How long to run the loop for, in milliseconds.
   * @param operations The list of operations to run.
   */
  public TimedLoopOp(OperationMetadata metadata, ExpectedResult expectedResult, long durationMs, List<Operation> operations)
  {
    super(metadata, expectedResult);
    this.durationMs = durationMs;
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
    return new TimedLoopOp(this.metadata, expectedResult, this.durationMs, this.operations);
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

    final long endTime = System.currentTimeMillis() + durationMs;

    while (System.currentTimeMillis() < endTime) {
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
