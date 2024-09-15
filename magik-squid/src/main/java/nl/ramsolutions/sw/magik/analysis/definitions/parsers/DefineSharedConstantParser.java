package nl.ramsolutions.sw.magik.analysis.definitions.parsers;

import com.sonar.sslr.api.AstNode;
import java.net.URI;
import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.MagikFile;
import nl.ramsolutions.sw.magik.analysis.definitions.MagikDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.ParameterDefinition;
import nl.ramsolutions.sw.magik.analysis.helpers.ArgumentsNodeHelper;
import nl.ramsolutions.sw.magik.analysis.helpers.MethodInvocationNodeHelper;
import nl.ramsolutions.sw.magik.analysis.helpers.PackageNodeHelper;
import nl.ramsolutions.sw.magik.analysis.typing.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.parser.MagikCommentExtractor;
import nl.ramsolutions.sw.magik.parser.TypeDocParser;
import nl.ramsolutions.sw.moduledef.ModuleDefFile;

/** {@code define_shared_constant()} parser. */
public class DefineSharedConstantParser {

  private static final String DEFINE_SHARED_CONSTANT = "define_shared_constant()";
  private static final String FLAVOR_PRIVATE = ":private";
  private static final String TRUE = "_true";

  private final MagikFile magikFile;
  private final AstNode node;

  /**
   * Constructor.
   *
   * @param node {@code define_shared_constant()} node.
   */
  public DefineSharedConstantParser(final MagikFile magikFile, final AstNode node) {
    if (node.isNot(MagikGrammar.METHOD_INVOCATION)) {
      throw new IllegalArgumentException();
    }

    this.magikFile = magikFile;
    this.node = node;
  }

  /**
   * Test if node is a {@code define_shared_constant()}.
   *
   * @param node Node to test
   * @return True if node is a {@code define_shared_variable()}, false otherwise.
   */
  public static boolean isDefineSharedConstant(final AstNode node) {
    if (!node.is(MagikGrammar.METHOD_INVOCATION)) {
      return false;
    }

    final MethodInvocationNodeHelper helper = new MethodInvocationNodeHelper(node);
    if (!helper.isMethodInvocationOf(DEFINE_SHARED_CONSTANT)) {
      return false;
    }

    // Some sanity.
    final AstNode parentNode = node.getParent();
    final AstNode atomNode = parentNode.getFirstChild();
    if (atomNode.isNot(MagikGrammar.ATOM)) {
      return false;
    }
    final String exemplarName = atomNode.getTokenValue(); // Assume this is an exemplar.
    if (exemplarName == null) {
      return false;
    }

    final AstNode argumentsNode = node.getFirstChild(MagikGrammar.ARGUMENTS);
    final ArgumentsNodeHelper argumentsHelper = new ArgumentsNodeHelper(argumentsNode);
    final AstNode argument0Node = argumentsHelper.getArgument(0, MagikGrammar.SYMBOL);
    final AstNode argument2Node =
        argumentsHelper.getArgument(2, MagikGrammar.SYMBOL, MagikGrammar.TRUE, MagikGrammar.FALSE);
    return argument0Node != null && argument2Node != null;
  }

  /**
   * Parse defitions.
   *
   * @return List of parsed definitions.
   */
  public List<MagikDefinition> parseDefinitions() {
    // Some sanity.
    final AstNode parentNode = this.node.getParent();
    final AstNode atomNode = parentNode.getFirstChild();
    if (atomNode.isNot(MagikGrammar.ATOM)) {
      throw new IllegalStateException();
    }
    final String identifier = atomNode.getTokenValue(); // Assume this is an exemplar.
    if (identifier == null) {
      throw new IllegalStateException();
    }

    final AstNode argumentsNode = this.node.getFirstChild(MagikGrammar.ARGUMENTS);
    final ArgumentsNodeHelper argumentsHelper = new ArgumentsNodeHelper(argumentsNode);
    final AstNode argument0Node = argumentsHelper.getArgument(0, MagikGrammar.SYMBOL);
    final AstNode argument2Node =
        argumentsHelper.getArgument(2, MagikGrammar.SYMBOL, MagikGrammar.TRUE, MagikGrammar.FALSE);
    if (argument0Node == null || argument2Node == null) {
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

    // Figure doc.
    final String doc = MagikCommentExtractor.extractDocComment(parentNode);

    // Figure type doc.
    final TypeDocParser docParser = new TypeDocParser(parentNode);
    final List<TypeString> returnTypeRefs = docParser.getReturnTypes();
    final TypeString typeRef =
        returnTypeRefs.isEmpty() ? TypeString.UNDEFINED : returnTypeRefs.get(0);

    final String constantNameSymbol = argument0Node.getTokenValue();
    final String constantName = constantNameSymbol.substring(1);

    final Set<MethodDefinition.Modifier> modifiers = new HashSet<>();
    final String isPrivate = argument2Node.getTokenValue();
    if (isPrivate.equals(FLAVOR_PRIVATE) || isPrivate.equals(TRUE)) {
      modifiers.add(MethodDefinition.Modifier.PRIVATE);
    }
    final List<ParameterDefinition> parameters = Collections.emptyList();
    final TypeString exemplarName = TypeString.ofIdentifier(identifier, pakkage);
    final MethodDefinition methodDefinition =
        new MethodDefinition(
            location,
            timestamp,
            moduleName,
            doc,
            statementNode,
            exemplarName,
            constantName,
            modifiers,
            parameters,
            null,
            Collections.emptySet(),
            new ExpressionResultString(typeRef),
            ExpressionResultString.EMPTY);
    return List.of(methodDefinition);
  }

  private String getCurrentPakkage() {
    final PackageNodeHelper helper = new PackageNodeHelper(this.node);
    return helper.getCurrentPackage();
  }
}
