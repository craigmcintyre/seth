// Copyright (c) 2019 Boray Data Co. Ltd.  All rights reserved.

package com.rapidsdata.seth.plan.expectedResults;

import com.rapidsdata.seth.contexts.AppContext;
import com.rapidsdata.seth.plan.OperationMetadata;

import java.util.List;

public abstract class RowDataExpectedResult extends ExpectedResult
{
  protected static final int MAX_NUM_ROWS_TO_SHOW = 10;

  protected final List<ExpectedRow> expectedRows;
  protected final ExpectedColumnNames expectedColumnNames;

  public RowDataExpectedResult(ExpectedResultType type, String description, OperationMetadata opMetadata,
                               AppContext ctx, List<ExpectedRow> expectedRows, ExpectedColumnNames expectedColumnNames)
  {
    super(type, description, opMetadata, ctx);
    this.expectedRows = expectedRows;
    this.expectedColumnNames = expectedColumnNames;
  }
}
