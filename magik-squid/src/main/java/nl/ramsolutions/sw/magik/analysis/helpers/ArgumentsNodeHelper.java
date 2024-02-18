package nl.ramsolutions.sw.magik.analysis.helpers;

import com.sonar.sslr.api.AstNode;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import nl.ramsolutions.sw.magik.api.MagikGrammar;

/** Helper for ARGUMENTS nodes. */
public class ArgumentsNodeHelper {

  private final AstNode node;

  /**
   * Constructor.
   *
   * @param node Node to encapsulate.
   */
  public ArgumentsNodeHelper(final AstNode node) {
    if (!node.is(MagikGrammar.ARGUMENTS)) {
      throw new IllegalArgumentException();
    }

    this.node = node;
  }

  /**
   * Get the nth argument.
   *
   * @param nth Nth argument.
   * @return Nth argument.
   */
  @CheckForNull
  public AstNode getArgument(final int nth) {
    final List<AstNode> argumentNodes = this.node.getChildren(MagikGrammar.ARGUMENT);
    if (argumentNodes.size() - 1 < nth) {
      return null;
    }
    final AstNode argumentNode = argumentNodes.get(nth);
    return argumentNode.getFirstChild(MagikGrammar.EXPRESSION);
  }

  /**
   * Get the nth argument, with expecting any of the matching types.
   *
   * @param nth Nth argument.
   * @param types Expected (required) matching types.
   * @return Node of given type.
   */
  @CheckForNull
  public AstNode getArgument(final int nth, final MagikGrammar... types) {
    final AstNode expressionNode = this.getArgument(nth);
    if (expressionNode == null) {
      return null;
    }

    final AstNode atomNode = expressionNode.getFirstChild(MagikGrammar.ATOM);
    if (atomNode == null) {
      return null;
    }

    return Stream.of(types)
        .map(atomNode::getFirstChild)
        .filter(Objects::nonNull)
        .findAny()
        .orElse(null);
  }
}
