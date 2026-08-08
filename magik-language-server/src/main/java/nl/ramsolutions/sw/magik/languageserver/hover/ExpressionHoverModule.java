package nl.ramsolutions.sw.magik.languageserver.hover;

import com.sonar.sslr.api.AstNode;
import java.util.Optional;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.analysis.typing.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.reasoner.LocalTypeReasonerState;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Provides hover for the type of an expression, such as a binary operator expression. */
public class ExpressionHoverModule implements HoverModule<MagikTypedFile> {

  private static final Logger LOGGER = LoggerFactory.getLogger(ExpressionHoverModule.class);

  @Override
  public Optional<String> tryHover(final HoverContext<MagikTypedFile> context) {
    final AstNode hoveredNode = context.getHoveredNode();
    final AstNode expressionNode = hoveredNode.getParent();
    if (expressionNode == null || !expressionNode.is(MagikGrammar.EXPRESSION)) {
      return Optional.empty();
    }

    final MagikTypedFile magikFile = context.file();
    final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();
    final ExpressionResultString result = reasonerState.getNodeTypeSilent(expressionNode);
    final StringBuilder builder = new StringBuilder();
    if (result != null) {
      LOGGER.debug("Providing hover for node: {}", expressionNode.getTokenValue()); // NOSONAR
      HoverDocBuilder.buildTypeDoc(magikFile, expressionNode, builder);
    }
    return Optional.of(builder.toString());
  }
}
