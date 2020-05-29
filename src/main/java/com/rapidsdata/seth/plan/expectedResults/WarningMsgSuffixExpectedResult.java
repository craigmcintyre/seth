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
import java.util.ArrayList;
import java.util.List;

/**
 * An expected result class where we expect the operation to have returned successfully
 * but with a particular warning message. The expected warning message must be the ending subset
 * of the actual warning response. Case is sensitive. There may be multiple warning messages
 * but at least one of them must match this expected result.
 */
public class WarningMsgSuffixExpectedResult extends ExpectedResult
{
  private static final String WARNING_DESC = "SqlWarning: ";
  protected final String expectedWarningMsg;

  /**
   * Constructor
   * @param description A textual description of the expected result.
   * @param opMetadata The metadata about the operation that produced the actual result.
   * @param appContext The application context container.
   * @param expectedWarningMsg the warning message that is expected to be received.
   */
  public WarningMsgSuffixExpectedResult(String description, OperationMetadata opMetadata,
                                        AppContext appContext, Options options, String expectedWarningMsg)
  {
    super(ExpectedResultType.WARNING_MSG_SUFFIX, description, opMetadata, appContext, options);
    this.expectedWarningMsg = expectedWarningMsg;
  }

  /** @return a string that formats all of the actual warnings returned. */
  private String formatWarnings(List<String> warningList)
  {
    if (warningList == null || warningList.isEmpty()) {
      return "<no warnings received>";

    } else if (warningList.size() == 1) {
      return WARNING_DESC + warningList.get(0);
    }

    StringBuilder sb = new StringBuilder(1024);

    for (String msg : warningList) {
      sb.append(WARNING_DESC).append(msg).append(System.lineSeparator());
    }

    return sb.toString();
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
    List<String> warningList = new ArrayList<>();

    // First iterate over the statement warnings.
    SQLWarning warn = warnings;

    while (warn != null) {
      String msg = warn.getMessage();
      warningList.add(msg);

      if (msg.trim().endsWith(expectedWarningMsg)) {
        return;
      }

      warn = warn.getNextWarning();
    }

    if (rs != null) {

      // But we've got a resultset here, and it can have different warnings, so we need to check them too.
      try {
        warn = rs.getWarnings();

      } catch (SQLException e) {
        final String commentDesc = "An exception was received instead of an warning message.";
        final String actualResultDesc = "An exception was received: " + e.getMessage();
        throw new ExpectedResultFailureException(opMetadata, commentDesc, actualResultDesc, this.describe());
      }

      while (warn != null) {
        String msg = warn.getMessage();
        warningList.add(msg);

        if (msg.trim().endsWith(expectedWarningMsg)) {
          return;
        }

        warn = warn.getNextWarning();
      }
    }

    // We expected a warning but either didn't get one or didn't get the right one.
    final String commentDesc = "The expected warning message was not received.";
    throw new ExpectedResultFailureException(opMetadata, commentDesc, formatWarnings(warningList), this.describe());
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
    assertWarnings(warnings, rs);
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
    assertWarnings(warnings);
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
    // We got an exception but we actually expected the command to succeed with warnings.
    final String commentDesc = "The operation failed with an error instead of returning a warning message.";
    final String actualResultDesc = "failure: '" + e.getMessage() + "'";
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
    // We got an exception but we actually expected the command to succeed with warnings.
    final String commentDesc = "The operation failed with an error instead of returning a warning message.";
    final String actualResultDesc = "failure: '" + e.getMessage() + "'";
    throw new ExpectedResultFailureException(opMetadata, commentDesc, actualResultDesc, this.describe());
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
    assertWarnings(warnings);
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
    // We got an exception but we actually expected the command to succeed with warnings.
    final String commentDesc = "The operation failed with an error instead of returning a warning message.";
    final String actualResultDesc = "failure: '" + msg + "'";
    throw new ExpectedResultFailureException(opMetadata, commentDesc, actualResultDesc, this.describe());
  }
}
