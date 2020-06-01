// Copyright (c) 2020 Boray Data Co. Ltd.  All rights reserved.

package com.rapidsdata.seth;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class Options extends HashMap<String, Object>
{
  public static final String CASE_INSENSITIVE = "ignorecase";

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

  public static boolean ignoreCase(List<Options> optionList)
  {
    // Iterate through each non-null option entry. Earlier options override later ones.
    for (Options options : optionList) {
      if (options == null) {
        continue;
      }

      if (options.containsKey(CASE_INSENSITIVE)) {
        Object objVal = options.get(CASE_INSENSITIVE);

        if ( (objVal instanceof Boolean && !((Boolean) objVal)) ||
             (objVal instanceof Integer && ((Integer) objVal) == 0) ||
             (objVal instanceof Long && ((Long) objVal) == 0l)
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
