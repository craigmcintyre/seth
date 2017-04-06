// Copyright (c) 2017 Boray Data Co. Ltd.  All rights reserved.

package com.rapidsdata.seth.plan;

import com.rapidsdata.seth.parser.SethBaseVisitor;
import com.rapidsdata.seth.parser.SethLexer;
import com.rapidsdata.seth.parser.SethParser;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

import java.io.File;
import java.util.Deque;
import java.util.LinkedList;

public class TestGenerator extends SethBaseVisitor
{
  /** The parser we're using to parse the test file. */
  private final Parser parser;

  /** The test file we're reading. */
  private final File testFile;

  /** A stack of plans. The current plan is always the head. */
  private final Deque<Plan> planStack = new LinkedList<>();

  /**
   * A stack of operation queues that go with the stack of plans above.
   * The head of this queue points to either the queue of test operations
   * or the queue of cleanup operations.
   */
  private Deque<Deque<Operation>> currentOpQueueStack = new LinkedList<>();

  /** A stack of metadata for the current operation as seen by the parser. */
  private Deque<OperationMetadata> opMetadataStack = new LinkedList<>();

  /**
   * Constructor.
   * @param parser The parser we're using to parse the test file.
   * @param testFile The test file we're reading.
   */
  public TestGenerator(SethParser parser, File testFile)
  {
    this.parser = parser;
    this.testFile = testFile;
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
    Deque<Operation> testOps    = new LinkedList<>();
    Deque<Operation> cleanupOps = new LinkedList<>();
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
    Token stopToken  = ctx.getStop();
    String statementText = tokenStream.getText(startToken, stopToken);

    int line = startToken.getLine();

    OperationMetadata opMetadata = new OperationMetadata(statementText, testFile, line);

    opMetadataStack.push(opMetadata);

    visitChildren(ctx);
    return null;
  }

  @Override
  public Void visitLogStatement(SethParser.LogStatementContext ctx)
  {
    visitChildren(ctx);

    String msg = ctx.logStr.getText();
    Operation op = new LogOperation(opMetadataStack.pop(), msg);
    currentOpQueueStack.peek().addLast(op);

    return null;
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
