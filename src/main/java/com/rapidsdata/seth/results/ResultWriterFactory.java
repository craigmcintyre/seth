// Copyright (c) 2017 Boray Data Co. Ltd.  All rights reserved.

package com.rapidsdata.seth.results;

import com.rapidsdata.seth.CommandLineArgs;
import com.rapidsdata.seth.contexts.AppContext;
import com.rapidsdata.seth.exceptions.FeatureNotImplementedException;
import com.rapidsdata.seth.exceptions.InvalidResultFormatException;
import com.rapidsdata.seth.exceptions.SethSystemException;

import java.util.IllegalFormatException;

public class ResultWriterFactory
{
  private enum ResultFormat {
    JUNIT,
    LOG;

    public static String asStringList()
    {
      ResultFormat[] vals = values();
      StringBuilder sb = new StringBuilder(128);

      for (ResultFormat val : vals) {
        sb.append(val.name().toLowerCase());
        sb.append(", ");
      }

      // Remove the ", "
      sb.delete(sb.length() - 2, sb.length());
      return sb.toString();
    }
  }

  /**
   * Validates that a string describing a result format is indeed valid and supported.
   * @param resultFormat
   * @throws InvalidResultFormatException
   */
  public static void validate(String resultFormat) throws InvalidResultFormatException
  {
    ResultFormat format = formatStringToEnum(resultFormat);

    switch (format) {
      case JUNIT:
      case LOG:
        break;

      default:
        throw new FeatureNotImplementedException("Test result format is not yet implemented: " + format.name());
    }

  }

  /**
   * Creates a ResultWriter instance given the description of the type of ResultWriter requested
   * from the command line arguments.
   * @param args
   * @param context
   * @return a ResultWriter instance of the appropriate type.
   * @throws InvalidResultFormatException
   */
  public static ResultWriter get(CommandLineArgs args, AppContext context) throws InvalidResultFormatException
  {
    ResultWriter resultWriter;
    ResultFormat format = formatStringToEnum(args.resultFormat);


    switch (format) {
      case JUNIT:
        resultWriter = new JUnitResultWriter(context, args.resultDir, args.resultName);
        break;

      case LOG:
        resultWriter = new LoggableResultWriter(context);
        break;

      default:
        throw new SethSystemException("Unhandled ResultWriter format: " + format.name());
    }

    return resultWriter;
  }

  /**
   * Converts a string describing a result file format to a ResultFormat enum.
   * @param formatStr
   * @return the ResultFormat enum.
   * @throws InvalidResultFormatException
   */
  private static ResultFormat formatStringToEnum(String formatStr) throws InvalidResultFormatException
  {
    ResultFormat format;

    if (formatStr == null || formatStr.trim().isEmpty()) {
      final String msg = "Test result format cannot be null or empty. Valid values are: " +
                         ResultFormat.asStringList() + ".";
      throw new InvalidResultFormatException(msg);
    }

    // Convert string to enum
    try {
      format = ResultFormat.valueOf(formatStr.trim().toUpperCase());

    } catch (IllegalArgumentException e) {
      final String msg = "Invalid test result format: " + formatStr +
                         ". Valid values are: " + ResultFormat.asStringList() + ".";
      throw new InvalidResultFormatException(msg);
    }

    return format;
  }
}
