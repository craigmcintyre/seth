// Copyright (c) 2017 Boray Data Co. Ltd.  All rights reserved.

package com.rapidsdata.seth.exceptions;

/**
 * A checked exception that indicates that a given interval string could
 * not be parsed.
 */
public class IntervalParsingException extends SethException
{
  /** The string that was being parsed. */
  private final String intervalStr;


  public IntervalParsingException(String message, String intervalStr)
  {
    super(message);
    this.intervalStr = intervalStr;
  }



  public String getIntervalStr()
  {
    return intervalStr;
  }
}
