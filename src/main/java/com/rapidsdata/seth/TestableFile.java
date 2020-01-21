// Copyright (c) 2020 Boray Data Co. Ltd.  All rights reserved.

package com.rapidsdata.seth;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * A container for a test file to be executed, together with the instruction (test or skip)
 * of this file.
 */
public class TestableFile
{
  public enum Instruction
  {
    EXECUTE,
    SKIP
  }

  private final File file;
  private Instruction instruction;

  public TestableFile(File file, Instruction instruction)
  {
    this.file = file;
    this.instruction = instruction;
  }

  public File getFile()
  {
    return file;
  }

  public Instruction getInstruction()
  {
    return instruction;
  }

  public void setInstruction(Instruction instruction)
  {
    this.instruction = instruction;
  }

  /**
   * Create a list of TestFiles from a list of Files, all using the same given instruction.
   * @param files
   * @param instruction
   * @return a list of TestFiles from a list of Files, all using the same given instruction.
   */
  public static List<TestableFile> listOf(List<File> files, Instruction instruction)
  {
    List<TestableFile> testableFiles = new ArrayList<>(files.size());

    for (File file : files) {
      testableFiles.add(new TestableFile(file, instruction));
    }

    return testableFiles;
  }
}
