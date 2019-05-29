// Copyright (c) 2019 Boray Data Co. Ltd.  All rights reserved.

package com.rapidsdata.seth.plan.annotated;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestAnnotationInfo
{
  /** The path of the original test file that we will populate with expected results. */
  public final Path originalTestFile;

  /** The path of the output test file that will contain the new expected results. */
  public final Path outputTestFile;

  /**
   * A list of integer-pairs that are the start and end positions in the original test file of
   * the existing expected results to be removed.
   */
  private List<IntPair> existingResultsToRemove;

  /** The list of expected results to be put into the annotated test file. */
  public final Map<Integer, String> newResultsMap;


  public TestAnnotationInfo(Path originalTestFile, Path outputTestFile)
  {
    this.originalTestFile = originalTestFile;
    this.outputTestFile   = outputTestFile;
    this.existingResultsToRemove = new ArrayList<>();
    this.newResultsMap = new HashMap<>();

    assert(Files.exists(outputTestFile));
  }

  /**
   * Adds the start and end indexes of an existing expected result when parsing the originalTestFile,
   * that way the expected results can be stripped out when writing the new ones.
   * @param startIdx the index in the file of the first character belonging to the expected result.
   * @param stopIdx the index in the file just after the last character of the expected result.
   */
  public void identifyExistingResult(int startIdx, int stopIdx)
  {
    assert (startIdx >= 0);
    assert (stopIdx >= 0);

    existingResultsToRemove.add(new IntPair(startIdx, stopIdx));
  }

  /**
   * Adds a new expected result to be written to the new test file being annotated.
   * @param position the index of the first character in the old test file where this existing
   *                 result should appear.
   * @param expectedResult the contents of the expected result.
   */
  public void addNewExpectedResult(int position, String expectedResult)
  {
    assert (position >= 0);
    assert (expectedResult != null);

    newResultsMap.put(position, expectedResult);
  }

  /**
   * Reads the test file and replaces all expected result markers with their corresponding recorded results.
   * Writes the test file back out to the same path.
   * @throws IOException
   */
  public void annotate() throws IOException
  {
    try (
        BufferedReader reader = new BufferedReader(new FileReader(originalTestFile.toFile()));
        BufferedWriter writer = new BufferedWriter(new FileWriter(outputTestFile.toFile()));
    ) {

      final int bufSize = 1024;
      char[] buf = new char[bufSize];

      int currentIndex = 0;

      for (IntPair erIndex : existingResultsToRemove) {

        while (currentIndex != erIndex._1) {
          // Read up until the next expected result to remove.
          int charsToRead = erIndex._1 - currentIndex;
          assert (charsToRead >= 0);

          if (charsToRead > bufSize) {
            // Only read up to the buffer size
            charsToRead = bufSize;
          }

          int charsRead = reader.read(buf, 0, charsToRead);

          if (charsRead == -1) {
            // EOF
            return;
          }

          // Copy the data that was read into the new file.
          writer.write(buf, 0, charsRead);
          currentIndex += charsRead;
        }

        // We are now at the same point as an expected result in the original test file.
        // Skip over the content we don't want in the original test file.
        int skipCount = erIndex._2 - erIndex._1;

        if (skipCount == 0) {
          // write a newline for the expected result
          writer.write('\n');
        }

        reader.skip(skipCount);
        currentIndex += (skipCount);

        // Write out the new expected result instead.
        String newExpectedResult = newResultsMap.get(erIndex._1);
        assert (newExpectedResult != null);
        writer.write(newExpectedResult);
      }

      // now copy the rest of the file
      while (true) {
        int charsRead = reader.read(buf, 0, bufSize);

        if (charsRead == -1) {
          // EOF
          return;
        }

        writer.write(buf, 0, charsRead);
      }
    }
  }

  public Path getOriginalTestFile()
  {
    return originalTestFile;
  }

  public Path getOutputTestFile()
  {
    return outputTestFile;
  }


  /** A simply 2-tuple of ints */
  private class IntPair
  {
    public final int _1;
    public final int _2;

    public IntPair(int _1, int _2)
    {
      assert(_1 >= 0);
      assert(_2 >= 0);

      this._1 = _1;
      this._2 = _2;
    }
  }
}
