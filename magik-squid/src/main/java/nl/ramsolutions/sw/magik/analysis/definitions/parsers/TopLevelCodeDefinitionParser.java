package nl.ramsolutions.sw.magik.analysis.definitions.parsers;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.GenericTokenType;
import com.sonar.sslr.api.Token;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.MagikFile;
import nl.ramsolutions.sw.magik.Position;
import nl.ramsolutions.sw.magik.Range;
import nl.ramsolutions.sw.magik.analysis.definitions.MagikDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.Provenance;
import nl.ramsolutions.sw.magik.analysis.definitions.TopLevelCodeDefinition;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.moduledef.ModuleDefFile;
import nl.ramsolutions.sw.moduledef.ModuleDefinition;

/**
 * Parser for the top-level code of the transmit units of a file, see {@link
 * TopLevelCodeDefinition}.
 *
 * <p>A unit is the top-level code between two {@code $}, or the start or end of the file. It gets a
 * definition when one of its top-level statements has code outside every procedure and {@code
 * _global} statement; a package specification, a pragma or a syntax error is no such statement. The
 * code of a method, a procedure and a {@code _global} statement belongs to its own definition and
 * is skipped, also when it fails to parse (a mid-edit state the next index corrects).
 */
public class TopLevelCodeDefinitionParser {

  private final MagikFile magikFile;
  private final AstNode topNode;

  /**
   * Constructor.
   *
   * @param magikFile The file to read.
   * @param topNode The top node of the file.
   */
  public TopLevelCodeDefinitionParser(final MagikFile magikFile, final AstNode topNode) {
    this.magikFile = magikFile;
    this.topNode = topNode;
  }

  /**
   * Parse definitions, one per unit with top-level code.
   *
   * @return List of parsed definitions.
   */
  public List<MagikDefinition> parseDefinitions() {
    final String moduleName = this.findModuleName();
    final URI uri = this.magikFile.getUri();
    final List<MagikDefinition> definitions = new ArrayList<>();
    AstNode node = this.topNode.getFirstChild();
    while (node != null && node.isNot(GenericTokenType.EOF)) {
      final List<AstNode> unitNodes = TopLevelCodeDefinitionParser.findUnitNodes(node);
      final List<AstNode> codeNodes = TopLevelCodeDefinitionParser.selectCodeStatements(unitNodes);
      if (!codeNodes.isEmpty()) {
        final int index = definitions.size();
        final String name = AnonymousNamer.getNameForTopLevelCode(uri, moduleName, index);
        final TopLevelCodeDefinition definition =
            this.createDefinition(unitNodes, codeNodes, moduleName, name);
        definitions.add(definition);
      }

      final AstNode lastNode = unitNodes.get(unitNodes.size() - 1);
      node = lastNode.getNextSibling();
    }

    return definitions;
  }

  @CheckForNull
  private String findModuleName() {
    final ModuleDefFile moduleDefFile = this.magikFile.getModuleDefFile();
    if (moduleDefFile == null) {
      return null;
    }

    final ModuleDefinition moduleDefinition = moduleDefFile.getModuleDefinition();
    return moduleDefinition.getName();
  }

  private TopLevelCodeDefinition createDefinition(
      final List<AstNode> unitNodes,
      final List<AstNode> codeNodes,
      final @Nullable String moduleName,
      final String name) {
    final Location location = this.createUnitLocation(unitNodes);
    final Instant timestamp = this.magikFile.getTimestamp();
    final AstNode firstNode = codeNodes.get(0);
    final DefinitionUsageParser.Usages usages =
        DefinitionUsageParser.collectUsages(
            this.magikFile, codeNodes, TopLevelCodeDefinitionParser::isHeldByDefinition);
    return new TopLevelCodeDefinition(
        location,
        timestamp,
        moduleName,
        null,
        firstNode,
        name,
        usages.methods(),
        usages.globals(),
        usages.conditions(),
        usages.binaryOperators(),
        Provenance.UNKNOWN);
  }

  /**
   * The top-level nodes of the transmit unit holding a top-level node: those after the previous
   * {@code $} up to and including the next one, the end of the file left out.
   *
   * @param topLevelNode A child of the top node.
   * @return The nodes of its unit, in document order.
   */
  public static List<AstNode> findUnitNodes(final AstNode topLevelNode) {
    AstNode firstNode = topLevelNode;
    AstNode previousNode = firstNode.getPreviousSibling();
    while (previousNode != null && previousNode.isNot(MagikGrammar.TRANSMIT)) {
      firstNode = previousNode;
      previousNode = firstNode.getPreviousSibling();
    }

    final List<AstNode> unitNodes = new ArrayList<>();
    AstNode node = firstNode;
    while (node != null && node.isNot(GenericTokenType.EOF)) {
      unitNodes.add(node);
      node = node.is(MagikGrammar.TRANSMIT) ? null : node.getNextSibling();
    }

    return unitNodes;
  }

  /**
   * The top-level statements a {@link TopLevelCodeDefinition} holds: those of its unit with code
   * outside every method, procedure and {@code _global} statement.
   *
   * @param firstNode The definition's node, the first such statement of its unit.
   * @return The statements.
   */
  public static List<AstNode> findCodeStatements(final AstNode firstNode) {
    final List<AstNode> unitNodes = TopLevelCodeDefinitionParser.findUnitNodes(firstNode);
    return TopLevelCodeDefinitionParser.selectCodeStatements(unitNodes);
  }

  /** The top-level statements of a unit holding code outside every other definition. */
  private static List<AstNode> selectCodeStatements(final List<AstNode> unitNodes) {
    return unitNodes.stream()
        .filter(node -> node.is(MagikGrammar.STATEMENT))
        .filter(TopLevelCodeDefinitionParser::hasOwnCode)
        .toList();
  }

  /** Whether a statement has a token outside every node another definition holds. */
  private static boolean hasOwnCode(final AstNode statementNode) {
    final List<AstNode> statementNodes = List.of(statementNode);
    final List<AstNode> ownNodes =
        DefinitionUsageParser.collectNodes(
            statementNodes, TopLevelCodeDefinitionParser::isHeldByDefinition);
    return ownNodes.stream().anyMatch(node -> !node.hasChildren() && node.hasToken());
  }

  private Location createUnitLocation(final List<AstNode> unitNodes) {
    final AstNode firstNode = unitNodes.get(0);
    final AstNode lastNode = unitNodes.get(unitNodes.size() - 1);
    final Token firstToken = firstNode.getToken();
    final Token lastToken = lastNode.getLastToken();
    final Position start = Position.fromTokenStart(firstToken);
    final Position end = Position.fromTokenEnd(lastToken);
    final Range range = new Range(start, end);
    final URI uri = this.magikFile.getUri();
    return new Location(uri, range);
  }

  private static boolean isHeldByDefinition(final AstNode node) {
    return node.is(MagikGrammar.METHOD_DEFINITION, MagikGrammar.PROCEDURE_DEFINITION)
        || (node.is(MagikGrammar.VARIABLE_DEFINITION_STATEMENT)
            && GlobalDefinitionParser.isGlobalDefinition(node));
  }
}
