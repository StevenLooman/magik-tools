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
    // Get usages.
    final UsageExtractingAstWalker walker = new UsageExtractingAstWalker(magikFile);
    final Collection<Usage> usages = walker.getUsedDefinitions(node);

    // Convert usages to definitions.
    return usages.stream()
        .map(this::getDefinitions)
        .flatMap(Collection::stream)
        .collect(Collectors.toSet());
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
