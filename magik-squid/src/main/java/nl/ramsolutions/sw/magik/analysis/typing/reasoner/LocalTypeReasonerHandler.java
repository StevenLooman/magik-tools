package nl.ramsolutions.sw.magik.analysis.typing.reasoner;

import com.sonar.sslr.api.AstNode;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.helpers.MethodDefinitionNodeHelper;
import nl.ramsolutions.sw.magik.analysis.helpers.PackageNodeHelper;
import nl.ramsolutions.sw.magik.analysis.scope.GlobalScope;
import nl.ramsolutions.sw.magik.analysis.typing.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import nl.ramsolutions.sw.magik.analysis.typing.TypeStringResolver;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
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
    return this.typeResolver.getMethodDefinitions(calledType, methodName).stream()
        .map(methodDefinition -> methodDefinition.getReturnTypes())
        .reduce((result, element) -> new ExpressionResultString(result, element))
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
    return helper.getTypeString();
  }

  protected String getCurrentPackage(final AstNode node) {
    final PackageNodeHelper helper = new PackageNodeHelper(node);
    return helper.getCurrentPackage();
  }

  protected GlobalScope getGlobalScope() {
    return this.state.getMagikFile().getGlobalScope();
  }
}
