// Copyright (c) 2017 Boray Data Co. Ltd.  All rights reserved.

package com.rapidsdata.seth.plan.expectedResults;

import com.rapidsdata.seth.exceptions.FailureException;
import com.rapidsdata.seth.plan.OperationMetadata;

import java.sql.ResultSet;
import java.sql.SQLException;

public abstract class ExpectedResult
{
  /** The type of expected result this is. */
  protected final ExpectedResultType type;

  /** The metadata about the operation that produced the actual result. */
  protected final OperationMetadata opMetadata;

  /**
   * Constructor
   * @param type The type of expected result this is.
   * @param opMetadata The metadata about the operation that produced the actual result.
   */
  protected ExpectedResult(ExpectedResultType type, OperationMetadata opMetadata)
  {
    this.type = type;
    this.opMetadata = opMetadata;
  }


  /**
   * Compares the actual result, being a ResultSet, with the expected result.
   * @param rs The ResultSet to be compared to the expected result.
   * @throws FailureException if the expected result does not match with this actual result.
   */
  public abstract void compareActualAsResultSet(ResultSet rs) throws FailureException;

  /**
   * Compares the actual result, being an update count, with the expected result.
   * @param updateCount The update count to be compared to the expected result.
   * @throws FailureException if the expected result does not match with this actual result.
   */
  public abstract void compareActualAsUpdateCount(int updateCount) throws FailureException;

  /**
   * Compares the actual result, being a SQLException, with the expected result.
   * @param e The exception to be compared to the expected result.
   * @throws FailureException if the expected result does not match with this actual result.
   */
  public abstract void compareActualAsException(SQLException e) throws FailureException;

  /**
   * Compares the actual result, being a general purpose statement of success, with the expected result.
   * @throws FailureException if the expected result does not match with this actual result.
   */
  public abstract void compareActualAsSuccess() throws FailureException;

  /**
   * Compares the actual result, being a general purpose failure with an error message, with the expected result.
   * @param msg The error message to be compared to the expected result.
   * @throws FailureException if the expected result does not match with this actual result.
   */
  public abstract void compareActualAsFailure(String msg) throws FailureException;
}
