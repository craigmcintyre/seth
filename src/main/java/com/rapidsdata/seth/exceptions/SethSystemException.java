// Copyright (c) 2017 Boray Data Co. Ltd.  All rights reserved.

package com.rapidsdata.seth.exceptions;

/**
 * The base class for all unchecked exceptions in Seth.
 */
public class SethSystemException extends RuntimeException
{
  public SethSystemException(String message)
  {
    super(message);
  }

  public SethSystemException(String message, Throwable throwable)
  {
    super(message, throwable);
  }

  public SethSystemException(Throwable t)
  {
    super(t);
  }
}
