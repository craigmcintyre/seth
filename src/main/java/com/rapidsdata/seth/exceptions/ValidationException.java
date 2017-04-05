// Copyright (c) 2017 Boray Data Co. Ltd.  All rights reserved.

package com.rapidsdata.seth.exceptions;

/** An exception thrown when an operation fails to be validated. */
public class ValidationException extends SethException
{
  public ValidationException(String message)
  {
    super(message);
  }

  public ValidationException(String message, Throwable throwable)
  {
    super(message, throwable);
  }

  public ValidationException(Throwable t)
  {
    super(t);
  }
}
