package nl.ramsolutions.sw.magik.analysis.typing.reasoner.recursive;

import com.sonar.sslr.api.AstNode;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import nl.ramsolutions.sw.Usage;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.analysis.MagikAstWalker;
import nl.ramsolutions.sw.magik.analysis.definitions.BinaryOperatorUsage;
import nl.ramsolutions.sw.magik.analysis.definitions.GlobalUsage;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodUsage;
import nl.ramsolutions.sw.magik.analysis.definitions.SlotUsage;
import nl.ramsolutions.sw.magik.analysis.helpers.BinaryExpressionNodeHelper;
import nl.ramsolutions.sw.magik.analysis.helpers.MethodInvocationNodeHelper;
import nl.ramsolutions.sw.magik.analysis.helpers.UnaryExpressionNodeHelper;
import nl.ramsolutions.sw.magik.analysis.scope.GlobalScope;
import nl.ramsolutions.sw.magik.analysis.scope.Scope;
import nl.ramsolutions.sw.magik.analysis.scope.ScopeEntry;
import nl.ramsolutions.sw.magik.analysis.typing.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.SelfHelper;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import nl.ramsolutions.sw.magik.analysis.typing.reasoner.LocalTypeReasonerState;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.api.MagikKeyword;
import nl.ramsolutions.sw.magik.api.MagikOperator;

/** Get the usages from the AST, with filled typing formation. */
final class UsageExtractingAstWalker extends MagikAstWalker {

  private static final Map<String, String> UNARY_OPERATOR_METHODS =
      Map.of(
          MagikOperator.NOT.getValue(), "not",
          MagikKeyword.NOT.getValue(), "not",
          MagikOperator.MINUS.getValue(), "negated",
          MagikOperator.PLUS.getValue(), "unary_plus",
          MagikKeyword.SCATTER.getValue(), "for_scatter()");

  private final MagikTypedFile magikFile;
  private final Collection<Usage> usages = new HashSet<>();

  /**
   * Constructor.
   *
   * @param definitionKeeper {@link IDefinitionKeeper} to get definitions from.
   */
  public UsageExtractingAstWalker(final MagikTypedFile magikFile) {
    this.magikFile = magikFile;
  }

  public Collection<Usage> getUsedDefinitions(final AstNode node) {
    // TODO: This implicitly runs a LocalTypeReasoner over this.magikFile. Is this the place to do
    // this?
    this.walkAst(node);
    return Collections.unmodifiableCollection(this.usages);
  }

  @Override
  protected void walkPostSlot(final AstNode node) {
    final LocalTypeReasonerState state = this.magikFile.getTypeReasonerState();

    // Get surrounding METHOD_DEFINITION node.
    final AstNode methodDefinitionNode = node.getFirstAncestor(MagikGrammar.METHOD_DEFINITION);
    final AstNode exemplarNameNode =
        methodDefinitionNode.getFirstDescendant(MagikGrammar.EXEMPLAR_NAME);
    final ExpressionResultString exemplarNameResult = state.getNodeType(exemplarNameNode);
    final TypeString exemplarRef = exemplarNameResult.get(0, TypeString.UNDEFINED);
    final String slotName = node.getFirstChild(MagikGrammar.IDENTIFIER).getTokenValue();

    // TODO: Should this be resolved?

    final Location location = new Location(node);
    final SlotUsage usage = new SlotUsage(exemplarRef, slotName, location, node);
    this.usages.add(usage);
  }

  @Override
  protected void walkPostIdentifier(final AstNode node) {
    // Ensure this is a global identifier.
    final AstNode parent = node.getParent();
    if (!parent.is(MagikGrammar.ATOM)) {
      return;
    }

    final GlobalScope globalScope = this.magikFile.getGlobalScope();
    final Scope scope = globalScope.getScopeForNode(node);
    if (scope == null) {
      return;
    }

    final ScopeEntry scopeEntry = scope.getScopeEntry(node);
    if (scopeEntry == null || !scopeEntry.isType(ScopeEntry.Type.GLOBAL)) {
      return;
    }

    // Get type and store the usage.
    final LocalTypeReasonerState state = this.magikFile.getTypeReasonerState();
    final ExpressionResultString result = state.getNodeType(node);
    final TypeString ref = result.get(0, TypeString.UNDEFINED);
    final Location location = new Location(node);
    final GlobalUsage usage = new GlobalUsage(ref, location, node);
    this.usages.add(usage);
  }

  @Override
  protected void walkPostMethodInvocation(final AstNode node) {
    final LocalTypeReasonerState state = this.magikFile.getTypeReasonerState();
    final MethodInvocationNodeHelper helper = new MethodInvocationNodeHelper(node);
    final AstNode receiverNode = helper.getReceiverNode();
    final ExpressionResultString result = state.getNodeTypeSilent(receiverNode);
    final TypeString ref = result.get(0, TypeString.UNDEFINED);
    final TypeString nonSelfRef = SelfHelper.substituteSelf(ref, receiverNode);
    final String methodName = helper.getMethodName();
    final Location location = new Location(node);
    // TODO: Replace self with actual type?
    final MethodUsage usage = new MethodUsage(nonSelfRef, methodName, location, node);
    this.usages.add(usage);
  }

  @Override
  protected void walkPostUnaryExpression(final AstNode node) {
    if (node.hasDirectChildren(MagikKeyword.ALLRESULTS)) {
      return;
    }

    final LocalTypeReasonerState state = this.magikFile.getTypeReasonerState();
    final UnaryExpressionNodeHelper helper = new UnaryExpressionNodeHelper(node);
    final String operator = helper.getOperator();
    final String methodName = UsageExtractingAstWalker.UNARY_OPERATOR_METHODS.get(operator);
    final AstNode receiverNode = helper.getReceiverNode();
    final ExpressionResultString result = state.getNodeIterType(receiverNode);
    final TypeString ref = result.get(0, TypeString.UNDEFINED);
    final Location location = new Location(node);
    final MethodUsage usage = new MethodUsage(ref, methodName, location, node);
    this.usages.add(usage);
  }

  @Override
  protected void walkPostOrExpression(final AstNode node) {
    this.handleBinaryExpression(node);
  }

  @Override
  protected void walkPostXorExpression(final AstNode node) {
    this.handleBinaryExpression(node);
  }

  @Override
  protected void walkPostAndExpression(final AstNode node) {
    this.handleBinaryExpression(node);
  }

  @Override
  protected void walkPostEqualityExpression(final AstNode node) {
    this.handleBinaryExpression(node);
  }

  @Override
  protected void walkPostRelationalExpression(final AstNode node) {
    this.handleBinaryExpression(node);
  }

  @Override
  protected void walkPostAdditiveExpression(final AstNode node) {
    this.handleBinaryExpression(node);
  }

  @Override
  protected void walkPostMultiplicativeExpression(final AstNode node) {
    this.handleBinaryExpression(node);
  }

  @Override
  protected void walkPostExponentialExpression(final AstNode node) {
    this.handleBinaryExpression(node);
  }

  private void handleBinaryExpression(final AstNode node) {
    final LocalTypeReasonerState state = this.magikFile.getTypeReasonerState();
    final BinaryExpressionNodeHelper helper = new BinaryExpressionNodeHelper(node);
    // Start with start node, take its node value. Then take each previous operator node value.
    helper.getTriplets().stream()
        .forEach(
            triplet -> {
              final AstNode prevNode = triplet.getLeft(); // LHS or previous operator node.
              final AstNode operatorNode = triplet.getMiddle();
              final String operator = operatorNode.getTokenValue().toLowerCase();
              final AstNode rhsNode = triplet.getRight();

              final ExpressionResultString prevResult = state.getNodeType(prevNode);
              final TypeString prevRef = prevResult.get(0, TypeString.UNDEFINED);
              final ExpressionResultString rhsResult = state.getNodeType(rhsNode);
              final TypeString rhsRef = rhsResult.get(0, TypeString.UNDEFINED);
              final Location location = new Location(node);
              final Location validLocation = Location.validLocation(location);
              final BinaryOperatorUsage usage =
                  new BinaryOperatorUsage(prevRef, rhsRef, operator, validLocation, node);
              this.usages.add(usage);
            });
  }

  @Override
  protected void walkPostUnset(AstNode node) {
    final Location location = new Location(node);
    final GlobalUsage usage = new GlobalUsage(TypeString.SW_UNSET, location, node);
    this.usages.add(usage);
  }

  // TODO: Other types...
}
