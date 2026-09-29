package nl.ramsolutions.sw.magik.analysis.definitions.parsers;

import com.sonar.sslr.api.AstNode;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import java.util.List;
import nl.ramsolutions.sw.magik.analysis.helpers.PackageNodeHelper;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.api.MagikKeyword;

/** Names a {@code _proc() .. _endproc}: after the global it is bound to, or anonymously. */
public final class ProcedureNamer {

  private ProcedureNamer() {}

  /**
   * Get the name for a PROCEDURE_DEFINITION.
   *
   * <p>A procedure bound to a top level name -- {@code name << _proc ..} or {@code _global name <<
   * _proc ..} -- is named {@code <package>:<name>}, matching what a dumped type database records
   * for it. Anything else gets its anonymous name.
   *
   * @param node PROCEDURE_DEFINITION node.
   * @return Name.
   */
  public static TypeString getNameForProcedure(final AstNode node) {
    if (node.isNot(MagikGrammar.PROCEDURE_DEFINITION)) {
      throw new IllegalArgumentException();
    }

    final String name = ProcedureNamer.getTopLevelAssignedName(node);
    if (name == null) {
      return AnonymousNamer.getNameForProcedure(node);
    }

    final PackageNodeHelper helper = new PackageNodeHelper(node);
    final String currentPackage = helper.getCurrentPackage();
    return TypeString.ofIdentifier(name, currentPackage);
  }

  @CheckForNull
  static String getTopLevelAssignedName(final AstNode node) {
    final AstNode atomNode = node.getParent();
    if (atomNode == null || atomNode.isNot(MagikGrammar.ATOM)) {
      return null;
    }

    final AstNode parentNode = atomNode.getParent();
    if (parentNode == null) {
      return null;
    }

    if (parentNode.is(MagikGrammar.ASSIGNMENT_EXPRESSION)) {
      return ProcedureNamer.getAssignmentExpressionName(parentNode, atomNode);
    }

    // `_global name << _proc ..`: ATOM -> EXPRESSION -> VARIABLE_DEFINITION.
    if (parentNode.is(MagikGrammar.EXPRESSION)) {
      return ProcedureNamer.getVariableDefinitionName(parentNode);
    }

    return null;
  }

  @CheckForNull
  private static String getAssignmentExpressionName(
      final AstNode assignmentNode, final AstNode atomNode) {
    final AstNode lastChildNode = assignmentNode.getLastChild();
    if (lastChildNode != atomNode) {
      return null;
    }

    // A chained `a << b << _proc ..` has more than one target; no single name to take.
    final List<AstNode> atomNodes = assignmentNode.getChildren(MagikGrammar.ATOM);
    if (atomNodes.size() != 2) {
      return null;
    }

    final AstNode targetNode = assignmentNode.getFirstChild();
    if (targetNode.isNot(MagikGrammar.ATOM)) {
      return null;
    }

    final AstNode identifierNode = targetNode.getFirstChild(MagikGrammar.IDENTIFIER);
    if (identifierNode == null || !ProcedureNamer.isTopLevelStatement(assignmentNode)) {
      return null;
    }

    // A `_local`/`_constant`/`_dynamic` declared anywhere at file top level scopes the identifier
    // to the file; only a `_global` declaration keeps it reachable by name.
    final String name = identifierNode.getTokenValue();
    if (ProcedureNamer.hasNonGlobalTopLevelDeclaration(assignmentNode, name)) {
      return null;
    }

    return name;
  }

  private static boolean hasNonGlobalTopLevelDeclaration(
      final AstNode assignmentNode, final String name) {
    final AstNode statementNode = assignmentNode.getFirstAncestor(MagikGrammar.STATEMENT);
    if (statementNode == null) {
      return false;
    }
    final AstNode magikNode = statementNode.getParent();
    if (magikNode == null) {
      return false;
    }
    for (final AstNode childStatement : magikNode.getChildren(MagikGrammar.STATEMENT)) {
      if (ProcedureNamer.isNonGlobalDeclarationOf(childStatement, name)) {
        return true;
      }
    }
    return false;
  }

  private static boolean isNonGlobalDeclarationOf(final AstNode statement, final String name) {
    final AstNode varDefStmt = statement.getFirstChild(MagikGrammar.VARIABLE_DEFINITION_STATEMENT);
    if (varDefStmt == null) {
      return false;
    }
    final AstNode modifierNode =
        varDefStmt.getFirstChild(MagikGrammar.VARIABLE_DEFINITION_MODIFIER);
    final String globalKeyword = MagikKeyword.GLOBAL.getValue();
    if (modifierNode != null && modifierNode.getTokenValue().equalsIgnoreCase(globalKeyword)) {
      return false;
    }
    final List<AstNode> variableDefinitionNodes =
        varDefStmt.getChildren(MagikGrammar.VARIABLE_DEFINITION);
    for (final AstNode variableDefinitionNode : variableDefinitionNodes) {
      final AstNode identifierNode = variableDefinitionNode.getFirstChild(MagikGrammar.IDENTIFIER);
      if (identifierNode != null && identifierNode.getTokenValue().equals(name)) {
        return true;
      }
    }
    return false;
  }

  @CheckForNull
  private static String getVariableDefinitionName(final AstNode expressionNode) {
    final AstNode variableDefinitionNode = expressionNode.getParent();
    if (variableDefinitionNode == null
        || variableDefinitionNode.isNot(MagikGrammar.VARIABLE_DEFINITION)) {
      return null;
    }

    final AstNode statementNode = variableDefinitionNode.getParent();
    if (statementNode == null
        || statementNode.isNot(MagikGrammar.VARIABLE_DEFINITION_STATEMENT)
        || !ProcedureNamer.isTopLevelStatement(statementNode)) {
      return null;
    }

    // Only `_global`: a `_local`/`_constant`/`_dynamic` procedure is not reachable by name.
    final AstNode modifierNode =
        statementNode.getFirstChild(MagikGrammar.VARIABLE_DEFINITION_MODIFIER);
    final String globalKeyword = MagikKeyword.GLOBAL.getValue();
    if (modifierNode == null || !modifierNode.getTokenValue().equalsIgnoreCase(globalKeyword)) {
      return null;
    }

    final AstNode identifierNode = variableDefinitionNode.getFirstChild(MagikGrammar.IDENTIFIER);
    return identifierNode != null ? identifierNode.getTokenValue() : null;
  }

  private static boolean isTopLevelStatement(final AstNode node) {
    final AstNode statementNode = node.getFirstAncestor(MagikGrammar.STATEMENT);
    if (statementNode == null) {
      return false;
    }

    final AstNode parentNode = statementNode.getParent();
    return parentNode != null && parentNode.is(MagikGrammar.MAGIK);
  }
}
