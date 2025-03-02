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
import nl.ramsolutions.sw.magik.analysis.helpers.AssignmentExpressionNodeHelper;
import nl.ramsolutions.sw.magik.analysis.helpers.MethodInvocationNodeHelper;
import nl.ramsolutions.sw.magik.analysis.typing.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.reasoner.LocalTypeReasonerState;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class MethodDefinitionReasoner extends AbstractDefinitionReasoner {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodDefinitionReasoner.class);
  private static final String DEFINE_SHARED_CONSTANT = "define_shared_constant()";

  ExpressionResultString nodeReturnTypes;
  ExpressionResultString nodeIterTypes;

  MethodDefinitionReasoner(
      final IDefinitionKeeper definitionKeeper, final MethodDefinition originalDefinition) {
    super(definitionKeeper, originalDefinition);
  }

  @Override
  Collection<MagikDefinition> getUsedDefinitions() {
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
    final AstNode methodDefinitionNode = fileMethodDefinition.getNode();
    final AstNode rootNode = this.findRootNode(methodDefinitionNode);
    final Collection<MagikDefinition> usedDefinitions =
        extractor.extractUsedDefinitions(magikFile, rootNode);

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
    final AstNode rootNode = this.findRootNode(definitionNode);

    // Reason over the file and get all used MagikDefinitions.
    final LocalTypeReasonerState state = magikFile.getTypeReasonerState();
    final ExpressionResultString result = state.getNodeType(rootNode);
    final ExpressionResultString iterResult = state.getNodeIterType(rootNode);

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
      if (!slotDefinition.getTypeName().containsUndefined()) {
        // Type is known, no need to reason further.
        return false;
      }

      // In the original definition, test if this slot is used or assigned to.
      final MethodDefinition methodDefinition = (MethodDefinition) this.originalDefinition;
      final String slotName = slotDefinition.getName();
      return methodDefinition.getUsedSlots().stream()
          .filter(slotUsage -> slotUsage.getSlotName().equals(slotName))
          .anyMatch(
              slotUsage -> {
                final AstNode slotUsageNode = slotUsage.getNode();
                final AstNode atomNode = slotUsageNode.getParent();
                final AstNode assignmentExpressionNode =
                    atomNode.getFirstAncestor(MagikGrammar.ASSIGNMENT_EXPRESSION);
                final AssignmentExpressionNodeHelper helper =
                    new AssignmentExpressionNodeHelper(assignmentExpressionNode);
                return !helper.isAssignedTo(atomNode);
              });
    } else if (testedDefinition instanceof ExemplarDefinition) {
      return false;
    }

    throw new IllegalArgumentException();
  }

  @Override
  void updateDefinition(final MagikDefinition updatedDefinition) {
    if (!(updatedDefinition instanceof MethodDefinition)) {
      throw new IllegalArgumentException("Definition is not a MethodDefinition.");
    }

    // Remove the old MethodDefinition.
    final MethodDefinition methodDefinition = (MethodDefinition) this.originalDefinition;
    this.definitionKeeper.remove(methodDefinition);

    // Save the new MethodDefinition.
    this.definitionKeeper.add(updatedDefinition);
  }

  private AstNode findRootNode(final AstNode node) {
    if (node.is(MagikGrammar.METHOD_DEFINITION, MagikGrammar.PROCEDURE_DEFINITION)) {
      return node;
    }

    // Try EXPRESSION.
    final AstNode expressionNode = node.getFirstAncestor(MagikGrammar.EXPRESSION);
    if (expressionNode != null) {
      return expressionNode;
    }

    // Try define_shared_constant().
    final AstNode methodInvocationNode = node.getFirstDescendant(MagikGrammar.METHOD_INVOCATION);
    if (node.is(MagikGrammar.STATEMENT) && methodInvocationNode != null) {
      final MethodInvocationNodeHelper helper =
          new MethodInvocationNodeHelper(methodInvocationNode);
      if (helper.isMethodInvocationOf(DEFINE_SHARED_CONSTANT)) {
        return node.getFirstDescendant(MagikGrammar.EXPRESSION);
      }
    }

    throw new IllegalStateException();
  }
}
