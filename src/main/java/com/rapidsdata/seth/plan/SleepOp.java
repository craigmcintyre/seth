// Copyright (c) 2017 Boray Data Co. Ltd.  All rights reserved.

package com.rapidsdata.seth.plan;

import com.rapidsdata.seth.contexts.ExecutionContext;
import com.rapidsdata.seth.exceptions.FailureException;
import com.rapidsdata.seth.exceptions.ValidationException;
import com.rapidsdata.seth.plan.expectedResults.ExpectedResult;

public class SleepOp extends Operation
{
  /** How long to sleep for, in milliseconds. */
  private final long millis;

  /**
   * Constructor
   * @param metadata The metadata about where this operation came from in the test file.
   * @param millis How long to sleep for, in milliseconds.
   */
  public SleepOp(OperationMetadata metadata, ExpectedResult expectedResult, long millis)
  {
    super(metadata, expectedResult);

    this.millis = millis;
  }

  /**
   * Rewrites the current operation with the given expected result.
   * @param expectedResult the expected result to compare to.
   * @return the newly rewritten, immutable Operation with the new expected result.
   */
  @Override
  public Operation rewriteWith(ExpectedResult expectedResult)
  {
    return new SleepOp(this.metadata, expectedResult, this.millis);
  }

  /**
   * Validates the operation.
   * This does not execute the operation, but it ensures that the operation is semantically correct.
   * e.g., the statement has the correct expected result, etc.
   *
   * @param xContext The execution context, which encapsulates any necessary parameters.
   * @throws ValidationException if the validation fails.
   */
  @Override
  public void validate(ExecutionContext xContext) throws ValidationException
  {
    // TODO: Validate the expected result type.
    // TODO: Validate that the millis is >= 0
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
    // We only want to sleep for a short period at a time because another thread may
    // have a failure and cause the test to end early. And we don't want to be waiting
    // for this operation to complete.

    final long defaultSleepChunkMs = 200; // sleep this long at a time.
    long sleepChunk = defaultSleepChunkMs;

    long endTime = System.currentTimeMillis() + millis;
    long remaining = millis;

    while (remaining > 0) {

      if (!xContext.continueTesting() && metadata.getPhase() == TestPhase.TEST) {
        break;
      }

      if (remaining < sleepChunk) {
        sleepChunk = remaining;
      }

      try {
        Thread.sleep(sleepChunk);

      } catch (InterruptedException e) {
        // Ignore for now. We need to wait until we are told to stop testing.
      }

      remaining = endTime - System.currentTimeMillis();
    }
  }
}
