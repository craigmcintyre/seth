// Copyright (c) 2017 Boray Data Co. Ltd.  All rights reserved.

package com.rapidsdata.seth.plan.expectedResults;

public enum ExpectedResultType
{
  DONT_CARE,
  MUTE,
  SUCCESS,
  FAILURE_CODE_AND_MSG_PREFIX,
  FAILURE_CODE_AND_MSG_SUFFIX,
  FAILURE_CODE,
  FAILURE_MSG_PREFIX,
  FAILURE_MSG_SUFFIX,
  FAILURE_ANY,
  UNORDERED_ROWS,
  ORDERED_ROWS,
  ROW_COUNT,
  AFFECTED_ROWS
}
