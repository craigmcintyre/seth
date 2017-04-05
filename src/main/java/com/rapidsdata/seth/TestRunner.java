// Copyright (c) 2017 Boray Data Co. Ltd.  All rights reserved.

package com.rapidsdata.seth;

import com.rapidsdata.seth.plan.Plan;

public class TestRunner implements Runnable
{
  /** The test plan to be executed. */
  private final Plan plan;

  /**
   * If true then this thread is the first thread running the test. When this thread reaches the
   * end of the test statements then the test ends and all child threads will start cleaning up.
   */
  private final boolean isPrimaryThread;

  /**
   * Constructor
   * @param plan the plan to be executed.
   * @param isPrimaryThread whether this is running as the primary thread of the test.
   */
  public TestRunner(Plan plan, boolean isPrimaryThread)
  {
    this.plan = plan;
    this.isPrimaryThread = isPrimaryThread;
  }

  /**
   * Constructor for child threads of a test.
   * @param plan the plan to be executed.
   */
  public TestRunner(Plan plan)
  {
    this.plan = plan;
    this.isPrimaryThread = false;
  }

  /**
   * When an object implementing interface <code>Runnable</code> is used
   * to create a thread, starting the thread causes the object's
   * <code>run</code> method to be called in that separately executing
   * thread.
   * <p>
   * The general contract of the method <code>run</code> is that it may
   * take any action whatsoever.
   *
   * @see Thread#run()
   */
  @Override
  public void run()
  {

  }
}
