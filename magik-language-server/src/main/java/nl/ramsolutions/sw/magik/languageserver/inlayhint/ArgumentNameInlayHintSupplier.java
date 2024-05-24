package nl.ramsolutions.sw.magik.languageserver.inlayhint;

import com.sonar.sslr.api.AstNode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;
import nl.ramsolutions.sw.MagikToolsProperties;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.Position;
import nl.ramsolutions.sw.magik.Range;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.ParameterDefinition;
import nl.ramsolutions.sw.magik.analysis.helpers.MethodInvocationNodeHelper;
import nl.ramsolutions.sw.magik.analysis.typing.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import nl.ramsolutions.sw.magik.analysis.typing.TypeStringResolver;
import nl.ramsolutions.sw.magik.analysis.typing.reasoner.LocalTypeReasonerState;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.languageserver.Lsp4jConversion;
import nl.ramsolutions.sw.magik.languageserver.MagikLanguageServerSettings;
import org.eclipse.lsp4j.InlayHint;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

/** Method invocation argument name {@link InlayHint} provider. */
class ArgumentNameInlayHintSupplier {

  private final MagikToolsProperties properties;

  ArgumentNameInlayHintSupplier(final MagikToolsProperties properties) {
    this.properties = properties;
  }

  /**
   * Get method invocation argument {@link InlayHint}s.
   *
   * @param magikFile Magik file.
   * @param range Range to get {@link InlayHint}s for.
   * @return {@link InlayHint}s.
   */
  Stream<InlayHint> getArgumentNameInlayHints(final MagikTypedFile magikFile, final Range range) {
    final MagikLanguageServerSettings settings = new MagikLanguageServerSettings(this.properties);
    if (!settings.getTypingShowArgumentInlayHints()) {
      return Stream.empty();
    }

    final AstNode topNode = magikFile.getTopNode();
    return topNode.getDescendants(MagikGrammar.METHOD_INVOCATION).stream()
        .filter(node -> Range.fromTree(node).overlapsWith(range))
        .flatMap(node -> this.getInlayHintsForMethodInvocationNode(magikFile, node));
  }

  private Stream<InlayHint> getInlayHintsForMethodInvocationNode(
      final MagikTypedFile magikFile, final AstNode methodInvocationNode) {
    final AstNode argumentsNode = methodInvocationNode.getFirstDescendant(MagikGrammar.ARGUMENTS);
    if (argumentsNode == null) {
      return Stream.of();
    }

    // Get type from method invocation.
    final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();
    final AstNode previousSiblingNode = methodInvocationNode.getPreviousSibling();
    final ExpressionResultString result = reasonerState.getNodeType(previousSiblingNode);
    final TypeString typeStr = result.get(0, TypeString.UNDEFINED);
    if (typeStr == TypeString.UNDEFINED) {
      return Stream.of();
    }

    // Get invoked method.
    final MethodInvocationNodeHelper helper = new MethodInvocationNodeHelper(methodInvocationNode);
    final String methodName = helper.getMethodName();
    final TypeStringResolver resolver = magikFile.getTypeStringResolver();
    final Collection<MethodDefinition> methodDefinitions =
        resolver.getMethodDefinitions(typeStr, methodName);
    final MethodDefinition methodDefinition = methodDefinitions.stream().findAny().orElse(null);
    if (methodDefinition == null) {
      return Stream.of();
    }

    // Get argument hints.
    final List<ParameterDefinition> parameterDefs = methodDefinition.getParameters();
    final List<InlayHint> inlayHints = new ArrayList<>();
    final List<AstNode> argumentNodes = argumentsNode.getChildren(MagikGrammar.ARGUMENT);
    for (int i = 0; i < argumentNodes.size(); ++i) {
      final AstNode argumentNode = argumentNodes.get(i);
      if (!this.isSimpleAtomArgument(argumentNode) || i >= parameterDefs.size()) {
        continue;
      }

      final ParameterDefinition parameterDef = parameterDefs.get(i);
      final InlayHint inlayHint = this.getArgumentInlayHint(argumentNode, parameterDef);
      inlayHints.add(inlayHint);
    }

    return inlayHints.stream();
  }

  private boolean isSimpleAtomArgument(final AstNode argumentNode) {
    final AstNode expressionNode = argumentNode.getFirstDescendant(MagikGrammar.EXPRESSION);
    if (expressionNode == null) {
      return false;
    }

    final AstNode atomNode = expressionNode.getFirstChild(MagikGrammar.ATOM);
    if (atomNode == null) {
      return false;
    }

    return atomNode.getFirstChild(
            MagikGrammar.UNSET,
            MagikGrammar.TRUE,
            MagikGrammar.FALSE,
            MagikGrammar.MAYBE,
            MagikGrammar.NUMBER,
            MagikGrammar.SYMBOL,
            MagikGrammar.STRING)
        != null;
  }

  private InlayHint getArgumentInlayHint(
      final AstNode argumentNode, final ParameterDefinition parameterDef) {
    final AstNode atomNode = argumentNode.getFirstDescendant(MagikGrammar.ATOM);
    final Position position = Position.fromTokenStart(atomNode.getToken());
    return new InlayHint(
        Lsp4jConversion.positionToLsp4j(position), Either.forLeft(parameterDef.getName() + ":"));
  }
}
