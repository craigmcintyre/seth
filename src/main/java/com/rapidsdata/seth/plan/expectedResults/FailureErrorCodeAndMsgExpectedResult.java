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
 * error code.
 */
public class FailureErrorCodeAndMsgExpectedResult extends ExpectedResult
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
  public FailureErrorCodeAndMsgExpectedResult(String description, OperationMetadata opMetadata,
                                              AppContext appContext, int expectedErrCode,
                                              String expectedErrMsg)
  {
    super(ExpectedResultType.FAILURE_CODE_AND_MSG, description, opMetadata, appContext);
    this.expectedErrCode = expectedErrCode;
    this.expectedErrMsg = expectedErrMsg;
  }

  /**
   * Compares the actual result, being a ResultSet, with the expected result.
   * @param rs The ResultSet to be compared to the expected result.
   * @throws FailureException if the expected result does not match with this actual result.
   */
  @Override
  public void compareActualAsResultSet(ResultSet rs) throws FailureException
  {
    // We expected failure, not a result set.
    String actualResultDesc = "A ResultSet was received";
    throw new ExpectedResultFailureException(opMetadata, actualResultDesc, this);
  }

  /**
   * Compares the actual result, being an update count, with the expected result.
   *
   * @param updateCount The update count to be compared to the expected result.
   * @throws FailureException if the expected result does not match with this actual result.
   */
  @Override
  public void compareActualAsUpdateCount(int updateCount) throws FailureException
  {
    // We expected failure, not an update count.
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
  public void compareActualAsException(SQLException e) throws FailureException
  {
    if (e.getErrorCode() != expectedErrCode) {
      throw new ExpectedResultFailureException(opMetadata, "Error code: " + e.getErrorCode(), this);
    }

    // Compare the actual error message up to the length of the expected error message. i.e.
    // actualErrorMsg == expectedErrorMsg iff expectedErrorMsg is a leading substring
    // of actualErrorMsg.
    if (!e.getMessage().startsWith(expectedErrMsg)) {
      // The error messages differ.
      throw new ExpectedResultFailureException(opMetadata, "Error message: " + e.getMessage(), this);
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
  public void compareActualAsException(Exception e) throws FailureException
  {
    // actual == expected iff the error message of the expected result is a leading substring
    // of the error message of the actual.

    if (e instanceof SQLException) {
      compareActualAsException((SQLException) e);
    }

    String actualResultDesc = "Exception type: " + e.getClass().getName() + " (has no error code)";
    throw new ExpectedResultFailureException(opMetadata, actualResultDesc, this);
  }

  /**
   * Compares the actual result, being a general purpose statement of success, with the expected result.
   *
   * @throws FailureException if the expected result does not match with this actual result.
   */
  @Override
  public void compareActualAsSuccess() throws FailureException
  {
    // We expected failure, not a general purpose success.
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
  public void compareActualAsFailure(String msg) throws FailureException
  {
    String actualResultDesc = "Error message only: " + msg;
    throw new ExpectedResultFailureException(opMetadata, actualResultDesc, this);
  }
}
