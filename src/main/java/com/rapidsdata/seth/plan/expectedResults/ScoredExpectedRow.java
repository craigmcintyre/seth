// Copyright (c) 2019 Boray Data Co. Ltd.  All rights reserved.

package com.rapidsdata.seth.plan.expectedResults;

/** An ExpectedRow that has a score associated with it, sortable by that score. */
public class ScoredExpectedRow extends ExpectedRow implements Comparable<ScoredExpectedRow>
{
  protected final double score;
  protected final int insertOrder;

  public ScoredExpectedRow(double score, int insertOrder, ExpectedRow row)
  {
    super(row);

    this.score = score;
    this.insertOrder = insertOrder;
  }

  public double getScore()
  {
    return score;
  }

  @Override
  public int compareTo(ScoredExpectedRow o)
  {
    if (o == null)    { return -1; }
    if (o == this)    { return 0;  }

    ScoredExpectedRow row = (ScoredExpectedRow) o;

    if (this.score < row.score)    { return -1; }
    if (this.score > row.score)    { return 1; }

    return (this.insertOrder < row.insertOrder ? -1 : 1);
  }

  @Override
  public boolean equals(Object o)
  {
    if (o == null)  { return false; }
    if (o == this)  { return true; }
    if (!(o instanceof ScoredExpectedRow))  { return false; }

    ScoredExpectedRow row = (ScoredExpectedRow) o;

    if (this.score != row.score)              { return false; }
    if (this.insertOrder != row.insertOrder)  { return false; }

    return true;
  }
}
