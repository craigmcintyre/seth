// Copyright (c) 2017 Boray Data Co. Ltd.  All rights reserved.

package com.rapidsdata.seth.plan;

import com.rapidsdata.seth.contexts.AppContext;
import com.rapidsdata.seth.exceptions.*;
import com.rapidsdata.seth.parser.SethBaseVisitor;
import com.rapidsdata.seth.parser.SethLexer;
import com.rapidsdata.seth.parser.SethParser;
import com.rapidsdata.seth.plan.expectedResults.*;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

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

  /**
   * Constructor.
   * @param parser The parser we're using to parse the test file.
   * @param testFile The test file we're reading.
   */
  public TestPlanGenerator(SethParser parser, File testFile, List<File> callStack, AppContext appContext)
  {
    this.parser = parser;
    this.testFile = testFile;
    this.callStack = callStack;
    this.appContext = appContext;
  }


  /**
   * Turns a parsed plan tree into a list of StatementOps.
   * @param tree the parsed plan tree.
   * @return a list of StatementOps representing the operations to be executed.
   */
  public Plan generateFor(ParseTree tree)
  {
    visit(tree);

    // The latest plan is the one at the head of the top of the planStack.
    Plan plan = planStack.pop();
    currentOpQueueStack.pop();

    return plan;
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
        Operation op = currentOpQueueStack.peek().remove(currentOpQueueStack.peek().size() - 1);
        ExpectedResult expectedResult = new DontCareExpectedResult(op.metadata, appContext);
        Operation newOp = op.rewriteWith(expectedResult);
        currentOpQueueStack.peek().add(newOp);
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
    String desc = opMetadata.getDescription();
    desc = desc.substring(0, desc.length() - 1);
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

    visitChildren(ctx);

    // Rewrite the operation description so it doesn't contain all the loop operations.
    OperationMetadata opMetadata = opMetadataStack.pop();
    String desc = opMetadata.getDescription();
    desc = desc.substring(0, desc.indexOf('{') + 1) + " ... }";
    OperationMetadata newOpMetadata = opMetadata.rewriteWith(desc);

    Long count = null;

    if (ctx.loopCount != null) {
      count = convertToLong(ctx.loopCount);
    }

    if (count != null && count < 0) {
      final String msg = "Loop count must be positive: " + count;
      throw semanticException(testFile, ctx.loopCount.getLine(), ctx.loopCount.getCharPositionInLine(),
                              newOpMetadata.getDescription(), msg);
    }

    Plan loopPlan = planStack.pop();
    currentOpQueueStack.pop();

    ExpectedResult expectedResult = new DontCareExpectedResult(newOpMetadata, appContext);
    Operation op = new LoopOp(newOpMetadata, expectedResult, count, plan.getTestOperations());
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

    try {
      subPlan = planner.newPlanFor(includeFile, subCallStack);

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

    visitChildren(ctx);

    currentExpectedResultDesc = null;
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
  public Void visitFailureCodeAndMsg(SethParser.FailureCodeAndMsgContext ctx)
  {
    visitChildren(ctx);

    int errCode = convertToInt(ctx.code);
    String errMsg = cleanString(ctx.msg.getText());

    // Get the metadata for the last statement that was added.
    List<Operation> opList = currentOpQueueStack.peek();
    OperationMetadata opMetadata = opList.get(opList.size() - 1).metadata;

    ExpectedResult er = new FailureErrorCodeAndMsgExpectedResult(currentExpectedResultDesc, opMetadata, appContext, errCode, errMsg);
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
  public Void visitFailureErrorMsg(SethParser.FailureErrorMsgContext ctx)
  {
    visitChildren(ctx);

    String errMsg = cleanString(ctx.msg.getText());

    // Get the metadata for the last statement that was added.
    List<Operation> opList = currentOpQueueStack.peek();
    OperationMetadata opMetadata = opList.get(opList.size() - 1).metadata;

    ExpectedResult er = new FailureErrorMsgExpectedResult(currentExpectedResultDesc, opMetadata, appContext, errMsg);
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
   * Wraps the PlanningException in an unchecked SethBrownBagException.
   * @param e The exception to wrap.
   * @return The wrapped, unchecked, SethBrownBagException.
   */
  private SethBrownBagException wrapException(PlanningException e)
  {
    return new SethBrownBagException(e);
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

      // Stop if we hit a carriage return. We don't want to include comments on the line before.
      if (token.getType() == SethLexer.WS &&
          token.getText().contains("\n")) {
        return tokenStream.get(index + 1);
      }

      --index;
    }

    return tokenStream.get(0);
  }
}
