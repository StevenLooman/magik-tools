package nl.ramsolutions.sw.magik.analysis.definitions.parsers;

import com.sonar.sslr.api.AstNode;
import java.net.URI;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.MagikFile;
import nl.ramsolutions.sw.magik.analysis.definitions.ExemplarDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.MagikDefinition;
import nl.ramsolutions.sw.magik.analysis.helpers.ArgumentsNodeHelper;
import nl.ramsolutions.sw.magik.analysis.helpers.PragmaNodeHelper;
import nl.ramsolutions.sw.magik.analysis.helpers.ProcedureInvocationNodeHelper;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.parser.MagikCommentExtractor;
import nl.ramsolutions.sw.moduledef.ModuleDefFile;

/** {@code def_indexed_exemplar} parser. */
public class DefIndexedExemplarParser extends BaseDefParser {

  private static final String DEF_INDEXED_EXEMPLAR = "def_indexed_exemplar";
  private static final String SW_DEF_INDEXED_EXEMPLAR = "sw:def_indexed_exemplar";

  /**
   * Constructor.
   *
   * @param node {@code def_indexed_exemplar()} node.
   */
  public DefIndexedExemplarParser(final MagikFile magikFile, final AstNode node) {
    super(magikFile, node);
  }

  /**
   * Test if node is a {@code def_indexed_exemplar()}.
   *
   * @param node Node to test
   * @return True if node is a {@code def_indexed_exemplar()}, false otherwise.
   */
  public static boolean isDefIndexedExemplar(final AstNode node) {
    if (!node.is(MagikGrammar.PROCEDURE_INVOCATION)) {
      return false;
    }

    final ProcedureInvocationNodeHelper helper = new ProcedureInvocationNodeHelper(node);
    if (!helper.isProcedureInvocationOf(DEF_INDEXED_EXEMPLAR)
        && !helper.isProcedureInvocationOf(SW_DEF_INDEXED_EXEMPLAR)) {
      return false;
    }

    // Some sanity.
    final AstNode argumentsNode = node.getFirstChild(MagikGrammar.ARGUMENTS);
    final ArgumentsNodeHelper argumentsHelper = new ArgumentsNodeHelper(argumentsNode);
    final AstNode argument0Node = argumentsHelper.getArgument(0, MagikGrammar.SYMBOL);
    if (argument0Node == null) {
      return false;
    }

    return true;
  }

  /**
   * Parse defitions.
   *
   * @return List of parsed definitions.
   */
  @Override
  public List<MagikDefinition> parseDefinitions() {
    final AstNode argumentsNode = this.node.getFirstChild(MagikGrammar.ARGUMENTS);
    final ArgumentsNodeHelper argumentsHelper = new ArgumentsNodeHelper(argumentsNode);

    // Some sanity.
    final AstNode argument0Node = argumentsHelper.getArgument(0, MagikGrammar.SYMBOL);
    if (argument0Node == null) {
      throw new IllegalStateException();
    }

    // Figure location.
    final URI uri = this.node.getToken().getURI();
    final Location location = new Location(uri, this.node);

    // Figure timestamp.
    final Instant timestamp = this.magikFile.getTimestamp();

    // Figure module name.
    final String moduleName = ModuleDefFile.getModuleNameForUri(uri);

    // Figure statement node.
    final AstNode statementNode = this.node.getFirstAncestor(MagikGrammar.STATEMENT);

    // Figure pakkage.
    final String pakkage = this.getCurrentPakkage();

    // Figure name.
    final String identifier = argument0Node.getTokenValue().substring(1);
    final TypeString name = TypeString.ofIdentifier(identifier, pakkage);

    // Figure parents.
    final AstNode argument2Node = argumentsHelper.getArgument(2);
    final List<TypeString> parents = this.extractParents(argument2Node);

    // Figure doc.
    final AstNode parentNode = this.node.getParent();
    final String doc = MagikCommentExtractor.extractDocComment(parentNode);

    // Figure topics.
    final AstNode pragmaNode = PragmaNodeHelper.getPragmaNode(node);
    final Set<String> topics =
        pragmaNode != null
            ? new PragmaNodeHelper(pragmaNode).getAllTopics()
            : Collections.emptySet();

    final ExemplarDefinition indexedExemplarDefinition =
        new ExemplarDefinition(
            location,
            timestamp,
            moduleName,
            doc,
            statementNode,
            ExemplarDefinition.Sort.INDEXED,
            name,
            Collections.emptyList(),
            parents,
            topics);
    return List.of(indexedExemplarDefinition);
  }
}
