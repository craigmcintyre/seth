// Copyright (c) 2017 Boray Data Co. Ltd.  All rights reserved.

package com.rapidsdata.seth.plan.expectedResults;

public enum ExpectedResultType
{
  DONT_CARE,
  MUTE,
  SUCCESS,
  FAILURE_CODE_AND_MSG,
  FAILURE_CODE,
  FAILURE_MSG,
  FAILURE_ANY,
  UNORDERED_ROWS,
  ORDERED_ROWS,
  ROW_COUNT,
  AFFECTED_ROWS
}
