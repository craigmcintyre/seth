// Copyright (c) 2017 Boray Data Co. Ltd.  All rights reserved.

package com.rapidsdata.seth.plan.expectedResults;

import com.rapidsdata.seth.contexts.AppContext;
import com.rapidsdata.seth.exceptions.ExpectedResultFailureException;
import com.rapidsdata.seth.exceptions.FailureException;
import com.rapidsdata.seth.plan.OperationMetadata;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * An expected result class where we the operation to have returned a ResultSet
 * with a certain number of rows.
 */
public class RowCountExpectedResult extends ExpectedResult
{
  private final long expectedRowCount;

  /**
   * Constructor
   * @param description A textual description of the expected result.
   * @param opMetadata The metadata about the operation that produced the actual result.
   * @param appContext The application context container.
   * @param expectedRowCount The expected number of rows in the ResultSet returned by the operation.
   */
  public RowCountExpectedResult(String description,
                                OperationMetadata opMetadata,
                                AppContext appContext,
                                long expectedRowCount)
  {
    super(ExpectedResultType.AFFECTED_ROWS, description, opMetadata, appContext);
    this.expectedRowCount = expectedRowCount;
  }

  /**
   * Compares the actual result, being a ResultSet, with the expected result and throws an
   * exception if they are not compatible.
   * @param rs The ResultSet to be compared to the expected result.
   * @throws FailureException if the expected result does not match with this actual result.
   */
  @Override
  public void assertActualAsResultSet(ResultSet rs) throws FailureException
  {
    long actualRowCount = 0;

    try {
      while (rs.next()) {
        ++actualRowCount;
      }

      if (actualRowCount != expectedRowCount) {
        final String commentDesc = "A different row count was received than was expected.";
        final String actualResultDesc = "rows: " + actualRowCount;
        throw new ExpectedResultFailureException(opMetadata, commentDesc, actualResultDesc, this.describe());
      }

    } catch (SQLException e) {
      final String commentDesc = "An exception was received instead of a ResultSet.";
      final String actualResultDesc = e.getClass().getSimpleName() + ": " + e.getMessage();
      throw new ExpectedResultFailureException(opMetadata, commentDesc, actualResultDesc, this.describe());
    }
  }

  /**
   * Compares the actual result, being an update count, with the expected result and throws an
   * exception if they are not compatible.
   *
   * @param updateCount The update count to be compared to the expected result.
   * @throws FailureException if the expected result does not match with this actual result.
   */
  @Override
  public void assertActualAsUpdateCount(long updateCount) throws FailureException
  {
    final String commentDesc = "An affected row count was received instead of a ResultSet.";
    final String actualResultDesc = "affected: " + updateCount;
    throw new ExpectedResultFailureException(opMetadata, commentDesc, actualResultDesc, this.describe());
  }

  /**
   * Compares the actual result, being a SQLException, with the expected result and throws an
   * exception if they are not compatible.
   *
   * @param e The exception to be compared to the expected result.
   * @throws FailureException if the expected result does not match with this actual result.
   */
  @Override
  public void assertActualAsException(SQLException e) throws FailureException
  {
    // Not what was expected.
    final String commentDesc = "An exception was received instead of a ResultSet.";
    final String actualResultDesc = e.getClass().getSimpleName() + ": " + e.getMessage();
    throw new ExpectedResultFailureException(opMetadata, commentDesc, actualResultDesc, this.describe());
  }

  /**
   * Compares the actual result, being an Exception, with the expected result and throws an
   * exception if they are not compatible.
   * Because this is a general exception, the stack trace will be included.
   *
   * @param e The exception to be compared to the expected result.
   * @throws FailureException if the expected result does not match with this actual result.
   */
  @Override
  public void assertActualAsException(Exception e) throws FailureException
  {
    // Not what was expected.
    final String commentDesc = "An exception was received instead of a ResultSet.";
    final String actualResultDesc = e.getClass().getSimpleName() + ": " + e.getMessage();
    throw new ExpectedResultFailureException(opMetadata, commentDesc, actualResultDesc, this.describe(), e);
  }

  /**
   * Compares the actual result, being a general purpose statement of success, with the expected
   * result and throws an exception if they are not compatible.
   *
   * @throws FailureException if the expected result does not match with this actual result.
   */
  @Override
  public void assertActualAsSuccess() throws FailureException
  {
    final String commentDesc = "The operation did not return a ResultSet as was expected.";
    final String actualResultDesc = "success";
    throw new ExpectedResultFailureException(opMetadata, commentDesc, actualResultDesc, this.describe());
  }

  /**
   * Compares the actual result, being a general purpose failure with an error message,
   * with the expected result and throws an exception if they are not compatible..
   *
   * @param msg The error message to be compared to the expected result.
   * @throws FailureException if the expected result does not match with this actual result.
   */
  @Override
  public void assertActualAsFailure(String msg) throws FailureException
  {
    // Not what was expected.
    final String commentDesc = "An exception was received instead of a ResultSet.";
    final String actualResultDesc = "Error message: " + msg;
    throw new ExpectedResultFailureException(opMetadata, commentDesc, actualResultDesc, this.describe());
  }
}
