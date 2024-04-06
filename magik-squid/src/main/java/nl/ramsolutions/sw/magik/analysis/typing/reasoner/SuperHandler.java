package nl.ramsolutions.sw.magik.analysis.typing.reasoner;

import com.sonar.sslr.api.AstNode;
import java.util.Collection;
import nl.ramsolutions.sw.magik.analysis.definitions.ExemplarDefinition;
import nl.ramsolutions.sw.magik.analysis.typing.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Super handler. */
class SuperHandler extends LocalTypeReasonerHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(SuperHandler.class);

  /**
   * Constructor.
   *
   * @param state Reasoner state.
   */
  SuperHandler(final LocalTypeReasonerState state) {
    super(state);
  }

  /**
   * Handle super.
   *
   * @param node
   */
  void handleSuper(final AstNode node) {
    // Determine which type we are.
    final TypeString methodOwnerTypeStr = this.getMethodOwnerType(node);
    if (methodOwnerTypeStr == TypeString.UNDEFINED) {
      LOGGER.debug("Unknown type for node: {}", node);
      return;
    }

    // Find specified super, if given.
    final AstNode identifierNode = node.getFirstChild(MagikGrammar.IDENTIFIER);
    final String identifier = identifierNode != null ? identifierNode.getTokenValue() : null;
    final TypeString superTypeStr;
    if (identifier != null) {
      final String pakkage = methodOwnerTypeStr.getPakkage();
      final TypeString typeStr = TypeString.ofIdentifier(identifier, pakkage);
      final ExemplarDefinition exemplarDefinition =
          this.typeResolver.getExemplarDefinition(typeStr);
      superTypeStr = exemplarDefinition != null ? exemplarDefinition.getTypeString() : null;
    } else {
      final Collection<TypeString> parents = this.typeResolver.getParents(methodOwnerTypeStr);
      superTypeStr = parents.stream().reduce(TypeString::combine).orElse(null);
    }

    if (superTypeStr == null) {
      return;
    }

    // TODO: Add all generics from this type?

    final ExpressionResultString result = new ExpressionResultString(superTypeStr);
    this.assignAtom(node, result);
  }
}
