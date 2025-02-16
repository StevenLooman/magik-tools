package nl.ramsolutions.sw.magik.analysis.typing.reasoner.recursive;

import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.MagikDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.SlotDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Recursive reasoner for a specific node. */
public class RecursiveDefinitionReasoner {

  // TODO: We need a class which reasons about the type of a node,
  // and its iter type if applicable.
  // Then, it should return the things that it is missing to determine the type.
  // TODO: How do we know what to reason?
  // A MethodDefinition is about recursing any methods/slots.
  // - And define_shared_constant(),
  // - And define_shared_variable().
  // A SlotDefinition is about finding all slot assignments.
  // - And slot access methods.
  // A BinaryOperatorDefinition is about finding the definition/proc.
  // A GlobalDefinition is about finding the definition.
  // TODO: Do we want a LocalUsage? Perhaps we can use that the Scope things.

  private static final Logger LOGGER = LoggerFactory.getLogger(RecursiveDefinitionReasoner.class);

  private final IDefinitionKeeper definitionKeeper;
  private final int maxDepth;

  /**
   * Constructor.
   *
   * @param definitionKeeper The definition keeper to use.
   * @param maxDepth The maximum depth to reason.
   */
  public RecursiveDefinitionReasoner(final IDefinitionKeeper definitionKeeper, final int maxDepth) {
    this.definitionKeeper = definitionKeeper;
    this.maxDepth = maxDepth;
  }

  /**
   * Reason the {@link MagikDefinition}, following used {@link MagikDefinitions} recursively.
   *
   * @param definition The {@link MagikDefinition} to reason.
   * @return True if the {@link MagikDefinition} was succesfully reasoned.
   */
  public boolean reason(final MagikDefinition definition) {
    final AbstractDefinitionReasoner reasoner = this.getReasoner(definition);
    if (!reasoner.isReasonable()) {
      return false;
    }

    // Get all used definitions, and see if no one is required anymore.
    final Collection<MagikDefinition> requiredDefinitions = this.reason(reasoner, 0);
    return requiredDefinitions.isEmpty();
  }

  /**
   * Do a single pass of reasoning for the {@link MagikDefinition}.
   *
   * @param originalDefinition The {@link MagikDefinition} to reason.
   * @param depth The current depth of reasoning.
   * @return The {@link MagikDefinition}s that are required but unreasoned.
   */
  private Collection<MagikDefinition> reason(
      final AbstractDefinitionReasoner reasoner, final int depth) {
    final MagikDefinition definition = reasoner.getOriginalDefinition();
    LOGGER.warn("Reasoning definition: {}, depth: {}", definition, depth);

    if (depth > this.maxDepth) {
      LOGGER.warn("Max depth reached for definition: {}, depth: {}", definition, depth);
      return Collections.emptySet();
    }

    Collection<MagikDefinition> lastRequiredDefinitions = Collections.emptySet();
    Collection<MagikDefinition> requiredDefinitions = Collections.singleton(definition);
    // TODO: Enfore a max number of loops?
    // TODO: This needs to be a more sophisticated loop, as we should be able to stop once we know
    //       the returning types. But not always! As sometimes we need more internals...
    //       So... update only after completing this loop?
    // Reason as long as we're seeing no more improvements.
    while (!lastRequiredDefinitions.equals(requiredDefinitions)) {
      lastRequiredDefinitions = requiredDefinitions;

      final Collection<MagikDefinition> usedDefinitions = reasoner.process();
      requiredDefinitions =
          usedDefinitions.stream()
              .filter(reasoner::needsFurtherReasoning)
              .collect(Collectors.toList());

      // Recurse over the required definitions.
      requiredDefinitions.forEach(requiredDefinition -> this.recurse(requiredDefinition, depth));
    }

    // Let the reasoner determine if the definition is reasoned sufficiently.
    // TODO: This is a bit weird. First we get the updated definition, then we check if we need
    //       further reasoning. But this should all be done in the Reasoner itself.
    //       Rename this to... updateAndCheckIfFurtherReasoning?
    final MagikDefinition updatedDefinition = reasoner.getUpdatedDefinition();
    if (!reasoner.needsFurtherReasoning(updatedDefinition)) {
      // Update the definition.
      LOGGER.debug("Reasoned definition: {}", updatedDefinition);
      reasoner.updateDefinition(updatedDefinition);

      return Collections.emptySet();
    }

    return requiredDefinitions;
  }

  private AbstractDefinitionReasoner getReasoner(final MagikDefinition definition) {
    if (definition instanceof final MethodDefinition methodDefinition) {
      return new MethodDefinitionReasoner(this.definitionKeeper, methodDefinition);
    } else if (definition instanceof final SlotDefinition slotDefinition) {
      return new SlotDefinitionReasoner(this.definitionKeeper, slotDefinition);
    }

    throw new IllegalStateException();
  }

  private void recurse(final MagikDefinition definition, final int depth) {
    if (definition instanceof final MethodDefinition methodDefinition) {
      final MethodDefinitionReasoner reasoner =
          new MethodDefinitionReasoner(this.definitionKeeper, methodDefinition);
      this.reason(reasoner, depth + 1);
    } else if (definition instanceof final SlotDefinition slotDefinition) {
      final SlotDefinitionReasoner reasoner =
          new SlotDefinitionReasoner(this.definitionKeeper, slotDefinition);
      this.reason(reasoner, depth + 1);
    } else {
      throw new IllegalStateException();
    }
  }
}
