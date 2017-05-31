// Copyright (c) 2017 Boray Data Co. Ltd.  All rights reserved.

package com.rapidsdata.seth.contexts;

import com.rapidsdata.seth.PathRelativity;
import com.rapidsdata.seth.TestResult;
import com.rapidsdata.seth.exceptions.FailureException;
import com.rapidsdata.seth.logging.TestLogger;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class TestContextImpl implements TestContext
{
  /** The application context that this test context uses. */
  private final AppContext appContext;

  /** The test file being executed. */
  private final File testFile;

  /** The object that will hold the result of the test. */
  private final TestResult testResult;

  /** A flag that indicates to test threads whether to keep running or to exit the test early. */
  private volatile boolean continueTesting = true;

  /** A lock used for the cleanupPhase. */
  private final Lock lock = new ReentrantLock();

  /** A condition to wait on for cleanup to start. */
  private final Condition cleanupPhase = lock.newCondition();

  /** A count of the number of active threads running the current test. */
  private final AtomicInteger numActiveThreads = new AtomicInteger(0);

  /** A map of objects that threads can synchronise on. */
  private final Map<String, CyclicBarrier> syncMap = new ConcurrentHashMap<>();

  /**
   * Constructor.
   * @param appContext The application context.
   */
  public TestContextImpl(AppContext appContext, File testFile, TestResult testResult)
  {
    this.appContext = appContext;
    this.testFile = testFile;
    this.testResult = testResult;
  }


  /**
   * Returns the time since the application started, in nanoseconds since the epoch.
   * @return the time since the application started, in nanoseconds since the epoch.
   */
  @Override
  public long getAppStartTime()
  {
    return appContext.getAppStartTime();
  }

  /**
   * Returns the list of test files to be executed.
   * @return the list of test files to be executed.
   */
  @Override
  public List<File> getTestFiles()
  {
    return appContext.getTestFiles();
  }

  /**
   * Returns the url that the JDBC driver should use to communicate to the system being tested.
   * @return the url that the JDBC driver should use to communicate to the system being tested.
   */
  @Override
  public String getUrl()
  {
    return appContext.getUrl();
  }

  /**
   * Returns how we are to treat relative paths.
   * @return how we are to treat relative paths.
   */
  @Override
  public PathRelativity getPathRelativity()
  {
    return appContext.getPathRelativity();
  }

  /**
   * Returns the logger we are to use to write to the console and to file.
   * @return the logger we are to use to write to the console and to file.
   */
  @Override
  public TestLogger getLogger()
  {
    return appContext.getLogger();
  }

  /**
   * Returns the thread pool executor service, by which new threads can be created.
   * @return the thread pool executor service, by which new threads can be created.
   */
  @Override
  public ExecutorService getThreadPool()
  {
    return appContext.getThreadPool();
  }

  /**
   * Returns the test file currently being executed.
   * @return the test file currently being executed.
   */
  @Override
  public File getTestFile()
  {
    return testFile;
  }

  /**
   * A notification flag to child threads whether to continue testing or whether the test is ending
   * and they should stop executing.
   * @return true if the test is continuing or false if it is ending.
   */
  @Override
  public boolean continueTesting()
  {
    return continueTesting;
  }

  /**
   * Blocks the current thread until testing operations have completed (either successfully
   * or in failure) and threads should now start executing cleanup operations.
   */
  @Override
  public void waitForCleanup()
  {
    lock.lock();

    try {
      while (continueTesting) {
        cleanupPhase.awaitUninterruptibly();
      }

    } finally {
      lock.unlock();
    }
  }

  /**
   * Marks the test result as being aborted and causes all the test threads to stop executing and
   * start cleaning up.
   */
  @Override
  public void abortTest()
  {
    lock.lock();

    try {
      if (continueTesting) {
        testResult.setAbort();
        signalEndOfTesting();
      }
    } finally {
      lock.unlock();
    }
  }

  /**
   * Marks the test result that the test has started executing.
   */
  @Override
  public void markAsStarted()
  {
    testResult.setStarted();
  }

  /**
   * Marks the test result as successful and causes the test threads to begin cleaning up.
   */
  @Override
  public void markAsSucceeded()
  {
    lock.lock();

    try {
      if (continueTesting) {
        testResult.setSuccess();
        signalEndOfTesting();
      }
    } finally {
      lock.unlock();
    }
  }

  /**
   * Marks the test result as a failure and causes the test threads to begin cleaning up.
   * @param failure
   */
  @Override
  public void markAsFailed(FailureException failure)
  {
    lock.lock();

    try {
      if (continueTesting) {
        testResult.setFailure(failure);
        signalEndOfTesting();
      }
    } finally {
      lock.unlock();
    }
  }

  /**
   * Tells any waiting threads on the cleanupPhase condition variable that the testing phase
   * has finished and the cleanup phase has begun. It releases any blocked, waiting threads.
   */
  private void signalEndOfTesting()
  {
    lock.lock();

    try {
      continueTesting = false;
      cleanupPhase.signalAll();

    } finally {
      lock.unlock();
    }
  }

  /**
   * Accumulates a count of test steps that occurred in a given testing thread. Each thread should
   * report the number of test steps is executed when the thread completes.
   * @param count the number of test steps that the testing thread executed.
   */
  @Override
  public void accumulateTestSteps(long count)
  {
    testResult.accumulateSteps(count);
  }

  /**
   * Increments a count of the number of active threads in the system.
   */
  @Override
  public void incrementActiveThreads()
  {
    numActiveThreads.incrementAndGet();
  }

  /**
   * Decrements a count of the number of active threads in the system.
   */
  @Override
  public void decrementActiveThreads()
  {
    numActiveThreads.decrementAndGet();
  }

  /**
   * Returns the number of active threads in the system.
   * @return the number of active threads in the system.
   */
  @Override
  public int getNumActiveThreads()
  {
    return numActiveThreads.get();
  }

  /**
   * Returns the synchronisation barrier associated with a given name for synchronising threads on.
   * Creates the barrier if one does not exist (threadsafe).
   * @param name the name to associate with the synchronisation barrier.
   * @param parties the number of threads to wait on.
   * @return the synchronisation barrier.
   */
  @Override
  public CyclicBarrier getOrCreateSyncObject(String name, int parties)
  {
    // is there currently a sync object with the given name?
    CyclicBarrier barrier = syncMap.get(name);

    if (barrier == null || barrier.getParties() != parties) {
      // Create a new CyclicBarrier and save it in the sync map.

      synchronized (syncMap) {
        // We had better check under synchronisation if someone got in before us.
        barrier = syncMap.get(name);

        if (barrier == null || barrier.getParties() != parties) {
          barrier = new CyclicBarrier(parties);
          syncMap.put(name, barrier);
        }
      }
    }

    return barrier;
  }

  /**
   * Removes the given synchronisation barrier associated with a given name.
   * @param name  the name of the object to remove.
   * @param barrier the actual synchronisation object to be removed.
   */
  @Override
  public void removeSyncObject(String name, CyclicBarrier barrier)
  {
    synchronized(syncMap) {
      syncMap.remove(name, barrier);
    }
  }
}
