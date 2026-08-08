package nl.ramsolutions.sw.magik.languageserver.hover;

import com.sonar.sslr.api.AstNode;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import java.util.Optional;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.analysis.AstQuery;
import nl.ramsolutions.sw.magik.analysis.helpers.MethodInvocationNodeHelper;
import nl.ramsolutions.sw.magik.analysis.typing.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.SelfHelper;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import nl.ramsolutions.sw.magik.analysis.typing.TypeStringResolver;
import nl.ramsolutions.sw.magik.analysis.typing.reasoner.LocalTypeReasonerState;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides hover for a method invocation: its method name, its arguments - including the brackets
 * of an indexed invocation such as {@code prop_list[:key]} - and its assignment operator.
 */
public class MethodInvocationHoverModule implements HoverModule<MagikTypedFile> {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodInvocationHoverModule.class);

  @Override
  public Optional<String> tryHover(final HoverContext<MagikTypedFile> context) {
    final AstNode hoveredNode = context.getHoveredNode();
    final AstNode invocationNode = this.getInvocationNode(hoveredNode);
    if (invocationNode == null) {
      return Optional.empty();
    }

    final MethodInvocationNodeHelper invocationHelper =
        new MethodInvocationNodeHelper(invocationNode);
    final AstNode receiverNode = invocationHelper.getReceiverNode();
    if (receiverNode == null) {
      return Optional.of("");
    }

    final String methodName = invocationHelper.getMethodName();
    LOGGER.debug(
        "Providing hover for node: {}, method: {}", receiverNode.getTokenValue(), methodName);

    final MagikTypedFile magikFile = context.file();
    final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();
    final ExpressionResultString result = reasonerState.getNodeType(receiverNode);
    final TypeString resultTypeStr = result.get(0, TypeString.UNDEFINED);
    final TypeString typeStr = SelfHelper.substituteSelf(resultTypeStr, hoveredNode);

    final TypeStringResolver resolver = magikFile.getTypeStringResolver();
    final StringBuilder builder = new StringBuilder();
    resolver
        .getRespondingMethodDefinitions(typeStr, methodName)
        .forEach(methodDef -> HoverDocBuilder.buildMethodSignatureDoc(methodDef, builder));
    return Optional.of(builder.toString());
  }

  /**
   * Get the {@link MagikGrammar#METHOD_INVOCATION} the hovered node is part of. An indexed
   * invocation has no {@link MagikGrammar#METHOD_NAME} node, so its brackets and assignment
   * operator are matched through the invocation node itself.
   *
   * @param hoveredNode Hovered node.
   * @return Method invocation node, or null if the hovered node is not part of one.
   */
  @CheckForNull
  private AstNode getInvocationNode(final AstNode hoveredNode) {
    if (hoveredNode.is(MagikGrammar.METHOD_INVOCATION)) {
      return hoveredNode;
    }

    final AstNode parentNode = hoveredNode.getParent();
    if (parentNode == null) {
      return null;
    }

    if (hoveredNode.is(MagikGrammar.ARGUMENTS) && parentNode.is(MagikGrammar.METHOD_INVOCATION)) {
      return parentNode;
    } else if (parentNode.is(MagikGrammar.METHOD_NAME)
        && AstQuery.parentIs(parentNode, MagikGrammar.METHOD_INVOCATION)) {
      return parentNode.getParent();
    }

    return null;
  }
}
