// Copyright (c) 2017 Boray Data Co. Ltd.  All rights reserved.

package com.rapidsdata.seth.exceptions;

import java.io.File;

public abstract class PlanningException extends SethException
{
  /** The file that the error occurred in. Should be non-null. */
  private final File file;

  /** The line that the error occurred on. Can be -1 if not applicable. */
  private final int line;

  /** The position on the line that the error occurred on. Can be -1 if not applicable. */
  private final int pos;

  /** The text of the token near where the error occurred. Can be null if not applicable. */
  private final String near;

  /** The parsed command where the error occurred. Can be null if not applicable. */
  private final String command;

  public PlanningException(String message, File file, int line, int pos, String near)
  {
    super(message);
    this.file = file;
    this.line = line;
    this.pos = pos;
    this.near = near;
    this.command = null;
  }

  public PlanningException(String message, File file, int line, int pos, String near, String command)
  {
    super(message);
    this.file = file;
    this.line = line;
    this.pos = pos;
    this.near = near;
    this.command = command;
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

  public String getCommand()
  {
    return command;
  }
}
