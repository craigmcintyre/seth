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
  private static final int MAX_NUM_ROWS_TO_SHOW = 10;
  private static final int MAX_CLOSEST_MATCHES = 3;

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
  public UnorderedRowsExpectedResult(String description,
                                     OperationMetadata opMetadata,
                                     AppContext appContext,
                                     List<ExpectedRow> expectedRows,
                                     ExpectedColumnNames expectedColumnNames)
  {
    super(ExpectedResultType.UNORDERED_ROWS, description, opMetadata, appContext);
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
                                   "of the expected resultset.";
        final String actualResultDesc = ResultSetFormatter.describeColumnNames(rs);
        throw new ExpectedResultFailureException(opMetadata, commentDesc, actualResultDesc, expectedColumnNames.toString());
      }

      // Make a copy of the expected row list so we can remove entries from it as we match them.
      List<ExpectedRow> remainingExpectedRows = new LinkedList<ExpectedRow>(expectedRows);

      // For each actual row
      while (rs.next()) {

        // Have we run out of expected rows to compare actual rows to?
        if (remainingExpectedRows.isEmpty()) {
          // We've got an actual row but no more expected rows.
          final String comment = "There were more actual rows returned than expected rows.";
          final String actual  = ResultSetFormatter.describeCurrentRow(rs);

          throw new ExpectedResultFailureException(opMetadata, comment, actual, "<no more rows>");
        }


        Iterator<ExpectedRow> erIterator = remainingExpectedRows.iterator();
        boolean gotMatch = false;

        // For each expected row remaining
        while (erIterator.hasNext()) {
          ExpectedRow expectedRow = erIterator.next();

          // Compare the current actual row to this expected row.
          if (expectedRow.compareTo(rs, appContext.getCommandLineArgs().round)) {
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

        // Can we find the expected row from the unmatched expected row list that matches
        // the actual row the closest.
        List<ScoredExpectedRow> closestMatches = ExpectedRow.findClosestMatchOf(remainingExpectedRows,
                                                                                rs,
                                                                                appContext.getCommandLineArgs().round,
                                                                                MAX_CLOSEST_MATCHES);

        if (closestMatches.size() > 0) {

          final String commentDesc = "The actual row does not match any expected rows. The " + closestMatches.size() +
                                     " *estimated* closest expected rows are shown below.";

          // Let's align the actual row and the expected rows. First we need to get the widths of them
          int[] columnWidths = new int[rs.getMetaData().getColumnCount()];

          for (int i = 0; i < columnWidths.length; i++) {
            columnWidths[i] = 0;
          }

          // Update the widths based on the actual row...
          ResultSetFormatter.updateColumnWidths(columnWidths, rs);

          // ...and the expected rows.
          for (ExpectedRow er : closestMatches) {
            ResultSetFormatter.updateColumnWidths(columnWidths, er);
          }

          // Also get the desired padding position of each column so that we know how to pad things like nulls.
          boolean[] padLeft = new boolean[columnWidths.length];
          ResultSetFormatter.updatePadLeft(padLeft, rs);

          // Now we can describe the actual row with these widths.
          final String actualResultDesc = ResultSetFormatter.describeCurrentRow(rs, columnWidths);

          // and the closest expected rows
          StringBuilder sb = new StringBuilder(2048);
          for (ScoredExpectedRow er : closestMatches) {
            if (sb.length() > 0) {
              sb.append(System.lineSeparator());
            }

            sb.append(er.toString(columnWidths, padLeft))
              .append(String.format("  [Score: %.2f]", er.getScore()));
          }

          throw new ExpectedResultFailureException(opMetadata, commentDesc, actualResultDesc, sb.toString());
        }


        // We could not compute the closest match, so simply show a subset of the expected rows instead.
        final String commentDesc = "The actual row does not match any expected rows.";

        // Let's align the actual row and the expected rows. First we need to get the widths of them
        int[] columnWidths = new int[rs.getMetaData().getColumnCount()];

        for (int i = 0; i < columnWidths.length; i++) {
          columnWidths[i] = 0;
        }

        // Update the widths based on the actual row...
        ResultSetFormatter.updateColumnWidths(columnWidths, rs);

        // ...and the expected rows.
        int count = 0;
        for (ExpectedRow er : expectedRows) {
          ResultSetFormatter.updateColumnWidths(columnWidths, er);
          if (++count >= MAX_NUM_ROWS_TO_SHOW) {
            break;
          }
        }

        // Also get the desired padding position of each column so that we know how to pad things like nulls.
        boolean[] padLeft = new boolean[columnWidths.length];
        ResultSetFormatter.updatePadLeft(padLeft, rs);

        // Now we can describe the actual row with these widths.
        final String actualResultDesc = ResultSetFormatter.describeCurrentRow(rs, columnWidths);

        // And some of the expected rows
        count = 0;
        StringBuilder sb = new StringBuilder(1024);
        for (ExpectedRow er : expectedRows) {
          if (sb.length() > 0) {
            sb.append(System.lineSeparator());
          }

          sb.append(er.toString(columnWidths, padLeft));

          if (++count >= MAX_NUM_ROWS_TO_SHOW) {
            break;
          }
        }

        int excessRows = expectedRows.size() - MAX_NUM_ROWS_TO_SHOW;
        if (excessRows > 0) {
          sb.append(System.lineSeparator()).append("...and ").append(excessRows).append(" more expected rows.");
        }

        throw new ExpectedResultFailureException(opMetadata, commentDesc, actualResultDesc, sb.toString());
      }

      // Are there any expected rows left over?
      if (!remainingExpectedRows.isEmpty()) {
        String commentDesc = "There are no more actual rows to compare to the remaining expected rows.";

        StringBuilder sb = new StringBuilder(1024);
        int i = 0;

        for (ExpectedRow expectedRow : remainingExpectedRows) {
          if (++i > MAX_NUM_ROWS_TO_SHOW) {
            continue;
          }

          if (i > 1) {
            sb.append(System.lineSeparator());
          }

          // TODO: align all these rows.
          sb.append(expectedRow.toString());
        }

        if (i > MAX_NUM_ROWS_TO_SHOW) {
          sb.append(System.lineSeparator());
          sb.append("...and ").append(i - MAX_NUM_ROWS_TO_SHOW).append(" more expected rows.");
        }

        final String missingRows = sb.toString();
        final String actualDesc = "<no remaining rows>";
        throw new ExpectedResultFailureException(opMetadata, commentDesc, actualDesc, missingRows);
      }

      // All good!

    } catch (SQLException e) {
      final String commentDesc = "An exception was received instead of returning a ResultSet.";
      final String actualResultDesc = e.getClass().getSimpleName() + ": " + e.getMessage();
      throw new ExpectedResultFailureException(opMetadata, commentDesc, actualResultDesc, this.describe());
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
    final String actualResultDesc = "Affected row count: " + updateCount;
    throw new ExpectedResultFailureException(opMetadata, commentDesc, actualResultDesc, this.describe());
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
    throw new ExpectedResultFailureException(opMetadata, commentDesc, actualResultDesc, this.describe());
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
    throw new ExpectedResultFailureException(opMetadata, commentDesc, actualResultDesc, this.describe(), e);
  }

  /**
   * Compares the actual result, being a general purpose statement of success, with the expected result.
   *
   * @throws FailureException if the expected result does not match with this actual result.
   */
  @Override
  public void assertActualAsSuccess() throws FailureException
  {
    final String commentDesc = "The operation did not return a ResultSet as expected.";
    final String actualResultDesc = "success";
    throw new ExpectedResultFailureException(opMetadata, commentDesc, actualResultDesc, this.describe());
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
    throw new ExpectedResultFailureException(opMetadata, commentDesc, actualResultDesc, this.describe());
  }
}
