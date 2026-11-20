package nl.ramsolutions.sw.magik.analysis.helpers;

import com.sonar.sslr.api.AstNode;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import nl.ramsolutions.sw.magik.MagikFile;
import nl.ramsolutions.sw.magik.analysis.definitions.ProcedureDefinition;
import nl.ramsolutions.sw.magik.analysis.typing.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import nl.ramsolutions.sw.magik.analysis.typing.TypeStringResolver;
import nl.ramsolutions.sw.magik.analysis.typing.reasoner.LocalTypeReasonerState;
import nl.ramsolutions.sw.magik.api.MagikGrammar;

/** Shared helper for procedure invocation definition resolution. */
public final class ProcedureInvocationDefinitionHelper {

  private ProcedureInvocationDefinitionHelper() {
    // Utility class.
  }

  public static TypeString getProcedureTypeInvokedOn(
      final AstNode invocationNode,
      final LocalTypeReasonerState reasonerState,
      final TypeStringResolver resolver) {
    final AstNode previousSibling = invocationNode.getPreviousSibling();
    if (previousSibling == null) {
      return TypeString.UNDEFINED;
    }

    final ExpressionResultString result = reasonerState.getNodeType(previousSibling);
    final TypeString originalTypeStr = result.get(0, TypeString.UNDEFINED);
    if (TypeString.SELF.equals(originalTypeStr)) {
      return TypeString.SW_PROCEDURE;
    }

    if (resolver.resolve(originalTypeStr).isEmpty()) {
      return TypeString.UNDEFINED;
    }

    return resolver.getResolvedTypeString(originalTypeStr);
  }

  public static Collection<ProcedureDefinition> getRespondingProcedureDefinitions(
      final AstNode invocationNode,
      final LocalTypeReasonerState reasonerState,
      final TypeStringResolver resolver,
      final MagikFile magikFile) {
    final AstNode previousSibling = invocationNode.getPreviousSibling();
    if (previousSibling == null) {
      return List.of();
    }

    final TypeString calledTypeStr =
        getProcedureTypeInvokedOn(invocationNode, reasonerState, resolver);
    final Collection<ProcedureDefinition> resolvedDefs =
        resolver.getRespondingProcedureDefinitions(calledTypeStr);
    if (!resolvedDefs.isEmpty()) {
      return resolvedDefs;
    }

    final AstNode inlineProcedureNode = getInlineProcedureNode(invocationNode);
    if (inlineProcedureNode == null) {
      return resolvedDefs;
    }

    return magikFile.getMagikDefinitions().stream()
        .filter(ProcedureDefinition.class::isInstance)
        .map(ProcedureDefinition.class::cast)
        .filter(def -> Objects.equals(def.getNode(), inlineProcedureNode))
        .toList();
  }

  /**
   * Get the procedure literal invoked directly, as in {@code _proc … _endproc()} or {@code (_proc …
   * _endproc)()}. Only parentheses around a single expression are looked through, so a literal
   * elsewhere in the called expression, such as an argument, is not the invoked procedure.
   *
   * @param invocationNode PROCEDURE_INVOCATION node.
   * @return The PROCEDURE_DEFINITION node, or {@code null}.
   */
  private static AstNode getInlineProcedureNode(final AstNode invocationNode) {
    AstNode calledNode = invocationNode.getPreviousSibling();
    while (calledNode != null && !calledNode.is(MagikGrammar.PROCEDURE_DEFINITION)) {
      calledNode = ProcedureInvocationDefinitionHelper.getWrappedNode(calledNode);
    }

    return calledNode;
  }

  /**
   * Get the node an ATOM or EXPRESSION passes through unchanged: the sole child, or the single
   * expression between parentheses.
   *
   * @param node Node to unwrap.
   * @return The wrapped node, or {@code null}.
   */
  private static AstNode getWrappedNode(final AstNode node) {
    if (!node.is(MagikGrammar.ATOM, MagikGrammar.EXPRESSION)) {
      return null;
    }

    if (node.getNumberOfChildren() == 1) {
      return node.getFirstChild();
    }

    final List<AstNode> expressionNodes = node.getChildren(MagikGrammar.EXPRESSION);
    if (!node.is(MagikGrammar.ATOM)
        || expressionNodes.size() != 1
        || node.getNumberOfChildren() != 3) {
      return null;
    }

    return expressionNodes.get(0);
  }
}
