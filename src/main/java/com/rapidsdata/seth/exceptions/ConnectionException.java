// Copyright (c) 2017 Boray Data Co. Ltd.  All rights reserved.

package com.rapidsdata.seth.exceptions;

/** A base exception class relating to Connection management issues. */
public class ConnectionException extends SethException
{

  public ConnectionException(String message)
  {
    super(message);
  }

  public ConnectionException(String message, Throwable throwable)
  {
    super(message, throwable);
  }

  public ConnectionException(Throwable t)
  {
    super(t);
  }
}
