// Copyright (c) 2017 Boray Data Co. Ltd.  All rights reserved.

package com.rapidsdata.seth.exceptions;

/**
 * An unchecked exception class that simply wraps another exception. For those situations
 * where you need to throw an exception but cannot throw a checked one because you cannot
 * change the function definition.
 */
public class SethBrownBagException extends SethSystemException
{
  public SethBrownBagException(String msg, Throwable t)
  {
    super(msg, t);
  }

  public SethBrownBagException(Throwable t)
  {
    super(t);
  }
}
