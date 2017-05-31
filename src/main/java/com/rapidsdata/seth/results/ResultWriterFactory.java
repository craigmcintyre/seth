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

  public static ResultWriter get(CommandLineArgs args, AppContext context) throws InvalidResultFormatException
  {
    ResultWriter resultWriter;
    ResultFormat format;

    try {
      format = ResultFormat.valueOf(args.resultFormat.toUpperCase());

    } catch (IllegalFormatException e) {
      final String msg = "Invalid test result format: " + args.resultFormat +
                         ". Valid values are: " + ResultFormat.asStringList() + ".";
      throw new InvalidResultFormatException(msg);
    }

    switch (format) {
      case JUNIT:
        throw new FeatureNotImplementedException("JUnit test result format is not yet implemented.");
        //resultWriter = new JUnitResultWriter(context, args.resultDir, args.resultName);
        //break;

      case LOG:
        resultWriter = new LoggableResultWriter(context);
        break;

      default:
        throw new SethSystemException("Unhandled ResultWriter format: " + format.name());
    }

    return resultWriter;
  }

}
