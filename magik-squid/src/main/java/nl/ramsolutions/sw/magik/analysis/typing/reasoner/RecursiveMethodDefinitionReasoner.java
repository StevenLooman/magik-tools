package nl.ramsolutions.sw.magik.analysis.typing.reasoner;

import com.sonar.sslr.api.AstNode;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import nl.ramsolutions.sw.FileCharsetDeterminer;
import nl.ramsolutions.sw.MagikToolsProperties;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.analysis.MagikAnalysisSettings;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodUsage;
import nl.ramsolutions.sw.magik.analysis.helpers.MethodInvocationNodeHelper;
import nl.ramsolutions.sw.magik.analysis.typing.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.SelfHelper;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import nl.ramsolutions.sw.magik.analysis.typing.TypeStringResolver;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Recursive {@link MethodDefinition} return/iter type reasoner, recursively following any used
 * methods.
 */
public class RecursiveMethodDefinitionReasoner {

  // TODO: define_shared_contant reasoning.
  // TODO: define_shared_variable reasoning.

  private static final MagikToolsProperties MAGIK_FILE_PROPERTIES =
      new MagikToolsProperties(
          Map.of(
              MagikAnalysisSettings.INDEX_GLOBAL_USAGES, "true",
              MagikAnalysisSettings.INDEX_METHOD_USAGES, "true",
              MagikAnalysisSettings.INDEX_SLOT_USAGES, "true",
              MagikAnalysisSettings.INDEX_CONDITION_USAGES, "true"));

  private static final Logger LOGGER =
      LoggerFactory.getLogger(RecursiveMethodDefinitionReasoner.class);

  private final IDefinitionKeeper definitionKeeper;
  private final int maxDepth;

  /**
   * Constructor.
   *
   * @param maxDepth the maximum depth to reason.
   */
  public RecursiveMethodDefinitionReasoner(
      final IDefinitionKeeper definitionKeeper, final int maxDepth) {
    this.definitionKeeper = definitionKeeper;
    this.maxDepth = maxDepth;
  }

  /**
   * Reason the {@link MethodDefinition} to find the return and iter types, recursively following
   * any used methods, up to {@link maxDepth} depth.
   *
   * <p>The {@link MethodDefinition}s in the {@link IDefinitionKeeper} will be replaced in case the
   * {@link MethodDefinition} return types were not UNDEFINED and could be reasoned from the current
   * code.
   *
   * @param methodDefinition The {@link MethodDefinition} to reason.
   * @return True if the {@link MethodDefinition} return type and iter type are not undefined, false
   *     otherwise.
   */
  public boolean reason(final MethodDefinition methodDefinition) {
    this.reason(methodDefinition, 0);

    // Test if method definition now has a return type.
    final TypeString typeStr = methodDefinition.getTypeName();
    final String methodName = methodDefinition.getName();
    final MethodDefinition updatedMethodDefinition =
        this.definitionKeeper.getMethodDefinitions(typeStr).stream()
            .filter(methodDef -> methodDef.getName().equals(methodName))
            .filter(methodDef -> methodDef.getLocation().equals(methodDefinition.getLocation()))
            .findAny()
            .orElseThrow();
    return updatedMethodDefinition.getReturnTypes() != ExpressionResultString.UNDEFINED
        && updatedMethodDefinition.getLoopTypes() != ExpressionResultString.UNDEFINED;
  }

  private void reason(final MethodDefinition methodDefinition, final int depth) {
    LOGGER.debug("Reasoning method definition: {}, depth: {}", methodDefinition, depth);

    if (depth > this.maxDepth) {
      // Don't go further than maxDepth.
      return;
    }

    // // TODO: Sometimes we do need to reason with the method definition itself, even if it
    // // doesn't return/iterate anything.
    // if (methodDefinition.getReturnTypes() != ExpressionResultString.UNDEFINED
    //     && methodDefinition.getLoopTypes() != ExpressionResultString.UNDEFINED) {
    //   // Already reasoned this method definition, or from TypeDoc,
    //   // or it just doesn't return/iterate anything.
    //   return;
    // }

    final Location location = methodDefinition.getLocation();
    if (location == null) {
      // Don't have any source code to reason from.
      return;
    }

    // Find method definition in the file.
    final MagikTypedFile magikFile1 = this.getMagikFile(location);
    final MethodDefinition fileMethodDefinition1 =
        this.getMethodDefinitionFromFile(methodDefinition, magikFile1);

    // If returned type is UNDEFINED, recurse from the METHOD_INVOCATION nodes.
    // Keep on iterating until we are no longer getting any improvements.
    final AstNode node1 = fileMethodDefinition1.getNode();
    Collection<MethodUsage> previousUndefinedMethodUsages = Collections.emptySet();
    Collection<MethodUsage> undefinedMethodUsages =
        this.extractUndefinedMethodUsages(magikFile1, node1);
    final TypeStringResolver resolver = magikFile1.getTypeStringResolver();
    while (!previousUndefinedMethodUsages.equals(undefinedMethodUsages)) {
      undefinedMethodUsages.stream()
          .map(usage -> this.getMethodDefinitions(resolver, usage))
          .flatMap(Collection::stream)
          .forEach(usedMethodDefinition -> this.reason(usedMethodDefinition, depth + 1));
      previousUndefinedMethodUsages = undefinedMethodUsages;
      undefinedMethodUsages = this.extractUndefinedMethodUsages(magikFile1, node1);
    }

    // After recusing, re-try this method.
    // Find method definition in the file.
    final MagikTypedFile magikFile2 = this.getMagikFile(location);
    final MethodDefinition fileMethodDefinition2 =
        this.getMethodDefinitionFromFile(methodDefinition, magikFile2);

    // Reason method and test if the returned type is not UNDEFINED now.
    final LocalTypeReasonerState reasonerState2 = magikFile2.getTypeReasonerState();
    final AstNode node2 = fileMethodDefinition2.getNode();
    final ExpressionResultString nodeType2 = reasonerState2.getNodeType(node2);
    final ExpressionResultString nodeIterType2 = reasonerState2.getNodeIterType(node2);
    if (nodeType2 != ExpressionResultString.UNDEFINED
        && nodeIterType2 != ExpressionResultString.UNDEFINED) {
      // Update the MethodDefinition with the known return type.
      this.updateMethodDefinitionTypes(
          methodDefinition, fileMethodDefinition2, nodeType2, nodeIterType2);
    }
  }

  private MethodDefinition getMethodDefinitionFromFile(
      final MethodDefinition methodDefinition, final MagikTypedFile magikFile) {
    final Location location = methodDefinition.getLocation();
    final String methodDefinitionName = methodDefinition.getName();
    return magikFile.getDefinitions().stream()
        .filter(MethodDefinition.class::isInstance)
        .map(MethodDefinition.class::cast)
        .filter(def -> def.getName().equals(methodDefinitionName))
        .filter(def -> def.getLocation().equals(location))
        .findAny()
        .orElseThrow();
  }

  private Collection<MethodUsage> extractUndefinedMethodUsages(
      final MagikTypedFile magikFile, final AstNode node) {
    final LocalTypeReasonerState reasonerState = magikFile.getTypeReasonerState();
    return node.getDescendants(MagikGrammar.METHOD_INVOCATION).stream()
        .map(
            methodInvocationNode -> {
              final ExpressionResultString invocationResult =
                  reasonerState.getNodeType(methodInvocationNode);
              final ExpressionResultString iterInvocationResult =
                  reasonerState.getNodeIterType(methodInvocationNode);
              if (invocationResult != ExpressionResultString.UNDEFINED
                  && iterInvocationResult != ExpressionResultString.UNDEFINED) {
                // Already known, no need to recurse.
                return null;
              }

              final MethodInvocationNodeHelper helper =
                  new MethodInvocationNodeHelper(methodInvocationNode);
              final AstNode receiverNode = helper.getReceiverNode();
              final ExpressionResultString receiverType = reasonerState.getNodeType(receiverNode);
              if (receiverType == ExpressionResultString.UNDEFINED) {
                // Cannot do anything with this.
                return null;
              }
              final TypeString resultTypeStr = receiverType.get(0, TypeString.UNDEFINED);
              final TypeString typeStr =
                  SelfHelper.substituteSelf(resultTypeStr, methodInvocationNode);
              if (typeStr.isUndefined()) {
                // Cannot do anything with this.
                return null;
              }

              final String invokedMethodName = helper.getMethodName();
              return new MethodUsage(typeStr, invokedMethodName);
            })
        .filter(Objects::nonNull)
        .distinct()
        .toList();
  }

  private void updateMethodDefinitionTypes(
      final MethodDefinition methodDefinition,
      final MethodDefinition fileMethodDefinition,
      final ExpressionResultString nodeType,
      final ExpressionResultString nodeIterType) {
    final MethodDefinition updatedMethodDefinition =
        new MethodDefinition(
            fileMethodDefinition.getLocation(),
            fileMethodDefinition.getTimestamp(),
            fileMethodDefinition.getModuleName(),
            fileMethodDefinition.getDoc(),
            null,
            fileMethodDefinition.getTypeName(),
            fileMethodDefinition.getMethodName(),
            fileMethodDefinition.getModifiers(),
            fileMethodDefinition.getParameters(),
            fileMethodDefinition.getAssignmentParameter(),
            fileMethodDefinition.getTopics(),
            nodeType,
            nodeIterType,
            fileMethodDefinition.getUsedGlobals(),
            fileMethodDefinition.getUsedMethods(),
            fileMethodDefinition.getUsedSlots(),
            fileMethodDefinition.getUsedConditions());

    // Save the new MethodDefinition.
    this.definitionKeeper.remove(methodDefinition);
    this.definitionKeeper.add(updatedMethodDefinition);
  }

  private MagikTypedFile getMagikFile(final Location location) {
    final URI uri = location.getUri();
    final Path path = Path.of(uri);
    final Charset charset = FileCharsetDeterminer.determineCharset(path);
    final String text;
    try {
      text = Files.readString(path, charset);
    } catch (final IOException exception) {
      throw new IllegalStateException(exception);
    }
    return new MagikTypedFile(
        RecursiveMethodDefinitionReasoner.MAGIK_FILE_PROPERTIES, uri, text, this.definitionKeeper);
  }

  private Collection<MethodDefinition> getMethodDefinitions(
      final TypeStringResolver resolver, final MethodUsage methodUsage) {
    final TypeString usedTypeName = methodUsage.getTypeName();
    final String usedMethodName = methodUsage.getMethodName();
    return resolver.getRespondingMethodDefinitions(usedTypeName, usedMethodName);
  }
}
