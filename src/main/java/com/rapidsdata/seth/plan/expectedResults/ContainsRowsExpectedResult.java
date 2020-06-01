// Copyright (c) 2017 Boray Data Co. Ltd.  All rights reserved.

package com.rapidsdata.seth.plan.expectedResults;

import com.rapidsdata.seth.Options;
import com.rapidsdata.seth.contexts.AppContext;
import com.rapidsdata.seth.contexts.ExecutionContext;
import com.rapidsdata.seth.exceptions.ExpectedResultFailureException;
import com.rapidsdata.seth.exceptions.FailureException;
import com.rapidsdata.seth.exceptions.SethBrownBagException;
import com.rapidsdata.seth.plan.OperationMetadata;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;


/**
 * An expected result class where we expect the operation to have returned set of rows that
 * either must contain a subset of rows, or must not contain any rows from a given subset.
 */
public class ContainsRowsExpectedResult extends RowDataExpectedResult
{
  /**
   * If true, the actual resultset must not contain any of the expected rows.
   *  If false, the actual resultset must contain all of the expected rows.
   */
  private final boolean invert;


  /**
   * Constructor
   * @param description A textual description of the expected result.
   * @param opMetadata The metadata about the operation that produced the actual result.
   * @param appContext The application context container.
   * @param invert Treat the list of expected rows as rows that must not be contained in the result.
   * @param expectedRows The list of rows expected to be returned by the operation.
   */
  public ContainsRowsExpectedResult(String description,
                                    OperationMetadata opMetadata,
                                    AppContext appContext,
                                    Options options,
                                    boolean invert,
                                    List<ExpectedRow> expectedRows)
  {
    super(ExpectedResultType.CONTAINS_ROWS, description, opMetadata, appContext, options, expectedRows, null);

    this.invert = invert;
  }

  /**
   * Returns a string description of what was actually expected from this result.
   * @return a string description of what was actually expected from this result.
   */
  public String describe()
  {
    // The description is a subset of the expected rows.
    try {
      final String expectedResultDesc = ResultSetFormatter.describeExpectedRows(expectedRows, MAX_NUM_ROWS_TO_SHOW);
      return expectedResultDesc;

    } catch (SQLException e) {
      throw new SethBrownBagException(e);
    }
  }

  /**
   * Compares the actual result, being a ResultSet, with the expected result.
   * @param xContext The context that the operator was executed within.
   * @param rs The ResultSet to be compared to the expected result.
   * @param warnings Any warnings from executing the statement. May be null.
   * @throws FailureException if the expected result does not match with this actual result.
   */
  @Override
  public void assertActualAsResultSet(ExecutionContext xContext, ResultSet rs, SQLWarning warnings) throws FailureException
  {
    if (invert) {
      assertActualDoesNotContainExpectedRows(xContext, rs);

    } else {
      assertActualContainsAllExpectedRows(xContext, rs);
    }
  }

  private void assertActualDoesNotContainExpectedRows(ExecutionContext xContext, ResultSet rs) throws FailureException
  {
    LinkedList<Options> optionList = Options.listOf(xContext.getAppOptions(), xContext.getTestOptions(), resultOptions);

    try {
      // For each actual row
      while (rs.next()) {

        Iterator<ExpectedRow> erIterator = expectedRows.iterator();

        while (erIterator.hasNext()) {
          ExpectedRow expectedRow = erIterator.next();

          // Compare the current actual row to this expected row.
          if (expectedRow.compareTo(rs, optionList)) {
            // The current row matches one that we shouldn't have.

            final String commentDesc = "A row was returned that matches one on the 'DOES NOT CONTAIN' expected row list.";

            AlignmentInfo alignment = ResultSetFormatter.alignRows(rs, expectedRows);

            final String actualResultDesc = ResultSetFormatter.describeCurrentRow(rs, alignment.columnWidths);
            final String expectedRowDesc  = ResultSetFormatter.describeExpectedRows(expectedRows, alignment, MAX_NUM_ROWS_TO_SHOW);

            throw new ExpectedResultFailureException(opMetadata, commentDesc, actualResultDesc, expectedRowDesc);
          }
        }
      }

      // All good!

    } catch (SQLException e) {
      final String commentDesc = "An exception was received instead of returning a ResultSet.";
      final String actualResultDesc = e.getClass().getSimpleName() + ": " + e.getMessage();
      throw new ExpectedResultFailureException(opMetadata, commentDesc, actualResultDesc, this.describe());
    }
  }

  private void assertActualContainsAllExpectedRows(ExecutionContext xContext, ResultSet rs) throws FailureException
  {
    try {
      // Make a copy of the expected row list so we can remove entries from it as we match them.
      List<ExpectedRow> remainingExpectedRows = new LinkedList<ExpectedRow>(expectedRows);

      LinkedList<Options> optionList = Options.listOf(xContext.getAppOptions(), xContext.getTestOptions(), resultOptions);

      // For each actual row
      while (rs.next()) {

        // Have we run out of expected rows to compare actual rows to?
        if (remainingExpectedRows.isEmpty()) {
          // We found the expected subset and can ignore all the other rows.
          return;
        }

        Iterator<ExpectedRow> erIterator = remainingExpectedRows.iterator();

        // For each expected row remaining
        while (erIterator.hasNext()) {
          ExpectedRow expectedRow = erIterator.next();

          // Compare the current actual row to this expected row.
          if (expectedRow.compareTo(rs, optionList)) {
            // We got a match! Remove this expected row.
            erIterator.remove();

            if (remainingExpectedRows.isEmpty()) {
              // We found all expected rows
              return;
            }

            // Still more expected rows to find.
            break;
          }

          // No match for this expected and actual row. Try another expected row.
        }

        // Actual row doesn't match any expected rows. That's ok so long as we find all expected rows.

      } // for each actual row


      // Are there any expected rows left over?
      if (!remainingExpectedRows.isEmpty()) {
        String commentDesc;

        if (remainingExpectedRows.size() == 1) {
          commentDesc = "An expected row was not found in the actual resultset.";
        } else {
          commentDesc = "The following expected rows were not found in the actual resultset.";
        }

        final String expectedRowsDesc = ResultSetFormatter.describeExpectedRows(rs.getMetaData(), remainingExpectedRows, MAX_NUM_ROWS_TO_SHOW);
        final String actualDesc = "<no remaining rows>";
        throw new ExpectedResultFailureException(opMetadata, commentDesc, actualDesc, expectedRowsDesc);
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
   * @param xContext The context that the operator was executed within.
   * @param updateCount The update count to be compared to the expected result.
   * @param warnings Any warnings from executing the statement. May be null.
   * @throws FailureException if the expected result does not match with this actual result.
   */
  @Override
  public void assertActualAsUpdateCount(ExecutionContext xContext, long updateCount, SQLWarning warnings) throws FailureException
  {
    // Not what was expected.
    final String commentDesc = "An affected row count was received instead of a ResultSet.";
    final String actualResultDesc = "affected: " + updateCount;
    throw new ExpectedResultFailureException(opMetadata, commentDesc, actualResultDesc, this.describe());
  }

  /**
   * Compares the actual result, being a SQLException, with the expected result.
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
   * Compares the actual result, being an Exception, with the expected result.
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
   * Compares the actual result, being a general purpose statement of success, with the expected result.
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
   * Compares the actual result, being a general purpose failure with an error message, with the expected result.
   * @param xContext The context that the operator was executed within.
   * @param msg The error message to be compared to the expected result.
   * @throws FailureException if the expected result does not match with this actual result.
   */
  @Override
  public void assertActualAsFailure(ExecutionContext xContext, String msg) throws FailureException
  {
    // Not what was expected.
    final String commentDesc = "An error message was received instead of a ResultSet.";
    final String actualResultDesc = "Error message: " + msg;
    throw new ExpectedResultFailureException(opMetadata, commentDesc, actualResultDesc, this.describe());
  }
}
