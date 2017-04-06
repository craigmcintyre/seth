// Copyright (c) 2017 Boray Data Co. Ltd.  All rights reserved.

package com.rapidsdata.seth.plan;

import com.rapidsdata.seth.exceptions.SethBrownBagException;
import com.rapidsdata.seth.exceptions.SethSystemException;
import com.rapidsdata.seth.exceptions.SyntaxException;
import com.rapidsdata.seth.parser.SethBaseVisitor;
import com.rapidsdata.seth.parser.SethLexer;
import com.rapidsdata.seth.parser.SethParser;
import com.rapidsdata.seth.plan.Plan;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTree;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;

/** The class responsible for creating execution plans. */
public class TestPlanner
{
  /**
   * Constructor.
   */
  public TestPlanner()
  {
    
  }
  
  public Plan newPlanFor(File testFile) throws FileNotFoundException, SyntaxException
  {
    if (!testFile.exists()) {
      throw new FileNotFoundException("File not found: " + testFile.getPath());
    }

    byte[] bytes;

    try {
      bytes = Files.readAllBytes(testFile.toPath());
    } catch (IOException | OutOfMemoryError | SecurityException e) {
      throw new SethSystemException(e);
    }

    String contents = new String(bytes);

    SethLexer lexer = new SethLexer(new ANTLRInputStream(contents));
    SethParser parser = new SethParser(new CommonTokenStream(lexer));
    parser.setErrorHandler(new ErrorHandler(testFile));

    // Parse the contents of the file.
    ParseTree tree;

    try {
      tree = parser.testFile();

    } catch (SethBrownBagException e) {

      if (e.getCause() instanceof SyntaxException) {
        throw (SyntaxException) e.getCause();

      } else {
        throw new SethSystemException("Unhandled exception " + e.getClass().getSimpleName(), e.getCause());
      }
    }

    // Now that we've parsed the statement into a ParseTree we now need to build
    // the list of Operations. We use the visitor pattern for walking the ParseTree.
    TestGenerator generator = new TestGenerator(parser, testFile);
    Plan plan = generator.generateFor(tree);

    return plan;
  }


  /**
   * Parser error handler.
   */
  protected static class ErrorHandler extends DefaultErrorStrategy
  {
    /** The file being tested. */
    private final File testFile;

    /** Constructor */
    public ErrorHandler(File testFile)
    {
      this.testFile = testFile;
    }

    @Override
    public void recover(Parser recognizer, RecognitionException e)
    {
      throw new SethSystemException("Parser internal error", e);
    }

    @Override
    protected void reportFailedPredicate(Parser recognizer, FailedPredicateException e)
    {
      throw new SethSystemException("Parser internal error", e);
    }

    @Override
    protected void reportUnwantedToken(Parser recognizer)
    {
      Token t = recognizer.getCurrentToken();
      int line = t.getLine();
      int pos = t.getCharPositionInLine()+1;
      String near = getTokenErrorDisplay(t);

      throw parseException(testFile, line, pos, near);
    }

    @Override
    protected void reportNoViableAlternative(Parser recognizer, NoViableAltException e)
    {
      Token t = e.getOffendingToken();
      int line = t.getLine();
      int pos = t.getCharPositionInLine()+1;
      String near = getTokenErrorDisplay(t);

      throw parseException(testFile, line, pos, near);
    }

    @Override
    protected void reportInputMismatch(Parser recognizer, InputMismatchException e)
    {
      Token t = e.getOffendingToken();
      int line = t.getLine();
      int pos = t.getCharPositionInLine()+1;
      String near = getTokenErrorDisplay(t);
      String expected = e.getExpectedTokens().toString(recognizer.getTokenNames());

      throw parseException(testFile, line, pos, near, expected);
    }

    @Override
    public void reportMissingToken(Parser recognizer)
    {
      Token t = recognizer.getCurrentToken();
      int line = t.getLine();
      int pos = t.getCharPositionInLine()+1;
      String near = getTokenErrorDisplay(t);
      String expected = recognizer.getExpectedTokens().toString(recognizer.getTokenNames());

      throw parseException(testFile, line, pos, near, expected);
    }
  }

  /**
   * Make and bag a parse exception. We do this because we can't change the overloaded
   * function definitions in the ErrorHandler above.
   */
  public static SethBrownBagException parseException(File file, int line, int pos, String near)
  {
    final String msg = String.format("Syntax error in file %s:%d:%d near %s",
                                     file.getPath(), line, pos, near);
    return new SethBrownBagException(new SyntaxException(msg, file, line, pos, near));
  }

  /**
   * Make and bag a parse exception. We do this because we can't change the overloaded
   * function definitions in the ErrorHandler above.
   */
  public static SethBrownBagException parseException(File file, int line, int pos, String near, String expected)
  {
    final String msg = String.format("Syntax error in file %s:%d:%d near %s; expected %s",
                                     file, line, pos, near, expected);
    return new SethBrownBagException(new SyntaxException(msg, file, line, pos, near));
  }
}
