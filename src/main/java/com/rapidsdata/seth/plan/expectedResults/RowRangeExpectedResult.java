// Copyright (c) 2017 Boray Data Co. Ltd.  All rights reserved.

package com.rapidsdata.seth.plan.expectedResults;

import com.rapidsdata.seth.contexts.AppContext;
import com.rapidsdata.seth.exceptions.ExpectedResultFailureException;
import com.rapidsdata.seth.exceptions.FailureException;
import com.rapidsdata.seth.plan.OperationMetadata;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;

/**
 * An expected result class where we expect the operation to have returned a ResultSet
 * with a number of rows within a given range.
 */
public class RowRangeExpectedResult extends ExpectedResult
{
  private final boolean inclusiveLower;
  private final boolean inclusiveUpper;
  private final long lowerVal;
  private final long upperVal;

  /**
   * Constructor
   * @param description A textual description of the expected result.
   * @param opMetadata The metadata about the operation that produced the actual result.
   * @param appContext The application context container.
   * @param inclusiveLower if true then the number of rows returned must be >= startVal. If false then it must be > startVal.
   * @param inclusiveUpper if true then the number of rows returned must be <= endVal. If false then it must be < endVal.
   * @param lowerVal the lower value of the range. Set to Long.MIN_VALUE to ignore the lower bound.
   * @param upperVal the upper value of the range. Set to Long.MIN_VALUE to ignore the upper bound.
   */
  public RowRangeExpectedResult(String description,
                                OperationMetadata opMetadata,
                                AppContext appContext,
                                boolean inclusiveLower,
                                boolean inclusiveUpper,
                                long lowerVal,
                                long upperVal)
  {
    super(ExpectedResultType.ROW_RANGE, description, opMetadata, appContext);

    this.inclusiveLower = inclusiveLower;
    this.inclusiveUpper = inclusiveUpper;
    this.lowerVal = lowerVal;
    this.upperVal = upperVal;
  }

  /**
   * Compares the actual result, being a ResultSet, with the expected result and throws an
   * exception if they are not compatible.
   * @param rs The ResultSet to be compared to the expected result.
   * @param warnings Any warnings from executing the statement. May be null.
   * @throws FailureException if the expected result does not match with this actual result.
   */
  @Override
  public void assertActualAsResultSet(ResultSet rs, SQLWarning warnings) throws FailureException
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


      if (lowerVal != Long.MIN_VALUE) {
        if (actualRowCount < (inclusiveLower ? lowerVal : lowerVal + 1)) {
          // problem
          final String commentDesc = "A different row count was received than was expected.";
          final String actualResultDesc = "rows: " + actualRowCount +
              (actualRowCount > 0 ? "\nThe following rows were received:\n" + sb.toString() : "");
          throw new ExpectedResultFailureException(opMetadata, commentDesc, actualResultDesc, this.describe());
        }
      }

      if (upperVal != Long.MIN_VALUE) {
        if (actualRowCount > (inclusiveUpper ? upperVal : upperVal - 1)) {
          // problem
          final String commentDesc = "A different row count was received than was expected.";
          final String actualResultDesc = "rows: " + actualRowCount +
              (actualRowCount > 0 ? "\nThe following rows were received:\n" + sb.toString() : "");
          throw new ExpectedResultFailureException(opMetadata, commentDesc, actualResultDesc, this.describe());
        }
      }

      // All good.

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
   * @param warnings Any warnings from executing the statement. May be null.
   * @throws FailureException if the expected result does not match with this actual result.
   */
  @Override
  public void assertActualAsUpdateCount(long updateCount, SQLWarning warnings) throws FailureException
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
   * @param warnings Any warnings from executing the statement. May be null.
   * @throws FailureException if the expected result does not match with this actual result.
   */
  @Override
  public void assertActualAsSuccess(SQLWarning warnings) throws FailureException
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
