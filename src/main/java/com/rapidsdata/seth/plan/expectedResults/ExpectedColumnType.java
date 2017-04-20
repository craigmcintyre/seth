// Copyright (c) 2017 Boray Data Co. Ltd.  All rights reserved.

package com.rapidsdata.seth.plan.expectedResults;

public enum ExpectedColumnType
{
  NULL              ("null"),
  BOOLEAN           (""),
  INTEGER           (""),
  DECIMAL           (""),
  FLOAT             (""),
  STRING            (""),
  DATE              (""),
  TIME              (""),
  TIMESTAMP         (""),
  INTERVAL          (""),
  DONT_CARE         ("*"),    // don't care of the value on the current column.
  IGNORE_REMAINING  ("...");  // don't care of the values of the current column and all remaining columns.

  /** The syntactic code that represents this value, if applicable. */
  private final String code;

  ExpectedColumnType(String code)
  {
    this.code = code;
  }

  /**
   * Returns the syntactic code that represents this value, if applicable.
   * @return the syntactic code that represents this value, if applicable.
   */
  public String getCode()
  {
    return code;
  }
}
