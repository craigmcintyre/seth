// Copyright (c) 2020 Boray Data Co. Ltd.  All rights reserved.

package com.rapidsdata.seth;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class Options extends HashMap<String, Object>
{
  /** Name of the key for comparing strings case insensitively. Default is case sensitive. */
  public static final String CASE_INSENSITIVE_KEY = "ignorecase";

  /** Name of the key for rounding numeric values to N digits of __precision__ before comparing. Default is no rounding. */
  public static final String PRECISION_ROUNDING_KEY = "precisionrounding";

  /** Name of the key for rounding numeric values to N __decimal places__ before comparing. Default is no rounding. */
  public static final String DECIMAL_ROUNDING_KEY = "decimalrounding";

  public static final int NO_ROUNDING = -1;

  /** Ignore any trailing whitespace in strings values when doing the comparison */
  public static final String IGNORE_TRAILING_WHITESPACE_KEY = "ignoretrailingspace";


  public Options()
  {
    super();
  }

  public String toString()
  {
    StringBuilder sb = new StringBuilder(256);

    sb.append('[');

    boolean first = true;
    for (Entry<String,Object> entry : this.entrySet()) {

      if (first) {
        first = false;

      } else {
        sb.append(", ");
      }

      String key = entry.getKey();
      Object val = entry.getValue();

      if (entry.getKey().contains(" ")) {
        sb.append("'").append(key).append("'");

      } else {
        sb.append(key);
      }

      if (val != null) {
        sb.append(" = ");

        if (val instanceof String) {
          sb.append("'");
          sb.append(val.toString());
          sb.append("'");

        } else {
          sb.append(val.toString());
        }
      }
    }

    sb.append(']');

    return sb.toString();
  }

  /**
   * Creates a list of Option objects where the priority of options is highest with the last options object specified.
   * @param args
   * @return a list of Option objects ordered from highest to lowest priority.
   */
  public static LinkedList<Options> listOf(Options... args)
  {
    LinkedList<Options> list = new LinkedList<>();

    for (Options opt : args) {
      list.addFirst(opt);
    }

    return list;
  }

  public static boolean getIgnoreCase(List<Options> optionList)
  {
    // Iterate through each non-null option entry. Earlier options override later ones.
    for (Options options : optionList) {
      if (options == null) {
        continue;
      }

      if (options.containsKey(CASE_INSENSITIVE_KEY)) {
        Object objVal = options.get(CASE_INSENSITIVE_KEY);

        if ( (objVal instanceof Boolean && !((Boolean) objVal)) ||
             (objVal instanceof Integer && ((Integer) objVal) == 0) ||
             (objVal instanceof Long && ((Long) objVal) == 0L)
           ) {
          return false;
        }

        return true;
      }

      // Not specified at this level. Keep trying another level.
    }

    // Overall default.
    return false;
  }

  public static int getPrecisionRounding(List<Options> optionList)
  {
    for (Options options : optionList) {
      if (options == null) {
        continue;
      }

      if (options.containsKey(PRECISION_ROUNDING_KEY)) {
        Object objVal = options.get(PRECISION_ROUNDING_KEY);

        if (objVal instanceof Number) {
          int intVal = ((Number) objVal).intValue();

          if (intVal >= 0 || intVal == NO_ROUNDING) {
            return intVal;
          }
        }
      }
    }

    return NO_ROUNDING;
  }

  public static int getDecimalRounding(List<Options> optionList)
  {
    for (Options options : optionList) {
      if (options == null) {
        continue;
      }

      if (options.containsKey(DECIMAL_ROUNDING_KEY)) {
        Object objVal = options.get(DECIMAL_ROUNDING_KEY);

        if (objVal instanceof Number) {
          int intVal = ((Number) objVal).intValue();

          if (intVal >= 0 || intVal == NO_ROUNDING) {
            return intVal;
          }
        }
      }
    }

    return NO_ROUNDING;
  }

  public static void setPrecisionRounding(Options options, int value) throws IllegalArgumentException
  {
    if (value < 0 && value != NO_ROUNDING) {
      throw new IllegalArgumentException("Invalid value specified for option \"" + PRECISION_ROUNDING_KEY + "\": " + value);
    }

    options.put(PRECISION_ROUNDING_KEY, value);
  }

  public static boolean getIgnoreTrailingWhitespace(List<Options> optionList)
  {
    // Iterate through each non-null option entry. Earlier options override later ones.
    for (Options options : optionList) {
      if (options == null) {
        continue;
      }

      if (options.containsKey(IGNORE_TRAILING_WHITESPACE_KEY)) {
        Object objVal = options.get(IGNORE_TRAILING_WHITESPACE_KEY);

        if ( (objVal instanceof Boolean && !((Boolean) objVal)) ||
                (objVal instanceof Integer && ((Integer) objVal) == 0) ||
                (objVal instanceof Long && ((Long) objVal) == 0L)
        ) {
          return false;
        }

        return true;
      }

      // Not specified at this level. Keep trying another level.
    }

    // Overall default.
    return false;
  }
}
