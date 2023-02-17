// Copyright (c) 2017 Boray Data Co. Ltd.  All rights reserved.

package com.rapidsdata.seth.plan.expectedResults;

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
    YEAR ("YEAR", "%1$d"),
    MONTH ("MONTH", "%2$d"),
    YEAR_TO_MONTH ("YEAR TO MONTH", "%1$d-%2$d"),
    DAY ("DAY", "%3$d"),
    HOUR ("HOUR", "%4$d"),
    MINUTE ("MINUTE", "%5$d"),
    SECOND ("SECOND", "%6$d.%7$06d"),
    DAY_TO_HOUR ("DAY TO HOUR", "%3$d %4$d"),
    DAY_TO_MINUTE ("DAY TO MINUTE", "%3$d %4$d:%5$d"),
    DAY_TO_SECOND ("DAY TO SECOND", "%3$d %4$d:%5$d:%6$d.%7$06d"),
    HOUR_TO_MINUTE ("HOUR TO MINUTE", "%4$d:%5$d"),
    HOUR_TO_SECOND ("HOUR TO SECOND", "%4$d:%5$d:%6$d.%7$06d"),
    MINUTE_TO_SECOND ("MINUTE TO SECOND", "%5$d:%6$d.%7$06d"),
    UNKNOWN ("", "%1$d-%2$d-%3$d %4$d:%5$d:%6$d.%7$06d"),
    UNKNOWN_YEAR_MONTH ("YEAR TO MONTH", "%1$d-%2$d"),
    UNKNOWN_DAY_TIME ("DAY TO SECOND", "%3$d %4$d:%5$d:%6$d.%7$06d");

    public final String desc;
    public final String format;

    private IntervalType(String desc, String format)
    {
      this.desc = desc;
      this.format = format;
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
    assert (micros > 0);
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

    assert (micros > 0);
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
   * Return a comparable interval from a RapidsSE interval column value that has been
   * retrieved as resultSet.getObject(i).toString() or
   * resultSet.toString(i)
   * This parses a string that looks like these examples:
   *  "INTERVAL '0-1' YEAR TO MONTH"
   *  "INTERVAL -'2' MONTH"
   *  "INTERVAL '-1 2:3:4.5' DAY TO SECOND"
   *  That is, a normal SQL interval.
   * @param intervalStr the string description of a PGInterval type
   * @return a ComparableInterval instance, or null if it could not be parsed.
   */
  public static ComparableInterval parseRapidsSEIntervalString(String intervalStr, boolean isNegative)
  {
    assert(intervalStr != null);

    Pattern pattern = Pattern.compile("(?:(-?\\d+) years?)?\\s?" +
        "(?:(-?\\d+) mons?)?\\s?" +
        "(?:(-?\\d+) days?)?\\s?" +
        "(?:(-?\\d+) hours?)?\\s?" +
        "(?:(-?\\d+) mins?)?\\s?" +
        "(?:(-?\\d+)\\.?(\\d+)? secs?)?");

    Matcher matcher = pattern.matcher(intervalStr);

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
   * retrieved as resultSet.getObject(i).toString().
   * This parses a string that looks like this:
   * "0 years 0 mons 1 days 2 hours 3 mins 4.567 secs"
   * @param intervalStr the string description of a PGInterval type
   * @return a ComparableInterval instance, or null if it could not be parsed.
   */
  public static ComparableInterval parsePostgresObjectToString(String intervalStr, boolean isNegative)
  {
    assert(intervalStr != null);

    Pattern pattern = Pattern.compile("(?:(-?\\d+) years?)?\\s?" +
                                             "(?:(-?\\d+) mons?)?\\s?" +
                                             "(?:(-?\\d+) days?)?\\s?" +
                                             "(?:(-?\\d+) hours?)?\\s?" +
                                             "(?:(-?\\d+) mins?)?\\s?" +
                                             "(?:(-?\\d+)\\.?(\\d+)? secs?)?");

    Matcher matcher = pattern.matcher(intervalStr);

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
  public static ComparableInterval parsePostgresString(String intervalStr, boolean isNegative)
  {
    assert(intervalStr != null);

    Pattern pattern = Pattern.compile("(?:(-?\\d+) years?)?\\s?" +
                                             "(?:(-?\\d+) mons?)?\\s?" +
                                             "(?:(-?\\d+) days?)?\\s?" +
                                             "(?:(-?\\d+):(\\d+):(\\d+)\\.?(\\d+)?)?");

    Matcher matcher = pattern.matcher(intervalStr);

    // TODO: Extract negative before time

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
          if (this.seconds != other.seconds &&
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
          if (this.normalisedDaysToSeconds() != other.normalisedDaysToSeconds() &&
              this.micros  != other.micros)  return false;
          break;

        case HOUR_TO_MINUTE:
          if (this.normalisedHoursToMinutes() != other.normalisedHoursToMinutes())  return false;
          break;

        case HOUR_TO_SECOND:
          if (this.normalisedHoursToSeconds() != other.normalisedHoursToSeconds() &&
              this.micros  != other.micros)  return false;
          break;

        case MINUTE_TO_SECOND:
          if (this.normalisedMinutesToSeconds() != other.normalisedMinutesToSeconds() &&
              this.micros  != other.micros)  return false;
          break;

        case UNKNOWN:
          if (this.normalisedYearsToMonths() != other.normalisedYearsToMonths() &&
              this.normalisedDaysToSeconds() != other.normalisedDaysToSeconds() &&
              this.micros  != other.micros)   return false;
          break;

      }

    } else {
      // The two interval types are compatible but not equal. At least one of them
      // is an UNKNOWN* interval type. So let's normalise all the year-month fields
      // and normalise all of the day-time fields and compare.

      if (this.normalisedYearsToMonths() != other.normalisedYearsToMonths() &&
          this.normalisedDaysToSeconds() != other.normalisedDaysToSeconds() &&
          this.micros  != other.micros)   return false;
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
                            this.seconds,
                            this.micros));

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
        //ExpectedVal ActualVal   ExpectedResult
        {  "1"         ,  "1"          , true  },
        {  "1"         ,  "1."         , true  },
        {  "1"         ,  "1.0"        , true  },
        {  "1"         ,  "1e0"        , true  },
        {  "1"         ,  "11e-1"      , true  },
        { "-1"         , "-1"          , true  },
        { "-1"         , "-1."         , true  },
        { "-1"         , "-1.0"        , true  },
        { "-1"         , "-1e0"        , true  },
        { "-1"         , "-11e-1"      , true  },

        {  "10"        ,  "10"         , true  },
        {  "10"        ,  "10."        , true  },
        {  "10"        ,  "10.0"       , true  },
        {  "10"        ,  "10e0"       , true  },
        {  "10"        ,  "101e-1"     , true  },
        { "-10"        , "-10"         , true  },
        { "-10"        , "-10."        , true  },
        { "-10"        , "-10.0"       , true  },
        { "-10"        , "-10e0"       , true  },
        { "-10"        , "-101e-1"     , true  },

        {  "10000"     ,  "10000"      , true  },
        {  "10000"     ,  "10000."     , true  },
        {  "10000"     ,  "10000.0"    , true  },
        {  "10000"     ,  "10000e0"    , true  },
        {  "10000"     ,  "100001e-1"  , true  },
        { "-10000"     , "-10000"      , true  },
        { "-10000"     , "-10000."     , true  },
        { "-10000"     , "-10000.0"    , true  },
        { "-10000"     , "-10000e0"    , true  },
        { "-10000"     , "-100001e-1"  , true  },
        { "-10000"     , "-10000001e-3", true  },

        {  "10.00"     ,  "10"         , true  },
        {  "10.00"     ,  "10."        , true  },
        {  "10.00"     ,  "10.0"       , true  },
        {  "10.00"     ,  "10e0"       , true  },
        {  "10.00"     ,  "10001e-3"   , true  },
        { "-10.00"     , "-10"         , true  },
        { "-10.00"     , "-10."        , true  },
        { "-10.00"     , "-10.0"       , true  },
        { "-10.00"     , "-10e0"       , true  },
        { "-10.00"     , "-10001e-3"   , true  },

        {  "10000.1"   ,  "10000.1"    , true  },
        {  "10000.1"   ,  "10000.10"   , true  },
        {  "10000.1"   ,  "10000.1e0"  , true  },
        {  "10000.1"   ,  "1000019e-2" , true  },
        { "-10000.1"   , "-10000.1"    , true  },
        { "-10000.1"   , "-10000.10"   , true  },
        { "-10000.1"   , "-10000.1e0"  , true  },
        { "-10000.1"   , "-1000019e-2" , true  },
        { "-10000.1"   , "-10000101e-3", true  },

        {  "1e3"       ,  "1000"       , true  },
        {  "1e3"       ,  "1000."      , true  },
        {  "1e3"       ,  "1000.0"     , true  },
        {  "1e3"       ,  "10e2"       , true  },
        {  "1e3"       ,  "11e2"       , true  },
        { "-1e3"       , "-1000"       , true  },
        { "-1e3"       , "-1000."      , true  },
        { "-1e3"       , "-1000.0"     , true  },
        { "-1e3"       , "-10e2"       , true  },
        { "-1e3"       , "-11e2"       , true  },

        {  "1.e3"      ,  "1000"       , true  },
        {  "1.e3"      ,  "1000."      , true  },
        {  "1.e3"      ,  "1000.0"     , true  },
        {  "1.e3"      ,  "10e2"       , true  },
        {  "1.e3"      ,  "11e2"       , true  },
        { "-1.e3"      , "-1000"       , true  },
        { "-1.e3"      , "-1000."      , true  },
        { "-1.e3"      , "-1000.0"     , true  },
        { "-1.e3"      , "-10e2"       , true  },
        { "-1.e3"      , "-11e2"       , true  },

        {  "1.0e3"     ,  "1000"       , true  },
        {  "1.0e3"     ,  "1000."      , true  },
        {  "1.0e3"     ,  "1000.0"     , true  },
        {  "1.0e3"     ,  "10e2"       , true  },
        {  "1.0e3"     ,  "101e1"      , true  },
        { "-1.0e3"     , "-1000"       , true  },
        { "-1.0e3"     , "-1000."      , true  },
        { "-1.0e3"     , "-1000.0"     , true  },
        { "-1.0e3"     , "-10e2"       , true  },
        { "-1.0e3"     , "-101e1"      , true  },

        {  "10e2"      ,  "1000"       , true  },
        {  "10e2"      ,  "1000."      , true  },
        {  "10e2"      ,  "1000.0"     , true  },
        {  "10e2"      ,  "10e2"       , true  },
        {  "10e2"      ,  "101e1"      , true  },
        { "-10e2"      , "-1000"       , true  },
        { "-10e2"      , "-1000."      , true  },
        { "-10e2"      , "-1000.0"     , true  },
        { "-10e2"      , "-10e2"       , true  },
        { "-10e2"      , "-101e1"      , true  },

        {  "0.1"       ,  "0.1"        , true  },
        {  "0.1"       ,  "0.10"       , true  },
        {  "0.1"       ,  "0.11"       , true  },
        {  "0.1"       ,  "1e-1"       , true  },
        {  "0.1"       ,  "11e-2"      , true  },
        { "-0.1"       , "-0.1"        , true  },
        { "-0.1"       , "-0.10"       , true  },
        { "-0.1"       , "-0.11"       , true  },
        { "-0.1"       , "-1e-1"       , true  },
        { "-0.1"       , "-11e-2"      , true  },

        {  "0.01"      ,  "0.01"       , true  },
        {  "0.01"      ,  "0.010"      , true  },
        {  "0.01"      ,  "0.011"      , true  },
        {  "0.01"      ,  "1e-2"       , true  },
        {  "0.01"      ,  "11e-3"      , true  },
        { "-0.01"      , "-0.01"       , true  },
        { "-0.01"      , "-0.010"      , true  },
        { "-0.01"      , "-0.011"      , true  },
        { "-0.01"      , "-1e-2"       , true  },
        { "-0.01"      , "-11e-3"      , true  },

        {  "1.e-2"     ,  "0.01"       , true  },
        {  "1.e-2"     ,  "0.010"      , true  },
        {  "1.e-2"     ,  "0.011"      , true  },
        {  "1.e-2"     ,  "1e-2"       , true  },
        {  "1.e-2"     ,  "11e-3"      , true  },
        { "-1.e-2"     , "-0.01"       , true  },
        { "-1.e-2"     , "-0.010"      , true  },
        { "-1.e-2"     , "-0.011"      , true  },
        { "-1.e-2"     , "-1e-2"       , true  },
        { "-1.e-2"     , "-11e-3"      , true  },

        {  "1.0e-2"    ,  "0.01"       , true  },
        {  "1.0e-2"    ,  "0.010"      , true  },
        {  "1.0e-2"    ,  "0.0101"     , true  },
        {  "1.0e-2"    ,  "1e-2"       , true  },
        {  "1.0e-2"    ,  "101e-4"     , true  },
        { "-1.0e-2"    , "-0.01"       , true  },
        { "-1.0e-2"    , "-0.010"      , true  },
        { "-1.0e-2"    , "-0.0101"     , true  },
        { "-1.0e-2"    , "-1e-2"       , true  },
        { "-1.0e-2"    , "-101e-4"     , true  },


        { "3.14e0"     , "3.14159"     , true  },
        { "3.14e0"     , "3.1"         , false },
        { "3.14e0"     , "3.149"       , true  },
        { "3.14e0"     , "314e-2"      , true  },
        { "100"        , "10e1"        , true  },
        { "100.0"      , "10e1"        , true  },
        { "100."       , "1001e-1"     , true  },
        { "100"        , "100e0"       , true  },
        { "100"        , "1001e-1"     , true  },
        { "100.0"      , "1001e-1"     , false },
        { "123"        , "1.23e2"      , true  },
        { "123"        , "1.2e2"       , false },

        { "6.022e23"   , "602200000000000000000000e0", true },
        { "6.022e23"   , "602210000000000000000000e0", true },
    };

    for (int i = 0; i < vals.length; i++) {
      String expected = (String) vals[i][0];
      String actual   = (String) vals[i][1];
      boolean expectedResult = (boolean) vals[i][2];

      ComparableInterval cf = new ComparableInterval(expected);
      boolean actualResult = cf.comparesTo(actual);

      String fmt = "%s: expected = %-12s actual = %-12s precision = %d,  expected result = %5b,  actual result = %5b";
      String resultStr = "PASS";

      if (expectedResult != actualResult) {
        resultStr = "FAIL";
      }

      String msg = String.format(fmt, resultStr, expected, actual, cf.properPrecision, expectedResult, actualResult);
      System.out.println(msg);
    }
  }
}
