// Copyright (c) 2017 Boray Data Co. Ltd.  All rights reserved.

package com.rapidsdata.seth.plan.expectedResults;

import com.rapidsdata.seth.exceptions.FailureException;
import com.rapidsdata.seth.plan.OperationMetadata;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * An expected result class where we don't care whether the operation succeeded or failed.
 */
public class DontCareExpectedResult extends ExpectedResult
{
  /**
   * Constructor
   * @param opMetadata The metadata about the operation that produced the actual result.
   */
  public DontCareExpectedResult(OperationMetadata opMetadata)
  {
    super(ExpectedResultType.DONT_CARE, opMetadata);
  }

  /**
   * Compares the actual result, being a ResultSet, with the expected result.
   * @param rs The ResultSet to be compared to the expected result.
   * @throws FailureException if the expected result does not match with this actual result.
   */
  @Override
  public void compareActualAsResultSet(ResultSet rs) throws FailureException
  {
    // We don't care.
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
    // We don't care.
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
    // We don't care.
  }

  /**
   * Compares the actual result, being a general purpose statement of success, with the expected result.
   *
   * @throws FailureException if the expected result does not match with this actual result.
   */
  @Override
  public void compareActualAsSuccess() throws FailureException
  {
    // We don't care.
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
    // We don't care.
  }
}
