// Copyright (c) 2017 Boray Data Co. Ltd.  All rights reserved.

package com.rapidsdata.seth.plan.expectedResults;

import com.rapidsdata.seth.contexts.AppContext;
import com.rapidsdata.seth.exceptions.ExpectedResultFailureException;
import com.rapidsdata.seth.exceptions.FailureException;
import com.rapidsdata.seth.plan.OperationMetadata;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.util.ArrayList;
import java.util.List;

/**
 * An expected result class where we expect the operation to have returned successfully
 * but with a warning message. The contents of the warning message is not important for
 * this particular expected result.
 */
public class WarningAnyExpectedResult extends ExpectedResult
{
  private static final String WARNING_DESC = "SqlWarning: ";

  /**
   * Constructor
   * @param description A textual description of the expected result.
   * @param opMetadata The metadata about the operation that produced the actual result.
   * @param appContext The application context container.
   */
  public WarningAnyExpectedResult(String description, OperationMetadata opMetadata, AppContext appContext)
  {
    super(ExpectedResultType.WARNING_ANY, description, opMetadata, appContext);
  }

  /**
   * Assert that we got a warning message that contains the expected warning substring.
   * @param warnings Statement warnings to check. Can be null.
   */
  private void assertWarnings(SQLWarning warnings) throws FailureException
  {
    assertWarnings(warnings, null);
  }

  /**
   * Assert that we got a warning message that contains the expected warning substring.
   * @param warnings Statement warnings to check. Can be null.
   * @param rs An optional ResultSet which may contain warnings to be checked too. Can be null.
   */
  private void assertWarnings(SQLWarning warnings, ResultSet rs) throws FailureException
  {
    if (warnings != null) {
      // We got a warning, that's all we need!
      return;
    }

    if (rs != null) {

      // But we've got a resultset here, and it can have different warnings, so we need to check them too.
      try {
        SQLWarning warn = rs.getWarnings();

        if (warn != null) {
          // We got a warning, that's all we need!
          return;
        }

      } catch (SQLException e) {
        final String commentDesc = "An exception was received instead of an warning message.";
        final String actualResultDesc = "An exception was received: " + e.getMessage();
        throw new ExpectedResultFailureException(opMetadata, commentDesc, actualResultDesc, this.describe());
      }
    }

    // We expected a warning but didn't get any.
    final String commentDesc = "Expected to get a warning message but none were received.";
    final String actualDesc = "<No warning messages were received>";
    throw new ExpectedResultFailureException(opMetadata, commentDesc, actualDesc, this.describe());
  }

  /**
   * Compares the actual result, being a ResultSet, with the expected result.
   * @param rs The ResultSet to be compared to the expected result.
   * @param warnings Any warnings from executing the statement. May be null.
   * @throws FailureException if the expected result does not match with this actual result.
   */
  @Override
  public void assertActualAsResultSet(ResultSet rs, SQLWarning warnings) throws FailureException
  {
    assertWarnings(warnings, rs);
  }

  /**
   * Compares the actual result, being an update count, with the expected result.
   *
   * @param updateCount The update count to be compared to the expected result.
   * @param warnings Any warnings from executing the statement. May be null.
   * @throws FailureException if the expected result does not match with this actual result.
   */
  @Override
  public void assertActualAsUpdateCount(long updateCount, SQLWarning warnings) throws FailureException
  {
    assertWarnings(warnings);
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
    // We got an exception but we actually expected the command to succeed with warnings.
    final String commentDesc = "The operation failed with an error instead of returning a warning message.";
    final String actualResultDesc = "failure: '" + e.getMessage() + "'";
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
    // We got an exception but we actually expected the command to succeed with warnings.
    final String commentDesc = "The operation failed with an error instead of returning a warning message.";
    final String actualResultDesc = "failure: '" + e.getMessage() + "'";
    throw new ExpectedResultFailureException(opMetadata, commentDesc, actualResultDesc, this.describe());
  }

  /**
   * Compares the actual result, being a general purpose statement of success, with the expected result.
   * @param warnings Any warnings from executing the statement. May be null.
   * @throws FailureException if the expected result does not match with this actual result.
   */
  @Override
  public void assertActualAsSuccess(SQLWarning warnings) throws FailureException
  {
    assertWarnings(warnings);
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
    // We got an exception but we actually expected the command to succeed with warnings.
    final String commentDesc = "The operation failed with an error instead of returning a warning message.";
    final String actualResultDesc = "failure: '" + msg + "'";
    throw new ExpectedResultFailureException(opMetadata, commentDesc, actualResultDesc, this.describe());
  }
}
