// Copyright (c) 2017 Boray Data Co. Ltd.  All rights reserved.

package com.rapidsdata.seth.exceptions;

/**
 * The base class for all checked exceptions in Seth.
 */
public class SethException extends Exception
{
  public SethException(String message)
  {
    super(message);
  }

  public SethException(String message, Throwable throwable)
  {
    super(message, throwable);
  }

  public SethException(Throwable t)
  {
    super(t);
  }
}
