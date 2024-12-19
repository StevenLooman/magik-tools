package nl.ramsolutions.sw.magik.analysis.definitions.parsers;

import com.sonar.sslr.api.AstNode;
import java.net.URI;
import java.time.Instant;
import java.util.*;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.MagikFile;
import nl.ramsolutions.sw.magik.analysis.AstQuery;
import nl.ramsolutions.sw.magik.analysis.definitions.MagikDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.ParameterDefinition;
import nl.ramsolutions.sw.magik.analysis.helpers.*;
import nl.ramsolutions.sw.magik.analysis.typing.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.parser.MagikCommentExtractor;
import nl.ramsolutions.sw.magik.parser.TypeDocParser;
import nl.ramsolutions.sw.magik.parser.TypeStringParser;
import nl.ramsolutions.sw.moduledef.ModuleDefFile;

/** {@code define_shared_constant()} parser. */
public class DefineSharedConstantParser {

  private static final String DEFINE_SHARED_CONSTANT = "define_shared_constant()";

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
    final Location location = new Location(uri, argument0Node);

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
    final List<TypeString> returnTypeRefs = new ArrayList<>(docParser.getReturnTypes());
    if (returnTypeRefs.isEmpty()) {
      returnTypeRefs.add(TypeString.UNDEFINED);
    }

    ExpressionResultString resultStr = new ExpressionResultString(returnTypeRefs);

    final AstNode argument1Node = argumentsHelper.getArgument(1, AtomTypeStringHelper.ATOM_TYPES);
    final AstNode actualArgument1Node = argumentsNode.getChildren(MagikGrammar.ARGUMENT).get(1);

    if (returnTypeRefs.size() == 1 && returnTypeRefs.get(0).equals(TypeString.UNDEFINED)) {
      if (argument1Node != null) {
        // guess type from kind of atom node (number, simple vector, ...)
        TypeString arg1TypeString = AtomTypeStringHelper.handleNode(argument1Node);
        if (arg1TypeString != null) {
          resultStr = new ExpressionResultString(arg1TypeString);
        }
      } else if (actualArgument1Node != null) {
        // guess type from invocation of .new on an exemplar
        AstNode methodIdentifier =
            AstQuery.getFirstChildFromChain(
                actualArgument1Node,
                MagikGrammar.EXPRESSION,
                MagikGrammar.POSTFIX_EXPRESSION,
                MagikGrammar.METHOD_INVOCATION,
                MagikGrammar.METHOD_NAME);

        if (methodIdentifier != null) {
          String exemplarName = methodIdentifier.getParent().getParent().getTokenValue();
          String methodName = methodIdentifier.getTokenValue();
          if (methodName != null && methodName.startsWith("new")) {
            try {
              resultStr =
                  new ExpressionResultString(
                      TypeStringParser.parseTypeString(exemplarName, getCurrentPakkage()));
            } catch (Exception ignored) {
            }
          }
        }
      }
    }

    final String constantNameSymbol = argument0Node.getTokenValue();
    final String constantName = constantNameSymbol.substring(1);

    final Set<MethodDefinition.Modifier> modifiers = new HashSet<>();
    modifiers.add(MethodDefinition.Modifier.SHARED_CONSTANT);
    final String isPrivate = argument2Node.getTokenValue();
    if (FlagFlavor.isPrivate(isPrivate)) {
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
            resultStr,
            ExpressionResultString.EMPTY);
    return List.of(methodDefinition);
  }

  private String getCurrentPakkage() {
    final PackageNodeHelper helper = new PackageNodeHelper(this.node);
    return helper.getCurrentPackage();
  }
}
