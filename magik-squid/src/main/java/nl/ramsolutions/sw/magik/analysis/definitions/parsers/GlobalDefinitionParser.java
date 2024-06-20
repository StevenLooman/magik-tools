package nl.ramsolutions.sw.magik.analysis.definitions.parsers;

import com.sonar.sslr.api.AstNode;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import nl.ramsolutions.sw.definitions.ModuleDefFileScanner;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.MagikFile;
import nl.ramsolutions.sw.magik.analysis.definitions.GlobalDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.MagikDefinition;
import nl.ramsolutions.sw.magik.analysis.helpers.PackageNodeHelper;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.api.MagikKeyword;
import nl.ramsolutions.sw.magik.parser.MagikCommentExtractor;
import nl.ramsolutions.sw.magik.parser.TypeDocParser;

/** {@code _global} parser. */
public class GlobalDefinitionParser {

  private final MagikFile magikFile;
  private final AstNode node;

  /**
   * Constructor.
   *
   * @param node {@code _global} node.
   */
  public GlobalDefinitionParser(final MagikFile magikFile, final AstNode node) {
    this.magikFile = magikFile;
    this.node = node;
  }

  /**
   * Test if node is a {@code _global}.
   *
   * @param node Node to test
   * @return True if node is a {@code _global}, false otherwise.
   */
  public static boolean isGlobalDefinition(final AstNode node) {
    final AstNode modifier = node.getFirstChild(MagikGrammar.VARIABLE_DEFINITION_MODIFIER);
    return modifier != null
        && modifier.getTokenValue().equalsIgnoreCase(MagikKeyword.GLOBAL.getValue());
  }

  /**
   * Parse defitions.
   *
   * @return List of parsed definitions.
   */
  public List<MagikDefinition> parseDefinitions() {
    final AstNode modifier = this.node.getFirstChild(MagikGrammar.VARIABLE_DEFINITION_MODIFIER);
    if (modifier == null
        || !modifier.getTokenValue().equalsIgnoreCase(MagikKeyword.GLOBAL.getValue())) {
      throw new IllegalStateException();
    }

    // Figure location.
    final URI uri = this.node.getToken().getURI();
    final Location location = new Location(uri, this.node);

    // Figure timestamp.
    final Instant timestamp = this.magikFile.getTimestamp();

    // Figure module name.
    final String moduleName = ModuleDefFileScanner.getModuleName(uri);

    // Figure name.
    final String packageName = this.getCurrentPakkage();
    final AstNode variableDefinitionNode =
        this.node.getFirstChild(MagikGrammar.VARIABLE_DEFINITION);
    final AstNode identifierNode = variableDefinitionNode.getFirstChild(MagikGrammar.IDENTIFIER);
    final String identifier = identifierNode.getTokenValue();
    final TypeString typeName = TypeString.ofIdentifier(identifier, packageName);

    // Figure type.
    // TODO: Handle procedure, if procedure.
    final TypeDocParser docParser = new TypeDocParser(node);
    final TypeString aliasedTypeRef =
        docParser.getReturnTypes().stream().findFirst().orElse(TypeString.UNDEFINED);

    // Figure doc.
    final AstNode parentNode = this.node.getParent();
    final String doc = MagikCommentExtractor.extractDocComment(parentNode);

    final GlobalDefinition globalDefinition =
        new GlobalDefinition(
            location, timestamp, moduleName, doc, this.node, typeName, aliasedTypeRef);
    return List.of(globalDefinition);
  }

  private String getCurrentPakkage() {
    final PackageNodeHelper helper = new PackageNodeHelper(this.node);
    return helper.getCurrentPackage();
  }
}
