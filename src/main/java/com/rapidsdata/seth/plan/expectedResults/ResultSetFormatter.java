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

      sb.append(", ");
    }

    // Remove the last ", "
    sb.delete(sb.length() - 2, sb.length());

    sb.append(')');

    return sb.toString();
  }
}
