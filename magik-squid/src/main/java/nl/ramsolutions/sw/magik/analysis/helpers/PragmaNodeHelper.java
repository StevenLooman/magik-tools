package nl.ramsolutions.sw.magik.analysis.helpers;

import com.sonar.sslr.api.AstNode;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import java.util.Set;
import java.util.stream.Collectors;
import nl.ramsolutions.sw.magik.api.MagikGrammar;

public class PragmaNodeHelper {

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

  public Set<String> getAllTopics() {
    // TODO: Does this work? I've seen `{` topics...
    return this.node.getDescendants(MagikGrammar.PRAGMA_VALUE).stream()
        .flatMap(valueNode -> valueNode.getDescendants(MagikGrammar.IDENTIFIER).stream())
        .map(node -> node.getTokenOriginalValue())
        .collect(Collectors.toSet());
  }

  @CheckForNull
  public static AstNode getPragmaNode(final AstNode node) {
    if (node.is(MagikGrammar.PRAGMA)) {
      return node;
    }

    final AstNode previousSibling = node.getPreviousSibling();
    if (previousSibling != null && previousSibling.is(MagikGrammar.PRAGMA)) {
      return previousSibling;
    }

    return null;
  }
}
