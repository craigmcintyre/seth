// Copyright (c) 2017 Boray Data Co. Ltd.  All rights reserved.

package com.rapidsdata.seth.exceptions;

/** Thrown if there is a conflict with the name associated with a given Connection object. */
public class ConnectionNameExistsException extends ConnectionException
{
  public ConnectionNameExistsException(String message)
  {
    super(message);
  }

  public ConnectionNameExistsException(String message, Throwable throwable)
  {
    super(message, throwable);
  }

  public ConnectionNameExistsException(Throwable t)
  {
    super(t);
  }
}
