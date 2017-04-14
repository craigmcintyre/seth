// Copyright (c) 2017 Boray Data Co. Ltd.  All rights reserved.

package com.rapidsdata.seth.plan.expectedResults;

import com.rapidsdata.seth.contexts.AppContext;
import com.rapidsdata.seth.exceptions.ExpectedResultFailureException;
import com.rapidsdata.seth.exceptions.FailureException;
import com.rapidsdata.seth.plan.OperationMetadata;

import java.sql.SQLException;

/**
 * An expected result class where we don't care whether the operation succeeded or failed,
 * but we will log warnings for any command failures we come across.
 */
public class DontCareExpectedResult extends MuteExpectedResult
{
  private static final String DESC = "<don't care>";

  /**
   * Constructor
   * @param opMetadata The metadata about the operation that produced the actual result.
   * @param appContext The application context container.
   */
  public DontCareExpectedResult(OperationMetadata opMetadata, AppContext appContext)
  {
    super(ExpectedResultType.DONT_CARE, DESC, opMetadata, appContext);
  }

  /**
   * Compares the actual result, being a SQLException, with the expected result.
   *
   * @param e The exception to be compared to the expected result.
   * @throws FailureException if the expected result does not match with this actual result.
   */
  @Override
  public void compareActualAsException(SQLException e) throws FailureException
  {
    // We don't care, but we should log the error.
    // Use the ExpectedResultFailureException to format a nice warning message for us.
    String actualResult = e.getClass().getSimpleName() + ": " + e.getMessage();

    final String msg = "Command returned exception" + System.lineSeparator() +
        (new ExpectedResultFailureException(opMetadata, actualResult, this)).getMessage();
    appContext.getLogger().warning(msg);
  }

  /**
   * Compares the actual result, being an Exception, with the expected result.
   * Because this is a general exception, the stack trace will be included.
   *
   * @param e The exception to be compared to the expected result.
   * @throws FailureException if the expected result does not match with this actual result.
   */
  @Override
  public void compareActualAsException(Exception e) throws FailureException
  {
    // We don't care, but we should log the error.
    // Use the ExpectedResultFailureException to format a nice warning message for us.
    String actualResult = e.getClass().getSimpleName() + ": " + e.getMessage();

    final String msg = "Command returned exception" + System.lineSeparator() +
        (new ExpectedResultFailureException(opMetadata, actualResult, this, e)).getMessage();
    appContext.getLogger().warning(msg);
  }

  /**
   * Compares the actual result, being a general purpose failure with an error message, with the expected result.
   *
   * @param error The error message to be compared to the expected result.
   * @throws FailureException if the expected result does not match with this actual result.
   */
  @Override
  public void compareActualAsFailure(String error) throws FailureException
  {
    // We don't care, but we should log the error.
    // Use the ExpectedResultFailureException to format a nice warning message for us.
    final String logMsg = "Command returned error" + System.lineSeparator() +
        (new ExpectedResultFailureException(opMetadata, error, this)).getMessage();

    appContext.getLogger().warning(logMsg);
  }
}
