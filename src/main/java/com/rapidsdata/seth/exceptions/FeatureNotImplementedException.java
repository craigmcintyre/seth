// Copyright (c) 2017 Boray Data Co. Ltd.  All rights reserved.

package com.rapidsdata.seth.exceptions;

/**
 * An unchecked exception class that indicates that a given code path is not yet implemented.
 */
public class FeatureNotImplementedException extends SethSystemException
{
  public FeatureNotImplementedException(String message)
  {
    super(message);
  }

  public FeatureNotImplementedException(String message, Throwable throwable)
  {
    super(message, throwable);
  }

  public FeatureNotImplementedException(Throwable t)
  {
    super(t);
  }
}
