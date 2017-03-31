// Copyright (c) 2017 Boray Data Co. Ltd.  All rights reserved.

package com.rapidsdata.seth;

/** How do we work with relative paths? */
public enum PathRelativity
{
  CWD,    // paths are relative to the current working directory
  REFERER // paths are relative to the path of the parent that is referencing you.
}
