package nl.ramsolutions.sw.magik.analysis.typing;

import com.sonar.sslr.api.AstNode;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import java.util.List;
import nl.ramsolutions.sw.magik.analysis.AstQuery;
import nl.ramsolutions.sw.magik.analysis.helpers.MethodDefinitionNodeHelper;
import nl.ramsolutions.sw.magik.api.MagikGrammar;

/** `_self` helper. */
public final class SelfHelper {

  private SelfHelper() {}

  /**
   * Resolve `_self`, if {@link TypeString} is self or a union with a self member. Otherwise return
   * the {@link TypeString}.
   *
   * @param typeStr {@link TypeString} to resolve.
   * @param node Node to use when resolving `_self`. This must be a node in the method/procedure
   *     definition.
   * @return Resolved {@link TypeString}.
   */
  public static TypeString substituteSelf(final TypeString typeStr, final AstNode node) {
    if (typeStr.isCombined()) {
      return SelfHelper.substituteSelfMembers(typeStr, node);
    }

    if (typeStr.isSelf() || typeStr.isPrivate()) {
      final TypeString ownerTypeStr = SelfHelper.getSelfOwnerType(node);
      return ownerTypeStr != null ? ownerTypeStr : typeStr;
    }

    return typeStr;
  }

  /**
   * Get the type a symbolic {@code _self} stands for at a node: the exemplar of the nearest
   * enclosing method, or {@code sw:procedure} when the nearest enclosing definition is a proc.
   *
   * @param node Node within the method/procedure definition.
   * @return Type of the definition's owner, or null outside any definition.
   */
  @CheckForNull
  public static TypeString getSelfOwnerType(final AstNode node) {
    final AstNode definitionNode = AstQuery.getEnclosingCallableNode(node);
    if (definitionNode == null) {
      return null;
    }

    if (definitionNode.is(MagikGrammar.PROCEDURE_DEFINITION)) {
      return TypeString.SW_PROCEDURE;
    }

    final MethodDefinitionNodeHelper definitionHelper =
        new MethodDefinitionNodeHelper(definitionNode);
    return definitionHelper.getExemplarTypeString();
  }

  private static TypeString substituteSelfMembers(final TypeString typeStr, final AstNode node) {
    final List<TypeString> memberTypeStrs = typeStr.getCombinedTypes();
    final boolean hasSelfMember =
        memberTypeStrs.stream().anyMatch(member -> member.isSelf() || member.isPrivate());
    if (!hasSelfMember) {
      return typeStr;
    }

    final TypeString[] substitutedTypeStrs =
        memberTypeStrs.stream()
            .map(member -> SelfHelper.substituteSelf(member, node))
            .toArray(TypeString[]::new);
    return TypeString.combine(substitutedTypeStrs);
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
