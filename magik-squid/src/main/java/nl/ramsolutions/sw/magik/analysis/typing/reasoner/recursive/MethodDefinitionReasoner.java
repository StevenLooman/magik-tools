package nl.ramsolutions.sw.magik.analysis.typing.reasoner.recursive;

import com.sonar.sslr.api.AstNode;
import java.util.Collection;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.analysis.definitions.ExemplarDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.MagikDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.SlotDefinition;
import nl.ramsolutions.sw.magik.analysis.typing.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import nl.ramsolutions.sw.magik.analysis.typing.reasoner.LocalTypeReasonerState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class MethodDefinitionReasoner extends AbstractDefinitionReasoner {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodDefinitionReasoner.class);

  ExpressionResultString nodeReturnTypes;
  ExpressionResultString nodeIterTypes;

  MethodDefinitionReasoner(
      final IDefinitionKeeper definitionKeeper, final MethodDefinition originalDefinition) {
    super(definitionKeeper, originalDefinition);
  }

  @Override
  Collection<MagikDefinition> process() {
    LOGGER.debug("Processing method definition: {}", this.originalDefinition);

    final Location location = this.originalDefinition.getLocation();
    final String methodName = this.originalDefinition.getName();

    // Get a fresh MagikTypedFile/MethodDefinition.
    final MagikTypedFile magikFile = this.getMagikFile();
    final MethodDefinition fileMethodDefinition =
        magikFile.getDefinitions().stream()
            .filter(MethodDefinition.class::isInstance)
            .map(MethodDefinition.class::cast)
            .filter(def -> def.getName().equals(methodName))
            .filter(def -> def.getLocation().equals(location))
            .findFirst()
            .orElseThrow();

    // Reason over the file and get all used MagikDefinitions.
    final UsedDefinitionExtractor extractor = new UsedDefinitionExtractor(this.definitionKeeper);
    final AstNode methodDefinitionNode1 = fileMethodDefinition.getNode();
    final Collection<MagikDefinition> usedDefinitions =
        extractor.extractUsedDefinitions(magikFile, methodDefinitionNode1);

    return usedDefinitions;
  }

  @Override
  MagikDefinition getUpdatedDefinition() {
    final Location location = this.originalDefinition.getLocation();
    final String methodName = this.originalDefinition.getName();

    // Get a fresh MagikTypedFile/MethodDefinition.
    final MagikTypedFile magikFile = this.getMagikFile();
    final MethodDefinition fileMethodDefinition =
        magikFile.getDefinitions().stream()
            .filter(MethodDefinition.class::isInstance)
            .map(MethodDefinition.class::cast)
            .filter(def -> def.getName().equals(methodName))
            .filter(def -> def.getLocation().equals(location))
            .findFirst()
            .orElseThrow();
    final AstNode definitionNode = fileMethodDefinition.getNode();

    // Reason over the file and get all used MagikDefinitions.
    final LocalTypeReasonerState state = magikFile.getTypeReasonerState();
    final ExpressionResultString result = state.getNodeType(definitionNode);
    final ExpressionResultString iterResult = state.getNodeIterType(definitionNode);

    final MethodDefinition methodDefinition = (MethodDefinition) this.originalDefinition;
    return new MethodDefinition(
        methodDefinition.getLocation(),
        methodDefinition.getTimestamp(),
        methodDefinition.getModuleName(),
        methodDefinition.getDoc(),
        null,
        methodDefinition.getTypeName(),
        methodDefinition.getMethodName(),
        methodDefinition.getModifiers(),
        methodDefinition.getParameters(),
        methodDefinition.getAssignmentParameter(),
        methodDefinition.getTopics(),
        result,
        iterResult,
        methodDefinition.getUsedGlobals(),
        methodDefinition.getUsedMethods(),
        methodDefinition.getUsedSlots(),
        methodDefinition.getUsedConditions(),
        methodDefinition.getUsedBinaryOperators());
  }

  @Override
  boolean needsFurtherReasoning(final MagikDefinition testedDefinition) {
    if (testedDefinition instanceof final MethodDefinition methodDefinition) {
      return methodDefinition.getReturnTypes() == ExpressionResultString.UNDEFINED
          || methodDefinition.getLoopTypes() == ExpressionResultString.UNDEFINED;
    } else if (testedDefinition instanceof final SlotDefinition slotDefinition) {
      return slotDefinition.getTypeName() == TypeString.UNDEFINED;
    } else if (testedDefinition instanceof ExemplarDefinition) {
      return false;
    }

    throw new IllegalArgumentException();
  }

  @Override
  void updateDefinition(final MagikDefinition updatedMethodDefinition) {
    if (!(updatedMethodDefinition instanceof MethodDefinition)) {
      throw new IllegalArgumentException("Definition is not a MethodDefinition.");
    }

    // Remove the old MethodDefinition.
    final MethodDefinition methodDefinition = (MethodDefinition) this.originalDefinition;
    this.definitionKeeper.remove(methodDefinition);

    // Save the new MethodDefinition.
    this.definitionKeeper.add(updatedMethodDefinition);
  }
}
