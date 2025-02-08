package nl.ramsolutions.sw.magik.analysis.typing.reasoner;

import com.sonar.sslr.api.AstNode;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.Position;
import nl.ramsolutions.sw.magik.analysis.AstQuery;
import nl.ramsolutions.sw.magik.analysis.definitions.ExemplarDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.SlotDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.SlotUsage;
import nl.ramsolutions.sw.magik.analysis.helpers.MethodDefinitionNodeHelper;
import nl.ramsolutions.sw.magik.analysis.helpers.SimpleVectorNodeHelper;
import nl.ramsolutions.sw.magik.analysis.typing.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.SlotUsageLocator;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import nl.ramsolutions.sw.magik.analysis.typing.TypeStringResolver;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reasoner for reasoning the type of a {@link SlotDefinition} by recursively reasoning the assigned
 * types.
 */
public class RecursiveSlotDefinitionReasoner extends AbstractRecursiveReasoner {

  // TODO: Reason with slot methods? Use the ParameterRecursiveReasoner to get the assigned types.
  //       Or a more generic RecursiveNodeReasoner? Do we also need a dispatcher for this?

  private static final Logger LOGGER =
      LoggerFactory.getLogger(RecursiveSlotDefinitionReasoner.class);

  /**
   * Constructor.
   *
   * @param definitionKeeper The definition keeper to use.
   * @param maxDepth The maximum depth to reason.
   */
  public RecursiveSlotDefinitionReasoner(
      final IDefinitionKeeper definitionKeeper, final int maxDepth) {
    super(definitionKeeper, maxDepth);
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
    LOGGER.debug("Reasoning slot definition: {}", slotDefinition);

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

    // Get slot type from def_slotted_exemplar.
    final TypeString defSlotTypeStr = this.reasonDefaultSlotValueType(slotDefinition);

    // Find all SlotUsages in all files.
    // TODO: We only need the methods where the slot is assigned.
    final List<Map.Entry<SlotUsage, MagikTypedFile>> slotUsagesPre =
        this.getSlotUsages(slotDefinition);

    // Get MethodDefinitions from the SlotUsages, and resason all those methods.
    this.getMethodDefinitionsFromSlotUsages(slotUsagesPre).forEach(this::reasonMethodDefinition);

    // Extract the assigned types to the slot.
    final List<Map.Entry<SlotUsage, MagikTypedFile>> slotUsagesPost =
        this.getSlotUsages(slotDefinition);
    final TypeString updatedSlotTypeStr;
    if (!slotUsagesPost.isEmpty()) {
      final TypeString assignedSlotTypeStr = this.extractAssginedTypes(slotUsagesPost);
      updatedSlotTypeStr = TypeString.combine(defSlotTypeStr, assignedSlotTypeStr);
    } else {
      updatedSlotTypeStr = defSlotTypeStr;
    }

    // A bit of UNDEFINED is allowed.
    if (!updatedSlotTypeStr.isUndefined()) {
      this.updateSlotDefinitionType(slotDefinition, updatedSlotTypeStr);
      return true;
    }

    return false;
  }

  private TypeString reasonDefaultSlotValueType(final SlotDefinition slotDefinition) {
    // Get second part of the slot type.
    final AstNode slotDefNode = slotDefinition.getNode();
    final SimpleVectorNodeHelper simpleVectorHelper =
        SimpleVectorNodeHelper.fromExpressionSafe(slotDefNode);
    if (simpleVectorHelper == null) {
      return TypeString.UNDEFINED;
    }

    final AstNode secondPartNode = simpleVectorHelper.getNth(1, MagikGrammar.values());
    if (secondPartNode == null) {
      // Safety first.
      return TypeString.UNDEFINED;
    }

    // Get type from (newly) reasoned file.
    final URI uri = secondPartNode.getToken().getURI();
    final Location location = new Location(uri, secondPartNode);
    final MagikTypedFile magikFile = this.getMagikFile(location);
    final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();
    final AstNode magikFileNode = magikFile.getTopNode();
    final Position position = location.getRange().getStartPosition();
    final AstNode magikFileWantedTokenNode = AstQuery.nodeAt(magikFileNode, position);
    final AstNode magikFileAtomNode = magikFileWantedTokenNode.getFirstAncestor(MagikGrammar.ATOM);
    final ExpressionResultString result = reasonerState.getNodeTypeSilent(magikFileAtomNode);
    if (result == null || result == ExpressionResultString.UNDEFINED) {
      return TypeString.UNDEFINED;
    }

    return result.get(0, TypeString.UNDEFINED);
  }

  @CheckForNull
  private TypeString extractAssginedTypes(
      final List<Map.Entry<SlotUsage, MagikTypedFile>> slotUsages) {
    return slotUsages.stream()
        .map(
            entry -> {
              final SlotUsage slotUsage = entry.getKey();
              final AstNode slotUsageNode = slotUsage.getNode();
              Objects.requireNonNull(slotUsageNode);
              final AstNode atomSlotUsageNode = slotUsageNode.getParent();

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
                        // Test if the slot node is actually being assigned, not used.
                        // TODO: Move this to AssignmentExpressionNodeHelper.
                        final AstNode rhsNode = assignmentNode.getLastChild();
                        final List<AstNode> lhsNodes =
                            assignmentNode.getChildren(MagikGrammar.values());
                        lhsNodes.remove(rhsNode);
                        if (!lhsNodes.contains(atomSlotUsageNode)) {
                          return null;
                        }

                        // Get the ASSIGNMENT_EXPRESSION node,
                        // as the reasoner has stored the type at this node.
                        // TODO: Is this ASSIGNMENT_EXPRESSION node for the same file?
                        final ExpressionResultString result =
                            reasonerState.getNodeType(assignmentNode);
                        return result.get(0, TypeString.UNDEFINED);
                      })
                  .filter(Objects::nonNull);
            })
        .flatMap(Function.identity())
        .reduce(TypeString::combine)
        .orElse(null);
  }

  private void reasonMethodDefinition(final MethodDefinition methodDef) {
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
    final TypeString slotTypeStr = slotDefinition.getOwnerTypeName();
    final String slotName = slotDefinition.getName();
    final SlotUsage wantedSlotUsage = new SlotUsage(slotTypeStr, slotName);
    return slotUsageLocator.getSlotUsages(wantedSlotUsage);
  }

  private void updateSlotDefinitionType(
      final SlotDefinition slotDefinition, final TypeString newSlotTypeStr) {
    // Find ExemplarDefinition for the slot.
    final TypeStringResolver resolver = new TypeStringResolver(this.definitionKeeper);
    final TypeString ownerTypeStr = slotDefinition.getOwnerTypeName();
    final ExemplarDefinition exemplarDefinition = resolver.getExemplarDefinition(ownerTypeStr);
    Objects.requireNonNull(exemplarDefinition);

    // Create a copy of the ExemplarDefinition, with our updated SlotDefinition.
    final SlotDefinition updatedSlotDefinition =
        new SlotDefinition(
            slotDefinition.getLocation(),
            slotDefinition.getTimestamp(),
            slotDefinition.getModuleName(),
            slotDefinition.getDoc(),
            slotDefinition.getNode(),
            slotDefinition.getOwnerTypeName(),
            slotDefinition.getName(),
            newSlotTypeStr);
    final List<SlotDefinition> updatedSlots =
        exemplarDefinition.getSlots().stream()
            .map(
                slotDef -> {
                  if (slotDef.equals(slotDefinition)) {
                    return updatedSlotDefinition;
                  }
                  return slotDef;
                })
            .toList();
    final ExemplarDefinition updatedExemplarDefinition =
        new ExemplarDefinition(
            exemplarDefinition.getLocation(),
            exemplarDefinition.getTimestamp(),
            exemplarDefinition.getModuleName(),
            exemplarDefinition.getDoc(),
            exemplarDefinition.getNode(),
            exemplarDefinition.getSort(),
            exemplarDefinition.getTypeString(),
            updatedSlots,
            exemplarDefinition.getParents(),
            exemplarDefinition.getTopics());

    // Save the new ExemplarDefinition.
    this.definitionKeeper.remove(exemplarDefinition);
    this.definitionKeeper.add(updatedExemplarDefinition);
  }
}
