// Copyright (c) 2017 Boray Data Co. Ltd.  All rights reserved.

package com.rapidsdata.seth.plan.expectedResults;

import com.rapidsdata.seth.CommandLineArgs;
import com.rapidsdata.seth.exceptions.SethSystemException;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
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
  public boolean compareTo(ResultSet rs, int rounding) throws SQLException
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
          if (wasNull || !equalRounded(expectedDecimal, rs.getBigDecimal(rsIndex), rounding)) {
            return false;
          }
          break;

        case FLOAT:
          // Compare floating points up to the requested level of precision
          if (wasNull || !equalRounded(expectedVal, rs.getString(rsIndex), rounding)) {
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


  private boolean equalRounded(BigDecimal x, BigDecimal y, int round)
  {
    if (round != CommandLineArgs.NO_ROUNDING) {
      MathContext mc = new MathContext(round, RoundingMode.DOWN);

      x = x.round(mc);
      y = y.round(mc);
    }

    return x.equals(y);
  }


  private boolean equalRounded(Object x, String y, int round)
  {
    if (round == CommandLineArgs.NO_ROUNDING) {
      ComparableFloat cf = (ComparableFloat) x;
      return cf.comparesTo(y);
    }

    MathContext mc = new MathContext(round, RoundingMode.DOWN);

    BigDecimal bdx = new BigDecimal((double) x, mc);
    BigDecimal bdy = new BigDecimal(y, mc);

    return x.equals(y);
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
          // Floats are stored as ComparableFloat objects so we can retain the precision.
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
          DateTimeFormatter dtf = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss");
          sb.append("TIMESTAMP '");
          sb.append(localDateTime.format(dtf));

          if (localDateTime.getNano() != 0) {
            sb.append('.');

            // Don't show trailing zeros of fractional seconds.
            long fractionalSecs = localDateTime.getNano();
            while (fractionalSecs % 10 == 0) {
              fractionalSecs = fractionalSecs / 10;
            }

            sb.append(fractionalSecs);
          }

          sb.append("'");
          break;

        case INTERVAL:
          formatInterval(sb, val);
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

  /**
   * Formats an interval type to a string and writes it to the StringBuilder parameter.
   * @param sb where the interval type is to be rendered to.
   * @param val the interval value.
   */
  private void formatInterval(StringBuilder sb, Object val)
  {
    String intervalType;

    sb.append("INTERVAL ");

    if (val instanceof Period) {
      Period period = (Period) val;
      intervalType = formatPeriodInterval(sb, period);

    } else if (val instanceof Duration) {
      Duration duration = (Duration) val;
      intervalType = formatDurationInterval(sb, duration);

    } else {
      throw new SethSystemException("Unrecognised interval type: " + val.getClass().getName());
    }
    sb.append("' ");
    sb.append(intervalType);
  }

  /**
   * Formats a Period interval type to a string and writes it to the StringBuilder parameter.
   * @param sb where the interval type is to be rendered to.
   * @param period The Period interval value to render.
   * @return The exact type of SQL interval that was rendered (e.g. YEARS TO MONTHS, MONTHS, ...).
   */
  private String formatPeriodInterval(StringBuilder sb, Period period)
  {
    if (period.getYears() < 0 || period.getMonths() < 0) {
      sb.append('-');
    }

    sb.append("'");

    String intervalType;

    if (period.getYears() != 0 && period.getMonths() != 0) {
      intervalType = "YEAR TO MONTH";
      sb.append(Math.abs(period.getYears()));
      sb.append('-');
      sb.append(Math.abs(period.getMonths()));

    } else if (period.getMonths() != 0) {
      intervalType = "MONTH";
      sb.append(Math.abs(period.getMonths()));

    } else {
      intervalType = "YEAR";
      sb.append(Math.abs(period.getYears()));
    }

    return intervalType;
  }

  /**
   * Formats a Duration interval type to a string and writes it to the StringBuilder parameter.
   * @param sb where the interval type is to be rendered to.
   * @param duration The Duration interval value to render.
   * @return The exact type of SQL interval that was rendered (e.g. DAYS TO MINUTES, SECONDS, ...).
   */
  private String formatDurationInterval(StringBuilder sb, Duration duration)
  {
    String intervalType;

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
      intervalType = "DAY";
      sb.append(String.format("%02d", Math.abs(days)));

      if (hours != 0 || minutes != 0 || seconds != 0 || nanos != 0) {
        intervalType = "DAY TO HOUR";
        sb.append(" ");
        sb.append(String.format("%02d", Math.abs(hours)));

        if (minutes != 0 || seconds != 0 || nanos != 0) {
          intervalType = "DAY TO MINUTE";
          sb.append(":");
          sb.append(String.format("%02d", Math.abs(minutes)));

          if (seconds != 0 || nanos != 0) {
            intervalType = "DAY TO SECOND";
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
      intervalType = "HOUR";
      sb.append(String.format("%02d", Math.abs(hours)));

      if (minutes != 0 || seconds != 0 || nanos != 0) {
        intervalType = "HOUR TO MINUTE";
        sb.append(":");
        sb.append(String.format("%02d", Math.abs(minutes)));

        if (seconds != 0 || nanos != 0) {
          intervalType = "HOUR TO SECOND";
          sb.append(":");
          sb.append(String.format("%02d", Math.abs(seconds)));

          if (nanos != 0) {
            sb.append(".");
            sb.append(String.format("%09d", Math.abs(nanos)));
          }
        }
      }

    } else if (minutes != 0) {
      intervalType = "MINUTE";
      sb.append(String.format("%02d", Math.abs(minutes)));

      if (seconds != 0 || nanos != 0) {
        intervalType = "MINUTE TO SECOND";
        sb.append(":");
        sb.append(String.format("%02d", Math.abs(seconds)));

        if (nanos != 0) {
          sb.append(".");
          sb.append(String.format("%09d", Math.abs(nanos)));
        }
      }

    } else {
      // seconds
      intervalType = "SECOND";
      sb.append(String.format("%02d", Math.abs(seconds)));

      if (nanos != 0) {
        sb.append(".");
        sb.append(String.format("%09d", Math.abs(nanos)));
      }
    }

    return intervalType;
  }
}
