package nl.ramsolutions.sw.magik.analysis.typing.reasoner.recursive;

import com.sonar.sslr.api.AstNode;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.Position;
import nl.ramsolutions.sw.magik.analysis.AstQuery;
import nl.ramsolutions.sw.magik.analysis.definitions.ExemplarDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.MagikDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.SlotDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.SlotUsage;
import nl.ramsolutions.sw.magik.analysis.helpers.MethodDefinitionNodeHelper;
import nl.ramsolutions.sw.magik.analysis.helpers.SimpleVectorNodeHelper;
import nl.ramsolutions.sw.magik.analysis.typing.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.SlotUsageLocator;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import nl.ramsolutions.sw.magik.analysis.typing.TypeStringResolver;
import nl.ramsolutions.sw.magik.analysis.typing.reasoner.LocalTypeReasonerState;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class SlotDefinitionReasoner extends AbstractDefinitionReasoner {

  private static final Logger LOGGER = LoggerFactory.getLogger(SlotDefinitionReasoner.class);

  SlotDefinitionReasoner(
      final IDefinitionKeeper definitionKeeper, final SlotDefinition originalDefinition) {
    super(definitionKeeper, originalDefinition);
  }

  @Override
  Collection<MagikDefinition> getUsedDefinitions() {
    LOGGER.debug("Processing slot definition: {}", this.originalDefinition);

    // Get used definitions for the default value in the def_slotted_exemplar().
    final Map.Entry<TypeString, Collection<MagikDefinition>> defaultValueEntry =
        this.reasonDefaultValueType();
    final Collection<MagikDefinition> defaultValueUsedDefinitions = defaultValueEntry.getValue();

    // Get used definitions assigning the slot.
    final Map.Entry<TypeString, Collection<MagikDefinition>> assignedSlotEntry =
        this.reasonSlotAssignments();
    final Collection<MagikDefinition> assignedSlotDefinitions = assignedSlotEntry.getValue();

    return Stream.concat(defaultValueUsedDefinitions.stream(), assignedSlotDefinitions.stream())
        .collect(Collectors.toSet());
  }

  @Override
  MagikDefinition getUpdatedDefinition() {
    final Map.Entry<TypeString, Collection<MagikDefinition>> defaultValueEntry =
        this.reasonDefaultValueType();
    final TypeString defaultType = defaultValueEntry.getKey();

    final Map.Entry<TypeString, Collection<MagikDefinition>> assignedSlotEntry =
        this.reasonSlotAssignments();
    final TypeString assignedType = assignedSlotEntry.getKey();

    final TypeString newTypeStr =
        assignedSlotEntry.getValue().isEmpty()
            ? defaultType // Never assigned, so use only default value type.
            : TypeString.combine(defaultType, assignedType);
    final SlotDefinition slotDefinition = (SlotDefinition) this.originalDefinition;
    return new SlotDefinition(
        slotDefinition.getLocation(),
        slotDefinition.getTimestamp(),
        slotDefinition.getModuleName(),
        slotDefinition.getDoc(),
        slotDefinition.getNode(),
        slotDefinition.getOwnerTypeName(),
        slotDefinition.getName(),
        newTypeStr);
  }

  @Override
  void updateDefinition(final MagikDefinition updatedDefinition) {
    if (!(updatedDefinition instanceof SlotDefinition)) {
      throw new IllegalArgumentException("Definition is not a SlotDefinition.");
    }

    // Find ExemplarDefinition for the slot.
    final TypeStringResolver resolver = new TypeStringResolver(this.definitionKeeper);
    final SlotDefinition slotDefinition = (SlotDefinition) this.originalDefinition;
    final TypeString ownerTypeStr = slotDefinition.getOwnerTypeName();
    final ExemplarDefinition exemplarDefinition = resolver.getExemplarDefinition(ownerTypeStr);
    Objects.requireNonNull(exemplarDefinition);

    // Ensure this is the correct ExemplarDefinition and the slot is known.
    final String slotName = slotDefinition.getName();
    final Location location = slotDefinition.getLocation();
    exemplarDefinition.getSlots().stream()
        .filter(slotDef -> slotDef.getName().equals(slotName))
        .filter(slotDef -> slotDef.getLocation().equals(location))
        .findAny()
        .orElseThrow();

    // Create a copy of the ExemplarDefinition, with our updated SlotDefinition.
    final SlotDefinition updatedSlotDefinition = (SlotDefinition) this.getUpdatedDefinition();
    final List<SlotDefinition> updatedSlots =
        exemplarDefinition.getSlots().stream()
            .map(
                slotDef -> {
                  if (slotDef.getName().equals(slotDefinition.getName())
                      && slotDef.getLocation().equals(slotDefinition.getLocation())) {
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

  @Override
  boolean needsFurtherReasoning(final MagikDefinition testedDefinition) {
    if (testedDefinition instanceof final SlotDefinition slotDefinition) {
      return slotDefinition.getTypeName() == TypeString.UNDEFINED;
    } else if (testedDefinition instanceof final MethodDefinition methodDefinition) {
      // TODO: How about the assigned types?
      // Test if there is a (TODO: assignment) SlotUsage in the MethodDefinition.
      final SlotDefinition slotDefinition = (SlotDefinition) this.originalDefinition;
      final TypeString typeStr = slotDefinition.getOwnerTypeName();
      final String slotName = slotDefinition.getName();
      return methodDefinition.getUsedSlots().stream()
          .anyMatch(
              slotUsage ->
                  slotUsage.getSlotName().equals(slotName)
                      && slotUsage.getTypeName().equals(typeStr));
    } else if (testedDefinition instanceof ExemplarDefinition) {
      return false;
    }

    throw new IllegalArgumentException();
  }

  private Map.Entry<TypeString, Collection<MagikDefinition>> reasonDefaultValueType() {
    final SlotDefinition slotDefinition = (SlotDefinition) this.originalDefinition;

    final MagikTypedFile magikFile = this.getMagikFile();
    final String slotName = slotDefinition.getName();
    final Location slotLocation = slotDefinition.getLocation();
    final SlotDefinition fileSlotDefinition =
        magikFile.getDefinitions().stream()
            .filter(ExemplarDefinition.class::isInstance)
            .map(ExemplarDefinition.class::cast)
            .flatMap(exemplarDefinition -> exemplarDefinition.getSlots().stream())
            .filter(SlotDefinition.class::isInstance)
            .map(SlotDefinition.class::cast)
            .filter(def -> def.getName().equals(slotName))
            .filter(def -> def.getLocation().equals(slotLocation))
            .findFirst()
            .orElseThrow();

    // Get second part of the slot type.
    final AstNode slotDefNode = fileSlotDefinition.getNode();
    final SimpleVectorNodeHelper simpleVectorHelper =
        SimpleVectorNodeHelper.fromExpressionSafe(slotDefNode);
    if (simpleVectorHelper == null) {
      // Safety first.
      return Map.entry(TypeString.UNDEFINED, Collections.emptySet());
    }

    final AstNode secondPartNode = simpleVectorHelper.getNth(1);
    if (secondPartNode == null) {
      // Safety first.
      return Map.entry(TypeString.UNDEFINED, Collections.emptySet());
    }

    // Get the reasoned type.
    final LocalTypeReasonerState state = magikFile.getTypeReasonerState();
    final ExpressionResultString result = state.getNodeType(secondPartNode);
    final TypeString defaultValueType = result.get(0, TypeString.UNDEFINED);

    // Get used definitions for the default value.
    final AstNode magikFileNode = magikFile.getTopNode();
    final Position position = new Location(secondPartNode).getRange().getStartPosition();
    final AstNode magikFileWantedTokenNode = AstQuery.nodeAt(magikFileNode, position);
    final AstNode magikFileAtomNode = magikFileWantedTokenNode.getFirstAncestor(MagikGrammar.ATOM);
    final UsedDefinitionExtractor extractor = new UsedDefinitionExtractor(this.definitionKeeper);
    final Collection<MagikDefinition> usedDefinitions =
        extractor.extractUsedDefinitions(magikFile, magikFileAtomNode);

    return Map.entry(defaultValueType, usedDefinitions);
  }

  private Map.Entry<TypeString, Collection<MagikDefinition>> reasonSlotAssignments() {
    // Get the type assigned.
    final List<Entry<SlotUsage, MagikTypedFile>> slotUsages = this.getSlotUsages();
    final TypeString assignedType = this.extractAssginedTypes(slotUsages);

    // Get the used definitions.
    // TODO: What about `.slot1 << .slot2`?
    final Collection<MagikDefinition> usedDefinitions =
        slotUsages.stream().map(this::getDefinitionFromSlotUsage).collect(Collectors.toSet());

    return Map.entry(assignedType, usedDefinitions);
  }

  private TypeString extractAssginedTypes(
      final List<Map.Entry<SlotUsage, MagikTypedFile>> slotUsages) {
    // TODO: Rewrite this perhaps...
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
        .flatMap(stream -> stream)
        .reduce(TypeString::combine)
        .orElse(TypeString.UNDEFINED);
  }

  private List<Map.Entry<SlotUsage, MagikTypedFile>> getSlotUsages() {
    final SlotUsageLocator slotUsageLocator = new SlotUsageLocator(this.definitionKeeper);
    final TypeString slotTypeStr = ((SlotDefinition) this.originalDefinition).getOwnerTypeName();
    final String slotName = this.originalDefinition.getName();
    final SlotUsage wantedSlotUsage = new SlotUsage(slotTypeStr, slotName);
    return slotUsageLocator.getSlotUsages(wantedSlotUsage);
  }

  private MethodDefinition getDefinitionFromSlotUsage(
      final Map.Entry<SlotUsage, MagikTypedFile> entry) {
    final SlotUsage slotUsage = entry.getKey();
    final AstNode slotUsageNode = slotUsage.getNode();
    Objects.requireNonNull(slotUsageNode);

    // Get METHOD_DEFINITION nodes.
    final AstNode methodDefinitionNode =
        slotUsageNode.getFirstAncestor(MagikGrammar.METHOD_DEFINITION);
    Objects.requireNonNull(methodDefinitionNode);

    // Convert to MethodDefinition.
    final MagikTypedFile magikFile = entry.getValue();
    final MethodDefinitionNodeHelper helper = new MethodDefinitionNodeHelper(methodDefinitionNode);
    final Location methodDefLocation = helper.getLocation();
    return magikFile.getDefinitions().stream()
        .filter(MethodDefinition.class::isInstance)
        .map(MethodDefinition.class::cast)
        .filter(methodDef -> methodDef.getLocation().equals(methodDefLocation))
        .findAny()
        .orElseThrow();
  }
}
