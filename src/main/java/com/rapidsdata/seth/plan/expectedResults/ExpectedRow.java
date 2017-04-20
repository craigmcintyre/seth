// Copyright (c) 2017 Boray Data Co. Ltd.  All rights reserved.

package com.rapidsdata.seth.plan.expectedResults;

import com.rapidsdata.seth.exceptions.SethSystemException;

import java.math.BigDecimal;
import java.sql.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ExpectedRow
{
  private final List<ExpectedColumnType> columnDefs;
  private final List<Object> columnValues;

  public ExpectedRow(List<ExpectedColumnType> columnDefs, List<Object> columnValues)
  {
    this.columnDefs = columnDefs;
    this.columnValues = columnValues;
  }

  /**
   * Compares the expected row to the row that the cursor is at in the ResultSet parameter.
   * @param rs The resultset which has the cursor on the current row to be compared.
   * @return true if the rows compare equally or false if they are different.
   * @throws SQLException
   */
  public boolean compareTo(ResultSet rs) throws SQLException
  {
    ResultSetMetaData rsmd = rs.getMetaData();

    int actualColumnCount = rsmd.getColumnCount();
    int expectedColumnDefCount = columnDefs.size();

    // If the last column definition is not '...' then the number of expected columns
    // should equal the number of actual columns.
    if (columnDefs.get(expectedColumnDefCount - 1) != ExpectedColumnType.IGNORE_REMAINING &&
        actualColumnCount != expectedColumnDefCount) {
      return false;
    }

    // Compare column by column
    int defIndex = -1;

    while (++defIndex < columnDefs.size()) {

      ExpectedColumnType type = columnDefs.get(defIndex);
      int rsIndex = defIndex + 1; // rs.getXXXX() uses 1-based indexes.

      if (type == ExpectedColumnType.IGNORE_REMAINING) {
        // We don't care about comparing this column or any other remaining ones.
        break;
      }

      if (defIndex + 1 > actualColumnCount) {
        // We received less actual columns than we were expecting.
        return false;
      }

      Object expectedVal = columnValues.get(defIndex);
      Object actualVal = rs.getObject(rsIndex);
      boolean wasNull = rs.wasNull();

      switch (type) {
        case DONT_CARE:
          // We don't care about comparing this column.
          continue;

        case NULL:
          if (!wasNull) {
            return false;
          }
          break;

        case BOOLEAN:
          boolean expectedBoolean = (boolean) expectedVal;
          if (wasNull || expectedBoolean != rs.getBoolean(rsIndex)) {
            return false;
          }
          break;

        case INTEGER:
          long expectedLong = (long) expectedVal;
          if (wasNull || expectedLong != rs.getLong(rsIndex)) {
            return false;
          }
          break;

        case DECIMAL:
          BigDecimal expectedDecimal = (BigDecimal) expectedVal;
          if (wasNull || !expectedDecimal.equals(rs.getBigDecimal(rsIndex))) {
            return false;
          }
          break;

        case FLOAT:
          double expectedDouble = (double) expectedVal;
          // Only compare floating points up to the level of precision specified in the expected value
          ComparableFloat cf = (ComparableFloat) expectedVal;
          if (wasNull || !cf.comparesTo(rs.getString(rsIndex))) {
            return false;
          }
          break;

        case STRING:
          String expectedString = (String) expectedVal;
          if (wasNull || !expectedString.equals(rs.getString(rsIndex))) {
            return false;
          }
          break;

        case DATE:
          LocalDate expectedDate = (LocalDate) expectedVal;

          if (wasNull) {
            return false;
          }

          LocalDate actualDate = rs.getDate(rsIndex).toLocalDate();
          if (!expectedDate.equals(actualDate)) {
            return false;
          }
          break;

        case TIME:
          LocalTime expectedTime = (LocalTime) expectedVal;

          if (wasNull) {
            return false;
          }

          LocalTime actualTime = rs.getTime(rsIndex).toLocalTime();
          if (!expectedTime.equals(actualTime)) {
            return false;
          }
          break;

        case TIMESTAMP:
          LocalDateTime expectedTsp = (LocalDateTime) expectedVal;

          if (wasNull) {
            return false;
          }

          LocalDateTime actualTsp = rs.getTimestamp(rsIndex).toLocalDateTime();
          if (!expectedTsp.equals(actualTsp)) {
            return false;
          }
          break;

        case INTERVAL:
          throw new SethSystemException("Interval not yet implemented.");
          // year-month intervals are represented by Period classes.
          // day-time intevals are represented by Duration classes.
          //break;

        case IGNORE_REMAINING: // Falls through
        default:
          throw new SethSystemException("Unhandled column type: " + type.name());
      }

    }

    return true;
  }

  /**
   * Returns a string representation of this expected row.
   * @return a string representation of this expected row.
   */
  public String toString()
  {
    StringBuilder sb = new StringBuilder(128);

    sb.append('(');

    for (int index = 0; index < columnDefs.size(); index++) {

      ExpectedColumnType type = columnDefs.get(index);
      Object val = columnValues.get(index);

      switch (type) {
        case NULL:
        case DONT_CARE:
        case IGNORE_REMAINING:
          sb.append(type.getCode());
          break;

        case BOOLEAN:
        case INTEGER:
          sb.append(val.toString());
          break;

        case FLOAT:
          // Floats are stored as strings so we can retain the precision.
          sb.append(val.toString());
          break;


        case DECIMAL:
          sb.append(((BigDecimal) val).toPlainString());
          break;

        case STRING:
          sb.append("'");
          sb.append(val);
          sb.append("'");
          break;

        case DATE:
          LocalDate localDate = (LocalDate) val;
          sb.append("DATE '");
          sb.append(localDate.toString());
          sb.append("'");
          break;

        case TIME:
          LocalTime localTime = (LocalTime) val;
          sb.append("TIME '");
          sb.append(localTime.toString());
          sb.append("'");
          break;

        case TIMESTAMP:
          LocalDateTime localDateTime = (LocalDateTime) val;
          DateTimeFormatter dtf = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss[.nnnnnnnnn]");
          sb.append("TIMESTAMP '");
          sb.append(localDateTime.format(dtf));
          sb.append("'");
          break;

        case INTERVAL:
          String intervalType;

          sb.append("INTERVAL ");

          if (val instanceof Period) {
            Period period = (Period) val;

            if (period.getYears() < 0 || period.getMonths() < 0) {
              sb.append('-');
            }

            sb.append("'");

            if (period.getYears() != 0 && period.getMonths() != 0) {
              intervalType = "YEARS TO MONTHS";
              sb.append(Math.abs(period.getYears()));
              sb.append('-');
              sb.append(Math.abs(period.getMonths()));

            } else if (period.getMonths() != 0) {
              intervalType = "MONTHS";
              sb.append(Math.abs(period.getMonths()));

            } else {
              intervalType = "YEARS";
              sb.append(Math.abs(period.getYears()));
            }

          } else if (val instanceof Duration) {
            Duration duration = (Duration) val;

            long days = duration.toDays();
            duration = duration.minusDays(days);
            long hours = duration.toHours();
            duration = duration.minusHours(hours);
            long minutes = duration.toMinutes();
            duration = duration.minusMinutes(minutes);
            long seconds = ((duration.getSeconds() * 1000000000) + duration.getNano()) / 1000000000;
            long nanos   = ((duration.getSeconds() * 1000000000) + duration.getNano()) % 1000000000;

            if (days < 0 || hours < 0 || minutes < 0 || seconds < 0 || nanos < 0) {
              sb.append('-');
            }

            sb.append("'");

            if (days != 0) {
              intervalType = "DAYS";
              sb.append(String.format("%02d", Math.abs(days)));

              if (hours != 0 || minutes != 0 || seconds != 0 || nanos != 0) {
                intervalType = "DAYS TO HOURS";
                sb.append(" ");
                sb.append(String.format("%02d", Math.abs(hours)));

                if (minutes != 0 || seconds != 0 || nanos != 0) {
                  intervalType = "DAYS TO MINUTES";
                  sb.append(":");
                  sb.append(String.format("%02d", Math.abs(minutes)));

                  if (seconds != 0 || nanos != 0) {
                    intervalType = "DAYS TO SECONDS";
                    sb.append(":");
                    sb.append(String.format("%02d", Math.abs(seconds)));

                    if (nanos != 0) {
                      sb.append(".");
                      sb.append(String.format("%09d", Math.abs(nanos)));
                    }
                  }
                }
              }

            } else if (hours != 0) {
              intervalType = "HOURS";
              sb.append(String.format("%02d", Math.abs(hours)));

              if (minutes != 0 || seconds != 0 || nanos != 0) {
                intervalType = "HOURS TO MINUTES";
                sb.append(":");
                sb.append(String.format("%02d", Math.abs(minutes)));

                if (seconds != 0 || nanos != 0) {
                  intervalType = "HOURS TO SECONDS";
                  sb.append(":");
                  sb.append(String.format("%02d", Math.abs(seconds)));

                  if (nanos != 0) {
                    sb.append(".");
                    sb.append(String.format("%09d", Math.abs(nanos)));
                  }
                }
              }

            } else if (minutes != 0) {
              intervalType = "MINUTES";
              sb.append(String.format("%02d", Math.abs(minutes)));

              if (seconds != 0 || nanos != 0) {
                intervalType = "MINUTES TO SECONDS";
                sb.append(":");
                sb.append(String.format("%02d", Math.abs(seconds)));

                if (nanos != 0) {
                  sb.append(".");
                  sb.append(String.format("%09d", Math.abs(nanos)));
                }
              }

            } else {
              // seconds
              intervalType = "SECONDS";
              sb.append(String.format("%02d", Math.abs(seconds)));

              if (nanos != 0) {
                sb.append(".");
                sb.append(String.format("%09d", Math.abs(nanos)));
              }
            }

          } else {
            throw new SethSystemException("Unrecognised interval type: " + val.getClass().getName());
          }
          sb.append("' ");
          sb.append(intervalType);
          break;

        default:
          throw new SethSystemException("Unhandled data type: " + type.name());
      }

      sb.append(", ");
    }

    // Remove the ", " characters on the end.
    sb.delete(sb.length() - 2, sb.length());

    sb.append(')');

    return sb.toString();
  }
}
