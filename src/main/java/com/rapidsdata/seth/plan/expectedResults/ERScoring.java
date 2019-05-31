// Copyright (c) 2019 Boray Data Co. Ltd.  All rights reserved.

package com.rapidsdata.seth.plan.expectedResults;

import org.apache.commons.text.similarity.LevenshteinDistance;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class ERScoring
{
  private static final long C_1B = 1000000000;
  private static final long NUMERIC_SCORE_SCALING = 10;

  private static final long NULL_SCORE = 4;   // An arbitrary number.

  private ERScoring() { }

  public static long compareNullWith(Object val)          { return NULL_SCORE; }

  public static long compareWithNull(boolean val)         { return NULL_SCORE; }
  public static long compareWithNull(long val)            { return NULL_SCORE; }
  public static long compareWithNull(BigDecimal val)      { return NULL_SCORE; }
  public static long compareWithNull(double val)          { return NULL_SCORE; }
  public static long compareWithNull(String val)          { return NULL_SCORE; }
  public static long compareWithNull(LocalDate val)       { return NULL_SCORE; }
  public static long compareWithNull(LocalTime val)       { return NULL_SCORE; }
  public static long compareWithNull(LocalDateTime val)   { return NULL_SCORE; }


  public static long compare (boolean val1, boolean val2)
  {
    return (val1 == val2 ? 0 : 1);
  }

  public static double compare (long val1, long val2)
  {
    // Compare the two values relative to the first value.
    // Be careful if the first value is zero.
    if (val1 == 0) {
      val1 += 1;
      val2 += 1;
    }

    // Add some scaling into this. If the two values are 10% different then we should have a score of 1
    // (equivalent to a string having one bad character).

    return Math.abs(val1 - val2) * NUMERIC_SCORE_SCALING / (double) val1;
  }

  public static double compare (BigDecimal val1, BigDecimal val2)
  {
    // The score is simply the difference between the two values relative to the first value.
    // Be careful of dividing by zero.
    if (val1.equals(BigDecimal.ZERO)) {
      val1 = BigDecimal.ONE;
      val2 = val2.add(BigDecimal.ONE);
    }

    double score = Math.abs(val1.subtract(val2).doubleValue()) * NUMERIC_SCORE_SCALING / val1.doubleValue();
    return score;
  }

  public static double compare (double val1, double val2)
  {
    // The score is simply the difference between the two values relative to the first value.
    // Be careful of dividing by zero.
    if (val1 == 0f) {
      val1 += 1.0;
      val2 += 1.0;
    }

    double score = Math.abs(val1 - val2) * NUMERIC_SCORE_SCALING / val1;
    return score;
  }

  public static long compare (String val1, String val2)
  {
    // Levenshtein for the win!
    LevenshteinDistance lev = LevenshteinDistance.getDefaultInstance();
    long score = lev.apply(val1, val2);
    return score;
  }

  public static long compare (LocalDate val1, LocalDate val2)
  {
    // The score is simply the difference in days between the two dates.
    return Math.abs(val1.toEpochDay() - val2.toEpochDay());
  }

  public static double compare (LocalTime val1, LocalTime val2)
  {
    // Seconds are our base unit, with fractional seconds too.
    double d1 = (double) val1.toNanoOfDay() / (double)C_1B;
    double d2 = (double) val2.toNanoOfDay() / (double)C_1B;
    return compare(d1, d2);
  }

  public static double compare (LocalDateTime val1, LocalDateTime val2)
  {
    // Take the geometric mean of the date and time components individually.
    double dateScore = compare(val1.toLocalDate(), val2.toLocalDate()) + 1;
    double timeScore = compare(val1.toLocalTime(), val2.toLocalTime()) + 1;

    double score = Math.abs(geomeanOf(dateScore, timeScore));
    return score;
  }


  public static double geomeanOf(double... args)
  {
    double acc = 1f;

    for (double val : args) {
      acc *= (val + 1f);
    }

    // Calculate the final score by taking the nth root of the cumulative score, where n is
    // the number of columns in the actual result.
    // Taking the nth root is the same as raising to the power of 1/n, however this can be
    // inaccurate. A better way is to use the equivalence
    //
    //   x^(1/n) == e^(ln(x)/n)
    //
    // We subtract one from the end because we added one to each column score in order to avoid
    // a multiplication by zero.
    double finalScore = Math.pow(Math.E, Math.log(acc)/args.length) - 1;
    return finalScore;
  }
}
