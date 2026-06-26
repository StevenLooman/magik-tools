package nl.ramsolutions.sw.magik.analysis.typing.reasoner;

import com.sonar.sslr.api.AstNode;
import java.util.List;
import java.util.Objects;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodDefinition;
import nl.ramsolutions.sw.magik.analysis.helpers.MethodDefinitionNodeHelper;
import nl.ramsolutions.sw.magik.analysis.helpers.PackageNodeHelper;
import nl.ramsolutions.sw.magik.analysis.scope.GlobalScope;
import nl.ramsolutions.sw.magik.analysis.scope.Scope;
import nl.ramsolutions.sw.magik.analysis.scope.ScopeEntry;
import nl.ramsolutions.sw.magik.analysis.typing.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import nl.ramsolutions.sw.magik.analysis.typing.TypeStringResolver;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.api.MagikKeyword;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Base class for handlers. */
@SuppressWarnings("visibilitymodifier")
abstract class LocalTypeReasonerHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(LocalTypeReasonerHandler.class);

  protected final LocalTypeReasonerState state;
  protected final IDefinitionKeeper definitionKeeper;
  protected final TypeStringResolver typeResolver;

  /**
   * Constructor.
   *
   * @param state Reasoner state.
   */
  LocalTypeReasonerHandler(final LocalTypeReasonerState state) {
    this.state = state;

    this.definitionKeeper = state.getMagikFile().getDefinitionKeeper();
    this.typeResolver = new TypeStringResolver(this.definitionKeeper);
  }

  /**
   * Get the resulting {@link ExpressionResultString} from a method invocation.
   *
   * @param calledType Type method is invoked on.
   * @param methodName Name of method to invoke.
   * @return Result of invocation.
   */
  protected ExpressionResultString getMethodInvocationResult(
      final TypeString calledType, final String methodName) {
    // return calledType.getMethods(methodName).stream()
    return this.typeResolver.getRespondingMethodDefinitions(calledType, methodName).stream()
        .map(MethodDefinition::getReturnTypes)
        .reduce(ExpressionResultString::new)
        .orElse(ExpressionResultString.UNDEFINED);
  }

  protected void assignAtom(final AstNode node, final TypeString typeString) {
    final ExpressionResultString result = new ExpressionResultString(typeString);
    this.assignAtom(node, result);
  }

  /**
   * Assign result to an ATOM-child-node.
   *
   * @param node Node to assign result to.
   * @param result Result to assign.
   */
  protected void assignAtom(final AstNode node, final ExpressionResultString result) {
    final AstNode atomNode = node.getParent();
    if (this.state.hasNodeType(atomNode)) {
      final ExpressionResultString existingResult = this.state.getNodeType(atomNode);
      LOGGER.debug(
          "Atom node {} already has type: {}, overwriting with {}",
          atomNode,
          existingResult,
          result);
    }

    this.state.setNodeType(atomNode, result);
  }

  /**
   * Add a type for a {@link AstNode}. Combines type if a type is already known.
   *
   * @param node AstNode.
   * @param result ExpressionResultString.
   */
  protected void addNodeType(final AstNode node, final ExpressionResultString result) {
    // TODO: Can we record a link, where it came from? Add (a) source node(s) perhaps.
    if (this.state.hasNodeType(node)) {
      // Combine types.
      final ExpressionResultString existingResult = this.state.getNodeType(node);
      final ExpressionResultString combinedResult =
          new ExpressionResultString(existingResult, result);
      this.state.setNodeType(node, combinedResult);
    } else {
      this.state.setNodeType(node, result);
    }
  }

  protected void addNodeIterType(final AstNode node, final ExpressionResultString result) {
    if (this.state.hasNodeIterType(node)) {
      // Combine types.
      final ExpressionResultString existingResult = this.state.getNodeIterType(node);
      final ExpressionResultString combinedResult =
          new ExpressionResultString(existingResult, result);
      this.state.setNodeIterType(node, combinedResult);
    } else {
      this.state.setNodeIterType(node, result);
    }
  }

  /**
   * Collect return and iter types from descendant statements of a method/proc definition node.
   *
   * <p>Collects types from:
   *
   * <ul>
   *   <li>{@code RETURN_STATEMENT} descendants targeting this definition.
   *   <li>{@code EMIT_STATEMENT} descendants directly in this definition's body.
   *   <li>{@code LOOPBODY} descendants targeting this definition (as iter types).
   * </ul>
   *
   * @param definitionNode METHOD_DEFINITION or PROCEDURE_DEFINITION node.
   */
  protected void collectReturnAndIterTypes(final AstNode definitionNode) {
    final AstNode bodyNode = definitionNode.getFirstChild(MagikGrammar.BODY);
    if (bodyNode == null) {
      // No body (e.g., abstract method).
      this.state.setNodeType(definitionNode, ExpressionResultString.EMPTY);
      return;
    }

    // Collect return types from RETURN_STATEMENT descendants targeting this definition.
    definitionNode.getDescendants(MagikGrammar.RETURN_STATEMENT).stream()
        .filter(
            returnNode ->
                returnNode
                    .getFirstAncestor(
                        MagikGrammar.METHOD_DEFINITION, MagikGrammar.PROCEDURE_DEFINITION)
                    .equals(definitionNode))
        .map(this.state::getNodeType)
        .forEach(result -> this.addNodeType(definitionNode, result));

    // Collect emit types from EMIT_STATEMENT descendants directly in this definition's body.
    definitionNode.getDescendants(MagikGrammar.EMIT_STATEMENT).stream()
        .filter(emitNode -> emitNode.getFirstAncestor(MagikGrammar.BODY).equals(bodyNode))
        .map(this.state::getNodeType)
        .forEach(result -> this.addNodeType(definitionNode, result));

    // Set empty result if nothing was collected.
    if (!this.state.hasNodeType(definitionNode)) {
      this.state.setNodeType(definitionNode, ExpressionResultString.EMPTY);
    }

    // Collect iter types from LOOPBODY descendants targeting this definition.
    definitionNode.getDescendants(MagikGrammar.LOOPBODY).stream()
        .filter(
            loopbodyNode ->
                loopbodyNode
                    .getFirstAncestor(
                        MagikGrammar.METHOD_DEFINITION, MagikGrammar.PROCEDURE_DEFINITION)
                    .equals(definitionNode))
        .map(this.state::getNodeType)
        .forEach(result -> this.addNodeIterType(definitionNode, result));
  }

  /**
   * Get method owner type.
   *
   * @param node Node.
   * @return Method owner type.
   */
  protected TypeString getMethodOwnerType(final AstNode node) {
    final AstNode defNode =
        node.getFirstAncestor(MagikGrammar.PROCEDURE_DEFINITION, MagikGrammar.METHOD_DEFINITION);
    if (defNode == null) {
      // Lets try to be safe.
      return TypeString.UNDEFINED;
    } else if (defNode.is(MagikGrammar.PROCEDURE_DEFINITION)) {
      return TypeString.SW_PROCEDURE;
    }

    // Method definition.
    final MethodDefinitionNodeHelper helper = new MethodDefinitionNodeHelper(defNode);
    return helper.getExemplarTypeString();
  }

  /**
   * Assign types to identifiers with an optional gather clause. Also set the scope variables
   *
   * @param bodyNode Body node of the for loop.
   * @param identifiersNode Node containing the identifiers with gather optional clause.
   * @param result Result of the gather clause, which should be assigned to the identifiers.
   */
  protected void assignIdentifiersWithGatherTypes(
      final AstNode bodyNode, final AstNode identifiersNode, final ExpressionResultString result) {
    final List<AstNode> identifierNodes = identifiersNode.getDescendants(MagikGrammar.IDENTIFIER);
    // Materialize so a trailing variadic loop type (e.g. @loop {sw:integer...}) expands to
    // (inner | sw:unset) per position for the non-gather positions. The gather position, if
    // any, is overridden below to sw:simple_vector.
    final List<TypeString> positionTypes = result.materialize(identifierNodes.size());
    for (int i = 0; i < identifierNodes.size(); ++i) {
      final AstNode identifierNode = identifierNodes.get(i);
      final AstNode identifierPreviousNode = identifierNode.getPreviousSibling();
      final TypeString typeStr;
      if (identifierPreviousNode != null
          && identifierPreviousNode
              .getTokenValue()
              .equalsIgnoreCase(MagikKeyword.GATHER.getValue())) {
        // TODO: Template this based on the type of the iterable, if possible.
        typeStr = TypeString.SW_SIMPLE_VECTOR;
      } else {
        typeStr = positionTypes.get(i);
      }

      final ExpressionResultString resultStr = new ExpressionResultString(typeStr);
      this.state.setNodeType(identifierNode, resultStr);

      final GlobalScope globalScope = this.getGlobalScope();
      final Scope scope = globalScope.getScopeForNode(bodyNode);
      Objects.requireNonNull(scope);
      final ScopeEntry scopeEntry = scope.getScopeEntry(identifierNode);
      Objects.requireNonNull(scopeEntry);
      this.state.setCurrentScopeEntryNode(scopeEntry, identifierNode);
    }
  }

  protected String getCurrentPackage(final AstNode node) {
    final PackageNodeHelper helper = new PackageNodeHelper(node);
    return helper.getCurrentPackage();
  }

  protected GlobalScope getGlobalScope() {
    return this.state.getMagikFile().getGlobalScope();
  }
}
