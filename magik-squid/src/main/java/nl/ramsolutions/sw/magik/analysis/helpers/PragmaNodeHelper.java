package nl.ramsolutions.sw.magik.analysis.helpers;

import com.sonar.sslr.api.AstNode;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import nl.ramsolutions.sw.magik.api.MagikGrammar;

/** Helper for PRAGMA nodes. */
public class PragmaNodeHelper {

  private static final String CLASSIFY_LEVEL = "classify_level";
  private static final String TOPIC = "topic";
  private static final String USAGE = "usage";

  private final AstNode node;

  /**
   * Constructor.
   *
   * @param node Node to encapsulate.
   */
  public PragmaNodeHelper(final AstNode node) {
    if (!node.is(MagikGrammar.PRAGMA)) {
      throw new IllegalArgumentException();
    }

    this.node = node;
  }

  public AstNode getNode() {
    return this.node;
  }

  /**
   * Get the classify level.
   *
   * @return Classify level.
   */
  public Set<String> getClassifyLevels() {
    return this.getPragmaValues().getOrDefault(CLASSIFY_LEVEL, Set.of());
  }

  /**
   * Get the topics.
   *
   * @return Topics.
   */
  public Set<String> getTopics() {
    return this.getPragmaValues().getOrDefault(TOPIC, Set.of());
  }

  /**
   * Get the usages.
   *
   * @return Usages.
   */
  public Set<String> getUsages() {
    return this.getPragmaValues().getOrDefault(USAGE, Set.of());
  }

  /**
   * Get all pragma keys/values.
   *
   * @return Classify level.
   */
  public Map<String, Set<String>> getPragmaValues() {
    return this.node.getDescendants(MagikGrammar.PRAGMA_PARAMS).stream()
        .map(paramsNode -> paramsNode.getChildren(MagikGrammar.PRAGMA_PARAM))
        .flatMap(Collection::stream)
        .collect(
            Collectors.toMap(
                paramNode ->
                    paramNode.getFirstChild(MagikGrammar.IDENTIFIER).getTokenOriginalValue(),
                paramNode ->
                    paramNode
                        .getFirstChild(MagikGrammar.PRAGMA_VALUE)
                        .getDescendants(MagikGrammar.IDENTIFIER)
                        .stream()
                        .map(AstNode::getTokenOriginalValue)
                        .collect(Collectors.toSet())));
  }

  /**
   * Get the accompanying PRAGMA node, if any.
   *
   * @return PRAGMA node or null.
   */
  @CheckForNull
  public static AstNode getPragmaNode(final AstNode node) {
    if (node.is(MagikGrammar.PRAGMA)) {
      return node;
    }

    final AstNode postPragmaNode;
    if (node.is(MagikGrammar.METHOD_DEFINITION)) {
      postPragmaNode = node;
    } else if (node.is(MagikGrammar.PROCEDURE_DEFINITION)) {
      postPragmaNode = node.getFirstAncestor(MagikGrammar.STATEMENT);
    } else if (node.is(MagikGrammar.METHOD_INVOCATION)) {
      postPragmaNode = node.getFirstAncestor(MagikGrammar.STATEMENT);
    } else if (node.is(MagikGrammar.PROCEDURE_INVOCATION)) {
      postPragmaNode = node.getFirstAncestor(MagikGrammar.STATEMENT);
    } else if (node.is(MagikGrammar.VARIABLE_DEFINITION_STATEMENT)) {
      postPragmaNode = node.getFirstAncestor(MagikGrammar.STATEMENT);
    } else {
      postPragmaNode = null;
    }

    if (postPragmaNode == null) {
      return null;
    }

    final AstNode previousSibling = postPragmaNode.getPreviousSibling();
    if (previousSibling != null && previousSibling.is(MagikGrammar.PRAGMA)) {
      return previousSibling;
    }

    return null;
  }

  /**
   * Get the PragmaHelper if node has/is pragma, null otherwise.
   *
   * @param node Node to check.
   * @return PragmaHelper if node has/is pragma, null otherwise.
   */
  @CheckForNull
  public static PragmaNodeHelper newSafe(final AstNode node) {
    final AstNode pragmaNode = PragmaNodeHelper.getPragmaNode(node);
    if (pragmaNode == null) {
      return null;
    }

    return new PragmaNodeHelper(pragmaNode);
  }
}
