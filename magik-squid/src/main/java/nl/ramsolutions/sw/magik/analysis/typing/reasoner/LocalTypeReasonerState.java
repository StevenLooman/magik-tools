package nl.ramsolutions.sw.magik.analysis.typing.reasoner;

import com.sonar.sslr.api.AstNode;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import java.util.HashMap;
import java.util.Map;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.analysis.scope.ScopeEntry;
import nl.ramsolutions.sw.magik.analysis.typing.types.ExpressionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** State for {@link LocalTypeReasoner}. */
public class LocalTypeReasonerState {

  private static final Logger LOGGER = LoggerFactory.getLogger(LocalTypeReasonerState.class);

  private final MagikTypedFile magikFile;
  private final Map<AstNode, ExpressionResult> nodeTypes = new HashMap<>();
  private final Map<AstNode, ExpressionResult> nodeIterTypes = new HashMap<>();
  private final Map<ScopeEntry, AstNode> currentScopeEntryNodes = new HashMap<>();

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
  public boolean hasNodeType(final AstNode node) {
    return this.nodeTypes.containsKey(node);
  }

  /**
   * Get the type for a {@link AstNode}.
   *
   * @param node AstNode.
   * @return Resulting type.
   */
  public ExpressionResult getNodeType(final AstNode node) {
    final ExpressionResult result = this.nodeTypes.get(node);
    if (result == null) {
      LOGGER.debug("Node without type: {}", node);
      return ExpressionResult.UNDEFINED;
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
  public ExpressionResult getNodeTypeSilent(final AstNode node) {
    return this.getNodeType(node);
  }

  /**
   * Set a type for a {@link AstNode}.
   *
   * @param node AstNode.
   * @param result ExpressionResult.
   */
  void setNodeType(final AstNode node, final ExpressionResult result) {
    LOGGER.trace("{} is of type: {}", node, result);
    this.nodeTypes.put(node, result);
  }

  /**
   * Test if the type for a {@link AstNode} is known.
   *
   * @param node AstNode.
   * @return True if known, false otherwise.
   */
  public boolean hasNodeIterType(final AstNode node) {
    return this.nodeIterTypes.containsKey(node);
  }

  /**
   * Get the loopbody type for a {@link AstNode}.
   *
   * @param node AstNode.
   * @return Resulting type.
   */
  public ExpressionResult getNodeIterType(final AstNode node) {
    final ExpressionResult result = this.nodeIterTypes.get(node);
    if (result == null) {
      LOGGER.debug("Node without type: {}", node);
      return ExpressionResult.UNDEFINED;
    }

    return result;
  }

  /**
   * Set a loopbody type for a {@link AstNode}.
   *
   * @param node AstNode.
   * @param result Type.
   */
  void setNodeIterType(final AstNode node, final ExpressionResult result) {
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
}
