package nl.ramsolutions.sw.magik.analysis.helpers;

import com.sonar.sslr.api.AstNode;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import nl.ramsolutions.sw.magik.MagikFile;
import nl.ramsolutions.sw.magik.analysis.definitions.ITypeStringDefinition;
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

    return resolver.resolve(originalTypeStr).stream()
        .map(ITypeStringDefinition::getTypeString)
        .findAny()
        .orElse(TypeString.UNDEFINED);
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

  private static AstNode getInlineProcedureNode(final AstNode invocationNode) {
    final AstNode calledNode = invocationNode.getPreviousSibling();
    if (calledNode == null) {
      return null;
    }

    if (calledNode.is(MagikGrammar.PROCEDURE_DEFINITION)) {
      return calledNode;
    }

    return calledNode.getFirstDescendant(MagikGrammar.PROCEDURE_DEFINITION);
  }
}
