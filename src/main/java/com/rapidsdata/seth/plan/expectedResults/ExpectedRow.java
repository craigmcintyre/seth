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
import java.util.*;

public class ExpectedRow
{
  private final List<ExpectedColumnType> columnDefs;
  private final List<Object> columnValues;

  public ExpectedRow(List<ExpectedColumnType> columnDefs, List<Object> columnValues)
  {
    this.columnDefs = columnDefs;
    this.columnValues = columnValues;
  }

  public ExpectedRow(ExpectedRow er)
  {
    this.columnDefs = er.columnDefs;
    this.columnValues = er.columnValues;
  }

  public List<ExpectedColumnType> getColumnDefs()
  {
    return columnDefs;
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
   * Returns a string representation of this expected row, where the columns are padded according to
   * the widths in the columnWidths argument.
   * @param columnWidths the maximum width to pad to for each column, or null if padding is to be ignored.
   * @param padLefts an array with values for each column indicating if we should add padding to the
   *                left of the value if true, or to the right of the value if false.
   * @return a string representation of this expected row
   */
  public String toString(int[] columnWidths, boolean[] padLefts)
  {
    if (columnWidths == null) {
      columnWidths = new int[columnDefs.size()];
      for (int i = 0; i < columnDefs.size(); i++) {
        columnWidths[i] = 0;
      }
    }

    if (padLefts == null) {
      padLefts = getNaturalPaddingDirections();
    }

    String columnVal;
    int padding;

    StringBuilder sb = new StringBuilder(128);

    sb.append('(');

    for (int colIndex = 0; colIndex < columnDefs.size(); colIndex++) {

      ExpectedColumnType type = columnDefs.get(colIndex);
      Object objectVal = columnValues.get(colIndex);
      boolean padLeft = padLefts[colIndex];

      switch (type) {
        case NULL:
        case DONT_CARE:
        case IGNORE_REMAINING:
          columnVal = type.getCode();
          padding = columnWidths[colIndex] - columnVal.length();

          if (!padLeft) {
            sb.append(columnVal);
          }

          for (int j = 0; j < padding; j++) {
            sb.append(' ');
          }

          if (padLeft) {
            sb.append(columnVal);
          }
          break;

        case BOOLEAN:
          columnVal = objectVal.toString();
          padding = columnWidths[colIndex] - columnVal.length();

          if (!padLeft) {
            sb.append(columnVal);
          }

          for (int j = 0; j < padding; j++) {
            sb.append(' ');
          }

          if (padLeft) {
            sb.append(columnVal);
          }
          break;

        case INTEGER:
        case FLOAT:
          columnVal = objectVal.toString();
          padding = columnWidths[colIndex] - columnVal.length();

          if (!padLeft) {
            sb.append(columnVal);
          }

          for (int j = 0; j < padding; j++) {
            sb.append(' ');
          }

          if (padLeft) {
            sb.append(columnVal);
          }
          break;


        case DECIMAL:
          columnVal = ((BigDecimal) objectVal).toPlainString();
          padding = columnWidths[colIndex] - columnVal.length();

          if (!padLeft) {
            sb.append(columnVal);
          }

          for (int j = 0; j < padding; j++) {
            sb.append(' ');
          }

          if (padLeft) {
            sb.append(columnVal);
          }
          break;

        case STRING:
          columnVal = objectVal.toString();
          padding = columnWidths[colIndex] - (columnVal.length() + 2);

          if (!padLeft) {
            sb.append("'").append(columnVal).append("'");
          }

          for (int j = 0; j < padding; j++) {
            sb.append(' ');
          }

          if (padLeft) {
            sb.append("'").append(columnVal).append("'");
          }
          break;

        case DATE:
          LocalDate localDate = (LocalDate) objectVal;

          columnVal = "DATE '" + localDate.toString() + "'";
          padding = columnWidths[colIndex] - columnVal.length();

          if (!padLeft) {
            sb.append(columnVal);
          }

          for (int j = 0; j < padding; j++) {
            sb.append(' ');
          }

          if (padLeft) {
            sb.append(columnVal);
          }
          break;

        case TIME:
          LocalTime localTime = (LocalTime) objectVal;

          columnVal = "TIME '" + localTime.toString() + "'";
          padding = columnWidths[colIndex] - columnVal.length();

          if (!padLeft) {
            sb.append(columnVal);
          }

          for (int j = 0; j < padding; j++) {
            sb.append(' ');
          }

          if (padLeft) {
            sb.append(columnVal);
          }
          break;

        case TIMESTAMP:
          LocalDateTime localDateTime = (LocalDateTime) objectVal;
          DateTimeFormatter dtf = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss");

          columnVal = "TIMESTAMP '" + localDateTime.format(dtf);

          if (localDateTime.getNano() != 0) {
            columnVal += '.';

            // Don't show trailing zeros of fractional seconds.
            long fractionalSecs = localDateTime.getNano();
            while (fractionalSecs % 10 == 0) {
              fractionalSecs = fractionalSecs / 10;
            }

            columnVal += String.valueOf(fractionalSecs);
          }

          columnVal += "'";
          padding = columnWidths[colIndex] - columnVal.length();

          if (!padLeft) {
            sb.append(columnVal);
          }

          for (int j = 0; j < padding; j++) {
            sb.append(' ');
          }

          if (padLeft) {
            sb.append(columnVal);
          }
          break;

        case INTERVAL:
          StringBuilder tempSb = new StringBuilder(128);
          formatInterval(tempSb, objectVal);
          columnVal = tempSb.toString();
          padding = columnWidths[colIndex] - columnVal.length();

          if (!padLeft) {
            sb.append(columnVal);
          }

          for (int j = 0; j < padding; j++) {
            sb.append(' ');
          }

          if (padLeft) {
            sb.append(columnVal);
          }
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
   * Returns a string representation of this expected row.
   * @return a string representation of this expected row.
   */
  public String toString()
  {
    return toString(null, null);
  }

  /** @returns the width of a column when printed to a string. */
  public int columnWidth(int col)
  {
    if (col > columnDefs.size()) {
      return 0;
    }

    ExpectedColumnType type = columnDefs.get(col);
    Object val = columnValues.get(col);

    switch (type) {
      case NULL:
      case DONT_CARE:
      case IGNORE_REMAINING:
        return type.getCode().length();

      case BOOLEAN:
      case INTEGER:
      case FLOAT:
        return val.toString().length();

      case DECIMAL:
        return ((BigDecimal) val).toPlainString().length();

      case STRING:
        return val.toString().replace("'", "''").length() + 2;  // for quotes

      case DATE:
        LocalDate localDate = (LocalDate) val;
        return localDate.toString().length() + 7; // for "DATE '" + "'"

      case TIME:
        LocalTime localTime = (LocalTime) val;
        return localTime.toString().length() + 7; // for "TIME '" + "'"

      case TIMESTAMP:
        LocalDateTime localDateTime = (LocalDateTime) val;
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss");
        int len = localDateTime.format(dtf).length() + 12; // for "TIMESTAMP '" + "'"

        if (localDateTime.getNano() != 0) {
          // Don't show trailing zeros of fractional seconds.
          long fractionalSecs = localDateTime.getNano();
          while (fractionalSecs % 10 == 0) {
            fractionalSecs = fractionalSecs / 10;
          }

          len += String.valueOf(fractionalSecs).length() + 1;  // for "."
        }

        return len;

      case INTERVAL:
        StringBuilder sb = new StringBuilder(128);
        formatInterval(sb, val);
        return sb.toString().length();

      default:
        throw new SethSystemException("Unhandled data type: " + type.name());
    }
  }

  /**
   * Returns an array of booleans that indicate the natural padding location for each expected column in
   * the definition. A value of true means pad to the left of the column value.
   * @return
   */
  private boolean[] getNaturalPaddingDirections()
  {
    boolean[] padLefts = new boolean[columnDefs.size()];

    for (int i = 0; i < columnDefs.size(); i++) {
      ExpectedColumnType type = columnDefs.get(i);

      switch (type) {

        case INTEGER:
        case DECIMAL:
        case FLOAT:
          padLefts[i] = false;
          break;

        default:
          padLefts[i] = true;
      }
    }

    return padLefts;
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

  /**
   * Calculates a score that represents the distance that this expected row is from an actual row in the ResultSet.
   * @param rs the ResultSet that is pointing to an actual row.
   * @return the score. A perfect score would be 0.
   */
  private double distanceFrom(ResultSet rs, int rounding) throws SQLException
  {
    // The accumulated score for a final geometric mean result.
    double cumulativeScore = 1f;

    ResultSetMetaData rsmd = rs.getMetaData();

    int actualColumnCount = rsmd.getColumnCount();
    int expectedColumnDefCount = columnDefs.size();

    // If the last column definition is not '...' then the number of expected columns
    // should equal the number of actual columns.
    if (columnDefs.get(expectedColumnDefCount - 1) != ExpectedColumnType.IGNORE_REMAINING &&
        actualColumnCount != expectedColumnDefCount) {
      // Not enough information to compute a score.
      return -1f;
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
        // Not enough information to compute a score.
        return -1f;
      }


      Object expectedVal = columnValues.get(defIndex);
      Object actualVal = rs.getObject(rsIndex);
      boolean wasNull = rs.wasNull();
      double columnScore;

      switch (type) {
        case DONT_CARE:
          // We don't care about comparing this column.
          columnScore = 0f;
          break;

        case NULL:
          if (!wasNull) { columnScore = ERScoring.compareNullWith(actualVal); }
          else          { columnScore = 0f; }
          break;

        case BOOLEAN:
          boolean expectedBoolean = (boolean) expectedVal;
          if (wasNull)    { columnScore = ERScoring.compareWithNull(expectedBoolean); }
          else            { columnScore = ERScoring.compare(rs.getBoolean(rsIndex), expectedBoolean); }
          break;

        case INTEGER:
          long expectedLong = (long) expectedVal;
          if (wasNull)    { columnScore = ERScoring.compareWithNull(expectedLong); }
          else            { columnScore = ERScoring.compare(rs.getLong(rsIndex), expectedLong); }
          break;

        case DECIMAL:
          BigDecimal expectedDecimal = (BigDecimal) expectedVal;
          if (wasNull)  {
            columnScore = ERScoring.compareWithNull(expectedDecimal);

          } else {
            if (rounding == CommandLineArgs.NO_ROUNDING) {
              columnScore = ERScoring.compare(rs.getBigDecimal(rsIndex), expectedDecimal);

            } else {
              // Round the numbers and compare as decimals
              MathContext mc = new MathContext(rounding, RoundingMode.HALF_UP);
              BigDecimal bd1 = rs.getBigDecimal(rsIndex).round(mc);
              BigDecimal bd2 = expectedDecimal.round(mc);
              columnScore = ERScoring.compare(bd1, bd2);
            }
          }
          break;

        case FLOAT:
          ComparableFloat cf = (ComparableFloat) expectedVal;
          if (rounding == CommandLineArgs.NO_ROUNDING) {
            if (wasNull)    { columnScore = ERScoring.compareWithNull(cf.toDouble()); }
            else            { columnScore = ERScoring.compare(rs.getDouble(rsIndex), cf.toDouble()); }

          } else {
            // Round the numbers and compare as decimals
            MathContext mc = new MathContext(rounding, RoundingMode.HALF_UP);
            BigDecimal bd1 = rs.getBigDecimal(rsIndex).round(mc);
            BigDecimal bd2 = cf.toBigDecimal().round(mc);

            if (wasNull)    { columnScore = ERScoring.compareWithNull(bd1); }
            else            { columnScore = ERScoring.compare(bd1, bd2); }
          }
          break;

        case STRING:
          String expectedString = (String) expectedVal;
          if (wasNull)    { columnScore = ERScoring.compareWithNull(expectedString); }
          else            { columnScore = ERScoring.compare(rs.getString(rsIndex), expectedString); }
          break;

        case DATE:
          LocalDate expectedDate = (LocalDate) expectedVal;
          if (wasNull)    { columnScore = ERScoring.compareWithNull(expectedDate); }
          else            { columnScore = ERScoring.compare(rs.getDate(rsIndex).toLocalDate(), expectedDate); }
          break;

        case TIME:
          LocalTime expectedTime = (LocalTime) expectedVal;
          if (wasNull)    { columnScore = ERScoring.compareWithNull(expectedTime); }
          else            { columnScore = ERScoring.compare(rs.getTime(rsIndex).toLocalTime(), expectedTime); }
          break;

        case TIMESTAMP:
          LocalDateTime expectedTsp = (LocalDateTime) expectedVal;
          if (wasNull)    { columnScore = ERScoring.compareWithNull(expectedTsp); }
          else            { columnScore = ERScoring.compare(rs.getTimestamp(rsIndex).toLocalDateTime(), expectedTsp); }
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

      // Combine with the cumulative score. We add 1 to all column scores so that a value of 0 does not
      // affect the whole result.
      cumulativeScore = cumulativeScore * (columnScore + 1);
    }

    // Calculate the final score by taking the nth root of the cumulative score, where n is
    // the number of columns in the actual result.
    // Taking the nth root is the same as raising to the power of 1/n, however this can be
    // inaccurate. A better way is to use the equivalence
    //
    //   x^(1/n) == e^(ln(x)/n)
    //
    // We subtract one from the end because we added one to each column score in order to avoid
    // a multiplication by zero.
    double finalScore = Math.pow(Math.E, Math.log(cumulativeScore)/actualColumnCount) - 1;
    return finalScore;
  }


  /**
   * Returns the ExpectedRow that most closely matches the row that the ResultSet argument is currently set at.
   * @param expectedRows the list of ExpectedRows to be compared.
   * @param rs an actual ResultSet, set at the current row to be compared.
   * @param rounding the number of decimal places of rounding to apply to floats/decimals before comparing them.
   * @param maxNumRows the maximum number of rows to be returned.
   * @return A list of ScoredExpectedRows that most closely matches the actual row, in descending order.
   */
  public static List<ScoredExpectedRow> findClosestMatchOf(List<ExpectedRow> expectedRows, ResultSet rs, int rounding, int maxNumRows)
                                                          throws SQLException
  {
    assert (expectedRows.size() > 0);

    if (expectedRows.size() == 1) {
      List<ScoredExpectedRow> matchedRowsList = new ArrayList<>();
      matchedRowsList.add(new ScoredExpectedRow(1, 0, expectedRows.get(0)));
      return matchedRowsList;
    }

    int i = 0;
    SortedSet<ScoredExpectedRow> sortedSet = new TreeSet<>();

    for (ExpectedRow er : expectedRows) {
      // calculate the Levenstein-like distance of this expected row to the actual row.
      double score = er.distanceFrom(rs, rounding);

      if (score >= 0f) {
        ScoredExpectedRow scoredExpectedRow = new ScoredExpectedRow(score, i++, er);
        sortedSet.add(scoredExpectedRow);
      }
    }

    List<ScoredExpectedRow> matchedRowsList = new ArrayList<>();
    Iterator<ScoredExpectedRow> iterator = sortedSet.iterator();

    while (iterator.hasNext() && matchedRowsList.size() < maxNumRows) {
      matchedRowsList.add(iterator.next());
    }

    return matchedRowsList;
  }
}
