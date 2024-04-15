package nl.ramsolutions.sw.magik.analysis.typing.reasoner;

import com.sonar.sslr.api.AstNode;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.analysis.MagikAstWalker;

/**
 * Reason over types in Magik code.
 *
 * <p>To determine result types from methods/procedures, all expressions are evaluated to determine
 * types of variables etc. This information is then used to determine the result type(s) of methods
 * and procedures. In turn, this information is stored and used for other methods/procedures.
 *
 * <p>If a type cannot be determined, the {@code UndefinedType} is used instead.
 *
 * <p>If {@code _self} or {@code _clone} is returned in a method, the {@code SelfType} is used. It
 * is up to the user to determine the real type. I.e., in case of {@code sw:date_time_mixin}, none
 * of the methods are called on that class directly, but always through an inherited class. On
 * declaration the inheriting classes are unknown, thus if {@code _self} is returned from a mixin,
 * we need to proxy the type.
 *
 * <p>Note that this class never writes to the {@link IDefinitionKeeper}.
 */
public class LocalTypeReasoner extends MagikAstWalker {

  private final LocalTypeReasonerState state;
  private final AssignmentHandler assignmentHandler;
  private final AtomHandler atomHandler;
  private final ConditionalBodyHandler conditionalBodyHandler;
  private final ExpressionHandler expressionHandler;
  private final IdentifierHandler identifierHandler;
  private final InvocationHandler invocationHandler;
  private final ParameterHandler parameterHandler;
  private final ProcedureDefinitionHandler procedureDefinitionHandler;
  private final StatementHandler statementHandler;
  private final SuperHandler superHandler;

  /**
   * Constructor.
   *
   * @param magikFile Magik file to reason on.
   */
  public LocalTypeReasoner(final MagikTypedFile magikFile) {
    this.state = new LocalTypeReasonerState(magikFile);

    this.assignmentHandler = new AssignmentHandler(this.state);
    this.atomHandler = new AtomHandler(this.state);
    this.conditionalBodyHandler = new ConditionalBodyHandler(this.state);
    this.expressionHandler = new ExpressionHandler(this.state);
    this.identifierHandler = new IdentifierHandler(this.state);
    this.invocationHandler = new InvocationHandler(this.state);
    this.parameterHandler = new ParameterHandler(this.state);
    this.procedureDefinitionHandler = new ProcedureDefinitionHandler(this.state);
    this.statementHandler = new StatementHandler(this.state);
    this.superHandler = new SuperHandler(this.state);
  }

  /**
   * Get the {@link LocalTypeReasonerState}.
   *
   * @return State.
   */
  public LocalTypeReasonerState getState() {
    return this.state;
  }

  /** Evaluate the given top {@link AstNode}. */
  public void run() {
    // Start walking.
    final AstNode topNode = this.state.getMagikFile().getTopNode();
    this.walkAst(topNode);
  }

  @Override
  protected void walkPostExemplarName(final AstNode node) {
    this.identifierHandler.handleExemplarName(node);
  }

  @Override
  protected void walkPostMethodDefinition(final AstNode node) {
    // TODO: Move this to somewhere else.
    this.expressionHandler.handleMethodDefinition(node);
  }

  @Override
  protected void walkPostBody(final AstNode node) {
    this.expressionHandler.handleBody(node);
  }

  @Override
  protected void walkPostConditionalExpression(final AstNode node) {
    this.conditionalBodyHandler.handleConditionalExpression(node);
  }

  @Override
  protected void walkPostIterableExpression(final AstNode node) {
    this.expressionHandler.handleIterableExpression(node);
  }

  @Override
  protected void walkPostParameter(final AstNode node) {
    this.parameterHandler.handleParameter(node);
  }

  // region: Atoms
  @Override
  protected void walkPostSlot(final AstNode node) {
    this.atomHandler.handleSlot(node);
  }

  @Override
  protected void walkPostIdentifier(final AstNode node) {
    this.identifierHandler.handleIdentifier(node);
  }

  @Override
  protected void walkPostNumber(final AstNode node) {
    this.atomHandler.handleNumber(node);
  }

  @Override
  protected void walkPostSelf(final AstNode node) {
    this.atomHandler.handleSelf(node);
  }

  @Override
  protected void walkPostClone(final AstNode node) {
    this.atomHandler.handleClone(node);
  }

  @Override
  protected void walkPostSuper(final AstNode node) {
    this.superHandler.handleSuper(node);
  }

  @Override
  protected void walkPostTrue(final AstNode node) {
    this.atomHandler.handleFalse(node); // True is of type sw:false.
  }

  @Override
  protected void walkPostFalse(final AstNode node) {
    this.atomHandler.handleFalse(node);
  }

  @Override
  protected void walkPostMaybe(final AstNode node) {
    this.atomHandler.handleMaybe(node);
  }

  @Override
  protected void walkPostUnset(final AstNode node) {
    this.atomHandler.handleUnset(node);
  }

  @Override
  protected void walkPostCharacter(final AstNode node) {
    this.atomHandler.handleCharacter(node);
  }

  @Override
  protected void walkPostRegexp(final AstNode node) {
    this.atomHandler.handleRegexp(node);
  }

  @Override
  protected void walkPostString(final AstNode node) {
    this.atomHandler.handleString(node);
  }

  @Override
  protected void walkPostSymbol(final AstNode node) {
    this.atomHandler.handleSymbol(node);
  }

  @Override
  protected void walkPostSimpleVector(final AstNode node) {
    this.atomHandler.handleSimpleVector(node);
  }

  @Override
  protected void walkPostGatherExpression(final AstNode node) {
    this.atomHandler.handleSimpleVector(node);
  }

  @Override
  protected void walkPostGlobalRef(final AstNode node) {
    this.atomHandler.handleGlobalRef(node);
  }

  @Override
  protected void walkPostThisthread(final AstNode node) {
    this.atomHandler.handleThread(node);
  }

  // endregion

  // region: Statements
  @Override
  protected void walkPostReturnStatement(final AstNode node) {
    this.statementHandler.handleReturn(node);
  }

  @Override
  protected void walkPostVariableDefinition(final AstNode node) {
    this.statementHandler.handleVariableDefinition(node);
  }

  @Override
  protected void walkPostVariableDefinitionMulti(final AstNode node) {
    this.statementHandler.handleVariableDefinitionMulti(node);
  }

  @Override
  protected void walkPostEmitStatement(final AstNode node) {
    this.statementHandler.handleEmit(node);
  }

  @Override
  protected void walkPostLeaveStatement(final AstNode node) {
    this.statementHandler.handleLeave(node);
  }

  @Override
  protected void walkPostLoopbody(final AstNode node) {
    this.expressionHandler.handleLoopbody(node);
  }

  @Override
  protected void walkPostProcedureDefinition(final AstNode node) {
    this.procedureDefinitionHandler.handleProcedureDefinition(node);
  }

  @Override
  protected void walkPostMultipleAssignmentStatement(final AstNode node) {
    this.assignmentHandler.handleMultipleAssignment(node);
  }

  @Override
  protected void walkPostTryVariable(final AstNode node) {
    this.identifierHandler.handleTryVariable(node);
  }

  // endregion

  // region: Expressions
  @Override
  protected void walkPostExpression(final AstNode node) {
    this.expressionHandler.handleExpression(node);
  }

  @Override
  protected void walkPostAssignmentExpression(final AstNode node) {
    this.assignmentHandler.handleAssignment(node);
  }

  @Override
  protected void walkPostAugmentedAssignmentExpression(final AstNode node) {
    this.expressionHandler.handleAugmentedAssignmentExpression(node);
  }

  @Override
  protected void walkPostTuple(final AstNode node) {
    this.expressionHandler.handleTuple(node);
  }

  // endregion

  // region: Operators
  @Override
  protected void walkPostOrExpression(final AstNode node) {
    this.applyBinaryOperator(node);
  }

  @Override
  protected void walkPostXorExpression(final AstNode node) {
    this.applyBinaryOperator(node);
  }

  @Override
  protected void walkPostAndExpression(final AstNode node) {
    this.applyBinaryOperator(node);
  }

  @Override
  protected void walkPostEqualityExpression(final AstNode node) {
    this.applyBinaryOperator(node);
  }

  @Override
  protected void walkPostRelationalExpression(final AstNode node) {
    this.applyBinaryOperator(node);
  }

  @Override
  protected void walkPostAdditiveExpression(final AstNode node) {
    this.applyBinaryOperator(node);
  }

  @Override
  protected void walkPostMultiplicativeExpression(final AstNode node) {
    this.applyBinaryOperator(node);
  }

  @Override
  protected void walkPostExponentialExpression(final AstNode node) {
    this.applyBinaryOperator(node);
  }

  private void applyBinaryOperator(final AstNode node) {
    this.expressionHandler.handleBinaryExpression(node);
  }

  @Override
  protected void walkPostUnaryExpression(final AstNode node) {
    this.expressionHandler.handleUnaryExpression(node);
  }

  // endregion

  @Override
  protected void walkPostPostfixExpression(final AstNode node) {
    this.expressionHandler.handlePostfixExpression(node);
  }

  @Override
  protected void walkPostMethodInvocation(final AstNode node) {
    this.invocationHandler.handleMethodInvocation(node);
  }

  @Override
  protected void walkPostProcedureInvocation(final AstNode node) {
    this.invocationHandler.handleProcedureInvocation(node);
  }
}
