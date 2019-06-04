// Copyright (c) 2019 Boray Data Co. Ltd.  All rights reserved.

package com.rapidsdata.seth.plan.expectedResults;

import com.rapidsdata.seth.contexts.AppContext;
import com.rapidsdata.seth.exceptions.ExpectedResultFailureException;
import com.rapidsdata.seth.exceptions.FailureException;
import com.rapidsdata.seth.plan.OperationMetadata;
import com.rapidsdata.seth.plan.annotated.TestAnnotationInfo;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * An expected result class where the actual result received is saved and eventually written a the
 * expected result of a new test file.
 */
public class RecordNewExpectedResult extends ExpectedResult
{
  private static final String DESC = "<record new>";

  /** The object that holds all the information needed to annotate the test file with new expected results. */
  private final TestAnnotationInfo testToAnnotate;

  /** The identifying index of this new expected result in the original test file. */
  private final int erIndex;

  /**
   * Constructor
   * @param opMetadata The metadata about the operation that produced the actual result.
   * @param appContext The application context container.
   * @param testToAnnotate where we will store the new expected result.
   * @param erIndex the identifying index of this expected result, in the original test file.
   */
  public RecordNewExpectedResult(OperationMetadata opMetadata, AppContext appContext,
                                 TestAnnotationInfo testToAnnotate, int erIndex)
  {
    super(ExpectedResultType.RECORD_NEW, DESC, opMetadata, appContext);
    this.testToAnnotate = testToAnnotate;
    this.erIndex = erIndex;
  }

  /**
   * Compares the actual result, being a ResultSet, with the expected result.
   * @param rs The ResultSet to be compared to the expected result.
   * @throws FailureException if the expected result does not match with this actual result.
   */
  @Override
  public void assertActualAsResultSet(ResultSet rs) throws FailureException
  {
    final String NULL_STR = "NULL";

    StringBuilder sb = new StringBuilder(2048);

    if (opMetadata.getDescription().toLowerCase().contains("order by")) {
      sb.append("ordered rows: ");

    } else {
      sb.append("unordered rows: ");
    }

    List<String[]> rows = new ArrayList<>();
    int columnCount = 0;
    int[] columnWidths;
    boolean[] leftPad;

    try {
      ResultSetMetaData rsmd = rs.getMetaData();
      columnCount = rsmd.getColumnCount();
      columnWidths = new int[columnCount];
      leftPad = new boolean[columnCount];

      for (int i=0; i< columnCount; i++) {
        columnWidths[i] = 0;
        leftPad[i] = false;
      }

      while (rs.next()) {

        String columnValue;
        String[] row = new String[columnCount];
        rows.add(row);

        for (int i=1; i<=columnCount; i++) {

          int columnType = rsmd.getColumnType(i);

          switch (columnType) {
            // Integers
            case Types.BIT:
            case Types.TINYINT:
            case Types.SMALLINT:
            case Types.INTEGER:
            case Types.BIGINT:
              leftPad[i-1] = true;
              long lVal = rs.getLong(i);
              if (rs.wasNull()) {
                columnValue = NULL_STR;
              } else {
                columnValue = String.valueOf(lVal);
              }
              break;

            // Floating point
            case Types.FLOAT:
            case Types.REAL:
            case Types.DOUBLE:
              leftPad[i-1] = true;
              double dblVal = rs.getDouble(i);
              if (rs.wasNull()) {
                columnValue = NULL_STR;
              } else {
                columnValue = String.format("%e", dblVal);
              }
              break;

            // Decimal
            case Types.NUMERIC:
            case Types.DECIMAL:
              leftPad[i-1] = true;
              BigDecimal decVal = rs.getBigDecimal(i);
              if (rs.wasNull()) {
                columnValue = NULL_STR;
              } else {
                columnValue = decVal.toPlainString();
              }
              break;

            // String
            case Types.CHAR:
            case Types.VARCHAR:
            case Types.LONGVARCHAR:
              String strVal = rs.getString(i);
              if (rs.wasNull()) {
                columnValue = NULL_STR;
              } else {
                columnValue = "'" + strVal.replace("'", "''") + "'";
              }
              break;

            // Date
            case Types.DATE:
              Date dateVal = rs.getDate(i);
              if (rs.wasNull()) {
                columnValue = NULL_STR;
              } else {
                columnValue = "DATE '" + dateVal.toString() +  "'";
              }
              break;

            // Time
            case Types.TIME:
              Time timeVal = rs.getTime(i);
              if (rs.wasNull()) {
                columnValue = NULL_STR;
              } else {
                columnValue = "TIME '" + timeVal.toString() + "'";
              }
              break;

            // Timestamp
            case Types.TIMESTAMP:
              Timestamp tspVal = rs.getTimestamp(i);
              if (rs.wasNull()) {
                columnValue = NULL_STR;
              } else {
                columnValue = "TIMESTAMP '" + tspVal.toString() + "'";
              }
              break;

            // Boolean:
            case Types.BOOLEAN:
              boolean boolVal = rs.getBoolean(i);
              if (rs.wasNull()) {
                columnValue = NULL_STR;
              } else {
                columnValue = String.valueOf(boolVal);
              }
              break;

            // NULL column type
            case Types.NULL:
              //sb.append(NULL_STR);
              columnValue = NULL_STR;
              break;

            // Binary
//            case Types.BINARY:
//            case Types.VARBINARY:
//            case Types.LONGVARBINARY:
//            case Types.BLOB:
//              break;

            // Others that we may support in the future
//            case Types.TIME_WITH_TIMEZONE:
//            case Types.JAVA_OBJECT:
//            case Types.ARRAY:
//            case Types.CLOB:
//            case Types.REF:
//            case Types.ROWID:

            default:
              final String errMsg = "Unsupported JDBC column type for column " + i + ": " + columnType;
              final String colVal = "Column Value = " + (rs.getString(i) == null ? "(null)" : rs.getString(i));
              throw new ExpectedResultFailureException(opMetadata, errMsg, colVal, this.describe());
          }

          row[i-1] = columnValue;
          if (columnValue.length() > columnWidths[i-1]) {
            columnWidths[i-1] = columnValue.length();
          }
        }
      }

    } catch (SQLException e) {
      final String commentDesc = "An exception was received while iterating the result set received.";
      final String actualResultDesc = e.getClass().getSimpleName() + ": " + e.getMessage();
      throw new ExpectedResultFailureException(opMetadata, commentDesc, actualResultDesc, this.describe());
    }

    if (rows.isEmpty()) {
      sb.append("0");  // as in "unordered rows: 0"

    } else {
      // Pretty-print the ResultSet
      for (String[] row : rows) {
        sb.append("\n  ( ");

        for (int i = 0; i < columnCount; i++) {

          if (i > 0) {
            sb.append(", ");
          }

          String columnVal = row[i];
          int padding = columnWidths[i] - columnVal.length();
          assert (padding >= 0);

          if (leftPad[i]) {
            for (int j = 0; j < padding; j++) {
              sb.append(' ');
            }

            sb.append(columnVal);

          } else {
            // right padding
            sb.append(columnVal);

            for (int j = 0; j < padding; j++) {
              sb.append(' ');
            }
          }
        }

        sb.append(" )");
      }
    }

    testToAnnotate.addNewExpectedResult(erIndex, sb.toString());
  }

  /**
   * Compares the actual result, being an update count, with the expected result.
   *
   * @param updateCount The update count to be compared to the expected result.
   * @throws FailureException if the expected result does not match with this actual result.
   */
  @Override
  public void assertActualAsUpdateCount(long updateCount) throws FailureException
  {
    String expectedResult;

    String cmd = opMetadata.getDescription().toLowerCase().trim();

    if (updateCount != 0 ||
        cmd.startsWith("insert into") ||
        cmd.startsWith("update") ||
        cmd.startsWith("delete from")) {

      expectedResult = "affected: " + updateCount;

    } else {
      expectedResult = "success";
    }

    testToAnnotate.addNewExpectedResult(erIndex, expectedResult);
  }

  /**
   * Compares the actual result, being a SQLException, with the expected result.
   *
   * @param e The exception to be compared to the expected result.
   * @throws FailureException if the expected result does not match with this actual result.
   */
  @Override
  public void assertActualAsException(SQLException e) throws FailureException
  {
    assertActualAsFailure(e.getMessage());
  }

  /**
   * Compares the actual result, being an Exception, with the expected result.
   * Because this is a general exception, the stack trace will be included.
   *
   * @param e The exception to be compared to the expected result.
   * @throws FailureException if the expected result does not match with this actual result.
   */
  @Override
  public void assertActualAsException(Exception e) throws FailureException
  {
    assertActualAsFailure(e.getMessage());
  }

  /**
   * Compares the actual result, being a general purpose statement of success, with the expected result.
   *
   * @throws FailureException if the expected result does not match with this actual result.
   */
  @Override
  public void assertActualAsSuccess() throws FailureException
  {
    String expectedResult = "success";
    testToAnnotate.addNewExpectedResult(erIndex, expectedResult);
  }

  /**
   * Compares the actual result, being a general purpose failure with an error message, with the expected result.
   *
   * @param msg The error message to be compared to the expected result.
   * @throws FailureException if the expected result does not match with this actual result.
   */
  @Override
  public void assertActualAsFailure(String msg) throws FailureException
  {
    String expectedResult = "failure contains: \"" + msg.replace("\"", "\"\"") + "\"";
    testToAnnotate.addNewExpectedResult(erIndex, expectedResult);
  }
}
