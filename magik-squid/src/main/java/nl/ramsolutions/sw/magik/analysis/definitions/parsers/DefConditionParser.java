package nl.ramsolutions.sw.magik.analysis.definitions.parsers;

import com.sonar.sslr.api.AstNode;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import nl.ramsolutions.sw.definitions.ModuleDefinitionScanner;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.MagikFile;
import nl.ramsolutions.sw.magik.analysis.definitions.ConditionDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.MagikDefinition;
import nl.ramsolutions.sw.magik.analysis.helpers.ArgumentsNodeHelper;
import nl.ramsolutions.sw.magik.analysis.helpers.ExpressionNodeHelper;
import nl.ramsolutions.sw.magik.analysis.helpers.MethodInvocationNodeHelper;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.parser.MagikCommentExtractor;

/** Condition definition parser. */
public class DefConditionParser {

  private static final String DEFINE_CONDITION = "define_condition()";
  private static final String DEFINE_TOP_CONDITION = "define_top_condition()";
  private static final String CONDITION = "condition";
  private static final String SW_CONDITION = "sw:condition";

  private final MagikFile magikFile;
  private final AstNode node;

  /**
   * Constructor.
   *
   * @param node Condition definition node.
   */
  public DefConditionParser(final MagikFile magikFile, final AstNode node) {
    if (node.isNot(MagikGrammar.METHOD_INVOCATION)) {
      throw new IllegalArgumentException();
    }

    this.magikFile = magikFile;
    this.node = node;
  }

  /**
   * Test if node is condition definition.
   *
   * @param node Node.
   * @return True if is condition definition, false otherwise.
   */
  public static boolean isDefineCondition(final AstNode node) {
    final MethodInvocationNodeHelper helper = new MethodInvocationNodeHelper(node);
    if (!helper.isMethodInvocationOf(DEFINE_CONDITION)
        && !helper.isMethodInvocationOf(DEFINE_TOP_CONDITION)) {
      return false;
    }

    // Some sanity.
    final AstNode parentNode = node.getParent();
    final AstNode atomNode = parentNode.getFirstChild();
    if (atomNode.isNot(MagikGrammar.ATOM)) {
      return false;
    }
    final String exemplarName = atomNode.getTokenValue(); // Assume this is an exemplar.
    if (!exemplarName.equalsIgnoreCase(CONDITION) && !exemplarName.equalsIgnoreCase(SW_CONDITION)) {
      return false;
    }

    final AstNode argumentsNode = node.getFirstChild(MagikGrammar.ARGUMENTS);
    final ArgumentsNodeHelper argumentsHelper = new ArgumentsNodeHelper(argumentsNode);
    final AstNode argument0Node = argumentsHelper.getArgument(0, MagikGrammar.SYMBOL);
    final AstNode argument1Node = argumentsHelper.getArgument(1, MagikGrammar.SYMBOL);
    final AstNode argument2Node = argumentsHelper.getArgument(2, MagikGrammar.SIMPLE_VECTOR);
    return argument0Node != null && argument1Node != null && argument2Node != null;
  }

  /**
   * Parse definition.
   *
   * @return List of {@link ConditionDefinition}s.
   */
  public List<MagikDefinition> parseDefinitions() {
    // Some sanity.
    final AstNode parentNode = this.node.getParent();
    final AstNode atomNode = parentNode.getFirstChild();
    if (atomNode.isNot(MagikGrammar.ATOM)) {
      throw new IllegalStateException();
    }
    final String exemplarName = atomNode.getTokenValue();
    if (!exemplarName.equalsIgnoreCase(CONDITION) && !exemplarName.equalsIgnoreCase(SW_CONDITION)) {
      throw new IllegalStateException();
    }

    final AstNode argumentsNode = node.getFirstChild(MagikGrammar.ARGUMENTS);
    final ArgumentsNodeHelper argumentsHelper = new ArgumentsNodeHelper(argumentsNode);
    final AstNode argument0Node = argumentsHelper.getArgument(0, MagikGrammar.SYMBOL);
    final AstNode argument1Node = argumentsHelper.getArgument(1, MagikGrammar.SYMBOL);
    final AstNode argument2Node = argumentsHelper.getArgument(2, MagikGrammar.SIMPLE_VECTOR);
    if (argument0Node == null || argument1Node == null || argument2Node == null) {
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
    final AstNode statementNode = node.getFirstAncestor(MagikGrammar.STATEMENT);

    // Figure definition.
    final String nameSymbol = argument0Node.getTokenValue();
    final String name = nameSymbol.substring(1);
    final String parentSymbol = argument1Node.getTokenValue();
    final String parent = parentSymbol.substring(1);
    final List<String> dataNames =
        argument2Node.getChildren(MagikGrammar.EXPRESSION).stream()
            .map(
                expressionNode -> {
                  final ExpressionNodeHelper expressionNodeHelper =
                      new ExpressionNodeHelper(expressionNode);
                  final String dataName = expressionNodeHelper.getConstant();
                  if (dataName.startsWith(":")) {
                    return dataName.substring(1);
                  }
                  return dataName;
                })
            .filter(Objects::nonNull)
            .toList();

    // Figure doc.
    final String doc = MagikCommentExtractor.extractDocComment(parentNode);

    final ConditionDefinition definition =
        new ConditionDefinition(
            location, timestamp, moduleName, doc, statementNode, name, parent, dataNames);
    return List.of(definition);
  }
}
