// Copyright (c) 2017 Boray Data Co. Ltd.  All rights reserved.

package com.rapidsdata.seth.plan.expectedResults;

import com.rapidsdata.seth.contexts.AppContext;
import com.rapidsdata.seth.exceptions.ExpectedResultFailureException;
import com.rapidsdata.seth.exceptions.FailureException;
import com.rapidsdata.seth.plan.OperationMetadata;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import static com.rapidsdata.seth.plan.expectedResults.ResultSetFormatter.describeCurrentRow;

/** An expected result class where we expect the operation to have returned an ordered set of rows. */
public class OrderedRowsExpectedResult extends ExpectedResult
{
  private final List<ExpectedRow> expectedRows;

  /**
   * Constructor
   * @param description A textual description of the expected result.
   * @param opMetadata The metadata about the operation that produced the actual result.
   * @param appContext The application context container.
   * @param expectedRows The list of rows expected to be returned by the operation.
   */
  public OrderedRowsExpectedResult(String description,
                                   OperationMetadata opMetadata,
                                   AppContext appContext,
                                   List<ExpectedRow> expectedRows)
  {
    super(ExpectedResultType.ORDERED_ROWS, description, opMetadata, appContext);
    this.expectedRows = expectedRows;
  }

  /**
   * Compares the actual result, being a ResultSet, with the expected result.
   * @param rs The ResultSet to be compared to the expected result.
   * @throws FailureException if the expected result does not match with this actual result.
   */
  @Override
  public void assertActualAsResultSet(ResultSet rs) throws FailureException
  {
    try {
      for (ExpectedRow expectedRow : expectedRows) {
        if (!rs.next()) {
          String actualResultDesc = "No more actual rows to compare to expected row: " + expectedRow.toString();
          throw new ExpectedResultFailureException(opMetadata, actualResultDesc, this);
        }

        if (!expectedRow.compareTo(rs)) {
          String actualResultDesc = "Actual row does not match expected row." + System.lineSeparator() +
                                    "Expected Row: " + expectedRow.toString() + System.lineSeparator() +
                                    "Actual Row  : " + describeCurrentRow(rs);

          throw new ExpectedResultFailureException(opMetadata, actualResultDesc, this);
        }
      }

      // Are there more actual rows compared to expected rows?
      StringBuilder sb = null;
      while (rs.next()) {
        if (sb == null) {
          sb = new StringBuilder(1024);
          sb.append("There are more actual rows than expected rows: ");
        }

        sb.append(System.lineSeparator());
        sb.append("Additional Row: ");
        sb.append(describeCurrentRow(rs));
      }

      if (sb != null) {
        throw new ExpectedResultFailureException(opMetadata, sb.toString(), this);
      }

    } catch (SQLException e) {
      String actualResultDesc = e.getClass().getSimpleName() + ": " + e.getMessage();
      throw new ExpectedResultFailureException(opMetadata, actualResultDesc, this);
    }
  }

  /**
   * Compares the actual result, being an update count, with the expected result.
   *
   * @param updateCount The update count to be compared to the expected result.
   * @throws FailureException if the expected result does not match with this actual result.
   */
  @Override
  public void assertActualAsUpdateCount(long updateCount) throws FailureException
  {
    // Not what was expected.
    String actualResultDesc = "An update count was received";
    throw new ExpectedResultFailureException(opMetadata, actualResultDesc, this);
  }

  /**
   * Compares the actual result, being a SQLException, with the expected result.
   *
   * @param e The exception to be compared to the expected result.
   * @throws FailureException if the expected result does not match with this actual result.
   */
  @Override
  public void assertActualAsException(SQLException e) throws FailureException
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
  public void assertActualAsException(Exception e) throws FailureException
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
  public void assertActualAsSuccess() throws FailureException
  {
    String actualResultDesc = "success";
    throw new ExpectedResultFailureException(opMetadata, actualResultDesc, this);
  }

  /**
   * Compares the actual result, being a general purpose failure with an error message, with the expected result.
   *
   * @param msg The error message to be compared to the expected result.
   * @throws FailureException if the expected result does not match with this actual result.
   */
  @Override
  public void assertActualAsFailure(String msg) throws FailureException
  {
    // Not what was expected.
    String actualResultDesc = "Error message: " + msg;
    throw new ExpectedResultFailureException(opMetadata, actualResultDesc, this);
  }
}
