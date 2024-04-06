package nl.ramsolutions.sw.magik.analysis.typing.reasoner;

import com.sonar.sslr.api.AstNode;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import java.util.HashMap;
import java.util.Map;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.analysis.definitions.DefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.TypeStringDefinition;
import nl.ramsolutions.sw.magik.analysis.scope.ScopeEntry;
import nl.ramsolutions.sw.magik.analysis.typing.types.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.types.TypeString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Adapter to adapt State {@link LocalTypeReasonerStateAdapter} + {@link DefinitionKeeper} for older
 * components.
 */
public class LocalTypeReasonerState {

  private static final Logger LOGGER = LoggerFactory.getLogger(LocalTypeReasonerState.class);

  private final MagikTypedFile magikFile;
  private final Map<AstNode, ExpressionResultString> nodeTypes = new HashMap<>();
  private final Map<AstNode, ExpressionResultString> nodeIterTypes = new HashMap<>();
  private final Map<AstNode, TypeStringDefinition> nodeTypeDefinitions = new HashMap<>();
  private final Map<ScopeEntry, AstNode> currentScopeEntryNodes = new HashMap<>();
  private final Map<TypeString, TypeStringDefinition> typeStringDefinitions = new HashMap<>();

  LocalTypeReasonerState(final MagikTypedFile magikFile) {
    this.magikFile = magikFile;
  }

  public MagikTypedFile getMagikFile() {
    return this.magikFile;
  }

  /**
   * Test if the type for a {@link AstNode} is known.
   *
   * @param node AstNode.
   * @return True if known, false otherwise.
   */
  boolean hasNodeType(final AstNode node) {
    return this.nodeTypes.containsKey(node);
  }

  /**
   * Get the type for a {@link AstNode}.
   *
   * @param node AstNode.
   * @return Resulting type.
   */
  public ExpressionResultString getNodeType(final AstNode node) {
    final ExpressionResultString result = this.nodeTypes.get(node);
    if (result == null) {
      LOGGER.debug("Node without type: {}", node);
      return ExpressionResultString.UNDEFINED;
    }

    return result;
  }

  /**
   * Get the type for a {@link AstNode}.
   *
   * @param node AstNode.
   * @return Resulting type.
   */
  @CheckForNull
  public ExpressionResultString getNodeTypeSilent(final AstNode node) {
    return this.nodeTypes.get(node);
  }

  /**
   * Set a type for a {@link AstNode}.
   *
   * @param node AstNode.
   * @param result ExpressionResult.
   */
  void setNodeType(final AstNode node, final ExpressionResultString result) {
    LOGGER.trace("{} is of type: {}", node, result);
    this.nodeTypes.put(node, result);
  }

  /**
   * Test if the type for a {@link AstNode} is known.
   *
   * @param node AstNode.
   * @return True if known, false otherwise.
   */
  boolean hasNodeIterType(final AstNode node) {
    return this.nodeIterTypes.containsKey(node);
  }

  /**
   * Get the loopbody type for a {@link AstNode}.
   *
   * @param node AstNode.
   * @return Resulting type.
   */
  public ExpressionResultString getNodeIterType(final AstNode node) {
    final ExpressionResultString result = this.nodeIterTypes.get(node);
    if (result == null) {
      LOGGER.debug("Node without type: {}", node);
      return ExpressionResultString.UNDEFINED;
    }

    return result;
  }

  /**
   * Set a loopbody type for a {@link AstNode}.
   *
   * @param node AstNode.
   * @param result Type.
   */
  void setNodeIterType(final AstNode node, final ExpressionResultString result) {
    this.nodeIterTypes.put(node, result);
  }

  /**
   * Get the currently linked {@link AstNode} to {@link ScopeEntry}.
   *
   * @param scopeEntry {@link ScopeEntry} to get the currently linked {@link AstNode} for.
   * @return Current {@link AstNode}.
   */
  AstNode getCurrentScopeEntryNode(final ScopeEntry scopeEntry) {
    return this.currentScopeEntryNodes.get(scopeEntry);
  }

  /**
   * Set the current {@link ScopeEntry} {@link AstNode}.
   *
   * @param scopeEntry {@link ScopeEntry} to set the current {@link AstNode} for.
   * @param node Current {@link AstNode}.
   */
  void setCurrentScopeEntryNode(final ScopeEntry scopeEntry, final AstNode node) {
    this.currentScopeEntryNodes.put(scopeEntry, node);
  }

  @CheckForNull
  public TypeStringDefinition getTypeStringDefinition(final TypeString typeString) {
    final TypeStringDefinition def = this.typeStringDefinitions.get(typeString);
    if (def == null) {
      LOGGER.debug("TypeString without type: {}", typeString);
    }
    return def;
  }

  void setTypeStringDefinition(final TypeString typeStr, final TypeStringDefinition definition) {
    this.typeStringDefinitions.put(typeStr, definition);
  }
}
