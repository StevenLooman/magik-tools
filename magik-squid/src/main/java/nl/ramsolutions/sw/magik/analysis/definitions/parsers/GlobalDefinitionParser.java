package nl.ramsolutions.sw.magik.analysis.definitions.parsers;

import com.sonar.sslr.api.AstNode;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.MagikFile;
import nl.ramsolutions.sw.magik.analysis.definitions.GlobalDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.MagikDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.Pragma;
import nl.ramsolutions.sw.magik.analysis.definitions.Provenance;
import nl.ramsolutions.sw.magik.analysis.helpers.AssignmentExpressionNodeHelper;
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

    final List<AstNode> variableDefinitionNodes =
        this.node.getChildren(MagikGrammar.VARIABLE_DEFINITION);
    final List<MagikDefinition> definitions = new ArrayList<>();
    for (final AstNode variableDefinitionNode : variableDefinitionNodes) {
      final GlobalDefinition definition = this.parseDefinition(variableDefinitionNode);
      if (definition != null) {
        definitions.add(definition);
      }
    }
    return definitions;
  }

  @CheckForNull
  private GlobalDefinition parseDefinition(final AstNode variableDefinitionNode) {
    final AstNode identifierNode = variableDefinitionNode.getFirstChild(MagikGrammar.IDENTIFIER);
    final AstNode valueNode = variableDefinitionNode.getFirstChild(MagikGrammar.EXPRESSION);
    if (valueNode == null && !this.isAssignedInScope(identifierNode)) {
      // Without a value, which only a body allows, the statement declares a use of the global.
      return null;
    }
    if (GlobalDefinitionParser.isNamedProcedureValue(valueNode)) {
      // The procedure is indexed under this very name; a global alias would duplicate it and
      // `resolveInPackageHierarchy` would combine both into `_undefined|<procedure>`.
      return null;
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
    final String identifier = identifierNode.getTokenValue();
    final TypeString typeName = TypeString.ofIdentifier(identifier, packageName);

    // Figure type.
    final TypeDocParser docParser = new TypeDocParser(this.node);
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

    final List<AstNode> valueNodes = valueNode != null ? List.of(valueNode) : List.of();
    final DefinitionUsageParser.Usages usages =
        DefinitionUsageParser.collectUsages(this.magikFile, valueNodes, node -> false);

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
            usages.methods(),
            usages.globals(),
            usages.conditions(),
            usages.binaryOperators(),
            Provenance.UNKNOWN);
    return globalDefinition;
  }

  /** A {@code _global} without a value defines the global when its scope assigns it. */
  private boolean isAssignedInScope(final AstNode identifierNode) {
    final GlobalScope globalScope = this.magikFile.getGlobalScope();
    final Scope scope = globalScope.getScopeForNode(identifierNode);
    final ScopeEntry scopeEntry = scope != null ? scope.getScopeEntry(identifierNode) : null;
    if (scopeEntry == null) {
      return false;
    }

    final List<AstNode> usageNodes = scopeEntry.getUsages();
    return usageNodes.stream().anyMatch(AssignmentExpressionNodeHelper::isAssignmentTarget);
  }

  private String getCurrentPakkage() {
    final PackageNodeHelper helper = new PackageNodeHelper(this.node);
    return helper.getCurrentPackage();
  }

  private static boolean isNamedProcedureValue(final @Nullable AstNode valueNode) {
    if (valueNode == null) {
      return false;
    }

    final AstNode procedureNode = valueNode.getFirstDescendant(MagikGrammar.PROCEDURE_DEFINITION);
    if (procedureNode == null) {
      return false;
    }

    if (procedureNode.getFirstChild(MagikGrammar.SYNTAX_ERROR) != null
        || procedureNode.getFirstChild(MagikGrammar.PARAMETERS) == null) {
      return false;
    }

    final TypeString typeString = ProcedureNamer.getNameForProcedure(procedureNode);
    return !typeString.isAnonymous();
  }
}
