// Copyright (c) 2017 Boray Data Co. Ltd.  All rights reserved.

package com.rapidsdata.seth.plan.expectedResults;

import com.rapidsdata.seth.Options;
import com.rapidsdata.seth.contexts.AppContext;
import com.rapidsdata.seth.contexts.ExecutionContext;
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
  public DontCareExpectedResult(OperationMetadata opMetadata, AppContext appContext, Options options)
  {
    super(ExpectedResultType.DONT_CARE, DESC, opMetadata, appContext, options);
  }

  /**
   * Compares the actual result, being a SQLException, with the expected result.
   * @param xContext The context that the operator was executed within.
   * @param e The exception to be compared to the expected result.
   * @throws FailureException if the expected result does not match with this actual result.
   */
  @Override
  public void assertActualAsException(ExecutionContext xContext, SQLException e) throws FailureException
  {
    // We don't care, but we should log the error.
    // Use the ExpectedResultFailureException to format a nice warning message for us.
    final String commentDesc = "An unexpected exception was returned.";
    final String actualResult = e.getClass().getSimpleName() + ": " + e.getMessage();
    final ExpectedResultFailureException erfe = new ExpectedResultFailureException(opMetadata,
                                                                                   commentDesc,
                                                                                   actualResult,
                                                                                   this.describe());
    final String msg = "Command returned exception" + System.lineSeparator() + erfe.getMessage();
    appContext.getLogger().warning(msg);
  }

  /**
   * Compares the actual result, being an Exception, with the expected result.
   * Because this is a general exception, the stack trace will be included.
   * @param xContext The context that the operator was executed within.
   * @param e The exception to be compared to the expected result.
   * @throws FailureException if the expected result does not match with this actual result.
   */
  @Override
  public void assertActualAsException(ExecutionContext xContext, Exception e) throws FailureException
  {
    // We don't care, but we should log the error.
    // Use the ExpectedResultFailureException to format a nice warning message for us.
    final String commentDesc = "An unexpected exception was returned.";
    final String actualResult = e.getClass().getSimpleName() + ": " + e.getMessage();
    final ExpectedResultFailureException erfe = new ExpectedResultFailureException(opMetadata,
                                                                                   commentDesc,
                                                                                   actualResult,
                                                                                   this.describe());

    final String msg = "Command returned exception" + System.lineSeparator() + erfe.getMessage();
    appContext.getLogger().warning(msg);
  }

  /**
   * Compares the actual result, being a general purpose failure with an error message, with the expected result.
   * @param xContext The context that the operator was executed within.
   * @param error The error message to be compared to the expected result.
   * @throws FailureException if the expected result does not match with this actual result.
   */
  @Override
  public void assertActualAsFailure(ExecutionContext xContext, String error) throws FailureException
  {
    // We don't care, but we should log the error.
    // Use the ExpectedResultFailureException to format a nice warning message for us.
    final String commentDesc = "An unexpected error message was returned.";
    final String actualResult = error;
    final ExpectedResultFailureException erfe = new ExpectedResultFailureException(opMetadata,
                                                                                   commentDesc,
                                                                                   actualResult,
                                                                                   this.describe());
    final String logMsg = "Command returned error" + System.lineSeparator() + erfe.getMessage();

    appContext.getLogger().warning(logMsg);
  }
}
