package nl.ramsolutions.sw.magik.analysis.definitions.parsers;

import com.sonar.sslr.api.AstNode;
import java.net.URI;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import nl.ramsolutions.sw.definitions.ModuleDefinitionScanner;
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

/** {@code def_enumeration_from}/{@code def_enumeration} parser. */
public class DefEnumerationParser extends BaseDefParser {

  private static final String DEF_ENUMERATION_FROM = "def_enumeration_from";
  private static final String DEF_ENUMERATION = "def_enumeration";
  private static final String SW_DEF_ENUMERATION_FROM = "sw:def_enumeration_from";
  private static final String SW_DEF_ENUMERATION = "sw:def_enumeration";
  private static final List<TypeString> ENUM_PARENTS = List.of(TypeString.SW_ENUMERATION_VALUE);

  /**
   * Constructor.
   *
   * @param node {@code def_enumeration_from}/{@code def_enumeration} node.
   */
  public DefEnumerationParser(final MagikFile magikFile, final AstNode node) {
    super(magikFile, node);
  }

  /**
   * Test if node is a {@code def_enumeration_from}/{@code def_enumeration}.
   *
   * @param node Node to test
   * @return True if node is a {@code def_enumeration_from}/{@code def_enumeration}, false
   *     otherwise.
   */
  public static boolean isDefEnumeration(final AstNode node) {
    if (!node.is(MagikGrammar.PROCEDURE_INVOCATION)) {
      return false;
    }

    final ProcedureInvocationNodeHelper helper = new ProcedureInvocationNodeHelper(node);
    if (!helper.isProcedureInvocationOf(DEF_ENUMERATION)
        && !helper.isProcedureInvocationOf(DEF_ENUMERATION_FROM)
        && !helper.isProcedureInvocationOf(SW_DEF_ENUMERATION)
        && !helper.isProcedureInvocationOf(SW_DEF_ENUMERATION_FROM)) {
      return false;
    }

    // Some sanity.
    final AstNode argumentsNode = node.getFirstChild(MagikGrammar.ARGUMENTS);
    final ArgumentsNodeHelper argumentsHelper = new ArgumentsNodeHelper(argumentsNode);
    final AstNode argument0Node = argumentsHelper.getArgument(0, MagikGrammar.SYMBOL);
    return argument0Node != null;
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
    final String moduleName = ModuleDefinitionScanner.getModuleName(uri);

    // Figure statement node.
    final AstNode statementNode = this.node.getFirstAncestor(MagikGrammar.STATEMENT);

    // Figure pakkage.
    final String pakkage = this.getCurrentPakkage();

    // Figure name.
    final String identifier = argument0Node.getTokenValue().substring(1);
    final TypeString name = TypeString.ofIdentifier(identifier, pakkage);

    // Figure doc.
    final AstNode parentNode = this.node.getParent();
    final String doc = MagikCommentExtractor.extractDocComment(parentNode);

    // Figure topics.
    final AstNode pragmaNode = PragmaNodeHelper.getPragmaNode(node);
    final Set<String> topics =
        pragmaNode != null
            ? new PragmaNodeHelper(pragmaNode).getAllTopics()
            : Collections.emptySet();

    final ExemplarDefinition definition =
        new ExemplarDefinition(
            location,
            timestamp,
            moduleName,
            doc,
            statementNode,
            ExemplarDefinition.Sort.SLOTTED,
            name,
            Collections.emptyList(),
            DefEnumerationParser.ENUM_PARENTS,
            topics);
    return List.of(definition);
  }
}
