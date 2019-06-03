// Copyright (c) 2017 Boray Data Co. Ltd.  All rights reserved.

package com.rapidsdata.seth.plan.expectedResults;

import com.rapidsdata.seth.exceptions.SethSystemException;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class ResultSetFormatter
{

  public static String describeCurrentRow(ResultSet rs) throws SQLException
  {
    ResultSetMetaData rsmd = rs.getMetaData();
    StringBuilder sb = new StringBuilder(1024);

    sb.append('(');

    int numColumns = rsmd.getColumnCount();

    for (int colIndex = 1; colIndex <= numColumns; colIndex++) {
      int columnType = rsmd.getColumnType(colIndex);

      // check for null first
      if (rs.getObject(colIndex) == null) {
        sb.append("NULL");

      } else {
        switch (columnType) {

          case Types.BIGINT:   // falls through
          case Types.BIT:      // falls through
          case Types.INTEGER:  // falls through
          case Types.SMALLINT: // falls through
          case Types.TINYINT:
            sb.append(rs.getLong(colIndex));
            break;

          case Types.BOOLEAN:
            sb.append(rs.getBoolean(colIndex));
            break;


          case Types.CHAR:
          case Types.VARCHAR:
            sb.append('\'');
            sb.append(rs.getString(colIndex).replace("'", "''"));
            sb.append('\'');
            break;

          case Types.DATE:
            sb.append("DATE '");
            sb.append(rs.getDate(colIndex).toString());
            sb.append("'");
            break;

          case Types.DECIMAL:
          case Types.NUMERIC:
            sb.append(rs.getBigDecimal(colIndex).toPlainString());
            break;

          case Types.DOUBLE:
          case Types.FLOAT:
          case Types.REAL:
            String s = String.valueOf(rs.getDouble(colIndex));
            sb.append(s);

            // Give it an exponent if it doesn't have one.
            if (!s.contains("e") && !s.contains("E")) {
              sb.append("e0");
            }
            break;

          case Types.JAVA_OBJECT:
          case Types.OTHER:
            sb.append(rs.getObject(colIndex).toString());
            break;

          case Types.NULL:
            sb.append("null");
            break;

          case Types.TIME:
          case Types.TIME_WITH_TIMEZONE:
            sb.append("TIME '");
            sb.append(rs.getTime(colIndex).toString());
            sb.append("'");
            break;

          case Types.TIMESTAMP:
          case Types.TIMESTAMP_WITH_TIMEZONE:
            sb.append("TIMESTAMP '");
            sb.append(rs.getTimestamp(colIndex).toString());
            sb.append("'");
            break;


          case Types.ARRAY:          // falls through
          case Types.BINARY:         // falls through
          case Types.BLOB:           // falls through
          case Types.CLOB:           // falls through
          case Types.DATALINK:       // falls through
          case Types.DISTINCT:       // falls through
          case Types.LONGNVARCHAR:   // falls through
          case Types.LONGVARBINARY:  // falls through
          case Types.LONGVARCHAR:    // falls through
          case Types.NCHAR:          // falls through
          case Types.NCLOB:          // falls through
          case Types.NVARCHAR:       // falls through
          case Types.REF:            // falls through
          case Types.REF_CURSOR:     // falls through
          case Types.ROWID:          // falls through
          case Types.SQLXML:         // falls through
          case Types.STRUCT:         // falls through
          case Types.VARBINARY:      // falls through
          default:
            throw new SethSystemException("Unhandled JDBC column type: " + columnType);
        }
      }

      sb.append(", ");
    }

    // Remove the last ", "
    sb.delete(sb.length() - 2, sb.length());

    sb.append(')');

    return sb.toString();
  }

  public static String describeCurrentRow(ResultSet rs, int[] columnWidths) throws SQLException
  {
    ResultSetMetaData rsmd = rs.getMetaData();
    StringBuilder sb = new StringBuilder(1024);

    sb.append('(');

    int numColumns = rsmd.getColumnCount();
    String columnVal;
    int padding;

    for (int colIndex = 1; colIndex <= numColumns; colIndex++) {
      int columnType = rsmd.getColumnType(colIndex);

      // check for null first
      if (rs.getObject(colIndex) == null) {
        sb.append("NULL");

      } else {
        switch (columnType) {

          case Types.BIGINT:   // falls through
          case Types.BIT:      // falls through
          case Types.INTEGER:  // falls through
          case Types.SMALLINT: // falls through
          case Types.TINYINT:
            columnVal = String.valueOf(rs.getLong(colIndex));
            padding = columnWidths[colIndex-1] - columnVal.length();

            // left padding
            for (int j = 0; j < padding; j++) {
              sb.append(' ');
            }

            sb.append(columnVal);
            break;

          case Types.BOOLEAN:
            columnVal = String.valueOf(rs.getBoolean(colIndex));
            padding = columnWidths[colIndex-1] - columnVal.length();

            sb.append(columnVal);

            // right padding
            for (int j = 0; j < padding; j++) {
              sb.append(' ');
            }
            break;


          case Types.CHAR:
          case Types.VARCHAR:
            columnVal = "'" + rs.getString(colIndex).replace("'", "''") + "'";
            padding = columnWidths[colIndex-1] - columnVal.length();

            sb.append(columnVal);

            // right padding
            for (int j = 0; j < padding; j++) {
              sb.append(' ');
            }
            break;

          case Types.DATE:
            columnVal = "DATE '" + rs.getDate(colIndex).toString() + "'";
            padding = columnWidths[colIndex-1] - columnVal.length();

            sb.append(columnVal);

            // right padding
            for (int j = 0; j < padding; j++) {
              sb.append(' ');
            }
            break;

          case Types.DECIMAL:
          case Types.NUMERIC:
            columnVal = rs.getBigDecimal(colIndex).toPlainString();
            padding = columnWidths[colIndex-1] - columnVal.length();

            // left padding
            for (int j = 0; j < padding; j++) {
              sb.append(' ');
            }

            sb.append(columnVal);
            break;

          case Types.DOUBLE:
          case Types.FLOAT:
          case Types.REAL:
            columnVal = String.format("%e", rs.getDouble(colIndex));
            padding = columnWidths[colIndex-1] - columnVal.length();

            // left padding
            for (int j = 0; j < padding; j++) {
              sb.append(' ');
            }

            sb.append(columnVal);
            break;

          case Types.JAVA_OBJECT:
          case Types.OTHER:
            columnVal = rs.getObject(colIndex).toString();
            padding = columnWidths[colIndex-1] - columnVal.length();

            sb.append(columnVal);

            // right padding
            for (int j = 0; j < padding; j++) {
              sb.append(' ');
            }
            break;

          case Types.NULL:
            columnVal = "NULL";
            padding = columnWidths[colIndex-1] - columnVal.length();

            sb.append(columnVal);

            // right padding
            for (int j = 0; j < padding; j++) {
              sb.append(' ');
            }
            break;

          case Types.TIME:
          case Types.TIME_WITH_TIMEZONE:
            columnVal = "TIME '" + rs.getTime(colIndex).toString() + "'";
            padding = columnWidths[colIndex-1] - columnVal.length();

            sb.append(columnVal);

            // right padding
            for (int j = 0; j < padding; j++) {
              sb.append(' ');
            }
            break;

          case Types.TIMESTAMP:
          case Types.TIMESTAMP_WITH_TIMEZONE:
            columnVal = "TIMESTAMP '" + rs.getTimestamp(colIndex).toString() + "'";
            padding = columnWidths[colIndex-1] - columnVal.length();

            sb.append(columnVal);

            // right padding
            for (int j = 0; j < padding; j++) {
              sb.append(' ');
            }
            break;


          case Types.ARRAY:          // falls through
          case Types.BINARY:         // falls through
          case Types.BLOB:           // falls through
          case Types.CLOB:           // falls through
          case Types.DATALINK:       // falls through
          case Types.DISTINCT:       // falls through
          case Types.LONGNVARCHAR:   // falls through
          case Types.LONGVARBINARY:  // falls through
          case Types.LONGVARCHAR:    // falls through
          case Types.NCHAR:          // falls through
          case Types.NCLOB:          // falls through
          case Types.NVARCHAR:       // falls through
          case Types.REF:            // falls through
          case Types.REF_CURSOR:     // falls through
          case Types.ROWID:          // falls through
          case Types.SQLXML:         // falls through
          case Types.STRUCT:         // falls through
          case Types.VARBINARY:      // falls through
          default:
            throw new SethSystemException("Unhandled JDBC column type: " + columnType);
        }
      }

      sb.append(", ");
    }

    // Remove the last ", "
    sb.delete(sb.length() - 2, sb.length());

    sb.append(')');

    return sb.toString();
  }

  /**
   * Prints the row data as Strings into an array of string column values.
   * @param rs
   * @param row
   * @throws SQLException
   */
  public static void describeCurrentRow(ResultSet rs, List<String> row) throws SQLException
  {
    assert(row != null);

    ResultSetMetaData rsmd = rs.getMetaData();

    int numColumns = rsmd.getColumnCount();

    for (int colIndex = 1; colIndex <= numColumns; colIndex++) {
      int columnType = rsmd.getColumnType(colIndex);

      // check for null first
      if (rs.getObject(colIndex) == null) {
        row.add("NULL");

      } else {
        switch (columnType) {

          case Types.BIGINT:   // falls through
          case Types.BIT:      // falls through
          case Types.INTEGER:  // falls through
          case Types.SMALLINT: // falls through
          case Types.TINYINT:
            row.add(String.valueOf(rs.getLong(colIndex)));
            break;

          case Types.BOOLEAN:
            row.add(String.valueOf(rs.getBoolean(colIndex)));
            break;


          case Types.CHAR:
          case Types.VARCHAR:
            row.add('\'' + rs.getString(colIndex).replace("'", "''") + '\'');
            break;

          case Types.DATE:
            row.add("DATE '" + rs.getDate(colIndex).toString() + "'");
            break;

          case Types.DECIMAL:
          case Types.NUMERIC:
            row.add(rs.getBigDecimal(colIndex).toPlainString());
            break;

          case Types.DOUBLE:
          case Types.FLOAT:
          case Types.REAL:
            row.add(String.format("%e", rs.getDouble(colIndex)));
            break;

          case Types.JAVA_OBJECT:
          case Types.OTHER:
            row.add(rs.getObject(colIndex).toString());
            break;

          case Types.NULL:
            row.add("NULL");
            break;

          case Types.TIME:
          case Types.TIME_WITH_TIMEZONE:
            row.add("TIME '" + rs.getTime(colIndex).toString() + "'");
            break;

          case Types.TIMESTAMP:
          case Types.TIMESTAMP_WITH_TIMEZONE:
            row.add("TIMESTAMP '" + rs.getTimestamp(colIndex).toString() + "'");
            break;


          case Types.ARRAY:          // falls through
          case Types.BINARY:         // falls through
          case Types.BLOB:           // falls through
          case Types.CLOB:           // falls through
          case Types.DATALINK:       // falls through
          case Types.DISTINCT:       // falls through
          case Types.LONGNVARCHAR:   // falls through
          case Types.LONGVARBINARY:  // falls through
          case Types.LONGVARCHAR:    // falls through
          case Types.NCHAR:          // falls through
          case Types.NCLOB:          // falls through
          case Types.NVARCHAR:       // falls through
          case Types.REF:            // falls through
          case Types.REF_CURSOR:     // falls through
          case Types.ROWID:          // falls through
          case Types.SQLXML:         // falls through
          case Types.STRUCT:         // falls through
          case Types.VARBINARY:      // falls through
          default:
            throw new SethSystemException("Unhandled JDBC column type: " + columnType);
        }
      }
    }
  }


  /**
   * Updates the columnWidths array if the width of any column in the current row is greater than
   * the corresponding value in the columnWidths array.
   * @param columnWidths
   * @param rs
   * @throws SQLException
   */
  public static void updateColumnWidths(int[] columnWidths, ResultSet rs) throws SQLException
  {
    ResultSetMetaData rsmd = rs.getMetaData();
    int numColumns = rsmd.getColumnCount();
    assert (columnWidths.length == numColumns);

    for (int colIndex = 1; colIndex <= numColumns; colIndex++) {
      int columnType = rsmd.getColumnType(colIndex);

      int actualColumnWidth = 0;

      // check for null first
      if (rs.getObject(colIndex) == null && rs.wasNull()) {
        actualColumnWidth = 4; // "NULL"

      } else {
        switch (columnType) {

          case Types.BIGINT:   // falls through
          case Types.BIT:      // falls through
          case Types.INTEGER:  // falls through
          case Types.SMALLINT: // falls through
          case Types.TINYINT:
            actualColumnWidth = String.valueOf(rs.getLong(colIndex)).length();
            break;

          case Types.BOOLEAN:
            actualColumnWidth = String.valueOf(rs.getBoolean(colIndex)).length();
            break;


          case Types.CHAR:
          case Types.VARCHAR:
            actualColumnWidth = ("'" + rs.getString(colIndex).replace("'", "''") + "'").length();
            break;

          case Types.DATE:
            actualColumnWidth = rs.getDate(colIndex).toString().length() + 7; // for "DATE '" + "'"
            break;

          case Types.DECIMAL:
          case Types.NUMERIC:
            actualColumnWidth = rs.getBigDecimal(colIndex).toPlainString().length();
            break;

          case Types.DOUBLE:
          case Types.FLOAT:
          case Types.REAL:
            actualColumnWidth = String.format("%e", rs.getDouble(colIndex)).length();
            break;

          case Types.JAVA_OBJECT:
          case Types.OTHER:
            actualColumnWidth = rs.getObject(colIndex).toString().length();
            break;

          case Types.NULL:
            actualColumnWidth = 4;
            break;

          case Types.TIME:
          case Types.TIME_WITH_TIMEZONE:
            actualColumnWidth = rs.getTime(colIndex).toString().length() + 7; // for "TIME '" + "'"
            break;

          case Types.TIMESTAMP:
          case Types.TIMESTAMP_WITH_TIMEZONE:
            actualColumnWidth = rs.getTimestamp(colIndex).toString().length() + 12; // for "TIMESTAMP '" + "'"
            break;


          case Types.ARRAY:          // falls through
          case Types.BINARY:         // falls through
          case Types.BLOB:           // falls through
          case Types.CLOB:           // falls through
          case Types.DATALINK:       // falls through
          case Types.DISTINCT:       // falls through
          case Types.LONGNVARCHAR:   // falls through
          case Types.LONGVARBINARY:  // falls through
          case Types.LONGVARCHAR:    // falls through
          case Types.NCHAR:          // falls through
          case Types.NCLOB:          // falls through
          case Types.NVARCHAR:       // falls through
          case Types.REF:            // falls through
          case Types.REF_CURSOR:     // falls through
          case Types.ROWID:          // falls through
          case Types.SQLXML:         // falls through
          case Types.STRUCT:         // falls through
          case Types.VARBINARY:      // falls through
          default:
            throw new SethSystemException("Unhandled JDBC column type: " + columnType);
        }
      }

      if (actualColumnWidth > columnWidths[colIndex - 1]) {
        columnWidths[colIndex - 1] = actualColumnWidth;
      }
    }
  }

  /**
   * Describes and aligns a set of expected rows.
   * @oaran rsmd the metadata about the result set. Needed to get padding info.
   * @param expectedRows the set of expected rows to describe, limited by MAX_NUM_ROWS_TO_SHOW.
   * @param maxRowsToShow to maximum number of rows to describe.
   * @return a nicely formatted string that describes such.
   * @throws SQLException
   */
  protected static String describeExpectedRows(ResultSetMetaData rsmd, List<? extends ExpectedRow> expectedRows, int maxRowsToShow) throws SQLException
  {
    StringBuilder sb = new StringBuilder(1024);

    // How many do we want to display?
    List<? extends ExpectedRow> displayableRows = expectedRows.subList(0, Math.min(maxRowsToShow, expectedRows.size()));

    // Align them.
    AlignmentInfo alignment = alignRows(rsmd, displayableRows);

    for (ExpectedRow expectedRow : displayableRows) {
      if (sb.length() > 0) {
        sb.append(System.lineSeparator());
      }

      sb.append(expectedRow.toString(alignment.columnWidths, alignment.padLefts));
    }

    int excessRows = expectedRows.size() - displayableRows.size();
    if (excessRows > 0) {
      sb.append(System.lineSeparator());
      sb.append("...and ").append(excessRows).append(" more expected rows.");
    }

    return sb.toString();
  }

  /**
   * Describes and aligns a set of expected rows.
   * @param expectedRows the set of expected rows to describe, limited by MAX_NUM_ROWS_TO_SHOW.
   * @param alignment the alignment to use when describing the rows.
   * @param maxRowsToShow to maximum number of rows to describe.
   * @return a nicely formatted string that describes such.
   * @throws SQLException
   */
  protected static String describeExpectedRows(List<? extends ExpectedRow> expectedRows, AlignmentInfo alignment, int maxRowsToShow) throws SQLException
  {
    StringBuilder sb = new StringBuilder(1024);

    // How many do we want to display?
    List<? extends ExpectedRow> displayableRows = expectedRows.subList(0, Math.min(maxRowsToShow, expectedRows.size()));

    for (ExpectedRow expectedRow : displayableRows) {
      if (sb.length() > 0) {
        sb.append(System.lineSeparator());
      }

      sb.append(expectedRow.toString(alignment.columnWidths, alignment.padLefts));

      if (expectedRow instanceof ScoredExpectedRow) {
        sb.append(String.format("  [Score: %.2f]", ((ScoredExpectedRow) expectedRow).getScore()));
      }
    }

    int excessRows = expectedRows.size() - displayableRows.size();
    if (excessRows > 0) {
      sb.append(System.lineSeparator());
      sb.append("...and ").append(excessRows).append(" more expected rows.");
    }

    return sb.toString();
  }

  /**
   * Returns a nicely formatted and aligned string that describes the remaining rows in a ResultSet.
   * @param rs The ResultSet that is currently pointing to a row.
   * @param maxRowsToShow to maximum number of rows to describe.
   * @return
   * @throws SQLException
   */
  protected static String describeRemainingActualRows(ResultSet rs, int maxRowsToShow) throws SQLException
  {
    List<List<String>> rows = new ArrayList<>();

    int columnCount = rs.getMetaData().getColumnCount();

    // Extract all of the column values for each row so that we can align them before printing them.
    do {
      List<String> row = new ArrayList<String>(columnCount);
      ResultSetFormatter.describeCurrentRow(rs, row);

      rows.add(row);

    } while (rows.size() < maxRowsToShow && rs.next() );

    // How many more actual rows are there?
    int totalRowCount = rows.size();
    while (rs.next()) {
      ++totalRowCount;
    }

    // Get the alignment data for all of the rows.
    AlignmentInfo alignmentInfo = alignAllRows(rs.getMetaData(), rows);

    // Nicely print the rows
    StringBuilder sb = new StringBuilder(1024);

    for (List<String> row : rows) {
      if (sb.length() > 0) {
        sb.append(System.lineSeparator());
      }

      sb.append('(');

      for (int i = 0; i < row.size(); i++) {
        String column = row.get(i);

        if (i > 0) {
          sb.append(", ");
        }

        int padding = alignmentInfo.columnWidths[i] - column.length();

        if (alignmentInfo.padLefts[i]) {
          for (int j = 0; j < padding; j++) {
            sb.append(' ');
          }
        }

        sb.append(column);

        if (!alignmentInfo.padLefts[i]) {
          for (int j = 0; j < padding; j++) {
            sb.append(' ');
          }
        }
      }

      sb.append(')');
    }

    int excessRows = totalRowCount - rows.size();
    if (excessRows > 0) {
      sb.append(System.lineSeparator());
      sb.append("...and ").append(excessRows).append(" more rows.");
    }

    return sb.toString();
  }

  /**
   * Works out the column widths and padding locations for each row for a set of expected rows and a single actual row.
   * @param rs thw result set pointing to the actual row to be aligned with the expected rows.
   * @param expectedRows the set of expected rows to be aligned.
   * @return a container of alignment data.
   * @throws SQLException
   */
  public static AlignmentInfo alignRows(ResultSet rs, List<? extends ExpectedRow> expectedRows) throws SQLException
  {
    // Let's align the actual row and the expected rows. First we need to get the widths of them
    int[] columnWidths;

    if (rs != null) {
      columnWidths = new int[rs.getMetaData().getColumnCount()];

    } else {
      int numColumns = 0;

      for (ExpectedRow er : expectedRows) {
        numColumns = Math.max(er.getColumnDefs().size(), numColumns);
      }

      columnWidths = new int[numColumns];
    }

    for (int i = 0; i < columnWidths.length; i++) {
      columnWidths[i] = 0;
    }

    // Update the widths based on the actual row...
    if (rs != null) {
      ResultSetFormatter.updateColumnWidths(columnWidths, rs);
    }

    // ...and the expected rows.
    for (ExpectedRow er : expectedRows) {
      ResultSetFormatter.updateColumnWidths(columnWidths, er);
    }

    // Also get the desired padding position of each column so that we know how to pad things like nulls.
    boolean[] padLefts = null;

    if (rs != null) {
      padLefts = new boolean[columnWidths.length];
      ResultSetFormatter.updatePadLeft(padLefts, rs.getMetaData());
    }

    return new AlignmentInfo(columnWidths, padLefts);
  }

  /**
   * Works out the column widths and padding locations for each row for a set of expected rows and a single actual row.
   * @param rsmd the metadata about the result set.
   * @param expectedRows the set of expected rows to be aligned.
   * @return a container of alignment data.
   * @throws SQLException
   */
  public static AlignmentInfo alignRows(ResultSetMetaData rsmd, List<? extends ExpectedRow> expectedRows) throws SQLException
  {
    // Let's align the actual row and the expected rows. First we need to get the widths of them
    int[] columnWidths = new int[rsmd.getColumnCount()];;

    for (int i = 0; i < columnWidths.length; i++) {
      columnWidths[i] = 0;
    }

    // Update the widths based on the expected rows.
    for (ExpectedRow er : expectedRows) {
      ResultSetFormatter.updateColumnWidths(columnWidths, er);
    }

    // Also get the desired padding position of each column so that we know how to pad things like nulls.
    boolean[] padLefts = new boolean[columnWidths.length];
    ResultSetFormatter.updatePadLeft(padLefts, rsmd);

    return new AlignmentInfo(columnWidths, padLefts);
  }

  /**
   *
   * Works out the column widths and padding locations for each row for a List of Lists containing column String values.
   * @param rsmd the metadata about the result set.
   * @param rows the set of actual rows to be aligned.
   * @return a container of alignment data.
   * @throws SQLException
   */
  public static AlignmentInfo alignAllRows(ResultSetMetaData rsmd, List<List<String>> rows) throws SQLException
  {
    int columnCount = rsmd.getColumnCount();

    // Get the maximum column widths
    int[] columnWidths = new int[columnCount];;

    for (int i = 0; i < columnWidths.length; i++) {
      columnWidths[i] = 0;
    }

    for (List<String> row : rows) {
      for (int i = 0; i < row.size(); i++) {
        String column = row.get(i);

        if (column.length() > columnWidths[i]) {
          columnWidths[i] = column.length();
        }
      }
    }

    // Get the padding location.
    boolean[] padLefts = new boolean[columnCount];
    ResultSetFormatter.updatePadLeft(padLefts, rsmd);

    return new AlignmentInfo(columnWidths, padLefts);
  }

  /**
   * Updates the columnWidths array if the width of any column in this expected row is greater than
   * the corresponding value in the columnWidths array.
   * @param columnWidths
   * @param er
   */
  protected static void updateColumnWidths(int[] columnWidths, ExpectedRow er)
  {
    for (int i = 0; i < columnWidths.length; i++) {

      int erWidth = er.columnWidth(i);

      if (erWidth > columnWidths[i]) {
        columnWidths[i] = erWidth;
      }
    }
  }

  protected static void updatePadLeft(boolean[] padLeft, ResultSetMetaData rsmd) throws SQLException
  {
    int numColumns = rsmd.getColumnCount();
    assert (padLeft.length == numColumns);

    for (int colIndex = 1; colIndex <= numColumns; colIndex++) {
      int columnType = rsmd.getColumnType(colIndex);

      switch (columnType) {

        case Types.BIGINT:    // falls through
        case Types.BIT:       // falls through
        case Types.INTEGER:   // falls through
        case Types.SMALLINT:  // falls through
        case Types.TINYINT:   // falls through
        case Types.DECIMAL:   // falls through
        case Types.NUMERIC:   // falls through
        case Types.DOUBLE:    // falls through
        case Types.FLOAT:     // falls through
        case Types.REAL:
          padLeft[colIndex - 1] = true;
          break;

        case Types.BOOLEAN:
        case Types.CHAR:
        case Types.VARCHAR:
        case Types.DATE:
        case Types.JAVA_OBJECT:
        case Types.OTHER:
        case Types.NULL:
        case Types.TIME:
        case Types.TIME_WITH_TIMEZONE:
        case Types.TIMESTAMP:
        case Types.TIMESTAMP_WITH_TIMEZONE:
          padLeft[colIndex - 1] = false;
          break;

        case Types.ARRAY:          // falls through
        case Types.BINARY:         // falls through
        case Types.BLOB:           // falls through
        case Types.CLOB:           // falls through
        case Types.DATALINK:       // falls through
        case Types.DISTINCT:       // falls through
        case Types.LONGNVARCHAR:   // falls through
        case Types.LONGVARBINARY:  // falls through
        case Types.LONGVARCHAR:    // falls through
        case Types.NCHAR:          // falls through
        case Types.NCLOB:          // falls through
        case Types.NVARCHAR:       // falls through
        case Types.REF:            // falls through
        case Types.REF_CURSOR:     // falls through
        case Types.ROWID:          // falls through
        case Types.SQLXML:         // falls through
        case Types.STRUCT:         // falls through
        case Types.VARBINARY:      // falls through
        default:
          throw new SethSystemException("Unhandled JDBC column type: " + columnType);
      }
    }
  }


  public static String describeColumnNames(ResultSet rs) throws SQLException
  {
    ResultSetMetaData rsmd = rs.getMetaData();
    StringBuilder sb = new StringBuilder(1024);

    sb.append('[');

    int numColumns = rsmd.getColumnCount();

    for (int colIndex = 1; colIndex <= numColumns; colIndex++) {

      String label = rsmd.getColumnLabel(colIndex);

      sb.append('\'');
      sb.append(label.replace("'", "''"));
      sb.append('\'');
      sb.append(", ");
    }

    // Remove the last ", "
    sb.delete(sb.length() - 2, sb.length());

    sb.append(']');

    return sb.toString();
  }
}
