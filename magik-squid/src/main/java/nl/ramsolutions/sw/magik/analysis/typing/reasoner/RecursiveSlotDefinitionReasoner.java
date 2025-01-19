package nl.ramsolutions.sw.magik.analysis.typing.reasoner;

import com.sonar.sslr.api.AstNode;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.SlotDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.SlotUsage;
import nl.ramsolutions.sw.magik.analysis.helpers.MethodDefinitionNodeHelper;
import nl.ramsolutions.sw.magik.analysis.typing.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.SlotUsageLocator;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import nl.ramsolutions.sw.magik.api.MagikGrammar;

/**
 * Reasoner for reasoning the type of a {@link SlotDefinition} by recursively reasoning the assigned
 * types.
 */
public class RecursiveSlotDefinitionReasoner {

  // TODO: Reason with slot methods.

  private final IDefinitionKeeper definitionKeeper;
  private final int maxDepth;

  /**
   * Constructor.
   *
   * @param maxDepth the maximum depth to reason.
   */
  public RecursiveSlotDefinitionReasoner(
      final IDefinitionKeeper definitionKeeper, final int maxDepth) {
    this.definitionKeeper = definitionKeeper;
    this.maxDepth = maxDepth;
  }

  /**
   * Reason the {@link SlotDefinition} to finnd the slot type, recursively following any used
   * methods,
   *
   * <p>The {@link SlotDefinition}s in the {@link IDefinitionKeeper} will be replaced in case the
   * {@link SlotDefinition} types were not UNDEFINED and could be reasoned from the current code.
   *
   * @param slotDefinition The {@link SlotDefinition} to reason.
   * @return True if the {@link SlotDefinition} type could be fully reasoned, false otherwise.
   */
  public boolean reason(final SlotDefinition slotDefinition) {
    // TODO: Also get slot type from def_slotted_exemplar.

    if (!slotDefinition.getTypeName().containsUndefined()) {
      // Already reasoned this SlotDefinition, or known from TypeDoc.
      return true;
    }

    final Location location = slotDefinition.getLocation();
    if (location == null) {
      // Don't have source code to reason from.
      return false;
    }

    // Find all SlotUsages in all files.
    // TODO: We only need the methods where the slot is assigned.
    final List<Map.Entry<SlotUsage, MagikTypedFile>> slotUsagesPre =
        this.getSlotUsages(slotDefinition);

    // Get MethodDefinitions from the SlotUsages, and resason all those methods.
    this.getMethodDefinitionsFromSlotUsages(slotUsagesPre).forEach(this::reasonMethodDefinition);

    // TODO: Extract the assigned types to the slot.
    final List<Map.Entry<SlotUsage, MagikTypedFile>> slotUsagesPost =
        this.getSlotUsages(slotDefinition);
    final TypeString updatedSlotTypeStr = this.extractAssginedTypes(slotUsagesPost);
    // A bit of UNDEFINED is allowed.
    if (!updatedSlotTypeStr.isUndefined()) {
      this.updateSlotDefinitionType(slotDefinition, updatedSlotTypeStr);
      return true;
    }

    return false;
  }

  private TypeString extractAssginedTypes(
      final List<Map.Entry<SlotUsage, MagikTypedFile>> slotUsages) {
    return slotUsages.stream()
        .map(
            entry -> {
              final SlotUsage slotUsage = entry.getKey();
              final AstNode slotUsageNode = slotUsage.getNode();
              Objects.requireNonNull(slotUsageNode);

              // Get METHOD_DEFINITION nodes.
              final AstNode methodDefinitionNode =
                  slotUsageNode.getFirstAncestor(MagikGrammar.METHOD_DEFINITION);
              Objects.requireNonNull(methodDefinitionNode);

              // Get reasoner state.
              final MagikTypedFile magikFile = entry.getValue();
              final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();

              // Find all assignments for this slot, and extract the type.
              // TODO: AUGMENTED_ASSIGNMENT_EXPRESSION
              // TODO: MULTIPLE_ASSIGNMENT_STATEMENT
              return methodDefinitionNode
                  .getDescendants(MagikGrammar.ASSIGNMENT_EXPRESSION)
                  .stream()
                  .map(
                      assignmentNode -> {
                        // Test if the slot node is being assigned.
                        final AstNode rightNode = assignmentNode.getLastChild();
                        final List<AstNode> assignedNodes =
                            assignmentNode.getChildren(MagikGrammar.values());
                        assignedNodes.remove(rightNode);
                        if (assignedNodes.contains(slotUsageNode)) {
                          return null;
                        }

                        // Only need to get the LHS of the assignment,
                        // as the reasoner has stored the type at the node.
                        final ExpressionResultString result =
                            reasonerState.getNodeType(slotUsageNode);
                        return result.get(0, TypeString.UNDEFINED);
                      })
                  .filter(Objects::nonNull);
            })
        .flatMap(s -> s)
        .reduce(TypeString::combine)
        .orElse(TypeString.UNDEFINED);
  }

  private void reasonMethodDefinition(MethodDefinition methodDef) {
    final RecursiveMethodDefinitionReasoner recursiveReasoner =
        new RecursiveMethodDefinitionReasoner(this.definitionKeeper, this.maxDepth);
    recursiveReasoner.reason(methodDef);
  }

  private Stream<MethodDefinition> getMethodDefinitionsFromSlotUsages(
      final List<Map.Entry<SlotUsage, MagikTypedFile>> slotUsages) {
    return slotUsages.stream()
        .flatMap(
            entry -> {
              final SlotUsage slotUsage = entry.getKey();
              final AstNode slotUsageNode = slotUsage.getNode();
              Objects.requireNonNull(slotUsageNode);

              // Get METHOD_DEFINITION nodes.
              final AstNode methodDefinitionNode =
                  slotUsageNode.getFirstAncestor(MagikGrammar.METHOD_DEFINITION);
              Objects.requireNonNull(methodDefinitionNode);

              // Convert to MethodDefinition.
              final MagikTypedFile magikFile = entry.getValue();
              final MethodDefinitionNodeHelper helper =
                  new MethodDefinitionNodeHelper(methodDefinitionNode);
              final Location methodDefLocation = helper.getLocation();
              return magikFile.getDefinitions().stream()
                  .filter(MethodDefinition.class::isInstance)
                  .map(MethodDefinition.class::cast)
                  .filter(methodDef -> methodDef.getLocation().equals(methodDefLocation));
            });
  }

  private List<Map.Entry<SlotUsage, MagikTypedFile>> getSlotUsages(
      final SlotDefinition slotDefinition) {
    final SlotUsageLocator slotUsageLocator = new SlotUsageLocator(this.definitionKeeper);
    final TypeString slotTypeStr = slotDefinition.getTypeName();
    final String slotName = slotDefinition.getName();
    final SlotUsage wantedSlotUsage = new SlotUsage(slotTypeStr, slotName);
    return slotUsageLocator.getSlotUsages(wantedSlotUsage);
  }

  private void updateSlotDefinitionType(
      final SlotDefinition slotDefinition, final TypeString typeStr) {
    final SlotDefinition updatedSlotDefinition =
        new SlotDefinition(
            slotDefinition.getLocation(),
            slotDefinition.getTimestamp(),
            slotDefinition.getModuleName(),
            slotDefinition.getDoc(),
            null,
            slotDefinition.getOwnerTypeName(),
            slotDefinition.getName(),
            typeStr);

    // Save the new SlotDefinition.
    this.definitionKeeper.remove(slotDefinition);
    this.definitionKeeper.add(updatedSlotDefinition);
  }
}
