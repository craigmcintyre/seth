// Copyright (c) 2017 Boray Data Co. Ltd.  All rights reserved.

package com.rapidsdata.seth.plan;

import com.rapidsdata.seth.contexts.ExecutionContext;
import com.rapidsdata.seth.exceptions.FailureException;
import com.rapidsdata.seth.exceptions.ValidationException;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class SyncOp extends Operation
{
  /** The name of the default synchronisation object. */
  private static final String DEFAULT_SYNC_NAME = "default";

  /** The name of the synchronisation object to use. */
  private final String name;

  /** The number of threads to wait on, or -1 if all currently active threads. */
  private final int count;

  /**
   * Constructor
   * @param metadata The metadata about where this operation came from in the test file.
   * @param name The name of the synchronisation object to use.
   * @param count The number of threads to wait on, or -1 if all currently active threads.
   */
  public SyncOp(OperationMetadata metadata, String name, int count)
  {
    super(metadata);

    if (name == null) {
      this.name = DEFAULT_SYNC_NAME;
    } else {
      this.name = name;
    }

    this.count = count;
  }

  /**
   * Validates the operation.
   * This does not execute the operation, but it ensures that the operation is semantically correct.
   * e.g., an INCLUDE statement can find the file it is including, the statement has the correct
   * expected result, etc.
   *
   * @param xContext The execution context, which encapsulates any necessary parameters.
   * @throws ValidationException if the validation fails.
   */
  @Override
  public void validate(ExecutionContext xContext) throws ValidationException
  {
    // TODO: Validate the expected result type.
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
    int parties = count;

    if (parties == -1) {
      parties = xContext.getNumActiveThreads();
    }

    CyclicBarrier barrier = xContext.getOrCreateSyncObject(name, parties);

    // Now we can wait for all threads to join us.
    while (true) {

      // A synchronise in the test phase can be broken by the end of test or
      // a test failure, however a synchronise in the cleanup phase cannot be
      // interrupted in this way.
      if (!xContext.continueTesting() && metadata.getPhase() == TestPhase.TEST) {
        xContext.removeSyncObject(name, barrier);
        return;
      }

      if (barrier.isBroken()) {

        // Only 1 thread can reset the barrier, so use the barrier as a synchronisation object.
        synchronized (barrier) {
          if (barrier.isBroken()) {
            //System.err.println("Thread " + Thread.currentThread().getId() + " resetting barrier.");
            barrier.reset();
          }
        }
      }

      int arrivalIndex = -1;

      try {
        arrivalIndex = barrier.await(200, TimeUnit.MILLISECONDS);

        // We achieved synchronisation!
        return;

      } catch (InterruptedException e) {

      } catch (BrokenBarrierException | TimeoutException e) {
        //final String msg = e.getClass().getSimpleName() + " exception in thread " + Thread.currentThread().getId();
        //System.err.println(msg);
        continue;
      }
    }
  }
}
