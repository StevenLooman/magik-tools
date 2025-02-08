package nl.ramsolutions.sw.magik.analysis.typing.reasoner;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.Token;
import java.net.URI;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.typing.ExpressionResultString;

/** Recursive reasoner for a specific node. */
public class RecursiveNodeTypeReasoner extends AbstractRecursiveReasoner {

  public RecursiveNodeTypeReasoner(final IDefinitionKeeper definitionKeeper, final int maxDepth) {
    super(definitionKeeper, maxDepth);
  }

  public void reason(final AstNode node) {
    this.reason(node, 0);
  }

  private void reason(final AstNode node, final int depth) {
    if (depth >= this.maxDepth) {
      return;
    }

    // Reason location of node.
    final Token token = node.getToken();
    final URI uri = token.getURI();
    final Location location = new Location(uri, node);
    final MagikTypedFile magikFile = this.getMagikFile(location);
    final LocalTypeReasonerState typeReasonerState = magikFile.getTypeReasonerState();
    final ExpressionResultString result = typeReasonerState.getNodeTypeSilent(node);
    final ExpressionResultString iterResult = typeReasonerState.getNodeIterTypeSilent(node);
    if (result != ExpressionResultString.UNDEFINED
        && iterResult != ExpressionResultString.UNDEFINED) {
      // Done reasoning.
      return;
    }

    // Keep on following method calls, recursively.

    // Find what we need to recurse/reason about. Either:
    // - slots
    // - methods
    // - procedures (results),  or rather, the globals there procs are assigned to.
    // - binary operators
    // - global variables
    // But should this be done here, or in a separate class?
    // There are all definitions.
  }
}
