// Copyright (c) 2017 Boray Data Co. Ltd.  All rights reserved.

package com.rapidsdata.seth.exceptions;

import java.io.File;

public class SyntaxException extends PlanningException
{

  public SyntaxException(String message, File file, int line, int pos, String near)
  {
    super(message, file, line, pos, near);
  }
}
