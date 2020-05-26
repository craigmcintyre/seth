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
 * An expected result class where we expect the operation to have failed with a particular
 * error message. The actual error message is only compared up to the length of the
 * expected error message, that way the expected error message can leave off any dynamic
 * error string components such as names.
 */
public class FailureErrorMsgPrefixExpectedResult extends ExpectedResult
{
  protected final String expectedErrMsg;

  /**
   * Constructor
   * @param description A textual description of the expected result.
   * @param opMetadata The metadata about the operation that produced the actual result.
   * @param appContext The application context container.
   * @param expectedErrMsg the error message that is expected to be received.
   */
  public FailureErrorMsgPrefixExpectedResult(String description, OperationMetadata opMetadata,
                                             AppContext appContext, String expectedErrMsg)
  {
    super(ExpectedResultType.FAILURE_MSG_PREFIX, description, opMetadata, appContext);
    this.expectedErrMsg = expectedErrMsg;
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
    // We expected failure, not a result set.
    final String commentDesc = "A ResultSet was received instead of an error message.";
    final String actualResultDesc = "A ResultSet";
    throw new ExpectedResultFailureException(opMetadata, commentDesc, actualResultDesc, this.describe());
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
    // We expected failure, not an update count.
    final String commentDesc = "A affected row count was received instead of an error message.";
    final String actualResultDesc = "An affected row count was received";
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
    assertActualAsException((Exception) e);
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
    // actual == expected iff the error message of the expected result is a leading substring
    // of the error message of the actual.

    if (!e.getMessage().startsWith(expectedErrMsg)) {
      // The error messages differ.
      final String commentDesc = "A different error message was received than was expected.";
      final String actualDesc = "Error message: " + e.getMessage();
      throw new ExpectedResultFailureException(opMetadata, commentDesc, actualDesc, this.describe());
    }

    // All ok.
  }

  /**
   * Compares the actual result, being a general purpose statement of success, with the expected result.
   * @param warnings Any warnings from executing the statement. May be null.
   * @throws FailureException if the expected result does not match with this actual result.
   */
  @Override
  public void assertActualAsSuccess(SQLWarning warnings) throws FailureException
  {
    // We expected failure, not a general purpose success.
    final String commentDesc = "The operation succeeded instead of returning an error message.";
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
    // actual == expected iff the error message of the expected result is a leading substring
    // of the error message of the actual.

    if (!msg.startsWith(expectedErrMsg)) {
      // The error messages differ.
      final String commentDesc = "A different error message was received than was expected.";
      final String actualDesc = "failure: '" + msg + "'";
      throw new ExpectedResultFailureException(opMetadata, commentDesc, actualDesc, this.describe());
    }

    // All ok.
  }
}
