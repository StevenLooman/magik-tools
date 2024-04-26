package nl.ramsolutions.sw.magik.analysis.definitions.parsers;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.Token;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import nl.ramsolutions.sw.magik.api.MagikGrammar;

/** Creates names for anonymous constructs, such as procedures. */
public final class AnonymousNamer {

  private static final URI DEFAULT_URI = URI.create("memory://source.magik");

  private AnonymousNamer() {}

  /**
   * Get a name for a PROCEDURE_DEFINITION.
   *
   * @param node PROCEDURE_DEFINITION node.
   * @return Name.
   */
  public static TypeString getNameForProcedure(final AstNode node) {
    if (node.isNot(MagikGrammar.PROCEDURE_DEFINITION)) {
      throw new IllegalArgumentException();
    }

    // Construct filename part.
    final Token token = node.getToken();
    final URI uri = token.getURI();
    final String filenamePart =
        DEFAULT_URI.equals(uri)
            ? "in_memory"
            : Path.of(uri).toString().replace("/", "_").replace("\\", "_");

    // Get procedure counter.
    AstNode rootNode = node;
    while (rootNode.getParent() != null) {
      rootNode = rootNode.getParent();
    }
    final List<AstNode> procNodes = rootNode.getDescendants(MagikGrammar.PROCEDURE_DEFINITION);
    final int procCounter = procNodes.indexOf(node);

    final String name = "_proc_" + filenamePart + "_" + procCounter;
    return TypeString.ofIdentifier(name, TypeString.ANONYMOUS_PACKAGE);
  }
}
