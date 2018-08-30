// Copyright (c) 2017 Boray Data Co. Ltd.  All rights reserved.

package com.rapidsdata.seth.plan.expectedResults;

import com.rapidsdata.seth.contexts.AppContext;
import com.rapidsdata.seth.exceptions.ExpectedResultFailureException;
import com.rapidsdata.seth.exceptions.FailureException;
import com.rapidsdata.seth.plan.OperationMetadata;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;


/** An expected result class where we expect the operation to have returned an ordered set of rows. */
public class OrderedRowsExpectedResult extends ExpectedResult
{
  private final List<ExpectedRow> expectedRows;
  private final ExpectedColumnNames expectedColumnNames;

  /**
   * Constructor
   * @param description A textual description of the expected result.
   * @param opMetadata The metadata about the operation that produced the actual result.
   * @param appContext The application context container.
   * @param expectedRows The list of rows expected to be returned by the operation.
   * @param expectedColumnNames The set of expected column names to be returned by the operation.
   */
  public OrderedRowsExpectedResult(String description,
                                   OperationMetadata opMetadata,
                                   AppContext appContext,
                                   List<ExpectedRow> expectedRows,
                                   ExpectedColumnNames expectedColumnNames)
  {
    super(ExpectedResultType.ORDERED_ROWS, description, opMetadata, appContext);
    this.expectedRows = expectedRows;
    this.expectedColumnNames = expectedColumnNames;
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
      // Compare the column names if they have been specified.
      if (expectedColumnNames != null && !expectedColumnNames.compareTo(rs)) {

        final String commentDesc = "The column names for the actual resultset does not match the column names " +
                "of the expected resultset: " + expectedColumnNames.toString();
        final String actualResultDesc = ResultSetFormatter.describeColumnNames(rs);
        throw new ExpectedResultFailureException(opMetadata, commentDesc, actualResultDesc, this);
      }

      for (ExpectedRow expectedRow : expectedRows) {
        if (!rs.next()) {
          final String commentDesc = "There are no more actual rows to compare to the expected row: " + expectedRow.toString();
          final String actualResultDesc = "<no remaining rows>";
          throw new ExpectedResultFailureException(opMetadata, commentDesc, actualResultDesc, this);
        }

        if (!expectedRow.compareTo(rs)) {
          final String commentDesc = "The actual row does not match the expected row: " + expectedRow.toString();
          final String actualResultDesc = ResultSetFormatter.describeCurrentRow(rs);

          throw new ExpectedResultFailureException(opMetadata, commentDesc, actualResultDesc, this);
        }
      }

      // Are there more actual rows compared to expected rows?
      StringBuilder sb = null;
      final String commentDesc = "There are more actual rows than expected rows.";

      while (rs.next()) {
        if (sb == null) {
          sb = new StringBuilder(1024);
        }

        if (sb.length() > 0) {
          sb.append(System.lineSeparator());
        }
        sb.append(ResultSetFormatter.describeCurrentRow(rs));
      }

      if (sb != null) {
        throw new ExpectedResultFailureException(opMetadata, commentDesc, sb.toString(), this);
      }

    } catch (SQLException e) {
      final String commentDesc = "An exception was received instead of a ResultSet.";
      final String actualResultDesc = e.getClass().getSimpleName() + ": " + e.getMessage();
      throw new ExpectedResultFailureException(opMetadata, commentDesc, actualResultDesc, this);
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
    final String commentDesc = "An affected row count was received instead of a ResultSet.";
    final String actualResultDesc = "affected: " + updateCount;
    throw new ExpectedResultFailureException(opMetadata, commentDesc, actualResultDesc, this);
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
    final String commentDesc = "An exception was received instead of a ResultSet.";
    final String actualResultDesc = e.getClass().getSimpleName() + ": " + e.getMessage();
    throw new ExpectedResultFailureException(opMetadata, commentDesc, actualResultDesc, this);
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
    final String commentDesc = "An exception was received instead of a ResultSet.";
    final String actualResultDesc = e.getClass().getSimpleName() + ": " + e.getMessage();
    throw new ExpectedResultFailureException(opMetadata, commentDesc, actualResultDesc, this, e);
  }

  /**
   * Compares the actual result, being a general purpose statement of success, with the expected result.
   *
   * @throws FailureException if the expected result does not match with this actual result.
   */
  @Override
  public void assertActualAsSuccess() throws FailureException
  {
    final String commentDesc = "The operation did not return a ResultSet as was expected.";
    final String actualResultDesc = "success";
    throw new ExpectedResultFailureException(opMetadata, commentDesc, actualResultDesc, this);
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
    final String commentDesc = "An error message was received instead of a ResultSet.";
    final String actualResultDesc = "Error message: " + msg;
    throw new ExpectedResultFailureException(opMetadata, commentDesc, actualResultDesc, this);
  }
}
