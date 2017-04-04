// Copyright (c) 2017 Boray Data Co. Ltd.  All rights reserved.

package com.rapidsdata.seth.exceptions;

/** Thrown if the name associated with a given connection object is not found or invalid. */
public class BadConnectionNameException extends ConnectionException
{
  public BadConnectionNameException(String message)
  {
    super(message);
  }

  public BadConnectionNameException(String message, Throwable throwable)
  {
    super(message, throwable);
  }

  public BadConnectionNameException(Throwable t)
  {
    super(t);
  }
}
