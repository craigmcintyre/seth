// Copyright (c) 2017 Boray Data Co. Ltd.  All rights reserved.

package com.rapidsdata.seth.plan;

import com.rapidsdata.seth.PathRelativity;
import com.rapidsdata.seth.contexts.AppContext;
import com.rapidsdata.seth.exceptions.*;
import com.rapidsdata.seth.parser.SethBaseVisitor;
import com.rapidsdata.seth.parser.SethParser;
import com.rapidsdata.seth.plan.annotated.TestAnnotationInfo;
import com.rapidsdata.seth.plan.expectedResults.*;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

import java.io.*;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TestPlanGenerator extends SethBaseVisitor
{
  /** The parser we're using to parse the test file. */
  private final Parser parser;

  /** The test file we're reading. */
  private final File testFile;

  /** The stack of files that are currently being parsed, resulting from file inclusions. */
  private final List<File> callStack;

  /** A collection of information and objects used by the application as a whole. */
  private final AppContext appContext;

  /** A stack of plans. The current plan is always the head. */
  private final Deque<Plan> planStack = new LinkedList<>();

  /**
   * A stack of a list of operation that go with the stack of plans above.
   * The head of this queue points to either the list of *test* operations
   * or the list of *cleanup* operations.
   */
  private Deque<List<Operation>> currentOpQueueStack = new LinkedList<>();

  /** A stack of metadata for the current operation as seen by the parser. */
  private Deque<OperationMetadata> opMetadataStack = new LinkedList<>();

  /**
   * A flag to indicate if we just processed an include statement, since this doesn't
   * generate an Operation.
   */
  private boolean gotIncludeStatement = false;

  /** A stack of expected results for the current statements being processed. */
  private Deque<ExpectedResult> expectedResultStack = new LinkedList<>();

  /** Description of the current expected result that is being processed. */
  private String currentExpectedResultDesc;

  /** A list of row definitions expected to be returned from an operation. */
  private List<ExpectedRow> expectedRowList;

  /** A list of expected column definitions for a single expected row. */
  private ArrayList<ExpectedColumnType> columnDefs;

  /** A list of expected column values for a single expected row. */
  private ArrayList<Object> columnVals;

  /** A set of column name definitions expected to be returned from an operation. May be null. */
  private ExpectedColumnNames expectedColumnNames;

  /* A list of tests to be annotated with expected results. */
  private List<TestAnnotationInfo> testsToAnnotate;

  /** The current test to be annotated with new expected results. */
  private TestAnnotationInfo testToAnnotate = null;


  /**
   * Constructor.
   * @param parser The parser we're using to parse the test file.
   * @param testFile The test file we're reading.
   */
  public TestPlanGenerator(SethParser parser, File testFile, List<File> callStack, AppContext appContext, List<TestAnnotationInfo> testsToAnnotate)
  {
    this.parser = parser;
    this.testFile = testFile;
    this.callStack = callStack;
    this.appContext = appContext;
    this.testsToAnnotate = testsToAnnotate;
  }

  /**
   * Constructor.
   * @param parser The parser we're using to parse the test file.
   * @param testFile The test file we're reading.
   */
  public TestPlanGenerator(SethParser parser,
                           File testFile,
                           List<File> callStack,
                           AppContext appContext,
                           Deque<List<Operation>> currentOpQueueStack,
                           List<TestAnnotationInfo> testsToAnnotate)
  {
    this.parser = parser;
    this.testFile = testFile;
    this.callStack = callStack;
    this.appContext = appContext;
    this.currentOpQueueStack = currentOpQueueStack;
    this.testsToAnnotate = testsToAnnotate;
  }

  /**
   * Turns a parsed plan tree into a list of StatementOps.
   * @param tree the parsed plan tree.
   * @return a list of StatementOps representing the operations to be executed.
   */
  public Plan generatePlanFor(ParseTree tree)
  {
    visit(tree);

    // The latest plan is the one at the head of the top of the planStack.
    Plan plan = planStack.pop();
    currentOpQueueStack.pop();

    return plan;
  }


  public ExpectedResult generateExpectedResultFor(ParseTree tree)
  {
    visit(tree);

    // The latest expected result is the one at the head of the top of the expectedResultStack.
    ExpectedResult er = expectedResultStack.pop();
    return er;
  }

  @Override
  public Void visitTestFile(SethParser.TestFileContext ctx)
  {
    // Create a new plan for this test file (or part thereof) and push it onto the stack.
    // Also push the currentOpQueue, which is the queue of testOps, onto a stack too.
    List<Operation> testOps    = new LinkedList<>();
    List<Operation> cleanupOps = new LinkedList<>();
    Plan plan = new Plan(testFile, testOps, cleanupOps);

    planStack.push(plan);
    currentOpQueueStack.push(testOps);

    if (appContext.getCommandLineArgs().recordResults) {

      // Make the output filename of the recorded results file.
      Path outputTestFile = Paths.get(appContext.getCommandLineArgs().resultDir.getPath(), testFile.getName());
      testToAnnotate = new TestAnnotationInfo(testFile.toPath(), outputTestFile);
      testsToAnnotate.add(testToAnnotate);
    }

    visitChildren(ctx);

    return null;
  }


  @Override
  public Void visitCleanupSection(SethParser.CleanupSectionContext ctx)
  {
    // Since we are entering a cleanup section we need to change the current op queue
    // to the queue of cleanup operations.
    Plan plan = planStack.peek();
    currentOpQueueStack.pop();
    currentOpQueueStack.push(plan.getCleanupOperations());

    visitChildren(ctx);
    return null;
  }

  @Override
  public Void visitStatement(SethParser.StatementContext ctx)
  {
    // Get the text of the current statement. We need to work around an issue with Antlr whereby
    // it ignores any comments and whitespace before the first parsable token. So we need to take
    // the index of the first token in this parser context and look backwards for any tokens
    // immediately before it that are not on the default token channel (0). This will include
    // any comments and whitespace, and allow us to save the statement string that the user
    // typed in.
    TokenStream tokenStream = parser.getTokenStream();
    Token startToken = getStartTokenIncludingHiddenToken(ctx.getStart());
    Token stopToken  = (ctx.sethStatement() != null ? ctx.sethStatement().getStop() : ctx.serverStatement().getStop());
    String statementText = tokenStream.getText(startToken, stopToken);

    int line = startToken.getLine();

    // Is this statement occuring in a test phase or a cleanup phase.
    TestPhase phase = TestPhase.TEST;

    if (currentOpQueueStack.peek() == planStack.peek().getCleanupOperations()) {
      phase = TestPhase.CLEANUP;
    }

    OperationMetadata opMetadata = new OperationMetadata(statementText, testFile, line, phase);

    opMetadataStack.push(opMetadata);
    gotIncludeStatement = false;

    visitChildren(ctx);

    // Do we have an expected result?
    if (ctx.expected != null) {

      // We can't allow expected results on include statements since the include occurs
      // here at plan time.
      if (gotIncludeStatement) {
        // We shouldn't have an expected result for an include statement.
        final String msg = "Include statements cannot have an expected result.";
        Token firstToken = ctx.expected.getStart();
        throw semanticException(testFile, firstToken.getLine(), firstToken.getCharPositionInLine(), opMetadata.getDescription(), msg);
      }

      // Rewrite the operation with the expected result.
      ExpectedResult expectedResult = expectedResultStack.pop();
      Operation op = currentOpQueueStack.peek().remove(currentOpQueueStack.peek().size() - 1);
      Operation newOp = op.rewriteWith(expectedResult);
      currentOpQueueStack.peek().add(newOp);

    } else {
      // We don't have an expected result, so lets add a "don't care" expected result.
      if (!gotIncludeStatement) {

        if (appContext.getCommandLineArgs().recordResults) {
          // We are recording the actual result instead.
          // Record the file positions of the expected results to be removed from the original test file.
          int startIdx = ctx.getStop().getStopIndex() + 1;
          testToAnnotate.identifyExistingResult(startIdx, startIdx);

          // Override the expected result with a RecordNewExpectedResult.
          Operation op = currentOpQueueStack.peek().remove(currentOpQueueStack.peek().size() - 1);
          ExpectedResult expectedResult = new RecordNewExpectedResult(op.metadata, appContext, testToAnnotate, startIdx);
          Operation newOp = op.rewriteWith(expectedResult);
          currentOpQueueStack.peek().add(newOp);

        } else {
          // Add a "don't care" expected result.
          Operation op = currentOpQueueStack.peek().remove(currentOpQueueStack.peek().size() - 1);
          ExpectedResult expectedResult = new DontCareExpectedResult(op.metadata, appContext);
          Operation newOp = op.rewriteWith(expectedResult);
          currentOpQueueStack.peek().add(newOp);
        }
      }
    }

    return null;
  }

  @Override
  public Void visitEnclosedServerStatement(SethParser.EnclosedServerStatementContext ctx)
  {
    visitChildren(ctx);

    // Rewrite the operation description so it doesn't contain the enclosing curly braces.
    OperationMetadata opMetadata = opMetadataStack.pop();
    String desc = opMetadata.getDescription().trim();
    desc = desc.substring(1, desc.length() - 1);
    OperationMetadata newOpMetadata = opMetadata.rewriteWith(desc);

    Operation op = new ServerOp(newOpMetadata, new DontCareExpectedResult(newOpMetadata, appContext));
    currentOpQueueStack.peek().add(op);

    return null;
  }

  @Override
  public Void visitNakedServerStatement(SethParser.NakedServerStatementContext ctx)
  {
    visitChildren(ctx);

    OperationMetadata opMetadata = opMetadataStack.pop();
    Operation op = new ServerOp(opMetadata, new DontCareExpectedResult(opMetadata, appContext));
    currentOpQueueStack.peek().add(op);

    return null;
  }

  @Override
  public Void visitLoopStatement(SethParser.LoopStatementContext ctx)
  {
    // Create a new plan for the loop operations to be put into and push it onto the stack.
    // Also push the currentOpQueue, which is the queue of testOps, onto a stack too.
    List<Operation> testOps    = new LinkedList<>();
    List<Operation> cleanupOps = new LinkedList<>();
    Plan plan = new Plan(testFile, testOps, cleanupOps);

    planStack.push(plan);
    currentOpQueueStack.push(testOps);

    // Rewrite the operation description so it doesn't contain all the loop operations.
    OperationMetadata opMetadata = opMetadataStack.pop();
    String desc = opMetadata.getDescription();
    desc = desc.substring(0, desc.indexOf('{') + 1) + " ... }";
    OperationMetadata newOpMetadata = opMetadata.rewriteWith(desc);
    opMetadataStack.push(newOpMetadata);

    visitChildren(ctx);

    return null;
  }


  @Override
  public Void visitCountedLoopStatement(SethParser.CountedLoopStatementContext ctx)
  {
    visitChildren(ctx);

    Long count = null;

    if (ctx.loopCount != null) {
      count = convertToLong(ctx.loopCount);
    }

    OperationMetadata opMetadata = opMetadataStack.pop();

    if (count != null && count < 0) {
      final String msg = "Loop count must be positive: " + count;
      throw semanticException(testFile, ctx.loopCount.getLine(), ctx.loopCount.getCharPositionInLine(),
                              opMetadata.getDescription(), msg);
    }

    Plan loopPlan = planStack.pop();
    currentOpQueueStack.pop();

    ExpectedResult expectedResult = new DontCareExpectedResult(opMetadata, appContext);
    Operation op = new CountedLoopOp(opMetadata, expectedResult, count, loopPlan.getTestOperations());
    currentOpQueueStack.peek().add(op);

    return null;
  }

  @Override
  public Void visitTimedLoopStatement(SethParser.TimedLoopStatementContext ctx)
  {
    visitChildren(ctx);

    OperationMetadata opMetadata = opMetadataStack.pop();

    long count = convertToLong(ctx.count);

    if (count < 0) {
      final String msg = "Loop time must be positive: " + count;
      throw semanticException(testFile, ctx.count.getLine(), ctx.count.getCharPositionInLine(),
                              opMetadata.getDescription(), msg);
    }

    Duration duration;

    if (ctx.HOURS() != null) {
      duration = Duration.ofHours(count);

    } else if (ctx.MINUTES() != null) {
      duration = Duration.ofMinutes(count);

    } else if (ctx.SECONDS() != null) {
      duration = Duration.ofSeconds(count);

    } else if (ctx.MILLISECONDS() != null) {
      duration = Duration.ofMillis(count);

    } else {
      throw new SethSystemException("Unhandled time unit.");
    }

    Plan loopPlan = planStack.pop();
    currentOpQueueStack.pop();

    ExpectedResult expectedResult = new DontCareExpectedResult(opMetadata, appContext);
    Operation op = new TimedLoopOp(opMetadata, expectedResult, duration.toMillis(), loopPlan.getTestOperations());
    currentOpQueueStack.peek().add(op);

    return null;
  }

  @Override
  public Void visitCreateThreadStatement(SethParser.CreateThreadStatementContext ctx)
  {
    visitChildren(ctx);

    int numThreads = 1;

    if (ctx.threadCount != null) {
      numThreads = convertToInt(ctx.threadCount);
    }

    if (numThreads <= 0) {
      final String msg = "Thread count must be positive: " + numThreads;
      throw semanticException(testFile, ctx.threadCount.getLine(), ctx.threadCount.getCharPositionInLine(),
                              opMetadataStack.peek().getDescription(), msg);
    }

    // Rewrite the operation description so it doesn't contain all the thread operations.
    OperationMetadata opMetadata = opMetadataStack.pop();
    String desc = opMetadata.getDescription();
    desc = desc.substring(0, desc.indexOf('{') + 1) + " ... }";
    OperationMetadata newOpMetadata = opMetadata.rewriteWith(desc);

    Plan threadPlan = planStack.pop();
    currentOpQueueStack.pop();

    ExpectedResult expectedResult = new DontCareExpectedResult(newOpMetadata, appContext);
    Operation op = new CreateThreadOp(newOpMetadata, expectedResult, numThreads, threadPlan);
    currentOpQueueStack.peek().add(op);

    return null;
  }

  @Override
  public Void visitSleepStatement(SethParser.SleepStatementContext ctx)
  {
    visitChildren(ctx);

    long millis = convertToLong(ctx.millis);

    if (millis < 0) {
      final String msg = "Sleep time must be positive: " + millis;
      throw semanticException(testFile, ctx.millis.getLine(), ctx.millis.getCharPositionInLine(),
                              opMetadataStack.peek().getDescription(), msg);
    }

    OperationMetadata opMetadata = opMetadataStack.pop();
    ExpectedResult expectedResult = new DontCareExpectedResult(opMetadata, appContext);

    Operation op = new SleepOp(opMetadata, expectedResult, millis);
    currentOpQueueStack.peek().add(op);

    return null;
  }

  @Override
  public Void visitLogStatement(SethParser.LogStatementContext ctx)
  {
    visitChildren(ctx);

    String msg = cleanString(ctx.logStr.getText());

    OperationMetadata opMetadata = opMetadataStack.pop();
    ExpectedResult expectedResult = new DontCareExpectedResult(opMetadata, appContext);

    Operation op = new LogOp(opMetadata, expectedResult, msg);
    currentOpQueueStack.peek().add(op);

    return null;
  }

  @Override
  public Void visitSynchroniseStmt(SethParser.SynchroniseStmtContext ctx)
  {
    visitChildren(ctx);

    String name = null;

    if (ctx.syncName != null) {
      name = cleanString(ctx.syncName.getText());
    }

    int count = -1;

    if (ctx.syncCount != null) {
      count = convertToInt(ctx.syncCount);

      if (count < 0) {
        final String msg = "Synchronisation count must be greater than zero: " + count;
        throw semanticException(testFile, ctx.syncCount.getLine(), ctx.syncCount.getCharPositionInLine(),
                                opMetadataStack.peek().getDescription(), msg);
      }
    }

    OperationMetadata opMetadata = opMetadataStack.pop();
    ExpectedResult expectedResult = new DontCareExpectedResult(opMetadata, appContext);

    Operation op = new SyncOp(opMetadata, expectedResult, name, count);
    currentOpQueueStack.peek().add(op);

    return null;
  }

  @Override
  public Void visitCreateConnStmt(SethParser.CreateConnStmtContext ctx)
  {
    visitChildren(ctx);

    String name = cleanString(ctx.connName.getText());

    if (name.trim().isEmpty()) {
      final String msg = "Connection name cannot be whitespace or empty.";
      throw semanticException(testFile, ctx.connName.getLine(), ctx.connName.getCharPositionInLine(),
                              opMetadataStack.peek().getDescription(), msg);
    }

    String url = null;

    if (ctx.url != null) {
      url = cleanString(ctx.url.getText());

      if (url.trim().isEmpty()) {
        final String msg = "Connection URL cannot be whitespace or empty.";
        throw semanticException(testFile, ctx.url.getLine(), ctx.url.getCharPositionInLine(),
                                opMetadataStack.peek().getDescription(), msg);
      }
    }

    OperationMetadata opMetadata = opMetadataStack.pop();
    ExpectedResult expectedResult = new DontCareExpectedResult(opMetadata, appContext);

    Operation op = new CreateConnectionOp(opMetadata, expectedResult, name, url);
    currentOpQueueStack.peek().add(op);

    return null;
  }

  @Override
  public Void visitUseConnectionStmt(SethParser.UseConnectionStmtContext ctx)
  {
    visitChildren(ctx);

    String name = cleanString(ctx.connName.getText());

    if (name.trim().isEmpty()) {
      final String msg = "Connection name cannot be whitespace or empty.";
      throw semanticException(testFile, ctx.connName.getLine(), ctx.connName.getCharPositionInLine(),
                              opMetadataStack.peek().getDescription(), msg);
    }

    OperationMetadata opMetadata = opMetadataStack.pop();
    ExpectedResult expectedResult = new DontCareExpectedResult(opMetadata, appContext);

    Operation op = new UseConnectionOp(opMetadata, expectedResult, name);
    currentOpQueueStack.peek().add(op);

    return null;
  }

  @Override
  public Void visitDropConnectionStmt(SethParser.DropConnectionStmtContext ctx)
  {
    visitChildren(ctx);

    String name = cleanString(ctx.connName.getText());

    if (name.trim().isEmpty()) {
      final String msg = "Connection name cannot be whitespace or empty.";
      throw semanticException(testFile, ctx.connName.getLine(), ctx.connName.getCharPositionInLine(),
                              opMetadataStack.peek().getDescription(), msg);
    }

    OperationMetadata opMetadata = opMetadataStack.pop();
    ExpectedResult expectedResult = new DontCareExpectedResult(opMetadata, appContext);

    Operation op = new DropConnectionOp(opMetadata, expectedResult, name);
    currentOpQueueStack.peek().add(op);

    return null;
  }

  @Override
  public Void visitIncludeFileStmt(SethParser.IncludeFileStmtContext ctx)
  {
    visitChildren(ctx);

    gotIncludeStatement = true;

    String path = cleanString(ctx.filePath.getText());
    File includeFile = new File(path);

    // Ensure we are not including ourselves.
    if (testFile.equals(includeFile)) {
      final String msg = "File cannot include itself.";
      throw semanticException(testFile, ctx.filePath.getLine(), ctx.filePath.getCharPositionInLine(),
                              opMetadataStack.peek().getDescription(),msg);
    }

    // Ensure that we do not have a circular dependency.
    for (File parent : callStack) {

      if (parent.equals(includeFile)) {
        StringBuilder sb = new StringBuilder(1024);
        sb.append("Circular file inclusion detected: ");

        for (File p : callStack) {
          sb.append(p.getPath()).append(" --> ");
        }

        sb.append(testFile.getPath());
        sb.append(" --> ");
        sb.append(includeFile.getPath());

        throw semanticException(testFile, ctx.filePath.getLine(), ctx.filePath.getCharPositionInLine(),
                                opMetadataStack.peek().getDescription(), sb.toString());
      }
    }


    TestPlanner planner = new TestPlanner(appContext);
    Plan subPlan = null;

    List<File> subCallStack = new LinkedList<>();
    subCallStack.addAll(callStack);
    subCallStack.add(testFile);

    // We need to resolve the included file if it is not an absolute path
    // and we are resolving relative locations to the current test file.
    if (!includeFile.isAbsolute() && appContext.getPathRelativity() == PathRelativity.REFERER) {

      String parent = testFile.getParent();

      if (parent == null) {
        parent = "";
      }

      includeFile = Paths.get(parent, includeFile.getPath()).toFile();
    }

    try {
      subPlan = planner.newPlanFor(includeFile, subCallStack, testsToAnnotate);

    } catch (PlanningException e) {
      throw wrapException(e);

    } catch (FileNotFoundException e) {
      // The included file path is not found so this is a semantic exception in the current file.
      // So convert this to a SemanticException.
      final String msg = "Included file not found: " + path;
      throw semanticException(testFile, ctx.filePath.getLine(), ctx.filePath.getCharPositionInLine(),
                              opMetadataStack.peek().getDescription(), msg);
    }

    // We can now add the operations for the subplan to the current plan.
    // i.e., the file inclusion occurs at planning time. It is not a dynamic operation
    // in and of itself.
    Plan currentPlan = planStack.peek();
    currentPlan.getTestOperations().addAll(subPlan.getTestOperations());
    currentPlan.getCleanupOperations().addAll(subPlan.getCleanupOperations());

    return null;
  }


  @Override
  public Void visitExpectedResult(SethParser.ExpectedResultContext ctx)
  {
    // Save the text of the expected result specified.
    currentExpectedResultDesc = parser.getTokenStream().getText(ctx.getStart(), ctx.getStop());

    if (appContext.getCommandLineArgs().recordResults) {
      // Record the file positions of the expected results to be removed.
      int startIdx = ctx.getStart().getStartIndex();
      int stopIdx = ctx.getStop().getStopIndex() + 1;
      testToAnnotate.identifyExistingResult(startIdx, stopIdx);

      // Get the metadata for the last statement that was added.
      List<Operation> opList = currentOpQueueStack.peek();
      OperationMetadata opMetadata = opList.get(opList.size() - 1).metadata;

      // Override the expected result with a RecordNewExpectedResult.
      ExpectedResult er = new RecordNewExpectedResult(opMetadata, appContext, testToAnnotate, startIdx);
      expectedResultStack.push(er);

    } else {
      visitChildren(ctx);
    }

    currentExpectedResultDesc = null;
    return null;
  }

  @Override
  public Void visitResultFile(SethParser.ResultFileContext ctx)
  {
    visitChildren(ctx);

    String path = cleanString(ctx.filePath.getText());
    File includeFile = new File(path);

    // Ensure we are not including ourselves.
    if (testFile.equals(includeFile)) {
      final String msg = "File cannot include itself.";
      throw semanticException(testFile, ctx.filePath.getLine(), ctx.filePath.getCharPositionInLine(),
                              opMetadataStack.peek().getDescription(),msg);
    }

    // Ensure that we do not have a circular dependency.
    for (File parent : callStack) {

      if (parent.equals(includeFile)) {
        StringBuilder sb = new StringBuilder(1024);
        sb.append("Circular file inclusion detected: ");

        for (File p : callStack) {
          sb.append(p.getPath()).append(" --> ");
        }

        sb.append(testFile.getPath());
        sb.append(" --> ");
        sb.append(includeFile.getPath());

        throw semanticException(testFile, ctx.filePath.getLine(), ctx.filePath.getCharPositionInLine(),
                                opMetadataStack.peek().getDescription(), sb.toString());
      }
    }


    TestPlanner planner = new TestPlanner(appContext);
    ExpectedResult er = null;

    List<File> subCallStack = new LinkedList<>();
    subCallStack.addAll(callStack);
    subCallStack.add(testFile);

    // We need to resolve the included file if it is not an absolute path
    // and we are resolving relative locations to the current test file.
    if (!includeFile.isAbsolute() && appContext.getPathRelativity() == PathRelativity.REFERER) {

      String parent = testFile.getParent();

      if (parent == null) {
        parent = "";
      }

      includeFile = Paths.get(parent, includeFile.getPath()).toFile();
    }

    try {
      er = planner.newExpectedResultFor(includeFile, subCallStack, currentOpQueueStack, testsToAnnotate);

    } catch (PlanningException e) {
      throw wrapException(e);

    } catch (FileNotFoundException e) {
      // The included file path is not found so this is a semantic exception in the current file.
      // So convert this to a SemanticException.
      final String msg = "Included file not found: " + path;
      throw semanticException(testFile, ctx.filePath.getLine(), ctx.filePath.getCharPositionInLine(),
                              opMetadataStack.peek().getDescription(), msg);
    }

    expectedResultStack.push(er);
    return null;
  }

  @Override
  public Void visitSuccess(SethParser.SuccessContext ctx)
  {
    visitChildren(ctx);

    // Get the metadata for the last statement that was added.
    List<Operation> opList = currentOpQueueStack.peek();
    OperationMetadata opMetadata = opList.get(opList.size() - 1).metadata;

    ExpectedResult er = new SuccessExpectedResult(currentExpectedResultDesc, opMetadata, appContext);
    expectedResultStack.push(er);

    return null;
  }

  @Override
  public Void visitMute(SethParser.MuteContext ctx)
  {
    visitChildren(ctx);

    // Get the metadata for the last statement that was added.
    List<Operation> opList = currentOpQueueStack.peek();
    OperationMetadata opMetadata = opList.get(opList.size() - 1).metadata;

    ExpectedResult er = new MuteExpectedResult(currentExpectedResultDesc, opMetadata, appContext);
    expectedResultStack.push(er);

    return null;
  }

  @Override
  public Void visitFailureCodeAndMsgPrefix(SethParser.FailureCodeAndMsgPrefixContext ctx)
  {
    visitChildren(ctx);

    int errCode = convertToInt(ctx.code);
    String errMsg = cleanString(ctx.msg.getText());

    // Get the metadata for the last statement that was added.
    List<Operation> opList = currentOpQueueStack.peek();
    OperationMetadata opMetadata = opList.get(opList.size() - 1).metadata;

    ExpectedResult er = new FailureErrorCodeAndMsgPrefixExpectedResult(currentExpectedResultDesc, opMetadata, appContext, errCode, errMsg);
    expectedResultStack.push(er);

    return null;
  }

  @Override
  public Void visitFailureCodeAndMsgSuffix(SethParser.FailureCodeAndMsgSuffixContext ctx)
  {
    visitChildren(ctx);

    int errCode = convertToInt(ctx.code);
    String errMsg = cleanString(ctx.msg.getText());

    // Get the metadata for the last statement that was added.
    List<Operation> opList = currentOpQueueStack.peek();
    OperationMetadata opMetadata = opList.get(opList.size() - 1).metadata;

    ExpectedResult er = new FailureErrorCodeAndMsgSuffixExpectedResult(currentExpectedResultDesc, opMetadata, appContext, errCode, errMsg);
    expectedResultStack.push(er);

    return null;
  }

  @Override
  public Void visitFailureCodeAndMsgSubset(SethParser.FailureCodeAndMsgSubsetContext ctx)
  {
    visitChildren(ctx);

    int errCode = convertToInt(ctx.code);
    String errMsg = cleanString(ctx.msg.getText());

    // Get the metadata for the last statement that was added.
    List<Operation> opList = currentOpQueueStack.peek();
    OperationMetadata opMetadata = opList.get(opList.size() - 1).metadata;

    ExpectedResult er = new FailureErrorCodeAndMsgSubsetExpectedResult(currentExpectedResultDesc, opMetadata, appContext, errCode, errMsg);
    expectedResultStack.push(er);

    return null;
  }

  @Override
  public Void visitFailureErrorCode(SethParser.FailureErrorCodeContext ctx)
  {
    visitChildren(ctx);

    int errCode = convertToInt(ctx.code);

    // Get the metadata for the last statement that was added.
    List<Operation> opList = currentOpQueueStack.peek();
    OperationMetadata opMetadata = opList.get(opList.size() - 1).metadata;

    ExpectedResult er = new FailureErrorCodeExpectedResult(currentExpectedResultDesc, opMetadata, appContext, errCode);
    expectedResultStack.push(er);

    return null;
  }

  @Override
  public Void visitFailureErrorMsgPrefix(SethParser.FailureErrorMsgPrefixContext ctx)
  {
    visitChildren(ctx);

    String errMsg = cleanString(ctx.msg.getText());

    // Get the metadata for the last statement that was added.
    List<Operation> opList = currentOpQueueStack.peek();
    OperationMetadata opMetadata = opList.get(opList.size() - 1).metadata;

    ExpectedResult er = new FailureErrorMsgPrefixExpectedResult(currentExpectedResultDesc, opMetadata, appContext, errMsg);
    expectedResultStack.push(er);

    return null;
  }

  @Override
  public Void visitFailureErrorMsgSuffix(SethParser.FailureErrorMsgSuffixContext ctx)
  {
    visitChildren(ctx);

    String errMsg = cleanString(ctx.msg.getText());

    // Get the metadata for the last statement that was added.
    List<Operation> opList = currentOpQueueStack.peek();
    OperationMetadata opMetadata = opList.get(opList.size() - 1).metadata;

    ExpectedResult er = new FailureErrorMsgSuffixExpectedResult(currentExpectedResultDesc, opMetadata, appContext, errMsg);
    expectedResultStack.push(er);

    return null;
  }

  @Override
  public Void visitFailureErrorMsgSubset(SethParser.FailureErrorMsgSubsetContext ctx)
  {
    visitChildren(ctx);

    String errMsg = cleanString(ctx.msg.getText());

    // Get the metadata for the last statement that was added.
    List<Operation> opList = currentOpQueueStack.peek();
    OperationMetadata opMetadata = opList.get(opList.size() - 1).metadata;

    ExpectedResult er = new FailureErrorMsgSubsetExpectedResult(currentExpectedResultDesc, opMetadata, appContext, errMsg);
    expectedResultStack.push(er);

    return null;
  }

  @Override
  public Void visitFailureAny(SethParser.FailureAnyContext ctx)
  {
    visitChildren(ctx);

    // Get the metadata for the last statement that was added.
    List<Operation> opList = currentOpQueueStack.peek();
    OperationMetadata opMetadata = opList.get(opList.size() - 1).metadata;

    ExpectedResult er = new FailureAnyExpectedResult(currentExpectedResultDesc, opMetadata, appContext);
    expectedResultStack.push(er);

    return null;
  }

  @Override
  public Void visitOrderedRows(SethParser.OrderedRowsContext ctx)
  {
    visitChildren(ctx);

    // Get the metadata for the last statement that was added.
    List<Operation> opList = currentOpQueueStack.peek();
    OperationMetadata opMetadata = opList.get(opList.size() - 1).metadata;

    ExpectedResult er;

    if (appContext.getCommandLineArgs().unordered) {
      er = new UnorderedRowsExpectedResult(currentExpectedResultDesc, opMetadata, appContext, expectedRowList, expectedColumnNames);

    } else {
      er = new OrderedRowsExpectedResult(currentExpectedResultDesc, opMetadata, appContext, expectedRowList, expectedColumnNames);
    }

    expectedResultStack.push(er);

    this.expectedRowList = null;

    return null;
  }

  @Override
  public Void visitUnorderedRows(SethParser.UnorderedRowsContext ctx)
  {
    visitChildren(ctx);

    // Get the metadata for the last statement that was added.
    List<Operation> opList = currentOpQueueStack.peek();
    OperationMetadata opMetadata = opList.get(opList.size() - 1).metadata;

    ExpectedResult er = new UnorderedRowsExpectedResult(currentExpectedResultDesc, opMetadata, appContext, expectedRowList, expectedColumnNames);
    expectedResultStack.push(er);

    this.expectedRowList = null;

    return null;
  }

  @Override
  public Void visitRowCount(SethParser.RowCountContext ctx)
  {
    visitChildren(ctx);

    long expectedRowCount = convertToLong(ctx.count);

    if (expectedRowCount < 0) {
      final String msg = "The expected row count must be >= 0.";
      throw semanticException(testFile, ctx.count.getLine(), ctx.count.getCharPositionInLine(), currentExpectedResultDesc, msg);
    }

    // Get the metadata for the last statement that was added.
    List<Operation> opList = currentOpQueueStack.peek();
    OperationMetadata opMetadata = opList.get(opList.size() - 1).metadata;

    ExpectedResult er = new RowCountExpectedResult(currentExpectedResultDesc, opMetadata, appContext, expectedRowCount);
    expectedResultStack.push(er);

    return null;
  }

  @Override
  public Void visitAffectedRowsCount(SethParser.AffectedRowsCountContext ctx)
  {
    visitChildren(ctx);

    long affectedRowCount = convertToLong(ctx.count);

    // Get the metadata for the last statement that was added.
    List<Operation> opList = currentOpQueueStack.peek();
    OperationMetadata opMetadata = opList.get(opList.size() - 1).metadata;

    ExpectedResult er = new AffectedRowsExpectedResult(currentExpectedResultDesc, opMetadata, appContext, affectedRowCount);
    expectedResultStack.push(er);

    return null;
  }

  @Override
  public Void visitResultSet(SethParser.ResultSetContext ctx)
  {
    this.expectedRowList = new ArrayList<>();

    visitChildren(ctx);

    return null;
  }

  @Override
  public Void visitColumnNames(SethParser.ColumnNamesContext ctx)
  {
    this.columnDefs = new ArrayList<ExpectedColumnType>();
    this.columnVals = new ArrayList<Object>();

    visitChildren(ctx);

    // check if there are any "IGNORE_REMAINING" column definitions. There can only be a
    // maximum of one of these and it must be the last column definition in the list.
    for (int i = 0; i < columnDefs.size(); i++) {
      ExpectedColumnType type = columnDefs.get(i);

      if (type == ExpectedColumnType.IGNORE_REMAINING && i != columnDefs.size() - 1) {
        final String msg = "The '...' column name definition can only be specified as the last " +
                           "column name definition.";
        Token token = ctx.columnName().get(i).ignoreRemainingColumns().ELLIPSIS().getSymbol();
        throw semanticException(testFile, token.getLine(), token.getCharPositionInLine(), currentExpectedResultDesc, msg);
      }
    }

    this.expectedColumnNames = new ExpectedColumnNames(columnDefs, columnVals);

    this.columnDefs = null;
    this.columnVals = null;

    return null;
  }


  @Override
  public Void visitRowData(SethParser.RowDataContext ctx)
  {
    this.columnDefs = new ArrayList<ExpectedColumnType>();
    this.columnVals = new ArrayList<Object>();

    visitChildren(ctx);

    // check if there are any "IGNORE_REMAINING" column definitions. There can only be a
    // maximum of one of these and it must be the last column definition in the list.
    for (int i = 0; i < columnDefs.size(); i++) {
      ExpectedColumnType type = columnDefs.get(i);

      if (type == ExpectedColumnType.IGNORE_REMAINING && i != columnDefs.size() - 1) {
        final String msg = "The '...' column definition can only be specified as the last " +
                           "definition for row.";
        Token token = ctx.columnData().get(i).ignoreRemainingColumns().ELLIPSIS().getSymbol();
        throw semanticException(testFile, token.getLine(), token.getCharPositionInLine(), currentExpectedResultDesc, msg);
      }
    }

    ExpectedRow expectedRow = new ExpectedRow(columnDefs, columnVals);
    this.expectedRowList.add(expectedRow);

    this.columnDefs = null;
    this.columnVals = null;

    return null;
  }

  @Override
  public Void visitBooleanVal(SethParser.BooleanValContext ctx)
  {
    visitChildren(ctx);

    this.columnDefs.add(ExpectedColumnType.BOOLEAN);
    this.columnVals.add(ctx.TRUE() != null);

    return null;
  }

  @Override
  public Void visitIntegerVal(SethParser.IntegerValContext ctx)
  {
    visitChildren(ctx);

    this.columnDefs.add(ExpectedColumnType.INTEGER);

    long val = convertToLong(ctx.INT().getSymbol());
    this.columnVals.add(val);

    return null;
  }

  @Override
  public Void visitDecimalVal(SethParser.DecimalValContext ctx)
  {
    visitChildren(ctx);

    this.columnDefs.add(ExpectedColumnType.DECIMAL);

    BigDecimal val = convertToBigDecimal(ctx.DEC().getSymbol());
    this.columnVals.add(val);

    return null;
  }

  @Override
  public Void visitFloatVal(SethParser.FloatValContext ctx)
  {
    visitChildren(ctx);

    this.columnDefs.add(ExpectedColumnType.FLOAT);

    // We internally store floats as strings to we can extract the required precision
    // needed for comparisons.
    String val = ctx.FLT().getText();
    ComparableFloat comparableFloat;

    try {
      comparableFloat = new ComparableFloat(val);

    } catch (NumberFormatException e) {
      // This shouldn't happen - the parser should pick up any syntax error.
      throw new SethSystemException(e);
    }

    this.columnVals.add(comparableFloat);

    return null;
  }

  @Override
  public Void visitStringVal(SethParser.StringValContext ctx)
  {
    visitChildren(ctx);

    this.columnDefs.add(ExpectedColumnType.STRING);

    String val = cleanString(ctx.STR().getText());
    this.columnVals.add(val);

    return null;
  }

  @Override
  public Void visitDateVal(SethParser.DateValContext ctx)
  {
    visitChildren(ctx);

    this.columnDefs.add(ExpectedColumnType.DATE);

    Token strToken = ctx.STR().getSymbol();
    String strVal = cleanString(strToken.getText());

    try {
      LocalDate localDate = LocalDate.parse(strVal);
      this.columnVals.add(localDate);

    } catch (DateTimeParseException e) {
      final String errMsg = "Unable to parse date string: '" + strVal + "'. Must be 'yyyy-mm-dd'.";
      throw semanticException(testFile, strToken.getLine(), strToken.getCharPositionInLine(), currentExpectedResultDesc, errMsg);
    }

    return null;
  }

  @Override
  public Void visitTimeVal(SethParser.TimeValContext ctx)
  {
    visitChildren(ctx);

    this.columnDefs.add(ExpectedColumnType.TIME);

    Token strToken = ctx.STR().getSymbol();
    String strVal = cleanString(strToken.getText());

    try {
      LocalTime localTime = LocalTime.parse(strVal);
      this.columnVals.add(localTime);

    } catch (DateTimeParseException e) {
      final String errMsg = "Unable to parse time string: '" + strVal + "'. Must be 'hh:mm:dd'.";
      throw semanticException(testFile, strToken.getLine(), strToken.getCharPositionInLine(), currentExpectedResultDesc, errMsg);
    }

    return null;
  }

  @Override
  public Void visitTimestampVal(SethParser.TimestampValContext ctx)
  {
    visitChildren(ctx);

    this.columnDefs.add(ExpectedColumnType.TIMESTAMP);

    Token strToken = ctx.STR().getSymbol();
    String strVal = cleanString(strToken.getText());

    try {
      LocalDateTime localDateTime = Timestamp.valueOf(strVal).toLocalDateTime();
      this.columnVals.add(localDateTime);

    } catch (IllegalArgumentException e) {
      final String errMsg = "Unable to parse timestamp string: '" + strVal + "'. Must be 'yyyy-mm-dd hh:mm:ss[.fff...]'.";
      throw semanticException(testFile, strToken.getLine(), strToken.getCharPositionInLine(), currentExpectedResultDesc, errMsg);
    }

    return null;
  }

  @Override
  public Void visitYearMonthInterval(SethParser.YearMonthIntervalContext ctx)
  {
    visitChildren(ctx);

    Token strToken = ctx.STR().getSymbol();
    String strVal = cleanString(strToken.getText()).trim();

    // There can be a sign outside of the interval string as well as one inside.
    // This initially caters for the sign outside of the interval string.
    boolean isMinus = ctx.minus != null;

    if (strVal.startsWith("-")) {
      isMinus = !isMinus;
    }

    // Validate the string syntax
    if (!strVal.matches("\\s*[+-]?\\s*\\d+(-\\d+)?")) {
      final String msg = "Invalid year-month interval format.";
      throw semanticException(testFile, strToken.getLine(), strToken.getCharPositionInLine(), currentExpectedResultDesc, msg);
    }

    Period period = Period.ZERO;
    long val;

    try {

      if (ctx.m != null) {
        // month only
        val = Long.valueOf(strVal);

        if (val < 0 || val > 11) {
          final String msg = "Interval month value must be between [0,11].";
          throw semanticException(testFile, strToken.getLine(), strToken.getCharPositionInLine(), currentExpectedResultDesc, msg);
        }

        if (isMinus) {
          val = -val;
        }
        period = period.plusMonths(val);

      } else {
        // Could be year only or year-month
        Pattern p = Pattern.compile("\\d+");
        Matcher m = p.matcher(strVal);

        if (!m.find()) {
          final String msg = "Invalid interval year value.";
          throw semanticException(testFile, strToken.getLine(), strToken.getCharPositionInLine(), currentExpectedResultDesc, msg);
        }

        // We've got the years
        val = Long.valueOf(m.group());
        period = period.plusYears(val);

        // Is there a months component too?
        if (ctx.y2m != null) {
          if (!m.find()) {
            final String msg = "Invalid interval month value.";
            throw semanticException(testFile, strToken.getLine(), strToken.getCharPositionInLine(), currentExpectedResultDesc, msg);
          }

          val = Long.valueOf(m.group());

          if (val < 0 || val > 11) {
            final String msg = "Interval month value must be between [0,11].";
            throw semanticException(testFile, strToken.getLine(), strToken.getCharPositionInLine(), currentExpectedResultDesc, msg);
          }

          period = period.plusMonths(val);
        }
      }

    } catch (NumberFormatException e) {
      final String msg = "Unable to convert value to a long: " + strVal;
      throw new SethSystemException(msg, e);
    }

    this.columnDefs.add(ExpectedColumnType.INTERVAL);
    this.columnVals.add(period);

    return null;
  }

  @Override
  public Void visitDayTimeInterval(SethParser.DayTimeIntervalContext ctx)
  {
    visitChildren(ctx);

    Token strToken = ctx.STR().getSymbol();
    String strVal = cleanString(strToken.getText()).trim();

    // There can be a sign outside of the interval string as well as one inside.
    // This initially caters for the sign outside of the interval string.
    boolean isMinus = ctx.minus != null;

    if (strVal.startsWith("-")) {
      isMinus = !isMinus;
    }

    // Validate the string syntax
    final String[] formats = {
        "\\s*[+-]?\\s*\\d+( \\d+(:\\d+(:\\d+(\\.\\d+)?)?)?)?",  // day-time or day or hour or minute.
        "\\s*[+-]?\\s*\\d+(:\\d+(:\\d+(\\.\\d+)?)?)?",          // hour to minute or hour to second.
        "\\s*[+-]?\\s*\\d+(:\\d+(\\.\\d+)?)?",                  // minute to second.
        "\\s*[+-]?\\s*\\d+(\\.\\d+)?",                          // second only.
    };
    if (!strVal.matches(formats[0]) &&
        !strVal.matches(formats[1]) &&
        !strVal.matches(formats[2]) &&
        !strVal.matches(formats[3])) {
      final String msg = "Invalid day-time interval format.";
      throw semanticException(testFile, strToken.getLine(), strToken.getCharPositionInLine(), currentExpectedResultDesc, msg);
    }

    Duration duration = Duration.ZERO;
    long val;

    Pattern p = Pattern.compile("\\d+");
    Matcher m = p.matcher(strVal);

    try {

      if (!m.find()) {
        final String msg = "Invalid day-time interval value.";
        throw semanticException(testFile, strToken.getLine(), strToken.getCharPositionInLine(), currentExpectedResultDesc, msg);
      }

      // Read the first value.
      val = Long.valueOf(m.group());

      if (isMinus) {
        val = -val;
      }

      if (ctx.d != null) {
        // days only
        duration = duration.plusDays(val);

      } else if (ctx.h != null) {
        // hours only
        duration = duration.plusHours(val);

      } else if (ctx.m != null) {
        // minutes only
        duration = duration.plusMinutes(val);

      } else if (ctx.s != null) {
        // seconds only
        duration = duration.plusSeconds(val);

        // fractional seconds
        if (m.find()) {
          String s = strVal.substring(m.start(), m.end());
          int numDigits = s.length();

          // read the fractional seconds
          val = Long.valueOf(m.group());

          if (isMinus) {
            val = -val;
          }

          // Convert to nanoseconds
          val = val * (long) Math.pow(10, 9 - numDigits);
          duration = duration.plusNanos(val);
        }

      } else if (ctx.d2h != null || ctx.d2m != null || ctx.d2s != null) {
        // day to hour
        duration = duration.plusDays(val);

        if (!m.find()) {
          final String msg = "Missing hour field in day-time interval value.";
          throw semanticException(testFile, strToken.getLine(), strToken.getCharPositionInLine(), currentExpectedResultDesc, msg);
        }

        // read the hour
        val = Long.valueOf(m.group());

        if (isMinus) {
          val = -val;
        }

        duration = duration.plusHours(val);

        if (ctx.d2m != null || ctx.d2s != null) {
          if (!m.find()) {
            final String msg = "Missing minute field in day-time interval value.";
            throw semanticException(testFile, strToken.getLine(), strToken.getCharPositionInLine(), currentExpectedResultDesc, msg);
          }

          // read the minute
          val = Long.valueOf(m.group());

          if (isMinus) {
            val = -val;
          }

          duration = duration.plusMinutes(val);

          if (ctx.d2s != null) {
            if (!m.find()) {
              final String msg = "Missing second field in day-time interval value.";
              throw semanticException(testFile, strToken.getLine(), strToken.getCharPositionInLine(), currentExpectedResultDesc, msg);
            }

            // read the second
            val = Long.valueOf(m.group());

            if (isMinus) {
              val = -val;
            }

            duration = duration.plusSeconds(val);

            // fractional seconds
            if (m.find()) {
              String s = strVal.substring(m.start(), m.end());
              int numDigits = s.length();

              // read the fractional seconds
              val = Long.valueOf(m.group());

              if (isMinus) {
                val = -val;
              }

              // Convert to nanoseconds
              val = val * (long) Math.pow(10, 9 - numDigits);
              duration = duration.plusNanos(val);
            }
          }
        }

      } else if (ctx.h2m != null || ctx.h2s != null) {
        // hours to minutes or hours to seconds.
        duration = duration.plusHours(val);

        if (!m.find()) {
          final String msg = "Missing minute field in day-time interval value.";
          throw semanticException(testFile, strToken.getLine(), strToken.getCharPositionInLine(), currentExpectedResultDesc, msg);
        }

        // read the minute
        val = Long.valueOf(m.group());

        if (isMinus) {
          val = -val;
        }

        duration = duration.plusMinutes(val);

        if (ctx.h2s != null) {
          if (!m.find()) {
            final String msg = "Missing second field in day-time interval value.";
            throw semanticException(testFile, strToken.getLine(), strToken.getCharPositionInLine(), currentExpectedResultDesc, msg);
          }

          // read the second
          val = Long.valueOf(m.group());

          if (isMinus) {
            val = -val;
          }

          duration = duration.plusSeconds(val);

          // fractional seconds
          if (m.find()) {
            String s = strVal.substring(m.start(), m.end());
            int numDigits = s.length();

            // read the fractional seconds
            val = Long.valueOf(m.group());

            if (isMinus) {
              val = -val;
            }

            // Convert to nanoseconds
            val = val * (long) Math.pow(10, 9 - numDigits);
            duration = duration.plusNanos(val);
          }
        }

      } else if (ctx.m2s != null) {
        // minutes to seconds.
        duration = duration.plusMinutes(val);

        if (!m.find()) {
          final String msg = "Missing seconds field in day-time interval value.";
          throw semanticException(testFile, strToken.getLine(), strToken.getCharPositionInLine(), currentExpectedResultDesc, msg);
        }

        // read the seconds
        val = Long.valueOf(m.group());

        if (isMinus) {
          val = -val;
        }

        duration = duration.plusSeconds(val);

        // fractional seconds
        if (m.find()) {
          String s = strVal.substring(m.start(), m.end());
          int numDigits = s.length();

          // read the fractional seconds
          val = Long.valueOf(m.group());

          if (isMinus) {
            val = -val;
          }

          // Convert to nanoseconds
          val = val * (long) Math.pow(10, 9 - numDigits);
          duration = duration.plusNanos(val);
        }

      } else {
        throw new SethSystemException("Shouldn't get here.");
      }

    } catch (NumberFormatException e) {
      final String msg = "Unable to convert value to a long: " + strVal;
      throw new SethSystemException(msg, e);
    }

    this.columnDefs.add(ExpectedColumnType.INTERVAL);
    this.columnVals.add(duration);

    return null;
  }

  @Override
  public Void visitNullVal(SethParser.NullValContext ctx)
  {
    visitChildren(ctx);

    this.columnDefs.add(ExpectedColumnType.NULL);
    this.columnVals.add(null);

    return null;
  }

  @Override
  public Void visitDontCareVal(SethParser.DontCareValContext ctx)
  {
    visitChildren(ctx);

    this.columnDefs.add(ExpectedColumnType.DONT_CARE);
    this.columnVals.add(null);

    return null;
  }

  @Override
  public Void visitIgnoreRemainingColumns(SethParser.IgnoreRemainingColumnsContext ctx)
  {
    visitChildren(ctx);

    this.columnDefs.add(ExpectedColumnType.IGNORE_REMAINING);
    this.columnVals.add(null);

    return null;
  }


  /**
   * Creates a SemanticException and wraps a SethBrownBagException around it.
   * @param file the file that the error occurred in.
   * @param line the line that the error occurred in.
   * @param pos the position on the line that the error occurred in.
   * @param command the command that has the semantic error.
   * @param errorMsg an error message.
   * @return an unchecked exception ready for throwing.
   */
  private SethBrownBagException semanticException(File file, int line, int pos, String command, String errorMsg)
  {
    final String format = "Semantic error in file %s:%d:%d : %s";
    final String msg = String.format(format, file, line, pos + 1, errorMsg);

    return wrapException(new SemanticException(msg, file, line, pos, command));
  }

  /**
   * Wraps the Exception in an unchecked SethBrownBagException.
   * @param e The exception to wrap.
   * @return The wrapped, unchecked, SethBrownBagException.
   */
  private SethBrownBagException wrapException(Exception e)
  {
    return new SethBrownBagException(e);
  }

  /**
   * Wraps the Exception in an unchecked SethBrownBagException.
   * @param e The exception to wrap.
   * @return The wrapped, unchecked, SethBrownBagException.
   */
  private SethBrownBagException wrapException(String msg, Exception e)
  {
    return new SethBrownBagException(msg, e);
  }


  /**
   * Return a cleaned-up string for raw string tokens.
   */
  private String cleanString(String raw)
  {
    String str = raw.substring(1, raw.length() - 1);
    str = str.replaceAll("\'\'", "\'");
    return str;
  }

  /**
   * Converts the text of a token into an integer.
   * @param token the token to convert
   * @return a int.
   */
  private int convertToInt(Token token)
  {
    String s = token.getText();
    int val;

    try {
      val = Integer.valueOf(s);

    } catch (NumberFormatException e) {
      final String msg = "Unable to convert value to an integer: " + s;
      throw new SethSystemException(msg, e);
    }

    return val;
  }

  /**
   * Converts the text of a token into a long.
   * @param token the token to convert
   * @return a long.
   */
  private long convertToLong(Token token)
  {
    String s = token.getText();
    long val;

    try {
      val = Long.valueOf(s);

    } catch (NumberFormatException e) {
      final String msg = "Unable to convert value to a long: " + s;
      throw new SethSystemException(msg, e);
    }

    return val;
  }

  /**
   * Converts the text of a token into a BigDecimal.
   * @param token the token to convert
   * @return a BigDecimal.
   */
  private BigDecimal convertToBigDecimal(Token token)
  {
    String s = token.getText();
    BigDecimal val;

    try {
      val = new BigDecimal(s);

    } catch (NumberFormatException e) {
      final String msg = "Unable to convert value to a BigDecimal: " + s;
      throw new SethSystemException(msg, e);
    }

    return val;
  }

  /**
   * Converts the text of a token into a double.
   * @param token the token to convert
   * @return a double.
   */
  private double convertToDouble(Token token)
  {
    String s = token.getText();
    double val;

    try {
      val = Double.valueOf(s);

    } catch (NumberFormatException e) {
      final String msg = "Unable to convert value to a double: " + s;
      throw new SethSystemException(msg, e);
    }

    return val;
  }

  /**
   * Given a starting token, search backwards in the parser's token stream to see if there
   * are any other tokens on non-default channels that would also belong to the same
   * parsing context. This will find comments and whitespace that occur before the first
   * parsable token. For some reason Antlr does not include these, but they do include these
   * hidden tokens if they occur in the middle or end of the non-hidden tokens.
   * @param startToken the starting token for a parable context.
   * @return The actual starting token, accounting for any tokens on hidden channels before the
   * startToken parameter.
   */
  private Token getStartTokenIncludingHiddenToken(Token startToken)
  {
    TokenStream tokenStream = parser.getTokenStream();
    int index = startToken.getTokenIndex() - 1;

    while(index >= 0) {
      Token token = tokenStream.get(index);

      // If the token is on channel 0 (the default) then it must belong to a different
      // parsing context, so don't include it.
      if (token.getChannel() == 0) {
        return tokenStream.get(index + 1);
      }

      // If this token is on a line before the start token then don't include it.
      if (token.getLine() < startToken.getLine()) {
        return tokenStream.get(index + 1);
      }

      --index;
    }

    return tokenStream.get(0);
  }
}
