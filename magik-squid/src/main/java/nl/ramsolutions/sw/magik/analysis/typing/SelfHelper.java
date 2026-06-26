package nl.ramsolutions.sw.magik.analysis.typing;

import com.sonar.sslr.api.AstNode;
import nl.ramsolutions.sw.magik.analysis.helpers.MethodDefinitionNodeHelper;
import nl.ramsolutions.sw.magik.api.MagikGrammar;

/** `_self` helper. */
public final class SelfHelper {

  private SelfHelper() {}

  /**
   * Resolve `_self`, if {@link TypeString} is self. Otherwise return the {@link TypeString}.
   *
   * @param typeStr {@link TypeString} to resolve.
   * @param node Node to use when resolving `_self`. This must be a node in the method/procedure
   *     definition.
   * @return Resolved {@link TypeString}.
   */
  public static TypeString substituteSelf(final TypeString typeStr, final AstNode node) {
    if (typeStr.isSelf() || typeStr.isPrivate()) {
      final AstNode definitionNode =
          node.getFirstAncestor(MagikGrammar.METHOD_DEFINITION, MagikGrammar.PROCEDURE_DEFINITION);
      if (definitionNode == null) {
        return typeStr;
      } else if (definitionNode.is(MagikGrammar.METHOD_DEFINITION)) {
        final MethodDefinitionNodeHelper definitionHelper =
            new MethodDefinitionNodeHelper(definitionNode);
        return definitionHelper.getExemplarTypeString();
      } else {
        return TypeString.SW_PROCEDURE;
      }
    }

    return typeStr;
  }

  /**
   * Resolve `_self` for all typer in {@link ExpressionResultString}.
   *
   * @param expressionResultString {@link ExpressionResultString} to resolve.
   * @param node Node to use when resolving `_self`. This must be a node in the method/procedure
   *     definition.
   * @return Resolved {@link ExpressionResultString}.
   */
  public static ExpressionResultString substituteSelf(
      final ExpressionResultString expressionResultString, final AstNode node) {
    return expressionResultString.stream()
        .map(typeStr -> SelfHelper.substituteSelf(typeStr, node))
        .collect(ExpressionResultString.COLLECTOR);
  }
}
