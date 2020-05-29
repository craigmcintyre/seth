// Copyright (c) 2017 Boray Data Co. Ltd.  All rights reserved.

package com.rapidsdata.seth.plan.expectedResults;

import com.rapidsdata.seth.Options;
import com.rapidsdata.seth.contexts.AppContext;
import com.rapidsdata.seth.contexts.ExecutionContext;
import com.rapidsdata.seth.exceptions.ExpectedResultFailureException;
import com.rapidsdata.seth.exceptions.FailureException;
import com.rapidsdata.seth.plan.OperationMetadata;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;

/**
 * An expected result class where we the operation expects to have returned a ResultSet
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
                                Options options,
                                long expectedRowCount)
  {
    super(ExpectedResultType.ROW_COUNT, description, opMetadata, appContext, options);
    this.expectedRowCount = expectedRowCount;
  }

  /**
   * Compares the actual result, being a ResultSet, with the expected result and throws an
   * exception if they are not compatible.
   * @param xContext The context that the operator was executed within.
   * @param rs The ResultSet to be compared to the expected result.
   * @param warnings Any warnings from executing the statement. May be null.
   * @throws FailureException if the expected result does not match with this actual result.
   */
  @Override
  public void assertActualAsResultSet(ExecutionContext xContext, ResultSet rs, SQLWarning warnings) throws FailureException
  {
    long actualRowCount = 0;

    StringBuilder sb = new StringBuilder(4096);
    final int maxRowsToDisplay = 100;

    try {
      while (rs.next()) {
        ++actualRowCount;

        if (actualRowCount > maxRowsToDisplay) {
          continue;
        }

        if (sb.length() > 0) {
          sb.append("\n");
        }

        sb.append("  ");
        sb.append(ResultSetFormatter.describeCurrentRow(rs));
      }

      if (actualRowCount > maxRowsToDisplay) {
        sb.append("\n  ...")
          .append("\nand ")
          .append(actualRowCount - maxRowsToDisplay)
          .append(" more rows.");
      }

      if (actualRowCount != expectedRowCount) {
        final String commentDesc = "A different row count was received than was expected.";
        final String actualResultDesc = "rows: " + actualRowCount + "\nThe following rows were received:\n" + sb.toString();
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
   * @param xContext The context that the operator was executed within.
   * @param updateCount The update count to be compared to the expected result.
   * @param warnings Any warnings from executing the statement. May be null.
   * @throws FailureException if the expected result does not match with this actual result.
   */
  @Override
  public void assertActualAsUpdateCount(ExecutionContext xContext, long updateCount, SQLWarning warnings) throws FailureException
  {
    final String commentDesc = "An affected row count was received instead of a ResultSet.";
    final String actualResultDesc = "affected: " + updateCount;
    throw new ExpectedResultFailureException(opMetadata, commentDesc, actualResultDesc, this.describe());
  }

  /**
   * Compares the actual result, being a SQLException, with the expected result and throws an
   * exception if they are not compatible.
   * @param xContext The context that the operator was executed within.
   * @param e The exception to be compared to the expected result.
   * @throws FailureException if the expected result does not match with this actual result.
   */
  @Override
  public void assertActualAsException(ExecutionContext xContext, SQLException e) throws FailureException
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
   * @param xContext The context that the operator was executed within.
   * @param e The exception to be compared to the expected result.
   * @throws FailureException if the expected result does not match with this actual result.
   */
  @Override
  public void assertActualAsException(ExecutionContext xContext, Exception e) throws FailureException
  {
    // Not what was expected.
    final String commentDesc = "An exception was received instead of a ResultSet.";
    final String actualResultDesc = e.getClass().getSimpleName() + ": " + e.getMessage();
    throw new ExpectedResultFailureException(opMetadata, commentDesc, actualResultDesc, this.describe(), e);
  }

  /**
   * Compares the actual result, being a general purpose statement of success, with the expected
   * result and throws an exception if they are not compatible.
   * @param xContext The context that the operator was executed within.
   * @param warnings Any warnings from executing the statement. May be null.
   * @throws FailureException if the expected result does not match with this actual result.
   */
  @Override
  public void assertActualAsSuccess(ExecutionContext xContext, SQLWarning warnings) throws FailureException
  {
    final String commentDesc = "The operation did not return a ResultSet as was expected.";
    final String actualResultDesc = "success";
    throw new ExpectedResultFailureException(opMetadata, commentDesc, actualResultDesc, this.describe());
  }

  /**
   * Compares the actual result, being a general purpose failure with an error message,
   * with the expected result and throws an exception if they are not compatible..
   * @param xContext The context that the operator was executed within.
   * @param msg The error message to be compared to the expected result.
   * @throws FailureException if the expected result does not match with this actual result.
   */
  @Override
  public void assertActualAsFailure(ExecutionContext xContext, String msg) throws FailureException
  {
    // Not what was expected.
    final String commentDesc = "An exception was received instead of a ResultSet.";
    final String actualResultDesc = "Error message: " + msg;
    throw new ExpectedResultFailureException(opMetadata, commentDesc, actualResultDesc, this.describe());
  }
}
