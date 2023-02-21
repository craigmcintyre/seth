// Copyright (c) 2017 Boray Data Co. Ltd.  All rights reserved.

package com.rapidsdata.seth.plan.expectedResults;

import com.rapidsdata.seth.CommandLineArgs;
import com.rapidsdata.seth.Options;
import com.rapidsdata.seth.exceptions.SethSystemException;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.sql.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static com.rapidsdata.seth.plan.expectedResults.ExpectedColumnType.IGNORE_REMAINING;

public class ExpectedRow
{
  private final List<ExpectedColumnType> columnDefs;
  private final List<Object> columnValues;
  private final Options rowOptions;

  public ExpectedRow(List<ExpectedColumnType> columnDefs, List<Object> columnValues, Options rowOptions)
  {
    this.columnDefs   = columnDefs;
    this.columnValues = columnValues;
    this.rowOptions   = rowOptions;
  }

  public ExpectedRow(ExpectedRow er)
  {
    this.columnDefs   = er.columnDefs;
    this.columnValues = er.columnValues;
    this.rowOptions   = er.rowOptions;
  }

  public List<ExpectedColumnType> getColumnDefs()
  {
    return columnDefs;
  }

  /**
   * Compares the expected row to the row that the cursor is at in the ResultSet parameter.
   * @param rs The resultset which has the cursor on the current row to be compared.
   * @param optionList a list of any options on the whole expected result, test file, application, etc.
   * @return true if the rows compare equally or false if they are different.
   * @throws SQLException
   */
  public boolean compareTo(ResultSet rs, LinkedList<Options> optionList) throws SQLException
  {
    ResultSetMetaData rsmd = rs.getMetaData();

    int actualColumnCount = rsmd.getColumnCount();
    int expectedColumnDefCount = columnDefs.size();

    // If the last column definition is not '...' then the number of expected columns
    // should equal the number of actual columns.
    if (columnDefs.get(expectedColumnDefCount - 1) != IGNORE_REMAINING &&
        actualColumnCount != expectedColumnDefCount) {
      return false;
    }

    int rounding = Options.NO_ROUNDING;

    // Compare column by column
    int defIndex = -1;

    while (++defIndex < columnDefs.size()) {

      ExpectedColumnType type = columnDefs.get(defIndex);
      int rsIndex = defIndex + 1; // rs.getXXXX() uses 1-based indexes.

      if (type == IGNORE_REMAINING) {
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
          optionList.addFirst(rowOptions);
          rounding = Options.getRounding(optionList);
          optionList.removeFirst();

          if (wasNull || !equalRounded(expectedDecimal, rs.getBigDecimal(rsIndex), rounding)) {
            return false;
          }
          break;

        case FLOAT:
          // Compare floating points up to the requested level of precision
          optionList.addFirst(rowOptions);
          rounding = Options.getRounding(optionList);
          optionList.removeFirst();

          if (wasNull || !equalRounded((ComparableFloat) expectedVal, rs.getString(rsIndex), rounding)) {
            return false;
          }
          break;

        case STRING:
          String expectedString = (String) expectedVal;
          if (wasNull) {
            return false;
          }

          // Any row options override result options.
          optionList.addFirst(rowOptions);
          boolean ignoreCase = Options.getIgnoreCase(optionList);
          optionList.removeFirst();

          if ( (ignoreCase && !expectedString.equalsIgnoreCase(rs.getString(rsIndex))) ||
               (!ignoreCase && !expectedString.equals(rs.getString(rsIndex))) ) {
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
          ComparableInterval expectedInterval = (ComparableInterval) expectedVal;

          if (wasNull) {
            return false;
          }

          ComparableInterval actualInterval = ComparableInterval.fromResultSet(rs, rsIndex);
          if (actualInterval == null) {
            return false;
          }

          if( !expectedInterval.comparesTo(actualInterval)) {
            return false;
          }
          break;  // Matched

        case IGNORE_REMAINING: // Falls through
        default:
          throw new SethSystemException("Unhandled column type: " + type.name());
      }

    }

    return true;
  }


  private boolean equalRounded(BigDecimal x, BigDecimal y, int round)
  {
    if (round != Options.NO_ROUNDING) {
      MathContext mc = new MathContext(round, RoundingMode.DOWN);

      x = x.round(mc);
      y = y.round(mc);
    }

    return x.equals(y);
  }


  private boolean equalRounded(ComparableFloat cf, String y, int round)
  {
    if (round == Options.NO_ROUNDING) {
      return cf.comparesTo(y);
    }

    MathContext mc = new MathContext(round, RoundingMode.DOWN);

    BigDecimal bdx = new BigDecimal(cf.toString(), mc);
    BigDecimal bdy = new BigDecimal(y, mc);

    return bdx.equals(bdy);
  }


  public Options getRowOptions()
  {
    return rowOptions;
  }


  /**
   * Returns a string representation of this expected row, where the columns are padded according to
   * the widths in the columnWidths argument.
   * @param columnWidths the maximum width to pad to for each column, or null if padding is to be ignored.
   * @param padLefts an array with values for each column indicating if we should add padding to the
   *                left of the value if true, or to the right of the value if false.
   * @return a string representation of this expected row
   */
  public String toString(int[] columnWidths, boolean[] padLefts, int optionWidth)
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

    // First add the row option padding.
    if (rowOptions == null) {
      for (int i = 0; i < optionWidth; i++) {
        sb.append(' ');
      }

    } else {
      String optStr = rowOptions.toString();
      sb.append(optStr);
      sb.append(" ");

      for (int i = optStr.length() + 1; i < optionWidth; i++) {
        sb.append(' ');
      }
    }

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
          ComparableInterval interval = (ComparableInterval) objectVal;
          columnVal = interval.toString();
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
    return toString(null, null, 0);
  }

  /** @returns the width of a column when printed to a string. */
  public int columnWidth(int col)
  {
    if (col >= columnDefs.size()) {
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
        ComparableInterval interval = (ComparableInterval) val;
        return interval.toString().length();

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
    if (columnDefs.get(expectedColumnDefCount - 1) != IGNORE_REMAINING &&
        actualColumnCount != expectedColumnDefCount) {
      // Not enough information to compute a score.
      return -1f;
    }

    // Compare column by column
    int defIndex = -1;

    while (++defIndex < columnDefs.size()) {

      ExpectedColumnType type = columnDefs.get(defIndex);
      int rsIndex = defIndex + 1; // rs.getXXXX() uses 1-based indexes.

      if (type == IGNORE_REMAINING) {
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
            if (rounding == Options.NO_ROUNDING) {
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
          if (rounding == Options.NO_ROUNDING) {
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
          ComparableInterval expectedInterval = (ComparableInterval) expectedVal;
          if (wasNull)    { columnScore = ERScoring.compareWithNull(expectedInterval); }
          else {
            ComparableInterval actualInterval = ComparableInterval.fromResultSet(rs, rsIndex);

            if (actualInterval == null) {
              // Actual value is not a parseable interval
              columnScore = ERScoring.compareWithNull(expectedInterval);
            } else {
              columnScore = ERScoring.compare(actualInterval, expectedInterval);
            }
          }
          break;

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
   * @param optionList the list of options that may apply to this operation.
   * @param maxNumRows the maximum number of rows to be returned.
   * @return A list of ScoredExpectedRows that most closely matches the actual row, in descending order.
   */
  public static List<ScoredExpectedRow> findClosestMatchOf(List<ExpectedRow> expectedRows, ResultSet rs, LinkedList<Options> optionList, int maxNumRows)
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
      optionList.addFirst(er.rowOptions);
      int rounding = Options.getRounding(optionList);
      optionList.removeFirst();


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
