// Copyright (c) 2017 Boray Data Co. Ltd.  All rights reserved.

package com.rapidsdata.seth.plan.expectedResults;

import com.rapidsdata.seth.exceptions.SethSystemException;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;

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
        sb.append("null");

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
        sb.append("null");

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
   * Updates the columnWidths array if the width of any column in this expected row is greater than
   * the corresponding value in the columnWidths array.
   * @param columnWidths
   * @param er
   */
  public static void updateColumnWidths(int[] columnWidths, ExpectedRow er)
  {
    for (int i = 0; i < columnWidths.length; i++) {

      int erWidth = er.columnWidth(i);

      if (erWidth > columnWidths[i]) {
        columnWidths[i] = erWidth;
      }
    }
  }

  public static void updatePadLeft(boolean[] padLeft, ResultSet rs) throws SQLException
  {
    ResultSetMetaData rsmd = rs.getMetaData();
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
