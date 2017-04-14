// Copyright (c) 2017 Boray Data Co. Ltd.  All rights reserved.

package com.rapidsdata.seth.plan;

import com.rapidsdata.seth.TestRunner;
import com.rapidsdata.seth.contexts.ExecutionContext;
import com.rapidsdata.seth.exceptions.FailureException;
import com.rapidsdata.seth.exceptions.SethSystemException;
import com.rapidsdata.seth.exceptions.ValidationException;
import com.rapidsdata.seth.plan.expectedResults.ExpectedResult;

import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;

public class CreateThreadOp extends Operation
{
  /** The number of threads to create. */
  private final int numThreads;

  /** The plan that each of the threads will execute. */
  private final Plan subPlan;

  /**
   * Constructor
   * @param metadata The metadata about where this operation came from in the test file.
   * @param numThreads The number of threads to create.
   * @param subPlan The plan that each of the threads will execute.
   */
  public CreateThreadOp(OperationMetadata metadata, ExpectedResult expectedResult, int numThreads, Plan subPlan)
  {
    super(metadata, expectedResult);
    this.numThreads = numThreads;
    this.subPlan = subPlan;
  }

  /**
   * Rewrites the current operation with the given expected result.
   * @param expectedResult the expected result to compare to.
   * @return the newly rewritten, immutable Operation with the new expected result.
   */
  @Override
  public Operation rewriteWith(ExpectedResult expectedResult)
  {
    return new CreateThreadOp(this.metadata, expectedResult, this.numThreads, this.subPlan);
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
    for (int i = 0; i < numThreads; i++) {
      // Create the TestRunner that executes all the operations of the subplan.
      TestRunner runner = new TestRunner(subPlan, xContext);

      // Launch it!
      Future<?> future;

      try {
        future = xContext.getThreadPool().submit(runner);

      } catch (RejectedExecutionException e) {
        expectedResult.compareActualAsException(e);

        // Since the above call returned, we must have expected this failure otherwise
        // an exception would have been thrown. Job done.
        return;

      } catch (NullPointerException e) {
        throw new SethSystemException(e);
      }

      // Save the future so that our own TestRunner will wait for this child thread to complete.
      xContext.registerFuture(future);
    }

    expectedResult.compareActualAsSuccess();
  }
}
