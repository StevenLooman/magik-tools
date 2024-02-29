package nl.ramsolutions.sw.magik.analysis.definitions.parsers;

import com.sonar.sslr.api.AstNode;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import nl.ramsolutions.sw.definitions.ModuleDefinitionScanner;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.analysis.definitions.Definition;
import nl.ramsolutions.sw.magik.analysis.definitions.PackageDefinition;
import nl.ramsolutions.sw.magik.analysis.helpers.ArgumentsNodeHelper;
import nl.ramsolutions.sw.magik.analysis.helpers.ExpressionNodeHelper;
import nl.ramsolutions.sw.magik.analysis.helpers.ProcedureInvocationNodeHelper;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.parser.MagikCommentExtractor;

/** {@code def_package()} parser. */
public class DefPackageParser {

  private static final String DEF_PACKAGE = "def_package";
  private static final String SW_DEF_PACKAGE = "sw:def_package";

  private final AstNode node;

  /**
   * Constructor.
   *
   * @param node {@code def_package()} node.
   */
  public DefPackageParser(final AstNode node) {
    if (node.isNot(MagikGrammar.PROCEDURE_INVOCATION)) {
      throw new IllegalArgumentException();
    }

    this.node = node;
  }

  /**
   * Test if node is a {@code def_package()}.
   *
   * @param node Node to test
   * @return True if node is a {@code def_package()}, false otherwise.
   */
  public static boolean isDefPackage(final AstNode node) {
    if (!node.is(MagikGrammar.PROCEDURE_INVOCATION)) {
      return false;
    }

    final ProcedureInvocationNodeHelper helper = new ProcedureInvocationNodeHelper(node);
    if (!helper.isProcedureInvocationOf(DEF_PACKAGE)
        && !helper.isProcedureInvocationOf(SW_DEF_PACKAGE)) {
      return false;
    }

    final AstNode argumentsNode = node.getFirstChild(MagikGrammar.ARGUMENTS);
    final ArgumentsNodeHelper argumentsHelper = new ArgumentsNodeHelper(argumentsNode);

    // Figure name.
    final AstNode argument0Node = argumentsHelper.getArgument(0, MagikGrammar.SYMBOL);
    return argument0Node != null;
  }

  /**
   * Parse defitions.
   *
   * @return List of parsed definitions.
   */
  @SuppressWarnings("checkstyle:NestedIfDepth")
  public List<Definition> parseDefinitions() {
    final AstNode argumentsNode = this.node.getFirstChild(MagikGrammar.ARGUMENTS);
    final ArgumentsNodeHelper argumentsHelper = new ArgumentsNodeHelper(argumentsNode);
    final AstNode argument0Node = argumentsHelper.getArgument(0, MagikGrammar.SYMBOL);
    if (argument0Node == null) {
      throw new IllegalStateException();
    }

    // Figure location.
    final URI uri = this.node.getToken().getURI();
    final Location location = new Location(uri, this.node);

    // Figure module name.
    final String moduleName = ModuleDefinitionScanner.getModuleName(uri);

    // Figure statement node.
    final AstNode statementNode = this.node.getFirstAncestor(MagikGrammar.STATEMENT);

    // Figure name.
    final String name = argument0Node.getTokenValue().substring(1);

    // Figure uses-list.
    final List<String> uses = new ArrayList<>();
    final AstNode argument1Node = argumentsHelper.getArgument(1, MagikGrammar.SYMBOL);
    if (argument1Node != null && argument1Node.getTokenValue().equals(":uses")) {
      final AstNode argument2Node = argumentsHelper.getArgument(2, MagikGrammar.SIMPLE_VECTOR);
      if (argument2Node != null) {
        for (final AstNode expressionNode : argument2Node.getChildren(MagikGrammar.EXPRESSION)) {
          final ExpressionNodeHelper expressionHelper = new ExpressionNodeHelper(expressionNode);
          final String usesSymbol = expressionHelper.getConstant();
          if (usesSymbol != null) {
            final String usesName = usesSymbol.substring(1);
            uses.add(usesName);
          }
        }
      }
    } else {
      uses.add("sw");
    }

    // Figure doc.
    final AstNode parentNode = this.node.getParent();
    final String doc = MagikCommentExtractor.extractDocComment(parentNode);

    final PackageDefinition packageDefinition =
        new PackageDefinition(location, moduleName, doc, statementNode, name, uses);
    return List.of(packageDefinition);
  }
}
