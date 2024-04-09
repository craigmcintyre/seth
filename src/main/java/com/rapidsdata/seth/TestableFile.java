// Copyright (c) 2020 Boray Data Co. Ltd.  All rights reserved.

package com.rapidsdata.seth;

import com.rapidsdata.seth.exceptions.SethSystemException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
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
    READ,     // read a file from disk and then execute
    EXECUTE,  // execute a script we've already read in
    SKIP,     // do not execute the specified file
  }

  public static final String SCRIPT_TEST_NAME = "<command line script>";

  private final File file;
  private final String script;
  private Instruction instruction;

  public TestableFile(File file, Instruction instruction)
  {
    this.file = file;
    this.script = null;
    this.instruction = instruction;
  }

  public TestableFile(String script)
  {
    String cwd = Paths.get(".").toAbsolutePath().normalize().toString();
    this.file = new File(cwd);  // current working directory
    this.script = script;
    this.instruction = Instruction.EXECUTE;
  }

  public File getFile()
  {
    return file;
  }

  public String getScript()
  {
    return script;
  }

  public Instruction getInstruction()
  {
    return instruction;
  }

  public void setInstruction(Instruction instruction)
  {
    this.instruction = instruction;
  }

  public String describePath()
  {
    switch (instruction) {
      case EXECUTE:
        return SCRIPT_TEST_NAME;

      default:
        return file.getPath();
    }
  }

  public String getDirectory()
  {
    switch (instruction) {
      case EXECUTE:
        // A script doesn't have a directory, so it was set to the current working directory.
        return file.getPath();

      default:
        // If we are executing a file then we use getParent() to get the enclosing directory.
        return file.getParent();
    }
  }

  public boolean exists()
  {
    switch (instruction) {
      case EXECUTE:
        return true;

      default:
        return file.exists();
    }
  }

  public String contents()
  {
    switch (instruction) {
      case EXECUTE:
        return script;

      default:
        byte[] bytes;

        try {
          bytes = Files.readAllBytes(file.toPath());
        } catch (IOException | OutOfMemoryError | SecurityException e) {
          throw new SethSystemException(e);
        }

        return new String(bytes);
    }
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
