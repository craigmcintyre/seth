// Copyright (c) 2017 Boray Data Co. Ltd.  All rights reserved.

package com.rapidsdata.seth.exceptions;

import com.rapidsdata.seth.TestableFile;

import java.io.File;

public class SemanticException extends PlanningException
{

  public SemanticException(String message, TestableFile testableFile, int line, int pos, String command)
  {
    super(message, testableFile, line, pos, null, command);
  }
}
