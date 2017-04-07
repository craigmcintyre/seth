// Copyright (c) 2017 Boray Data Co. Ltd.  All rights reserved.

package com.rapidsdata.seth.exceptions;

import java.io.File;

public class SemanticException extends SethException
{
  private final File file;
  private final int line;
  private final int pos;
  private final String near;

  public SemanticException(String message, File file, int line, int pos, String near)
  {
    super(message);
    this.file = file;
    this.line = line;
    this.pos = pos;
    this.near = near;
  }

  public File getFile()
  {
    return file;
  }

  public int getLine()
  {
    return line;
  }

  public int getPos()
  {
    return pos;
  }

  public String getNear()
  {
    return near;
  }
}
