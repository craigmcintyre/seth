// Copyright (c) 2017 Boray Data Co. Ltd.  All rights reserved.

package com.rapidsdata.seth.plan.expectedResults;

import com.rapidsdata.seth.exceptions.SethSystemException;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * A class used to represent an expected floating point value. It contains
 * the logic for comparing itself to a floating point number. The values are
 * compared to the level of precision specified in this expected value.
 *
 * e.g.:
 *   ExpectedVal    ActualVal   ValuesCompare?
 *   0.123          0.12        false, not enough precision
 *   0.123          0.123       true
 *   0.123          0.1234      true
 *   0.123          1.234e-1    true
 *   0.123          0.123e0     true
 *   123e2          12300       true
 *   123e2          12399       true, expectedVal only has 3 significant digits
 */
public class ComparableFloat
{
  private final String originalValue;
  private final int properPrecision;
  private final MathContext mathContext;
  private final BigDecimal expectedBigDecimal;

  public ComparableFloat(String value)
  {
    this.originalValue = value;
    this.properPrecision = getNumSignificantDigits(value);
    this.mathContext = new MathContext(properPrecision, RoundingMode.DOWN);
    this.expectedBigDecimal = new BigDecimal(value, mathContext);
  }

  public boolean comparesTo(String actualValue)
  {
    BigDecimal actualBigDecimal;

    try {
      actualBigDecimal = new BigDecimal(actualValue, mathContext);

    } catch (NumberFormatException e) {
      return false;
    }

    return comparesTo(actualBigDecimal);
  }

  public boolean comparesTo(double actualValue)
  {
    BigDecimal actualBigDecimal;

    try {
      actualBigDecimal = new BigDecimal(actualValue, mathContext);

    } catch (NumberFormatException e) {
      return false;
    }

    return comparesTo(actualBigDecimal);
  }

  public boolean comparesTo(BigDecimal actualBigDecimal)
  {
    actualBigDecimal = actualBigDecimal.setScale(expectedBigDecimal.scale(), BigDecimal.ROUND_DOWN);
    boolean result = expectedBigDecimal.equals(actualBigDecimal);
    return result;
  }


  private int getNumSignificantDigits(String value)
  {
    BigDecimal input = new BigDecimal(value);

    int sigDigits;

    value = value.trim();

    // We try to take advantage of the BigDecimal class, however it doesnt seem to report
    // the right precision when the following conditions occur:
    //   - the value does not contain an exponent,
    //   - the value does a decimal place, and
    //   - the value has digits after the decimal place.
    // e.g. "100.00" should have a precision of 5, however BigDecimal will only report 3.
    // For that situation, we will work out the precision manually.

    if (!value.contains("e") && !value.contains("E")) {

      int decPointIndex = value.indexOf('.');
      if (decPointIndex != -1 && value.length() > (decPointIndex + 1)) {
        if (value.startsWith("-") || value.startsWith("+")) {
          sigDigits = value.length() - 1 /*sign*/ - 1 /*decimal point*/;
        } else {
          sigDigits = value.length() - 1 /*decimal point*/;
        }

        return sigDigits;
      }
    }

    // Using BigDecimal to help us with precision.
    if (input.scale() < 0) {
      sigDigits = input.precision();

    } else if (input.scale() == 0) {
      sigDigits = input.precision();

    } else {
      sigDigits = input.scale() + 1; // +1 because the leading 0 is still part of the precision.
    }

    return sigDigits;
  }

  public static void main(String[] args)
  {
    //testPrecision();
    //System.out.println();
    //System.out.println();
    testComparison();
  }

  private static void testPrecision()
  {
    Object[][] vals = {
        {"1",       1 },
        {"10",      2 },
        {"100",     3 },
        {"100.",    3 },
        {"10000.",  5 },
        {"1000000.",7 },
        {"1000000", 7 },
        {"10000.0", 6 },
        {"10000.1", 6 },
        {"100.0",   4 },
        {"100.00",  5 },
        {"1e3",     1 },
        {"1.e3",    1 },
        {"1.0e3",   2 },
        {"1.00e3",  3 },
        {"1.1e3",   2 },
        {"10e2",    2 },
        {"0",       1 },
        {"0.1",     2 },
        {"0.01",    3 },
        {"0.010",   4 },
        {"0.0100",  5 },
        {"1e-1",    2 },
        {"1e-2",    3 },
        {"10e-2",   3 },
        {"1.0e-2",  4 },
    };

    for (int i=0; i<vals.length; i++) {
      String value = (String) vals[i][0];
      int expectedSigDigs = (int) vals[i][1];

      ComparableFloat cf = new ComparableFloat(value);
      int actualSigDigs = cf.properPrecision;

      final String msg;

      String fmt = "%s: %8s : expected = %d, actual = %d, scale = %2d, properPrecision = %2d, plain = %8s, toString = %8s";

      if (expectedSigDigs == actualSigDigs) {
        msg = String.format(fmt, "PASS", value, expectedSigDigs, actualSigDigs, cf.expectedBigDecimal.scale(),
            cf.expectedBigDecimal.precision(), cf.expectedBigDecimal.toPlainString(), cf.expectedBigDecimal.toString());
      } else {
        msg = String.format(fmt, "FAIL", value, expectedSigDigs, actualSigDigs, cf.expectedBigDecimal.scale(),
            cf.expectedBigDecimal.precision(), cf.expectedBigDecimal.toPlainString(), cf.expectedBigDecimal.toString());
      }

      System.out.println(msg);
    }
  }

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

      ComparableFloat cf = new ComparableFloat(expected);
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
