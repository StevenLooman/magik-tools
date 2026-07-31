package nl.ramsolutions.sw.magik.analysis.definitions.parsers;

import com.sonar.sslr.api.AstNode;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import nl.ramsolutions.sw.MagikToolsProperties;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.MagikFile;
import nl.ramsolutions.sw.magik.analysis.MagikAnalysisSettings;
import nl.ramsolutions.sw.magik.analysis.definitions.BinaryOperatorUsage;
import nl.ramsolutions.sw.magik.analysis.definitions.GlobalDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.GlobalUsage;
import nl.ramsolutions.sw.magik.analysis.definitions.MagikDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodUsage;
import nl.ramsolutions.sw.magik.analysis.definitions.Pragma;
import nl.ramsolutions.sw.magik.analysis.helpers.BinaryExpressionNodeHelper;
import nl.ramsolutions.sw.magik.analysis.helpers.MethodInvocationNodeHelper;
import nl.ramsolutions.sw.magik.analysis.helpers.PackageNodeHelper;
import nl.ramsolutions.sw.magik.analysis.helpers.PragmaNodeHelper;
import nl.ramsolutions.sw.magik.analysis.scope.GlobalScope;
import nl.ramsolutions.sw.magik.analysis.scope.Scope;
import nl.ramsolutions.sw.magik.analysis.scope.ScopeEntry;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.api.MagikKeyword;
import nl.ramsolutions.sw.magik.parser.MagikCommentExtractor;
import nl.ramsolutions.sw.magik.parser.TypeDocParser;
import nl.ramsolutions.sw.moduledef.ModuleDefFile;

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
   * Parse definitions.
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
    final ModuleDefFile moduleDefFile = this.magikFile.getModuleDefFile();
    final String moduleName =
        moduleDefFile != null ? moduleDefFile.getModuleDefinition().getName() : null;

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

    // Figure pragma.
    final PragmaNodeHelper pragmaHelper = PragmaNodeHelper.newSafe(this.node);
    final Pragma pragma =
        pragmaHelper != null
            ? new Pragma(
                pragmaHelper.getNode(),
                pragmaHelper.getClassifyLevels(),
                pragmaHelper.getTopics(),
                pragmaHelper.getUsages())
            : null;

    final AstNode valueNode = variableDefinitionNode.getFirstChild(MagikGrammar.EXPRESSION);
    final MagikToolsProperties properties = this.magikFile.getProperties();
    final MagikAnalysisSettings settings = new MagikAnalysisSettings(properties);
    final List<MethodUsage> usedMethods = this.getUsedMethods(valueNode, settings);
    final List<GlobalUsage> usedGlobals = this.getUsedGlobals(valueNode, settings);
    final List<BinaryOperatorUsage> usedBinaryOperators =
        this.getUsedBinaryOperators(valueNode, settings);

    final GlobalDefinition globalDefinition =
        new GlobalDefinition(
            location,
            timestamp,
            moduleName,
            doc,
            this.node,
            typeName,
            aliasedTypeRef,
            pragma,
            usedMethods,
            usedGlobals,
            usedBinaryOperators);
    return List.of(globalDefinition);
  }

  private List<MethodUsage> getUsedMethods(
      final AstNode valueNode, final MagikAnalysisSettings settings) {
    if (valueNode == null || !settings.getTypingIndexMethodUsages()) {
      return List.of();
    }

    final URI uri = this.node.getToken().getURI();
    return valueNode.getDescendants(MagikGrammar.METHOD_INVOCATION).stream()
        .map(
            invocationNode -> {
              final MethodInvocationNodeHelper helper =
                  new MethodInvocationNodeHelper(invocationNode);
              final String methodName = helper.getMethodName();
              final Location location = Location.validLocation(new Location(uri, invocationNode));
              return new MethodUsage(TypeString.UNDEFINED, methodName, location, invocationNode);
            })
        .toList();
  }

  private List<GlobalUsage> getUsedGlobals(
      final AstNode valueNode, final MagikAnalysisSettings settings) {
    if (valueNode == null || !settings.getTypingIndexGlobalUsages()) {
      return List.of();
    }

    final URI uri = this.node.getToken().getURI();
    final GlobalScope globalScope = this.magikFile.getGlobalScope();
    final String currentPakkage = this.getCurrentPakkage();
    return valueNode.getDescendants(MagikGrammar.IDENTIFIER).stream()
        .map(
            identifierNode -> {
              final Scope scope = globalScope.getScopeForNode(identifierNode);
              if (scope == null) {
                return null;
              }
              final ScopeEntry scopeEntry = scope.getScopeEntry(identifierNode);
              if (scopeEntry == null
                  || !scopeEntry.isType(ScopeEntry.Type.GLOBAL, ScopeEntry.Type.DYNAMIC)) {
                return null;
              }
              final TypeString ref =
                  TypeString.ofIdentifier(identifierNode.getTokenValue(), currentPakkage);
              final Location location = Location.validLocation(new Location(uri, identifierNode));
              return new GlobalUsage(ref, location, identifierNode);
            })
        .filter(Objects::nonNull)
        .toList();
  }

  private List<BinaryOperatorUsage> getUsedBinaryOperators(
      final AstNode valueNode, final MagikAnalysisSettings settings) {
    if (valueNode == null || !settings.getTypingIndexBinaryOperatorUsages()) {
      return List.of();
    }

    final URI uri = this.node.getToken().getURI();
    return valueNode
        .getDescendants(
            MagikGrammar.OR_EXPRESSION,
            MagikGrammar.XOR_EXPRESSION,
            MagikGrammar.AND_EXPRESSION,
            MagikGrammar.EQUALITY_EXPRESSION,
            MagikGrammar.RELATIONAL_EXPRESSION,
            MagikGrammar.ADDITIVE_EXPRESSION,
            MagikGrammar.MULTIPLICATIVE_EXPRESSION,
            MagikGrammar.EXPONENTIAL_EXPRESSION)
        .stream()
        .flatMap(
            binaryExpressionNode -> {
              final BinaryExpressionNodeHelper helper =
                  new BinaryExpressionNodeHelper(binaryExpressionNode);
              final Location location =
                  Location.validLocation(new Location(uri, binaryExpressionNode));
              return helper.getTriplets().stream()
                  .map(
                      triplet -> {
                        final String operator = triplet.getMiddle().getTokenValue().toLowerCase();
                        return new BinaryOperatorUsage(
                            TypeString.UNDEFINED,
                            TypeString.UNDEFINED,
                            operator,
                            location,
                            binaryExpressionNode);
                      });
            })
        .toList();
  }

  private String getCurrentPakkage() {
    final PackageNodeHelper helper = new PackageNodeHelper(this.node);
    return helper.getCurrentPackage();
  }
}
