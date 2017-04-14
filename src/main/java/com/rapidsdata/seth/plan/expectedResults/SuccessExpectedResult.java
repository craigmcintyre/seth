// Copyright (c) 2017 Boray Data Co. Ltd.  All rights reserved.

package com.rapidsdata.seth.plan.expectedResults;

import com.rapidsdata.seth.contexts.AppContext;
import com.rapidsdata.seth.exceptions.ExpectedResultFailureException;
import com.rapidsdata.seth.exceptions.FailureException;
import com.rapidsdata.seth.plan.OperationMetadata;

import java.sql.ResultSet;
import java.sql.SQLException;

/** An expected result class where we the operation to have succeeded. */
public class SuccessExpectedResult extends ExpectedResult
{
  /**
   * Constructor
   * @param description A textual description of the expected result.
   * @param opMetadata The metadata about the operation that produced the actual result.
   * @param appContext The application context container.
   */
  public SuccessExpectedResult(String description, OperationMetadata opMetadata, AppContext appContext)
  {
    super(ExpectedResultType.SUCCESS, description, opMetadata, appContext);
  }

  /**
   * Compares the actual result, being a ResultSet, with the expected result.
   * @param rs The ResultSet to be compared to the expected result.
   * @throws FailureException if the expected result does not match with this actual result.
   */
  @Override
  public void compareActualAsResultSet(ResultSet rs) throws FailureException
  {
    // A ResultSet is success.
  }

  /**
   * Compares the actual result, being an update count, with the expected result.
   *
   * @param updateCount The update count to be compared to the expected result.
   * @throws FailureException if the expected result does not match with this actual result.
   */
  @Override
  public void compareActualAsUpdateCount(int updateCount) throws FailureException
  {
    // An update count is success.
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
    // Not what was expected.
    String actualResultDesc = e.getClass().getSimpleName() + ": " + e.getMessage();
    throw new ExpectedResultFailureException(opMetadata, actualResultDesc, this);
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
    // Not what was expected.
    String actualResultDesc = e.getClass().getSimpleName() + ": " + e.getMessage();
    throw new ExpectedResultFailureException(opMetadata, actualResultDesc, this, e);
  }

  /**
   * Compares the actual result, being a general purpose statement of success, with the expected result.
   *
   * @throws FailureException if the expected result does not match with this actual result.
   */
  @Override
  public void compareActualAsSuccess() throws FailureException
  {
    // A general purpose notification of success is success.
  }

  /**
   * Compares the actual result, being a general purpose failure with an error message, with the expected result.
   *
   * @param msg The error message to be compared to the expected result.
   * @throws FailureException if the expected result does not match with this actual result.
   */
  @Override
  public void compareActualAsFailure(String msg) throws FailureException
  {
    // Not what was expected.
    String actualResultDesc = "Error message: " + msg;
    throw new ExpectedResultFailureException(opMetadata, actualResultDesc, this);
  }
}
