// Copyright (c) 2017 Boray Data Co. Ltd.  All rights reserved.

package com.rapidsdata.seth.exceptions;

/** Thrown if the an invalid operation was being performed on the default connection. */
public class DefaultConnectionNameException extends ConnectionException
{
  public DefaultConnectionNameException(String message)
  {
    super(message);
  }

  public DefaultConnectionNameException(String message, Throwable throwable)
  {
    super(message, throwable);
  }

  public DefaultConnectionNameException(Throwable t)
  {
    super(t);
  }
}
