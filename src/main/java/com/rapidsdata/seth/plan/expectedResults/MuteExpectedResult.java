// Copyright (c) 2017 Boray Data Co. Ltd.  All rights reserved.

package com.rapidsdata.seth.plan.expectedResults;

import com.rapidsdata.seth.Options;
import com.rapidsdata.seth.contexts.AppContext;
import com.rapidsdata.seth.contexts.ExecutionContext;
import com.rapidsdata.seth.exceptions.FailureException;
import com.rapidsdata.seth.plan.OperationMetadata;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;

/**
 * An expected result class where we don't care whether the operation succeeded or failed
 * and we are explicitly not going to log any command failure details.
 * */
public class MuteExpectedResult extends ExpectedResult
{
  /**
   * Constructor
   * @param description A textual description of the expected result.
   * @param opMetadata The metadata about the operation that produced the actual result.
   * @param appContext The application context container.
   */
  public MuteExpectedResult(String description, OperationMetadata opMetadata, AppContext appContext, Options options)
  {
    super(ExpectedResultType.MUTE, description, opMetadata, appContext, options);
  }

  /**
   * Constructor for overridden classes.
   * @param type The type of expected result this is.
   * @param description A textual description of the expected result.
   * @param opMetadata The metadata about the operation that produced the actual result.
   * @param appContext The application context container.
   */
  protected MuteExpectedResult(ExpectedResultType type, String description, OperationMetadata opMetadata, AppContext appContext, Options options)
  {
    super(type, description, opMetadata, appContext, options);
  }

  /**
   * Compares the actual result, being a ResultSet, with the expected result.
   * @param xContext The context that the operator was executed within.
   * @param rs The ResultSet to be compared to the expected result.
   * @param warnings Any warnings from executing the statement. May be null.
   * @throws FailureException if the expected result does not match with this actual result.
   */
  @Override
  public void assertActualAsResultSet(ExecutionContext xContext, ResultSet rs, SQLWarning warnings) throws FailureException
  {
    // We don't care.
  }

  /**
   * Compares the actual result, being an update count, with the expected result.
   * @param xContext The context that the operator was executed within.
   * @param updateCount The update count to be compared to the expected result.
   * @param warnings Any warnings from executing the statement. May be null.
   * @throws FailureException if the expected result does not match with this actual result.
   */
  @Override
  public void assertActualAsUpdateCount(ExecutionContext xContext, long updateCount, SQLWarning warnings) throws FailureException
  {
    // We don't care.
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
    // We don't care, and we are not going to log the error.
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
    // We don't care, and we are not going to log the error.
  }

  /**
   * Compares the actual result, being a general purpose statement of success, with the expected result.
   * @param xContext The context that the operator was executed within.
   * @param warnings Any warnings from executing the statement. May be null.
   * @throws FailureException if the expected result does not match with this actual result.
   */
  @Override
  public void assertActualAsSuccess(ExecutionContext xContext, SQLWarning warnings) throws FailureException
  {
    // We don't care.
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
    // We don't care, and we are not going to log the error.
  }
}
