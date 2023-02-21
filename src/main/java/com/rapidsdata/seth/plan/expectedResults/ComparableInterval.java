// Copyright (c) 2017 Boray Data Co. Ltd.  All rights reserved.

package com.rapidsdata.seth.plan.expectedResults;

import com.rapidsdata.seth.exceptions.SyntaxException;

import java.io.File;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * A class used to represent both an expected interval value as well as actual
 * interval values. This is because the JDBC standard does not define an interval
 * type. We don't want SETH to be dependent and bundled with specific drivers
 * (e.g. Postgres driver for the PgInterval class). So instead the approach is
 * to call resultSet.getString() on the interval column and parse it in this class.
 * The expected value is also an instance of this class. Then we can compare two
 * intervals more easily and with nuance.
 *
 * Nuance is important because we want to be as strict on types as we can be. However
 * some systems such as Postgres don't expose the specific interval sub-type, so we
 * need to do our best with the information we have.
 */
public class ComparableInterval
{
  public enum IntervalType
  {
    YEAR ("YEAR", "%1$d", true, false),
    MONTH ("MONTH", "%2$d", true, false),
    YEAR_TO_MONTH ("YEAR TO MONTH", "%1$d-%2$d", true, false),
    DAY ("DAY", "%3$d", false, true),
    HOUR ("HOUR", "%4$d", false, true),
    MINUTE ("MINUTE", "%5$d", false, true),
    SECOND ("SECOND", "%6$d.%7$06d", false, true),
    DAY_TO_HOUR ("DAY TO HOUR", "%3$d %4$d", false, true),
    DAY_TO_MINUTE ("DAY TO MINUTE", "%3$d %4$d:%5$d", false, true),
    DAY_TO_SECOND ("DAY TO SECOND", "%3$d %4$d:%5$d:%6$d", false, true),
    HOUR_TO_MINUTE ("HOUR TO MINUTE", "%4$d:%5$d", false, true),
    HOUR_TO_SECOND ("HOUR TO SECOND", "%4$d:%5$d:%6$d", false, true),
    MINUTE_TO_SECOND ("MINUTE TO SECOND", "%5$d:%6$d", false, true),
    UNKNOWN ("", "%1$d-%2$d-%3$d %4$d:%5$d:%6$d", false, false),
    UNKNOWN_YEAR_MONTH ("YEAR TO MONTH", "%1$d-%2$d", true, false),
    UNKNOWN_DAY_TIME ("DAY TO SECOND", "%3$d %4$d:%5$d:%6$d", false, true);

    public final String desc;
    public final String format;
    public final boolean isYearMonthType;
    public final boolean isDayTimeType;

    private IntervalType(String desc, String format, boolean isYearMonthType, boolean isDayTimeType)
    {
      this.desc = desc;
      this.format = format;
      this.isYearMonthType = isYearMonthType;
      this.isDayTimeType = isDayTimeType;
    }

    /**
     * Given a string like "YEAR TO MONTH", return the associated enum
     */
    public static IntervalType fromString(String typeStr)
    {
      typeStr = typeStr.trim();

      if (typeStr.equalsIgnoreCase("YEAR"))              return YEAR;
      if (typeStr.equalsIgnoreCase("MONTH"))             return MONTH;
      if (typeStr.equalsIgnoreCase("YEAR TO MONTH"))     return YEAR_TO_MONTH;
      if (typeStr.equalsIgnoreCase("DAY"))               return DAY;
      if (typeStr.equalsIgnoreCase("HOUR"))              return HOUR;
      if (typeStr.equalsIgnoreCase("MINUTE"))            return MINUTE;
      if (typeStr.equalsIgnoreCase("SECOND"))            return SECOND;
      if (typeStr.equalsIgnoreCase("DAY TO HOUR"))       return DAY_TO_HOUR;
      if (typeStr.equalsIgnoreCase("DAY TO MINUTE"))     return DAY_TO_MINUTE;
      if (typeStr.equalsIgnoreCase("DAY TO SECOND"))     return DAY_TO_SECOND;
      if (typeStr.equalsIgnoreCase("HOUR TO MINUTE"))    return HOUR_TO_MINUTE;
      if (typeStr.equalsIgnoreCase("HOUR TO SECOND"))    return HOUR_TO_SECOND;
      if (typeStr.equalsIgnoreCase("MINUTE TO SECOND"))  return MINUTE_TO_SECOND;

      return UNKNOWN;
    }
  }

  final IntervalType type;
  final boolean isNegative;
  final int years;
  final int months;
  final int days;
  final int hours;
  final int minutes;
  final int seconds;
  final int micros;

  /**
   * Constructor of a year-month interval
   */
  public ComparableInterval(IntervalType type, boolean isNegative, int years, int months)
  {
    assert(type == IntervalType.YEAR ||
           type == IntervalType.MONTH ||
           type == IntervalType.YEAR_TO_MONTH ||
           type == IntervalType.UNKNOWN ||
           type == IntervalType.UNKNOWN_YEAR_MONTH);

    this.type       = type;
    this.isNegative = isNegative;
    this.years      = years;
    this.months     = months;
    this.days       = 0;
    this.hours      = 0;
    this.minutes    = 0;
    this.seconds    = 0;
    this.micros     = 0;
  }

  /**
   * Constructor of a day-time interval
   */
  private ComparableInterval(IntervalType type, boolean isNegative,
                             int years, int months,
                             int days, int hours, int minutes, int seconds, int micros)
  {
    assert (micros >= 0);
    assert (micros < 1000000);

    this.type       = type;
    this.isNegative = isNegative;
    this.years      = years;
    this.months     = months;
    this.days       = days;
    this.hours      = hours;
    this.minutes    = minutes;
    this.seconds    = seconds;
    this.micros     = micros;
  }

  public ComparableInterval(IntervalType type, boolean isNegative, int days, int hours, int minutes, int seconds, int micros)
  {
    assert(type == IntervalType.DAY ||
           type == IntervalType.HOUR ||
           type == IntervalType.MINUTE ||
           type == IntervalType.SECOND ||
           type == IntervalType.DAY_TO_HOUR ||
           type == IntervalType.DAY_TO_MINUTE ||
           type == IntervalType.DAY_TO_SECOND ||
           type == IntervalType.HOUR_TO_MINUTE ||
           type == IntervalType.HOUR_TO_SECOND ||
           type == IntervalType.MINUTE_TO_SECOND ||
           type == IntervalType.UNKNOWN ||
           type == IntervalType.UNKNOWN_DAY_TIME);

    assert (micros >= 0);
    assert (micros < 1000000);

    this.type       = type;
    this.isNegative = isNegative;
    this.years      = 0;
    this.months     = 0;
    this.days       = days;
    this.hours      = hours;
    this.minutes    = minutes;
    this.seconds    = seconds;
    this.micros     = micros;
  }

  /**
   * @return the type of this interval
   */
  public IntervalType getType()
  {
    return this.type;
  }

  public int getYears()     { return this.years; }
  public int getMonths()    { return this.months; }
  public int getDays()      { return this.days; }
  public int getHours()     { return this.hours; }
  public int getMinutes()   { return this.minutes; }
  public int getSeconds()   { return this.seconds; }
  public int getMicros()    { return this.micros; }
  public int getNormalisedYearsAndMonths()       { return this.normalisedYearsToMonths(); }
  public int getNormalisedDaysAndHours()         { return this.normalisedDaysToHours(); }
  public int getNormalisedDaysHoursAndMins()     { return this.normalisedDaysToMinutes(); }
  public int getNormalisedDaysHoursMinsAndSecs() { return this.normalisedDaysToSeconds(); }
  public int getNormalisedHoursAndMins()         { return this.normalisedHoursToMinutes(); }
  public int getNormalisedHoursMinsAndSecs()     { return this.normalisedHoursToSeconds(); }
  public int getNormalisedMinsAndSecs()          { return this.normalisedMinutesToSeconds(); }

  /**
   * Tries to create a ComparableInterval from a specific column of the current row
   * of the resultSet. Returns null if it cannot create one.
   * @param rs
   * @param columnIdx
   * @return a ComparableInterval instance, or null
   */
  public static ComparableInterval fromResultSet(ResultSet rs, int columnIdx)
  {
    try {
      ResultSetMetaData rsmd = rs.getMetaData();

      boolean isIndexType = rsmd.getColumnType(columnIdx) == Types.OTHER &&
                            rsmd.getColumnTypeName(columnIdx).toLowerCase().startsWith("interval");

      if (!isIndexType) {
        return null;
      }

      // Try parsing an SE interval
      ComparableInterval actualInterval = ComparableInterval.parseIntervalLiteral(rs.getString(columnIdx));
      if (actualInterval != null) {
        return actualInterval;
      }

      // Try parsing a Postgres interval via rs.getObject(i).toString()
      actualInterval = ComparableInterval.parsePostgresString(rs.getString(columnIdx));
      if (actualInterval != null) {
        return actualInterval;
      }

      // Try parsing a Postgres interval via rs.getString(i)
      actualInterval = ComparableInterval.parsePostgresObjectToString(rs.getObject(columnIdx).toString());
      if (actualInterval != null) {
        return actualInterval;
      }

      // Give up, we don't know how to parse this interval type.
      return null;

    } catch (SQLException e) {
      return null;
    }
  }

  /**
   * This parses an interval literal of the form: INTERVAL [-]'...' <intervalType>
   * @param literal A string representing the full interval literal, including
   *                the INTERVAL keyword
   * @return a ComparableInterval or null if it could not be parsed.
   */
  public static ComparableInterval parseIntervalLiteral(String literal)
  {
    Pattern pattern = Pattern.compile("\\s*INTERVAL\\s+(-?)'([^']+)'\\s+([a-zA-z ]+)$", Pattern.CASE_INSENSITIVE);
    Matcher matcher = pattern.matcher(literal);

    if (!matcher.matches() || matcher.groupCount() != 3) {
      return null;
    }

    boolean isNegative = (matcher.group(1) != null && matcher.group(1).equals("-"));
    String intervalStr = matcher.group(2);
    String intervalTypeStr = matcher.group(3);

    assert(intervalStr != null);
    assert(!intervalStr.isEmpty());

    IntervalType eType = IntervalType.fromString(intervalTypeStr);
    if (eType == IntervalType.UNKNOWN) {
      return null;
    }

    // Parse the string in this interval literal. Result may be null.
    ComparableInterval interval = parseIntervalStringComponent(intervalStr, isNegative, eType);
    return interval;
  }

  /**
   * Parse the string component of an interval literal (not including the quotes).
   * @param intervalStr the string description of a PGInterval type
   * @return a ComparableInterval instance, or null if it could not be parsed.
   */
  public static ComparableInterval parseIntervalStringComponent(String intervalStr,
                                                                boolean isNegative,
                                                                IntervalType eType)
  {
    int year  = 0;
    int month = 0;
    int day   = 0;
    int hour  = 0;
    int min   = 0;
    int sec   = 0;
    int micro = 0;

    Pattern pattern;
    Matcher matcher;
    String microStr;

    try {
      switch (eType) {

        case YEAR:
          year = Integer.parseInt(intervalStr);
          break;

        case MONTH:
          month = Integer.parseInt(intervalStr);
          break;

        case YEAR_TO_MONTH:
          pattern = Pattern.compile("(-?\\d+)-(\\d+)");
          matcher = pattern.matcher(intervalStr);

          if (!matcher.matches() ||
              matcher.groupCount() != 2 ||
              matcher.group(1) == null  ||
              matcher.group(2) == null)  {
            return null;
          }

          year  = Integer.parseInt(matcher.group(1));
          month = Integer.parseInt(matcher.group(2));
          break;

        case DAY:
          day = Integer.parseInt(intervalStr);
          break;

        case HOUR:
          hour = Integer.parseInt(intervalStr);
          break;

        case MINUTE:
          min = Integer.parseInt(intervalStr);
          break;

        case SECOND:
          pattern = Pattern.compile("(-?\\d+)(?:\\.(\\d*))?");
          matcher = pattern.matcher(intervalStr);

          if (!matcher.matches() ||
              matcher.groupCount() != 2 ||
              matcher.group(1) == null)  {
            return null;
          }

          sec  = Integer.parseInt(matcher.group(1));

          microStr = matcher.group(2);
          if (microStr != null && !microStr.isEmpty()) {
            micro = Integer.parseInt(microStr);

            if (microStr.length() > 6) {
              return null;
            }

            // Correct micros for missing leading zeros
            int powers = 6 - microStr.length();
            while (powers > 0) {
              micro = micro * 10;
              --powers;
            }
          }
          break;

        case DAY_TO_HOUR:
          pattern = Pattern.compile("(-?\\d+) (\\d+)");
          matcher = pattern.matcher(intervalStr);

          if (!matcher.matches() ||
              matcher.groupCount() != 2 ||
              matcher.group(1) == null  ||
              matcher.group(2) == null)  {
            return null;
          }

          day  = Integer.parseInt(matcher.group(1));
          hour = Integer.parseInt(matcher.group(2));
          break;

        case DAY_TO_MINUTE:
          pattern = Pattern.compile("(-?\\d+) (\\d+):(\\d+)");
          matcher = pattern.matcher(intervalStr);

          if (!matcher.matches() ||
              matcher.groupCount() != 3 ||
              matcher.group(1) == null  ||
              matcher.group(2) == null  ||
              matcher.group(3) == null)  {
            return null;
          }

          day  = Integer.parseInt(matcher.group(1));
          hour = Integer.parseInt(matcher.group(2));
          min  = Integer.parseInt(matcher.group(3));
          break;

        case DAY_TO_SECOND:
          pattern = Pattern.compile("(-?\\d+) (\\d+):(\\d+):(\\d+)(?:\\.(\\d*))?");
          matcher = pattern.matcher(intervalStr);

          if (!matcher.matches() ||
              matcher.groupCount() != 5 ||
              matcher.group(1) == null  ||
              matcher.group(2) == null  ||
              matcher.group(3) == null  ||
              matcher.group(4) == null)  {
            return null;
          }

          day  = Integer.parseInt(matcher.group(1));
          hour = Integer.parseInt(matcher.group(2));
          min  = Integer.parseInt(matcher.group(3));
          sec  = Integer.parseInt(matcher.group(4));

          microStr = matcher.group(5);
          if (microStr != null && !microStr.isEmpty()) {
            micro = Integer.parseInt(microStr);

            if (microStr.length() > 6) {
              return null;
            }

            // Correct micros for missing leading zeros
            int powers = 6 - microStr.length();
            while (powers > 0) {
              micro = micro * 10;
              --powers;
            }
          }
          break;

        case HOUR_TO_MINUTE:
          pattern = Pattern.compile("(-?\\d+):(\\d+)");
          matcher = pattern.matcher(intervalStr);

          if (!matcher.matches() ||
              matcher.groupCount() != 2 ||
              matcher.group(1) == null  ||
              matcher.group(2) == null)  {
            return null;
          }

          hour = Integer.parseInt(matcher.group(1));
          min  = Integer.parseInt(matcher.group(2));
          break;

        case HOUR_TO_SECOND:
          pattern = Pattern.compile("(-?\\d+):(\\d+):(\\d+)(?:\\.(\\d*))?");
          matcher = pattern.matcher(intervalStr);

          if (!matcher.matches() ||
              matcher.groupCount() != 4 ||
              matcher.group(1) == null  ||
              matcher.group(2) == null  ||
              matcher.group(3) == null) {
            return null;
          }

          hour = Integer.parseInt(matcher.group(1));
          min  = Integer.parseInt(matcher.group(2));
          sec  = Integer.parseInt(matcher.group(3));

          microStr = matcher.group(4);
          if (microStr != null && !microStr.isEmpty()) {
            micro = Integer.parseInt(microStr);

            if (microStr.length() > 6) {
              return null;
            }

            // Correct micros for missing leading zeros
            int powers = 6 - microStr.length();
            while (powers > 0) {
              micro = micro * 10;
              --powers;
            }
          }
          break;

        case MINUTE_TO_SECOND:
          pattern = Pattern.compile("(-?\\d+):(\\d+)(?:\\.(\\d*))?");
          matcher = pattern.matcher(intervalStr);

          if (!matcher.matches() ||
              matcher.groupCount() != 3 ||
              matcher.group(1) == null  ||
              matcher.group(2) == null) {
            return null;
          }

          min  = Integer.parseInt(matcher.group(1));
          sec  = Integer.parseInt(matcher.group(2));

          microStr = matcher.group(3);
          if (microStr != null && !microStr.isEmpty()) {
            micro = Integer.parseInt(microStr);

            if (microStr.length() > 6) {
              return null;
            }

            // Correct micros for missing leading zeros
            int powers = 6 - microStr.length();
            while (powers > 0) {
              micro = micro * 10;
              --powers;
            }
          }
          break;

        default:                  // fall through
        case UNKNOWN:             // fall through
        case UNKNOWN_YEAR_MONTH:  // fall through
        case UNKNOWN_DAY_TIME:
          return null;
      }
    } catch (NumberFormatException e) {
      return null;
    }

    // Handle negative integers in the string
    if (year < 0 || month < 0 || day < 0 || hour < 0 || min < 0 || sec < 0 || micro < 0) {
      isNegative = !isNegative;
      year   = Math.abs(year);
      month  = Math.abs(month);
      day    = Math.abs(day);
      hour   = Math.abs(hour);
      min    = Math.abs(min);
      sec    = Math.abs(sec);
      micro  = Math.abs(micro);
    }

    return new ComparableInterval(eType, isNegative,
        year, month,
        day, hour, min, sec, micro);
  }

  /**
   * Return a comparable interval from a Postgres interval column value that has been
   * retrieved as resultSet.getObject(i).toString().
   * This parses a string that looks like this:
   * "0 years 0 mons 1 days 2 hours 3 mins 4.567 secs"
   * @param intervalStr the string description of a PGInterval type
   * @return a ComparableInterval instance, or null if it could not be parsed.
   */
  public static ComparableInterval parsePostgresObjectToString(String intervalStr)
  {
    assert(intervalStr != null);

    Pattern pattern = Pattern.compile("(?:(-?\\d+) years?)?\\s?" +
                                             "(?:(-?\\d+) mons?)?\\s?" +
                                             "(?:(-?\\d+) days?)?\\s?" +
                                             "(?:(-?\\d+) hours?)?\\s?" +
                                             "(?:(-?\\d+) mins?)?\\s?" +
                                             "(?:(-?\\d+)\\.?(\\d+)? secs?)?");

    Matcher matcher = pattern.matcher(intervalStr);

    if (!matcher.matches()) {
      return null;
    }

    boolean isNegative = false;

    String valueStr;
    int year  = 0;
    int month = 0;
    int day   = 0;
    int hour  = 0;
    int min   = 0;
    int sec   = 0;
    int micro = 0;

    int numGroups = matcher.groupCount();

    try {
      switch (numGroups) {
        case 7:
          valueStr = matcher.group(7);
          if (valueStr != null && !valueStr.isEmpty())    {
            micro = Integer.parseInt(valueStr);

          // Correct micros for missing leading zeros
            int powers = 6 - valueStr.length();
            while (powers > 0) {
              micro = micro * 10;
              --powers;
            }
          }
          // fall through
        case 6:
          valueStr = matcher.group(6);
          if (valueStr != null && !valueStr.isEmpty())    { sec = Integer.parseInt(valueStr);   }
          // fall through
        case 5:
          valueStr = matcher.group(5);
          if (valueStr != null && !valueStr.isEmpty())    { min = Integer.parseInt(valueStr);   }
          // fall through
        case 4:
          valueStr = matcher.group(4);
          if (valueStr != null && !valueStr.isEmpty())    { hour = Integer.parseInt(valueStr);  }
          // fall through
        case 3:
          valueStr = matcher.group(3);
          if (valueStr != null && !valueStr.isEmpty())    { day = Integer.parseInt(valueStr);   }
          // fall through
        case 2:
          valueStr = matcher.group(2);
          if (valueStr != null && !valueStr.isEmpty())    { month = Integer.parseInt(valueStr); }
          // fall through
        case 1:
          valueStr = matcher.group(1);
          if (valueStr != null && !valueStr.isEmpty())    { year = Integer.parseInt(valueStr);  }
          break;

        default:
          // Not able to parse this interval string.
          return null;
      }
    } catch (NumberFormatException e) {
      return null;
    }

    // Handle negative integers in the string
    if (year < 0 || month < 0 || day < 0 || hour < 0 || min < 0 || sec < 0 || micro < 0) {
      isNegative = !isNegative;
      year   = Math.abs(year);
      month  = Math.abs(month);
      day    = Math.abs(day);
      hour   = Math.abs(hour);
      min    = Math.abs(min);
      sec    = Math.abs(sec);
      micro  = Math.abs(micro);
    }

    // Try to determine the type of interval based on the non-zero values.
    // We use the UNKNOWN_* to do a very soft comparison of the interval type.
    IntervalType eType;

    if (year != 0 || month != 0) {
      if (day != 0 || hour != 0 || min != 0 || sec != 0 || micro != 0) {
        eType = IntervalType.UNKNOWN;
      } else {
        eType = IntervalType.UNKNOWN_YEAR_MONTH;
      }

    } else if (day != 0 || hour != 0 || min != 0 || sec != 0 || micro != 0) {
      eType = IntervalType.UNKNOWN_DAY_TIME;

    } else {
      // All values zero
      eType = IntervalType.UNKNOWN;
    }


    return new ComparableInterval(eType, isNegative,
                                  year, month,
                                  day, hour, min, sec, micro);
  }

  /**
   * Return a comparable interval from a Postgres interval column value that has been
   * retrieved as resultSet.getString(i).
   * This parses a string that looks like any of these:
   *   "1 year 2 mons"
   *   "-1 years -2 mons"
   *   "1 mon"
   *   "1 day 02:03:04.567"
   *   "-1 days -02:03:04.567"
   *   "-12:00:00"
   * @param intervalStr the string description of a PGInterval type
   * @return a ComparableInterval instance, or null if it could not be parsed.
   */
  public static ComparableInterval parsePostgresString(String intervalStr)
  {
    assert(intervalStr != null);

    Pattern pattern = Pattern.compile("(?:(-?\\d+) years?)?\\s?" +
                                             "(?:(-?\\d+) mons?)?\\s?" +
                                             "(?:(-?\\d+) days?)?\\s?" +
                                             "(?:(-?\\d+):(\\d+):(\\d+)\\.?(\\d+)?)?");

    Matcher matcher = pattern.matcher(intervalStr);

    if (!matcher.matches()) {
      return null;
    }

    boolean isNegative = false;

    String valueStr;
    int year  = 0;
    int month = 0;
    int day   = 0;
    int hour  = 0;
    int min   = 0;
    int sec   = 0;
    int micro = 0;

    int numGroups = matcher.groupCount();

    try {
      switch (numGroups) {
        case 7:
          valueStr = matcher.group(7);
          if (valueStr != null && !valueStr.isEmpty())    {
            micro = Integer.parseInt(valueStr);

            // Correct micros for missing leading zeros
            int powers = 6 - valueStr.length();
            while (powers > 0) {
              micro = micro * 10;
              --powers;
            }
          }
          // fall through
        case 6:
          valueStr = matcher.group(6);
          if (valueStr != null && !valueStr.isEmpty())    { sec = Integer.parseInt(valueStr);   }
          // fall through
        case 5:
          valueStr = matcher.group(5);
          if (valueStr != null && !valueStr.isEmpty())    { min = Integer.parseInt(valueStr);   }
          // fall through
        case 4:
          valueStr = matcher.group(4);
          if (valueStr != null && !valueStr.isEmpty())    { hour = Integer.parseInt(valueStr);  }
          // fall through
        case 3:
          valueStr = matcher.group(3);
          if (valueStr != null && !valueStr.isEmpty())    { day = Integer.parseInt(valueStr);   }
          // fall through
        case 2:
          valueStr = matcher.group(2);
          if (valueStr != null && !valueStr.isEmpty())    { month = Integer.parseInt(valueStr); }
          // fall through
        case 1:
          valueStr = matcher.group(1);
          if (valueStr != null && !valueStr.isEmpty())    { year = Integer.parseInt(valueStr);  }
          break;

        default:
          // Not able to parse this interval string.
          return null;
      }
    } catch (NumberFormatException e) {
      return null;
    }

    // Handle negative integers in the string
    boolean bGotNegative = (
        year < 0 || month < 0 || day < 0 || hour < 0 || min < 0 || sec < 0 || micro < 0)  ||
        (hour == 0 && matcher.group(4) != null && !matcher.group(4).isEmpty() && matcher.group(4).startsWith("-"));

    if (bGotNegative) {
      isNegative = !isNegative;
      year   = Math.abs(year);
      month  = Math.abs(month);
      day    = Math.abs(day);
      hour   = Math.abs(hour);
      min    = Math.abs(min);
      sec    = Math.abs(sec);
      micro  = Math.abs(micro);
    }

    // Try to determine the type of interval based on the non-zero values.
    // We use the UNKNOWN_* to do a very soft comparison of the interval type.
    IntervalType eType;

    if (year != 0 || month != 0) {
      if (day != 0 || hour != 0 || min != 0 || sec != 0 || micro != 0) {
        eType = IntervalType.UNKNOWN;
      } else {
        eType = IntervalType.UNKNOWN_YEAR_MONTH;
      }

    } else if (day != 0 || hour != 0 || min != 0 || sec != 0 || micro != 0) {
      eType = IntervalType.UNKNOWN_DAY_TIME;

    } else {
      // All values zero
      eType = IntervalType.UNKNOWN;
    }

    return new ComparableInterval(eType, isNegative,
        year, month,
        day, hour, min, sec, micro);
  }

  /**
   * Compare two ComparableInterval instances. This is a nuanced compare.
   * If one of the instances has an UNKNOWN* type then we compare as best as
   * we can. Otherwise we are strict on the interval sub-type comparison.
   * We also compare values by normalising down to months or seconds, depending
   * on the type. So 12 months and 1 year will compare equally.
   * @param other the other ComparableInstance to compare to.
   * @return true if they are logically equal or false if they are not.
   */
  public boolean comparesTo(ComparableInterval other)
  {
    if (other == null) {
      return false;
    }

    // First compare the specific interval types, with nuance.
    if (!compareIntervalTypes(other)) {
      return false;
    }

    // Compare the values of the intervals, again with nuance.
    // The intervals might not be normalised.

    // The easier comparisons are where the two types are equal
    if (this.type == other.type) {

      switch (this.type) {

        case YEAR:
          if (this.years != other.years)      return false;
          break;

        case MONTH:
          if (this.months != other.months)    return false;
          break;

        case YEAR_TO_MONTH:       // fall through
        case UNKNOWN_YEAR_MONTH:
          if (this.normalisedYearsToMonths() != other.normalisedYearsToMonths())    return false;
          break;

        case DAY:
          if (this.days != other.days)        return false;
          break;

        case HOUR:
          if (this.hours != other.hours)      return false;
          break;

        case MINUTE:
          if (this.minutes != other.minutes)  return false;
          break;

        case SECOND:
          if (this.seconds != other.seconds ||
              this.micros  != other.micros)   return false;
          break;

        case DAY_TO_HOUR:
          if (this.normalisedDaysToHours() != other.normalisedDaysToHours())      return false;
          break;

        case DAY_TO_MINUTE:
          if (this.normalisedDaysToMinutes() != other.normalisedDaysToMinutes())  return false;
          break;

        case DAY_TO_SECOND:     // fall through
        case UNKNOWN_DAY_TIME:
          if (this.normalisedDaysToSeconds() != other.normalisedDaysToSeconds() ||
              this.micros  != other.micros)  return false;
          break;

        case HOUR_TO_MINUTE:
          if (this.normalisedHoursToMinutes() != other.normalisedHoursToMinutes())  return false;
          break;

        case HOUR_TO_SECOND:
          if (this.normalisedHoursToSeconds() != other.normalisedHoursToSeconds() ||
              this.micros  != other.micros)  return false;
          break;

        case MINUTE_TO_SECOND:
          if (this.normalisedMinutesToSeconds() != other.normalisedMinutesToSeconds() ||
              this.micros  != other.micros)  return false;
          break;

        case UNKNOWN:
          if (this.normalisedYearsToMonths() != other.normalisedYearsToMonths() ||
              this.normalisedDaysToSeconds() != other.normalisedDaysToSeconds() ||
              this.micros  != other.micros)   return false;
          break;

      }

    } else {
      // The two interval types are compatible but not equal. At least one of them
      // is an UNKNOWN* interval type. So let's normalise all the year-month fields
      // and normalise all of the day-time fields and compare.

      if (this.normalisedYearsToMonths() != other.normalisedYearsToMonths() ||
          this.normalisedDaysToSeconds() != other.normalisedDaysToSeconds() ||
          this.micros  != other.micros)
        return false;
    }

    // Finally check the negative sign if we have a non-zero interval.
    if (this.isNegative != other.isNegative &&
           (this.years   != 0 ||
            this.months  != 0 ||
            this.days    != 0 ||
            this.hours   != 0 ||
            this.minutes != 0 ||
            this.seconds != 0 ||
            this.micros  != 0)
    ) {
      return false;
    }

    // The intervals must be equal.
    return true;
  }

  /**
   * Checks to see if the two intervals have a compatible type.
   * @param other the other interval to compare to.
   * @return true if they are the same, or false if they are unequal
   */
  private boolean compareIntervalTypes(ComparableInterval other)
  {
    // Note: Some systems such as Postgres don't return the exact
    // interval type. We are left to guess what it might be. We might
    // represent that in the type field as UNKNOWN or UNKNOWN_DAY_TIME
    // or UNKNOWN_YEAR_MONTH. This allows us to do soft comparisons with
    // the type. If both types are not of the UNKNOWN variety then we do
    // a hard comparison of them.

    switch (this.type) {
      case UNKNOWN:
        // It doesn't matter what the other type is
        break;

      case UNKNOWN_YEAR_MONTH:
        switch (other.type) {
          case YEAR:
          case MONTH:
          case YEAR_TO_MONTH:
          case UNKNOWN:
          case UNKNOWN_YEAR_MONTH:
            break;

          default:
            return false;
        }
        break;

      case UNKNOWN_DAY_TIME:
        switch (other.type) {
          case DAY:
          case HOUR:
          case MINUTE:
          case SECOND:
          case DAY_TO_HOUR:
          case DAY_TO_MINUTE:
          case DAY_TO_SECOND:
          case HOUR_TO_MINUTE:
          case HOUR_TO_SECOND:
          case MINUTE_TO_SECOND:
          case UNKNOWN:
          case UNKNOWN_DAY_TIME:
            break;

          default:
            return false;
        }
        break;

      case YEAR:          // fall through
      case MONTH:         // fall through
      case YEAR_TO_MONTH:
        if (this.type  != other.type &&
            other.type != IntervalType.UNKNOWN &&
            other.type != IntervalType.UNKNOWN_YEAR_MONTH) {
          return false;
        }
        break;

      case DAY:               // fall through
      case HOUR:              // fall through
      case MINUTE:            // fall through
      case SECOND:            // fall through
      case DAY_TO_HOUR:       // fall through
      case DAY_TO_MINUTE:     // fall through
      case DAY_TO_SECOND:     // fall through
      case HOUR_TO_MINUTE:    // fall through
      case HOUR_TO_SECOND:    // fall through
      case MINUTE_TO_SECOND:
        if (this.type  != other.type &&
            other.type != IntervalType.UNKNOWN &&
            other.type != IntervalType.UNKNOWN_DAY_TIME) {
          return false;
        }
        break;
    }

    return true;
  }

  /**
   * Returns the year and months normalised to months.
   */
  private int normalisedYearsToMonths()
  {
    return this.years * 12 + this.months;
  }

  /**
   * Returns the day, hour normalised to hours.
   */
  private int normalisedDaysToHours()
  {
    return this.days * 24 +
        this.hours;
  }

  /**
   * Returns the day, hour, minute normalised to minutes.
   */
  private int normalisedDaysToMinutes()
  {
    return this.days * 1440 +
        this.hours * 60 +
        this.minutes;
  }

  /**
   * Returns the day, hour, minute and whole seconds normalised to seconds.
   */
  private int normalisedDaysToSeconds()
  {
    return this.days * 86400 +
           this.hours * 3600 +
           this.minutes * 60 +
           this.seconds;
  }

  /**
   * Returns the hour, minute normalised to minutes.
   */
  private int normalisedHoursToMinutes()
  {
    return this.hours * 60 +
           this.minutes;
  }

  /**
   * Returns the hour, minute and whole seconds normalised to seconds.
   */
  private int normalisedHoursToSeconds()
  {
    return this.hours * 3600 +
           this.minutes * 60 +
           this.seconds;
  }

  /**
   * Returns the minutes and whole seconds normalised to seconds.
   */
  private int normalisedMinutesToSeconds()
  {
    return this.minutes * 60 +
           this.seconds;
  }


  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    sb.append("INTERVAL ");

    if (this.isNegative) {
      sb.append('-');
    }

    sb.append('\'');

    sb.append(String.format(this.type.format,
                            this.years,
                            this.months,
                            this.days,
                            this.hours,
                            this.minutes,
                            this.seconds));

    if (this.micros != 0) {

      switch (this.type) {
        case DAY_TO_SECOND:
        case HOUR_TO_SECOND:
        case MINUTE_TO_SECOND:
        case SECOND:
          sb.append(String.format(".%06d", this.micros));
          break;

        default:
          break;
      }
    }

    sb.append("' ");
    sb.append(this.type.desc);

    return sb.toString();
  }

  public static void main(String[] args)
  {
    testComparison();
  }


  /**
   * Tests that two floating point numbers compare in expected ways.
   */
  private static void testComparison()
  {
    Object[][] vals = {
        // Format: ExpectedVal, ActualVal, ExpectedResult

        // (1) Simple date equivalence
        {   new ComparableInterval(IntervalType.YEAR_TO_MONTH,     false, 1, 2),
            new ComparableInterval(IntervalType.YEAR_TO_MONTH,     false, 0, 14),
            true },

        // (2) Simple date equivalence with an UNKNOWN
        {   new ComparableInterval(IntervalType.YEAR_TO_MONTH,     false, 1, 2),
            new ComparableInterval(IntervalType.UNKNOWN,           false, 0, 14),
            true },

        // (3) Simple date equivalence with an UNKNOWN_YEAR_MONTH
        {   new ComparableInterval(IntervalType.YEAR_TO_MONTH,     false, 1, 2),
            new ComparableInterval(IntervalType.UNKNOWN_YEAR_MONTH,false, 0, 14),
            true },

        // (4) False comparison: interval values not equal
        {   new ComparableInterval(IntervalType.YEAR_TO_MONTH,     false, 1, 2),
            new ComparableInterval(IntervalType.UNKNOWN,           false, 0, 15),
            false },

        // (5) False comparison: sign is not equal and values are equivalent
        {   new ComparableInterval(IntervalType.YEAR_TO_MONTH,     false, 1, 2),
            new ComparableInterval(IntervalType.UNKNOWN,           true , 0, 14),
            false },

        // (6) True comparison: sign not equal but values are zero
        {   new ComparableInterval(IntervalType.YEAR_TO_MONTH,     false, 0, 0),
            new ComparableInterval(IntervalType.UNKNOWN,           true , 0, 0),
            true },

        // (7) False comparison: Sign is not equal and values are the same
        {   new ComparableInterval(IntervalType.YEAR_TO_MONTH,     false, 1, 0),
            new ComparableInterval(IntervalType.UNKNOWN,           true , 1, 0),
            false },

        // (8) False comparison: different interval types
        {   new ComparableInterval(IntervalType.YEAR_TO_MONTH,     false, 1, 2),
            new ComparableInterval(IntervalType.MONTH,             false, 0, 14),
            false },

        // (9) False comparison: invalid interval type involving an UNKNOWN*
        {   new ComparableInterval(IntervalType.HOUR_TO_MINUTE,    false, 0, 2, 14, 0,    0),
            new ComparableInterval(IntervalType.UNKNOWN_YEAR_MONTH,false, 0, 0,  0, 8040, 0),
            false },

        // (10) False comparison: Cannot compare unknown day-time and unknown year-month
        {   new ComparableInterval(IntervalType.UNKNOWN_YEAR_MONTH,false, 1, 0),
            new ComparableInterval(IntervalType.UNKNOWN_DAY_TIME,  false, 365, 0, 0, 0, 0),
            false },

        // (11) False comparison: different interval values within the type range
        {   new ComparableInterval(IntervalType.MONTH,             false, 1, 2),
            new ComparableInterval(IntervalType.MONTH,             false, 0, 14),
            false },

        // (12) True comparison: time values are equivalent, with an UNKNOWN
        {   new ComparableInterval(IntervalType.HOUR_TO_MINUTE,    false, 0, 2, 14, 0,    0),
            new ComparableInterval(IntervalType.UNKNOWN_DAY_TIME,  false, 0, 0,  0, 8040, 0),
            true },
    };

    int numPassed = 0;
    int numFailed = 0;

    int i = 0;
    for (Object[] val : vals) {
      ++i;
      ComparableInterval expected = (ComparableInterval) val[0];
      ComparableInterval actual   = (ComparableInterval) val[1];

      boolean expectedResult = (boolean) val[2];
      boolean actualResult   = expected.comparesTo(actual);

      String fmt = "(%02d) %s: expected = %-36s ; actual = %-36s ; expected result = %5b ; actual result = %5b";
      String resultStr = "PASS";

      if (expectedResult != actualResult) {
        resultStr = "FAIL";
        ++numFailed;
      } else {
        ++numPassed;
      }

      String msg = String.format(fmt, i, resultStr, expected, actual, expectedResult, actualResult);
      System.out.println(msg);
    }

    String passSummary;
    if (numFailed > 0) {
      passSummary = String.format("Test summary: %02d PASSED and %02d FAILED", numPassed, numFailed);
    } else {
      passSummary = String.format("Test summary: All %02d tests PASSED", numPassed);
    }

    System.out.println();
    System.out.println(passSummary);
    System.out.println();
  }
}
