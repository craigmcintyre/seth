// Copyright (c) 2017 Boray Data Co. Ltd.  All rights reserved.

package com.rapidsdata.seth.plan.expectedResults;

import com.rapidsdata.seth.contexts.AppContext;
import com.rapidsdata.seth.exceptions.ExpectedResultFailureException;
import com.rapidsdata.seth.exceptions.FailureException;
import com.rapidsdata.seth.plan.OperationMetadata;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * An expected result class where we expect the operation to have returned a set of rows
 * that is probably not in the same order as they are specified here.
 */
public class UnorderedRowsExpectedResult extends ExpectedResult
{
  private final List<ExpectedRow> expectedRows;

  /**
   * Constructor
   * @param description A textual description of the expected result.
   * @param opMetadata The metadata about the operation that produced the actual result.
   * @param appContext The application context container.
   * @param expectedRows The list of rows expected to be returned by the operation.
   */
  public UnorderedRowsExpectedResult(String description,
                                     OperationMetadata opMetadata,
                                     AppContext appContext,
                                     List<ExpectedRow> expectedRows)
  {
    super(ExpectedResultType.UNORDERED_ROWS, description, opMetadata, appContext);
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
    // Make a copy of the expected row list so we can remove entries from it as we match them.
    List<ExpectedRow> remainingExpectedRows = new LinkedList<ExpectedRow>(expectedRows);

    try {
      // For each actual row
      while (rs.next()) {

        // Have we run out of expected rows to compare actual rows to?
        if (remainingExpectedRows.isEmpty()) {
          // We've got an actual row but no more expected rows.
          StringBuilder sb = new StringBuilder(1024);
          sb.append("There are more actual rows than expected rows: ");

          do {
            sb.append(System.lineSeparator());
            sb.append("Additional Row: ");
            sb.append(ResultSetFormatter.describeCurrentRow(rs));

          } while (rs.next());

          throw new ExpectedResultFailureException(opMetadata, sb.toString(), this);
        }


        Iterator<ExpectedRow> erIterator = remainingExpectedRows.iterator();
        boolean gotMatch = false;

        // For each expected row remaining
        while (erIterator.hasNext()) {
          ExpectedRow expectedRow = erIterator.next();

          // Compare the current actual row to this expected row.
          if (expectedRow.compareTo(rs)) {
            // We got a match! Remove this expected row.
            erIterator.remove();
            gotMatch = true;
            break;
          }

          // No match for this expected and actual row. Try another expected row.
        }

        if (gotMatch) {
          continue;
        }

        // Actual row doesn't match any expected rows.
        String actualResultDesc = "Actual row does not match any expected rows." + System.lineSeparator() +
                                  "Actual Row  : " + ResultSetFormatter.describeCurrentRow(rs);
        throw new ExpectedResultFailureException(opMetadata, actualResultDesc, this);
      }

      // Are there any expected rows left over?
      if (!remainingExpectedRows.isEmpty()) {
        StringBuilder sb = new StringBuilder(1024);
        sb.append("No more actual rows to compare to remaining expected rows: ");

        for (ExpectedRow expectedRow : remainingExpectedRows) {
          sb.append(System.lineSeparator());
          sb.append("Expected Row: ");
          sb.append(expectedRow.toString());
        }

        String actualResultDesc = sb.toString();
        throw new ExpectedResultFailureException(opMetadata, actualResultDesc, this);
      }

      // All good!

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
