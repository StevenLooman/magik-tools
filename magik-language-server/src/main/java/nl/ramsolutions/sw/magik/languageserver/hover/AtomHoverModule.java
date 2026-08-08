package nl.ramsolutions.sw.magik.languageserver.hover;

import com.sonar.sslr.api.AstNode;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import java.util.Optional;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.analysis.typing.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.reasoner.LocalTypeReasonerState;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides hover for the type of an atom: an exemplar name, an atom or slot, a parameter, a
 * variable definition or a for-loop variable.
 */
public class AtomHoverModule implements HoverModule<MagikTypedFile> {

  private static final Logger LOGGER = LoggerFactory.getLogger(AtomHoverModule.class);

  @Override
  public Optional<String> tryHover(final HoverContext<MagikTypedFile> context) {
    final AstNode hoveredNode = context.getHoveredNode();
    final AstNode providingNode = this.getProvidingNode(hoveredNode);
    if (providingNode == null) {
      return Optional.empty();
    }

    final MagikTypedFile magikFile = context.file();
    final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();
    final ExpressionResultString result = reasonerState.getNodeTypeSilent(providingNode);
    final StringBuilder builder = new StringBuilder();
    if (result != null) {
      LOGGER.debug("Providing hover for node: {}", providingNode.getTokenValue()); // NOSONAR
      HoverDocBuilder.buildTypeDoc(magikFile, providingNode, builder);
    }
    return Optional.of(builder.toString());
  }

  @CheckForNull
  private AstNode getProvidingNode(final AstNode hoveredNode) {
    final AstNode parentNode = hoveredNode.getParent();
    if (parentNode == null) {
      return null;
    }

    final AstNode parentParentNode = parentNode.getParent();
    if (parentNode.is(MagikGrammar.EXEMPLAR_NAME)) {
      return parentNode;
    } else if (parentNode.is(MagikGrammar.ATOM) || parentNode.is(MagikGrammar.SLOT)) {
      return hoveredNode.getFirstAncestor(MagikGrammar.ATOM);
    } else if (parentNode.is(MagikGrammar.PARAMETER)
        || parentNode.is(MagikGrammar.VARIABLE_DEFINITION)
        || (parentParentNode != null && parentParentNode.is(MagikGrammar.FOR_VARIABLES))) {
      // The reasoner types the IDENTIFIER of a parameter, not the PARAMETER itself.
      return hoveredNode;
    }

    return null;
  }
}
