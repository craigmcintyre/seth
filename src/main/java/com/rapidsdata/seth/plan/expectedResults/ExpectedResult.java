// Copyright (c) 2017 Boray Data Co. Ltd.  All rights reserved.

package com.rapidsdata.seth.plan.expectedResults;

import com.rapidsdata.seth.Options;
import com.rapidsdata.seth.contexts.AppContext;
import com.rapidsdata.seth.contexts.ExecutionContext;
import com.rapidsdata.seth.exceptions.FailureException;
import com.rapidsdata.seth.plan.OperationMetadata;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;

public abstract class ExpectedResult
{
  /** The type of expected result this is. */
  protected final ExpectedResultType type;

  /** A textual description of the expected result. */
  protected final String description;

  /** The metadata about the operation that produced the actual result. */
  protected final OperationMetadata opMetadata;

  /** A container of information and objects useful to the application. */
  protected final AppContext appContext;

  /** Any hints and options that apply to this entire expected result. */
  protected final Options resultOptions;

  /**
   * Constructor
   * @param type The type of expected result this is.
   * @param description A textual description of the expected result.
   * @param opMetadata The metadata about the operation that produced the actual result.
   */
  protected ExpectedResult(ExpectedResultType type, String description, OperationMetadata opMetadata, AppContext ctx, Options resultOptions)
  {
    this.type = type;
    this.description = description;
    this.opMetadata = opMetadata;
    this.appContext = ctx;
    this.resultOptions = resultOptions;
  }

  /**
   * Returns a string description of what was actually expected from this result.
   * @return a string description of what was actually expected from this result.
   */
  public String describe()
  {
    return description;
  }

  /**
   * Compares the actual result, being a ResultSet, with the expected result.
   * @param xContext The context that the operator was executed within.
   * @param rs The ResultSet to be compared to the expected result.
   * @param warnings Any warnings from executing the statement. May be null.
   * @throws FailureException if the expected result does not match with this actual result.
   */
  public abstract void assertActualAsResultSet(ExecutionContext xContext, ResultSet rs, SQLWarning warnings) throws FailureException;

  /**
   * Compares the actual result, being an update count, with the expected result.
   * @param xContext The context that the operator was executed within.
   * @param updateCount The update count to be compared to the expected result.
   * @param warnings Any warnings from executing the statement. May be null.
   * @throws FailureException if the expected result does not match with this actual result.
   */
  public abstract void assertActualAsUpdateCount(ExecutionContext xContext, long updateCount, SQLWarning warnings) throws FailureException;

  /**
   * Compares the actual result, being a SQLException, with the expected result.
   * Stack trace will not be included.
   * @param xContext The context that the operator was executed within.
   * @param e The exception to be compared to the expected result.
   * @throws FailureException if the expected result does not match with this actual result.
   */
  public abstract void assertActualAsException(ExecutionContext xContext, SQLException e) throws FailureException;

  /**
   * Compares the actual result, being an Exception, with the expected result.
   * Because this is a general exception, the stack trace will be included.
   * @param xContext The context that the operator was executed within.
   * @param e The exception to be compared to the expected result.
   * @throws FailureException if the expected result does not match with this actual result.
   */
  public abstract void assertActualAsException(ExecutionContext xContext, Exception e) throws FailureException;

  /**
   * Compares the actual result, being a general purpose statement of success, with the expected result.
   * @param xContext The context that the operator was executed within.
   * @param warnings Any warnings from executing the statement. May be null.
   * @throws FailureException if the expected result does not match with this actual result.
   */
  public abstract void assertActualAsSuccess(ExecutionContext xContext, SQLWarning warnings) throws FailureException;

  /**
   * Compares the actual result, being a general purpose failure with an error message, with the expected result.
   * @param xContext The context that the operator was executed within.
   * @param msg The error message to be compared to the expected result.
   * @throws FailureException if the expected result does not match with this actual result.
   */
  public abstract void assertActualAsFailure(ExecutionContext xContext, String msg) throws FailureException;
}
