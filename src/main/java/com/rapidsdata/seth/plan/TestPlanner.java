// Copyright (c) 2017 Boray Data Co. Ltd.  All rights reserved.

package com.rapidsdata.seth.plan;

import com.rapidsdata.seth.contexts.AppContext;
import com.rapidsdata.seth.exceptions.*;
import com.rapidsdata.seth.parser.SethLexer;
import com.rapidsdata.seth.parser.SethParser;
import com.rapidsdata.seth.plan.annotated.TestAnnotationInfo;
import com.rapidsdata.seth.plan.expectedResults.ExpectedResult;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTree;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Deque;
import java.util.List;

/** The class responsible for creating execution plans. */
public class TestPlanner
{
  /** Contains various common information and objects for the application. */
  private final AppContext appContext;

  /**
   * Constructor.
   * @param appContext Contains various common information and objects for the application.
   */
  public TestPlanner(AppContext appContext)
  {
    this.appContext = appContext;
  }

  /**
   * Parses a testfile and returns a Plan instance representing it.
   * @param testFile The file to be parsed.
   * @param callStack the stack of files that are currently being parsed, resulting from file inclusions.
   * @param testsToAnnotate a list that will be populated with tests to annotate. Can be null.
   * @return a Plan that can be executed.
   * @throws FileNotFoundException if the test file doesn't exist.
   * @throws PlanningException if there is a problem with the contents of the test file.
   */
  public Plan newPlanFor(File testFile, List<File> callStack, List<TestAnnotationInfo> testsToAnnotate)
                         throws FileNotFoundException, PlanningException
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
    Plan plan;

    try {
      tree = parser.testFile(); // This will typically throw SyntaxExceptions.

      // Now that we've parsed the statement into a ParseTree we now need to build
      // the list of Operations. We use the visitor pattern for walking the ParseTree.
      TestPlanGenerator generator = new TestPlanGenerator(parser, testFile, callStack, appContext, testsToAnnotate);

      plan = generator.generatePlanFor(tree); // This will typically throw SemanticExceptions,
                                          // but can also throw SyntaxExceptions from included files
                                          // or even a FileNotFoundException for an included path.

    } catch (SethBrownBagException e) {
      if (e.getCause() instanceof PlanningException) {
        throw (PlanningException) e.getCause();

      } else if (e.getCause() instanceof FileNotFoundException) {
        throw (FileNotFoundException) e.getCause();

      } else {
        throw new SethSystemException("Unhandled exception " + e.getClass().getSimpleName(), e.getCause());
      }
    }

    return plan;
  }

  /**
   * Parses a file containing an expected result and returns an ExpectedResult instance representing it.
   * @param resultFile The file to be parsed.
   * @param callStack the stack of files that are currently being parsed, resulting from file inclusions.
   * @param testsToAnnotate a list that will be populated with tests to annotate. Can be null.
   * @return an ExpectedResult instance that can be compared.
   * @throws FileNotFoundException if the result file doesn't exist.
   * @throws PlanningException if there is a problem with the contents of the result file.
   */
  public ExpectedResult newExpectedResultFor(File resultFile,
                                             List<File> callStack,
                                             Deque<List<Operation>> currentOpQueueStack,
                                             List<TestAnnotationInfo> testsToAnnotate)
                        throws FileNotFoundException, PlanningException
  {
    if (!resultFile.exists()) {
      throw new FileNotFoundException("File not found: " + resultFile.getPath());
    }

    byte[] bytes;

    try {
      bytes = Files.readAllBytes(resultFile.toPath());
    } catch (IOException | OutOfMemoryError | SecurityException e) {
      throw new SethSystemException(e);
    }

    String contents = new String(bytes);

    SethLexer lexer = new SethLexer(new ANTLRInputStream(contents));
    SethParser parser = new SethParser(new CommonTokenStream(lexer));
    parser.setErrorHandler(new ErrorHandler(resultFile));

    // Parse the contents of the file.
    ParseTree tree;
    ExpectedResult er;

    try {
      tree = parser.expectedResult(); // This will typically throw SyntaxExceptions.

      // Now that we've parsed the statement into a ParseTree we now need to build
      // the list of Operations. We use the visitor pattern for walking the ParseTree.
      TestPlanGenerator generator = new TestPlanGenerator(parser, resultFile, callStack, appContext, currentOpQueueStack, testsToAnnotate);

      er = generator.generateExpectedResultFor(tree); // This will typically throw SemanticExceptions,
                                                      // but can also throw SyntaxExceptions from included files
                                                      // or even a FileNotFoundException for an included path.

    } catch (SethBrownBagException e) {
      if (e.getCause() instanceof PlanningException) {
        throw (PlanningException) e.getCause();

      } else if (e.getCause() instanceof FileNotFoundException) {
        throw (FileNotFoundException) e.getCause();

      } else {
        throw new SethSystemException("Unhandled exception " + e.getClass().getSimpleName(), e.getCause());
      }
    }

    return er;
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

    //@Override
    //public void recover(Parser recognizer, RecognitionException e)
    //{
    //  throw new SethSystemException("Parser internal error", e);
    //}

    //@Override
    //protected void reportFailedPredicate(Parser recognizer, FailedPredicateException e)
    //{
    //  throw new SethSystemException("Parser internal error", e);
    //}

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
