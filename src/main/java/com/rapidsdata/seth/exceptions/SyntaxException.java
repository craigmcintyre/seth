// Copyright (c) 2017 Boray Data Co. Ltd.  All rights reserved.

package com.rapidsdata.seth.exceptions;

import com.rapidsdata.seth.TestableFile;

import java.io.File;

public class SyntaxException extends PlanningException
{

  public SyntaxException(String message, TestableFile testableFile, int line, int pos, String near)
  {
    super(message, testableFile, line, pos, near);
  }
}
