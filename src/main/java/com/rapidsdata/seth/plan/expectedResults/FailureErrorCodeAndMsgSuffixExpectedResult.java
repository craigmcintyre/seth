// Copyright (c) 2017 Boray Data Co. Ltd.  All rights reserved.

package com.rapidsdata.seth.plan.expectedResults;

import com.rapidsdata.seth.contexts.AppContext;
import com.rapidsdata.seth.exceptions.ExpectedResultFailureException;
import com.rapidsdata.seth.exceptions.FailureException;
import com.rapidsdata.seth.plan.OperationMetadata;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * An expected result class where we expect the operation to have failed with a particular
 * error code and error message. The expected error message is only compared against the trailing end
 * (suffix) of the actual error message, that way the expected error message can leave off
 * any leading dynamic error string components such as names.
 */
public class FailureErrorCodeAndMsgSuffixExpectedResult extends ExpectedResult
{
  protected final int expectedErrCode;
  protected final String expectedErrMsg;

  /**
   * Constructor
   * @param description A textual description of the expected result.
   * @param opMetadata The metadata about the operation that produced the actual result.
   * @param appContext The application context container.
   * @param expectedErrCode the error code that is expected to be received.
   * @param expectedErrMsg the error message that is expected to be received.
   */
  public FailureErrorCodeAndMsgSuffixExpectedResult(String description, OperationMetadata opMetadata,
                                                    AppContext appContext, int expectedErrCode,
                                                    String expectedErrMsg)
  {
    super(ExpectedResultType.FAILURE_CODE_AND_MSG_SUFFIX, description, opMetadata, appContext);
    this.expectedErrCode = expectedErrCode;
    this.expectedErrMsg = expectedErrMsg;
  }

  /**
   * Compares the actual result, being a ResultSet, with the expected result.
   * @param rs The ResultSet to be compared to the expected result.
   * @throws FailureException if the expected result does not match with this actual result.
   */
  @Override
  public void assertActualAsResultSet(ResultSet rs) throws FailureException
  {
    // We expected failure, not a result set.
    final String commentDesc = "A ResultSet was returned instead of an error code and message.";
    final String actualResultDesc = "A ResultSet";
    throw new ExpectedResultFailureException(opMetadata, commentDesc, actualResultDesc, this);
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
    // We expected failure, not an update count.
    final String commentDesc = "An affected row count was returned instead of an error code and message.";
    final String actualResultDesc = "An update count was received";
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
    if (e.getErrorCode() != expectedErrCode) {
      final String commentDesc = "A different error code was returned than was expected.";
      final String actualDesc = "Error code: " + e.getErrorCode();
      throw new ExpectedResultFailureException(opMetadata, commentDesc, actualDesc, this);
    }

    // Compare the actual error message up to the length of the expected error message. i.e.
    // actualErrorMsg == expectedErrorMsg iff expectedErrorMsg is a leading substring
    // of actualErrorMsg.
    if (!e.getMessage().endsWith(expectedErrMsg)) {
      // The error messages differ.
      final String commentDesc = "A different error message was returned than was expected.";
      final String actualDesc = "Error message: " + e.getMessage();
      throw new ExpectedResultFailureException(opMetadata, commentDesc, actualDesc, this);
    }

    // otherwise all is ok.
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

    if (e instanceof SQLException) {
      assertActualAsException((SQLException) e);
    }

    final String commentDesc = "An exception was returned instead of an error code and message.";
    final String actualResultDesc = "Exception type: " + e.getClass().getName() + " (has no error code)";
    throw new ExpectedResultFailureException(opMetadata, commentDesc, actualResultDesc, this);
  }

  /**
   * Compares the actual result, being a general purpose statement of success, with the expected result.
   *
   * @throws FailureException if the expected result does not match with this actual result.
   */
  @Override
  public void assertActualAsSuccess() throws FailureException
  {
    // We expected failure, not a general purpose success.
    final String commentDesc = "The operation succeeeded instead of returning an error code.";
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
    final String commentDesc = "An error message was returned instead of an error code.";
    final String actualResultDesc = "Error message only: " + msg;
    throw new ExpectedResultFailureException(opMetadata, commentDesc, actualResultDesc, this);
  }
}
