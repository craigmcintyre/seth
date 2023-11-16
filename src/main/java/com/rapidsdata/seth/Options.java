// Copyright (c) 2020 Boray Data Co. Ltd.  All rights reserved.

package com.rapidsdata.seth;

import java.util.*;

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

  /** Name of the key for forcing do not evaluate and replace any variable references */
  public static final String NO_VAR_REF_EVAL_KEY = "novarrefeval";
  private static final boolean NO_VAR_REF_EVAL_DEFAULT_VAL = false;


  private static final String BAD_VAR_REF_ERROR = "error";
  private static final String BAD_VAR_REF_EMPTY = "empty";
  private static final String BAD_VAR_REF_NOEVAL = "noeval";

  /** An enum of the possible handlers for invalid variable references */
  public enum BadVarRefHandler {
    ERROR (BAD_VAR_REF_ERROR),
    EMPTY (BAD_VAR_REF_EMPTY),
    NO_EVAL (BAD_VAR_REF_NOEVAL);

    private final String desc;

    private BadVarRefHandler(String desc)
    {
      this.desc = desc;
    }

    public String getDesc()
    {
      return desc;
    }

    public static String[] validDescriptions()
    {
      return new String[] {BAD_VAR_REF_ERROR, BAD_VAR_REF_EMPTY, BAD_VAR_REF_NOEVAL};
    }

    /**
     * Returns the appropriate BadVarRefHandler enum value that corresponds
     * to the input description string, or null if the string doesn't match
     * (case insensitive).
     * @param desc
     * @return the appropriate BadVarRefHandler or null.
     */
    public static BadVarRefHandler fromDesc(String desc)
    {
      if (desc.equalsIgnoreCase(BAD_VAR_REF_ERROR)) {
        return ERROR;

      } else if (desc.equalsIgnoreCase(BAD_VAR_REF_EMPTY)) {
        return EMPTY;

      } else if (desc.equalsIgnoreCase(BAD_VAR_REF_NOEVAL)) {
        return NO_EVAL;

      } else {
        return null;
      }
    }
  }

  /** Name of the key for how invalid variable references are handled */
  public static final String BAD_VAR_REF_KEY = "badvarref";
  private static final BadVarRefHandler BAD_VAR_REF_DEFAULT_VAL = BadVarRefHandler.ERROR;

  /**
   *  A list of option key names that need to be executed immediately when parsed
   *  because they affect the parsing of subsequent operations.
   */
  public static final List<String> executeImmediateOptions = Arrays.asList(new String[]{
      NO_VAR_REF_EVAL_KEY,
      BAD_VAR_REF_KEY,
  });

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

    // Add them in reversed order so that we can more easily find the first
    // occurrence of an option setting, rather than the last occurrence.
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


  /**
   * Gets a boolean value indicating whether variable reference evaluation should be skipped.
   * @param optionList
   * @return true if var ref evaluation should be skipped. False if they should be evaluated.
   */
  public static boolean getNoVarRefEval(List<Options> optionList)
  {
    // Iterate through each non-null option entry. Earlier options override later ones.
    for (Options options : optionList) {
      if (options == null) {
        continue;
      }

      if (options.containsKey(NO_VAR_REF_EVAL_KEY)) {
        Object objVal = options.get(NO_VAR_REF_EVAL_KEY);

        if (!(objVal instanceof Boolean)) {
          String errMsg = String.format(
              "Expected option object to be of type Boolean. Instead got %s.",
              objVal.getClass().getName());
          throw new IllegalStateException(errMsg);
        }

        return (boolean) objVal;
      }

      // Not specified at this level. Keep trying another level.
    }

    // Overall default.
    return NO_VAR_REF_EVAL_DEFAULT_VAL;
  }

  /**
   * Sets whether variable references should not be evaluated.
   * @param options
   * @param noEval
   */
  public static void setNoVarRefEval(Options options, boolean noEval)
  {
    options.put(NO_VAR_REF_EVAL_KEY, noEval);
  }


  /**
   * Gets the method that should be used for handling invalid variable references.
   * @param optionList
   * @return a BadVarRefHandler enum
   */
  public static BadVarRefHandler getBadVarRefHandler(List<Options> optionList)
  {
    // Iterate through each non-null option entry. Earlier options override later ones.
    for (Options options : optionList) {
      if (options == null) {
        continue;
      }

      if (options.containsKey(BAD_VAR_REF_KEY)) {
        Object objVal = options.get(BAD_VAR_REF_KEY);

        if (!(objVal instanceof BadVarRefHandler)) {
          String errMsg = String.format(
              "Expected option object to be of type BadVarRefHandler. Instead got %s.",
              objVal.getClass().getName());
          throw new IllegalStateException(errMsg);
        }

        return (BadVarRefHandler) objVal;
      }

      // Not specified at this level. Keep trying another level.
    }

    // Overall default.
    return BAD_VAR_REF_DEFAULT_VAL;
  }

  /**
   * Set the method for handling bad variable reference evaluations.
   * @param options
   * @param handler
   */
  public static void setBadVarRefHandler(Options options, BadVarRefHandler handler)
  {
    options.put(BAD_VAR_REF_KEY, handler);
  }

}
