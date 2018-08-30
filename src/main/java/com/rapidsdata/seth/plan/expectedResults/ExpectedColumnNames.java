// Copyright (c) 2017 Boray Data Co. Ltd.  All rights reserved.

package com.rapidsdata.seth.plan.expectedResults;

import com.rapidsdata.seth.exceptions.SethSystemException;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ExpectedColumnNames
{
  private final List<ExpectedColumnType> columnNameDefs;
  private final List<Object> columnNameValues;

  public ExpectedColumnNames(List<ExpectedColumnType> columnNameDefs, List<Object> columnNameValues)
  {
    this.columnNameDefs = columnNameDefs;
    this.columnNameValues = columnNameValues;
  }

  /**
   * Compares the expected column names to the column names in the ResultSet parameter.
   * @param rs The resultset
   * @return true if the column names compare equally or false if they are different.
   * @throws SQLException
   */
  public boolean compareTo(ResultSet rs) throws SQLException
  {
    ResultSetMetaData rsmd = rs.getMetaData();

    int actualColumnCount = rsmd.getColumnCount();
    int expectedColumnDefCount = columnNameDefs.size();

    // If the last column definition is not '...' then the number of expected columns
    // should equal the number of actual columns.
    if (columnNameDefs.get(expectedColumnDefCount - 1) != ExpectedColumnType.IGNORE_REMAINING &&
        actualColumnCount != expectedColumnDefCount) {
      return false;
    }

    // Compare column by column
    int defIndex = -1;

    while (++defIndex < columnNameDefs.size()) {

      ExpectedColumnType type = columnNameDefs.get(defIndex);
      int rsIndex = defIndex + 1; // rs.getXXXX() uses 1-based indexes.

      if (type == ExpectedColumnType.IGNORE_REMAINING) {
        // We don't care about comparing this column or any other remaining ones.
        break;
      }

      if (defIndex + 1 > actualColumnCount) {
        // We received less actual columns than we were expecting.
        return false;
      }

      String expectedVal = (String) columnNameValues.get(defIndex);
      String actualVal = rsmd.getColumnLabel(rsIndex);

      if (type == ExpectedColumnType.DONT_CARE) {
        // We don't care about comparing this column.
        continue;
      }

      // Otherwise it must be a string
      if (type != ExpectedColumnType.STRING) {
        throw new SethSystemException("Unhandled column type: " + type.name());
      }

      if (!expectedVal.equals(actualVal)) {
        return false;
      }
    }

    return true;
  }

  /**
   * Returns a string representation of these expected column names.
   * @return a string representation of these expected column names.
   */
  public String toString()
  {
    StringBuilder sb = new StringBuilder(128);

    sb.append('[');

    for (int index = 0; index < columnNameDefs.size(); index++) {

      ExpectedColumnType type = columnNameDefs.get(index);
      Object val = columnNameValues.get(index);

      switch (type) {
        case DONT_CARE:
        case IGNORE_REMAINING:
          sb.append(type.getCode());
          break;

        case STRING:
          sb.append("'");
          sb.append(val);
          sb.append("'");
          break;

        default:
          throw new SethSystemException("Unhandled data type: " + type.name());
      }

      sb.append(", ");
    }

    // Remove the ", " characters on the end.
    sb.delete(sb.length() - 2, sb.length());

    sb.append(']');

    return sb.toString();
  }
}
