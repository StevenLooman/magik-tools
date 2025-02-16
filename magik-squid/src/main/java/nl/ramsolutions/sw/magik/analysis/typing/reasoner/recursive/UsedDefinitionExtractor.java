package nl.ramsolutions.sw.magik.analysis.typing.reasoner.recursive;

import com.sonar.sslr.api.AstNode;
import java.util.Collection;
import java.util.stream.Collectors;
import nl.ramsolutions.sw.Usage;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.analysis.definitions.BinaryOperatorUsage;
import nl.ramsolutions.sw.magik.analysis.definitions.GlobalUsage;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.MagikDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodUsage;
import nl.ramsolutions.sw.magik.analysis.definitions.SlotUsage;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import nl.ramsolutions.sw.magik.analysis.typing.TypeStringResolver;
import nl.ramsolutions.sw.magik.api.MagikGrammar;

/**
 * A class which reasons about the (iter) type of a node, then returns the definition it need to
 * further reason.
 */
public class UsedDefinitionExtractor {

  private final IDefinitionKeeper definitionKeeper;

  /**
   * Constructor.
   *
   * @param definitionKeeper The definition keeper.
   */
  public UsedDefinitionExtractor(final IDefinitionKeeper definitionKeeper) {
    this.definitionKeeper = definitionKeeper;
  }

  /** Reason about the type of a node. */
  public Collection<MagikDefinition> extractUsedDefinitions(
      final MagikTypedFile magikFile, final AstNode node) {
    // TODO: We already get the MagikTypedFile here. Do we need to store the IDefinitionKeeper
    //       in the constructor?
    // Find root node to get all used definitions from.
    final AstNode rootNode = this.findRootNode(node);

    // Get usages.
    final UsageExtractingAstWalker walker = new UsageExtractingAstWalker(magikFile);
    final Collection<Usage> usages = walker.getUsedDefinitions(rootNode);

    // Convert usages to definitions.
    return usages.stream()
        .map(usage -> this.getDefinitions(usage))
        .flatMap(Collection::stream)
        .collect(Collectors.toSet());
  }

  private AstNode findRootNode(final AstNode node) {
    if (node.is(MagikGrammar.METHOD_DEFINITION, MagikGrammar.PROCEDURE_DEFINITION)) {
      return node;
    }

    // Try METHOD_DEFINITION or PROCEDURE_DEFINITION.
    final AstNode methodOrProcDefNode =
        node.getFirstAncestor(MagikGrammar.METHOD_DEFINITION, MagikGrammar.PROCEDURE_DEFINITION);
    if (methodOrProcDefNode != null) {
      return methodOrProcDefNode;
    }

    // Try BODY.
    final AstNode bodyNode = node.getFirstAncestor(MagikGrammar.BODY);
    if (bodyNode != null) {
      return bodyNode.getParent();
    }

    // Try EXPRESSION.
    final AstNode expressionNode = node.getFirstAncestor(MagikGrammar.EXPRESSION);
    if (expressionNode != null) {
      return expressionNode;
    }

    throw new IllegalStateException();
  }

  private Collection<MagikDefinition> getDefinitions(final Usage usage) {
    final TypeStringResolver resolver = new TypeStringResolver(this.definitionKeeper);
    if (usage instanceof final SlotUsage slotUsage) {
      final TypeString ref = slotUsage.getTypeName();
      final String slotName = slotUsage.getSlotName();
      return resolver.getSlotDefinitions(ref, slotName).stream()
          .map(MagikDefinition.class::cast)
          .toList();
    } else if (usage instanceof final MethodUsage methodUsage) {
      final TypeString ref = methodUsage.getTypeName();
      final String methodName = methodUsage.getMethodName();
      return resolver.getRespondingMethodDefinitions(ref, methodName).stream()
          .map(MagikDefinition.class::cast)
          .toList();
    } else if (usage instanceof final BinaryOperatorUsage binaryOperatorUsage) {
      final TypeString lhsRef = binaryOperatorUsage.getLhsTypeName();
      final String operator = binaryOperatorUsage.getOperator();
      final TypeString rhsRef = binaryOperatorUsage.getRhsTypeName();
      return resolver.getBinaryOperatorDefinitions(lhsRef, operator, rhsRef);
    } else if (usage instanceof final GlobalUsage globalUsage) {
      final TypeString ref = globalUsage.getTypeName();
      return resolver.resolve(ref).stream().map(MagikDefinition.class::cast).toList();
      // } else if (usage instanceof final ProcedureUsage procedureUsage) {
      //   TODO: Use a method invocation `invoke()`, or create a procedure usage?
      //   final TypeString ref = procedureUsage.getTypeName();
      //   return this.definitionKeeper.getProcedureDefinition(ref);
    }

    throw new IllegalArgumentException();
  }
}
