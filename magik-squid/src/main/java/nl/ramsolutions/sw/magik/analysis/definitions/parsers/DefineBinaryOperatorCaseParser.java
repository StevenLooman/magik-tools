package nl.ramsolutions.sw.magik.analysis.definitions.parsers;

import com.sonar.sslr.api.AstNode;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.MagikFile;
import nl.ramsolutions.sw.magik.analysis.definitions.BinaryOperatorDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.MagikDefinition;
import nl.ramsolutions.sw.magik.analysis.helpers.ArgumentsNodeHelper;
import nl.ramsolutions.sw.magik.analysis.helpers.PackageNodeHelper;
import nl.ramsolutions.sw.magik.analysis.helpers.ProcedureInvocationNodeHelper;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.parser.MagikCommentExtractor;
import nl.ramsolutions.sw.magik.parser.TypeDocParser;
import nl.ramsolutions.sw.moduledef.ModuleDefFile;

/** {@code define_binary_operator_case()} parser. */
public class DefineBinaryOperatorCaseParser {

  private static final String DEFINE_BINARY_OPERATOR_CASE = "define_binary_operator_case";
  private static final String SW_DEFINE_BINARY_OPERATOR_CASE = "sw:define_binary_operator_case";

  private final MagikFile magikFile;
  private final AstNode node;

  /**
   * Constructor.
   *
   * @param node {@code define_binary_operator_case()} node.
   */
  public DefineBinaryOperatorCaseParser(final MagikFile magikFile, final AstNode node) {
    if (node.isNot(MagikGrammar.PROCEDURE_INVOCATION)) {
      throw new IllegalArgumentException();
    }

    this.magikFile = magikFile;
    this.node = node;
  }

  /**
   * Test if node is a {@code define_binary_operator_case()}.
   *
   * @param node Node to test
   * @return True if node is a {@code define_binary_operator_case()}, false otherwise.
   */
  public static boolean isBinaryOperatorCase(final AstNode node) {
    if (!node.is(MagikGrammar.PROCEDURE_INVOCATION)) {
      return false;
    }

    final ProcedureInvocationNodeHelper helper = new ProcedureInvocationNodeHelper(node);
    if (!helper.isProcedureInvocationOf(DEFINE_BINARY_OPERATOR_CASE)
        && !helper.isProcedureInvocationOf(SW_DEFINE_BINARY_OPERATOR_CASE)) {
      return false;
    }

    final AstNode argumentsNode = node.getFirstChild(MagikGrammar.ARGUMENTS);
    final ArgumentsNodeHelper argumentsHelper = new ArgumentsNodeHelper(argumentsNode);
    final AstNode argument0Node = argumentsHelper.getArgument(0, MagikGrammar.SYMBOL);
    if (argument0Node == null) {
      return false;
    }
    final AstNode argument1Node = argumentsHelper.getArgument(1, MagikGrammar.IDENTIFIER);
    if (argument1Node == null) {
      return false;
    }
    final AstNode argument2Node = argumentsHelper.getArgument(2, MagikGrammar.IDENTIFIER);
    if (argument2Node == null) {
      return false;
    }
    final AstNode argument3Node = argumentsHelper.getArgument(3); // Anything will do.
    if (argument3Node == null) {
      return false;
    }

    final String operatorSymbol = argument0Node.getTokenValue();
    return !operatorSymbol.isEmpty();
  }

  /**
   * Parse defitions.
   *
   * @return List of parsed definitions.
   */
  public List<MagikDefinition> parseDefinitions() {
    // Some sanity.
    final AstNode argumentsNode = node.getFirstChild(MagikGrammar.ARGUMENTS);
    final ArgumentsNodeHelper argumentsHelper = new ArgumentsNodeHelper(argumentsNode);
    final AstNode argument0Node = argumentsHelper.getArgument(0, MagikGrammar.SYMBOL);
    if (argument0Node == null) {
      throw new IllegalStateException();
    }
    final AstNode argument1Node = argumentsHelper.getArgument(1, MagikGrammar.IDENTIFIER);
    if (argument1Node == null) {
      throw new IllegalStateException();
    }
    final AstNode argument2Node = argumentsHelper.getArgument(2, MagikGrammar.IDENTIFIER);
    if (argument2Node == null) {
      throw new IllegalStateException();
    }
    final AstNode argument3Node = argumentsHelper.getArgument(3); // Anything will do.
    if (argument3Node == null) {
      throw new IllegalStateException();
    }

    final String operatorSymbol = argument0Node.getTokenValue();
    if (operatorSymbol.isEmpty()) {
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
    final AstNode statementNode = node.getFirstAncestor(MagikGrammar.STATEMENT);

    // Figure pakkage.
    final String currentPakkage = this.getCurrentPakkage();

    // Figure doc.
    final AstNode parentNode = this.node.getParent();
    final String doc = MagikCommentExtractor.extractDocComment(parentNode);

    // Figure type doc.
    final AstNode procDefNode = argument3Node.getFirstDescendant(MagikGrammar.PROCEDURE_DEFINITION);
    final TypeDocParser typeDocParser = new TypeDocParser(procDefNode);
    final List<TypeString> returnTypes = typeDocParser.getReturnTypes();
    final TypeString returnType = returnTypes.isEmpty() ? TypeString.UNDEFINED : returnTypes.get(0);

    // Figure operator & lhs & rhs.
    final String operator = operatorSymbol.substring(1);
    final String lhsName = argument1Node.getTokenValue();
    final TypeString lhs = TypeString.ofIdentifier(lhsName, currentPakkage);
    final String rhsName = argument2Node.getTokenValue();
    final TypeString rhs = TypeString.ofIdentifier(rhsName, currentPakkage);
    final BinaryOperatorDefinition operatorDefinition =
        new BinaryOperatorDefinition(
            location, timestamp, moduleName, doc, statementNode, operator, lhs, rhs, returnType);
    return List.of(operatorDefinition);
  }

  private String getCurrentPakkage() {
    final PackageNodeHelper helper = new PackageNodeHelper(this.node);
    return helper.getCurrentPackage();
  }
}
