package nl.ramsolutions.sw.magik.languageserver.hover;

import com.sonar.sslr.api.AstNode;
import java.util.Optional;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.analysis.typing.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import nl.ramsolutions.sw.magik.analysis.typing.reasoner.LocalTypeReasonerState;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Provides hover for the identifier of a procedure invocation. */
public class ProcedureInvocationHoverModule implements HoverModule<MagikTypedFile> {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(ProcedureInvocationHoverModule.class);

  @Override
  public Optional<String> tryHover(final HoverContext<MagikTypedFile> context) {
    final AstNode hoveredNode = context.getHoveredNode();
    if (!hoveredNode.is(MagikGrammar.IDENTIFIER)) {
      return Optional.empty();
    }

    final AstNode providingNode = hoveredNode.getParent();
    if (providingNode == null) {
      return Optional.empty();
    }

    final AstNode nextSiblingNode = providingNode.getNextSibling();
    if (nextSiblingNode == null || !nextSiblingNode.is(MagikGrammar.PROCEDURE_INVOCATION)) {
      return Optional.empty();
    }

    final MagikTypedFile magikFile = context.file();
    final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();
    final ExpressionResultString result = reasonerState.getNodeType(providingNode);
    final TypeString typeStr = result.get(0, null);
    final StringBuilder builder = new StringBuilder();
    if (typeStr != null) {
      LOGGER.debug("Providing hover for node: {}", providingNode.getTokenValue()); // NOSONAR
      HoverDocBuilder.buildProcDoc(magikFile, providingNode, builder);
    }
    return Optional.of(builder.toString());
  }
}
