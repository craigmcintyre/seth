// Copyright (c) 2019 Boray Data Co. Ltd.  All rights reserved.

package com.rapidsdata.seth.plan.expectedResults;

/** A container of alignment information. */
public class AlignmentInfo
{
  /** The maximum width of each column in a result. */
  public final int[] columnWidths;

  /**
   * An array of boolean values indicating whether padding should go on the left or right of the column value.
   * true for padding on the left, false for padding on the right.
   */
  public final boolean[] padLefts;

  public AlignmentInfo(int[] columnWidths, boolean[] padLefts)
  {
    this.columnWidths = columnWidths;
    this.padLefts = padLefts;
  }
}
