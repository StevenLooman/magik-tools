package nl.ramsolutions.sw.magik.analysis.scope;

import com.sonar.sslr.api.AstNode;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import nl.ramsolutions.sw.magik.analysis.AstQuery;
import nl.ramsolutions.sw.magik.analysis.MagikAstWalker;
import nl.ramsolutions.sw.magik.api.MagikGrammar;

/**
 * Collects the implicit locals of a method or procedure body: the undeclared names it assigns.
 *
 * <p>Magik makes a name assigned anywhere in a body a local of that body, whatever the order, the
 * block, or whether the assignment runs. A name is undeclared when no declaration (a variable
 * definition, a parameter, a loop or {@code _with} variable) in the body covers the assignment. A
 * nested procedure is a body of its own and is not searched.
 */
final class ImplicitLocalCollector extends MagikAstWalker {

  private final AstNode rootBodyNode;
  private final Collection<String> parameterNames;
  private final Deque<Set<String>> declaredNames = new ArrayDeque<>();
  private final Map<String, AstNode> implicitLocals = new LinkedHashMap<>();
  private int procedureDepth;

  private ImplicitLocalCollector(
      final AstNode rootBodyNode, final Collection<String> parameterNames) {
    this.rootBodyNode = rootBodyNode;
    this.parameterNames = parameterNames;
  }

  /**
   * Collect the implicit locals of a body.
   *
   * @param bodyNode BODY node of a method or procedure definition.
   * @param parameterNames Names of the definition's parameters.
   * @return Each implicit local, in source order, with the IDENTIFIER node of its first assignment.
   */
  static Map<String, AstNode> collect(
      final AstNode bodyNode, final Collection<String> parameterNames) {
    final ImplicitLocalCollector collector = new ImplicitLocalCollector(bodyNode, parameterNames);
    collector.walkAst(bodyNode);
    return collector.implicitLocals;
  }

  // A nested procedure is a body of its own; the walker still descends into it.
  @Override
  protected void walkPreProcedureDefinition(final AstNode node) {
    this.procedureDepth++;
  }

  @Override
  protected void walkPostProcedureDefinition(final AstNode node) {
    this.procedureDepth--;
  }

  @Override
  protected void walkPreBody(final AstNode node) {
    if (this.procedureDepth > 0) {
      return;
    }

    final Set<String> bodyNames = new HashSet<>();
    if (node == this.rootBodyNode) {
      bodyNames.addAll(this.parameterNames);
    }

    final List<AstNode> identifierNodes = this.getBoundIdentifiers(node);
    for (final AstNode identifierNode : identifierNodes) {
      final String identifier = identifierNode.getTokenValue();
      bodyNames.add(identifier);
    }

    this.declaredNames.push(bodyNames);
  }

  @Override
  protected void walkPostBody(final AstNode node) {
    if (this.procedureDepth > 0) {
      return;
    }

    this.declaredNames.pop();
  }

  @Override
  protected void walkPreVariableDefinitionStatement(final AstNode node) {
    if (this.procedureDepth > 0) {
      return;
    }

    this.declareVariables(node);
  }

  @Override
  protected void walkPreAssignmentExpression(final AstNode node) {
    if (this.procedureDepth > 0) {
      return;
    }

    this.addAssignmentTargets(node);
  }

  @Override
  protected void walkPreAugmentedAssignmentExpression(final AstNode node) {
    if (this.procedureDepth > 0) {
      return;
    }

    this.addAssignmentTargets(node);
  }

  @Override
  protected void walkPreMultipleAssignmentStatement(final AstNode node) {
    if (this.procedureDepth > 0) {
      return;
    }

    this.addMultipleAssignmentTargets(node);
  }

  /** The variables a construct binds for its body: loop, try and finally variables. */
  private List<AstNode> getBoundIdentifiers(final AstNode bodyNode) {
    final AstNode parentNode = bodyNode.getParent();
    if (parentNode.is(MagikGrammar.LOOP)) {
      final AstNode forNode =
          AstQuery.getParentFromChain(
              bodyNode, MagikGrammar.LOOP, MagikGrammar.OVER, MagikGrammar.FOR);
      if (forNode == null) {
        return List.of();
      }

      return AstQuery.getChildrenFromChain(
          forNode,
          MagikGrammar.FOR_VARIABLES,
          MagikGrammar.IDENTIFIERS_WITH_GATHER,
          MagikGrammar.IDENTIFIER);
    } else if (parentNode.is(MagikGrammar.WHEN)) {
      final AstNode tryNode = parentNode.getParent();
      return AstQuery.getChildrenFromChain(
          tryNode, MagikGrammar.TRY_VARIABLE, MagikGrammar.IDENTIFIER);
    } else if (parentNode.is(MagikGrammar.FINALLY)) {
      return AstQuery.getChildrenFromChain(
          parentNode, MagikGrammar.IDENTIFIERS_WITH_GATHER, MagikGrammar.IDENTIFIER);
    }

    return List.of();
  }

  private void declareVariables(final AstNode statementNode) {
    final Set<String> bodyNames = this.declaredNames.peek();
    final List<AstNode> identifierNodes =
        AstQuery.getChildrenFromChain(
            statementNode, MagikGrammar.VARIABLE_DEFINITION, MagikGrammar.IDENTIFIER);
    final List<AstNode> multiIdentifierNodes =
        AstQuery.getChildrenFromChain(
            statementNode,
            MagikGrammar.VARIABLE_DEFINITION_MULTI,
            MagikGrammar.IDENTIFIERS_WITH_GATHER,
            MagikGrammar.IDENTIFIER);
    for (final AstNode identifierNode : identifierNodes) {
      final String identifier = identifierNode.getTokenValue();
      bodyNames.add(identifier);
    }

    for (final AstNode identifierNode : multiIdentifierNodes) {
      final String identifier = identifierNode.getTokenValue();
      bodyNames.add(identifier);
    }
  }

  /** The targets of an assignment: every operand before its last chevron. */
  private void addAssignmentTargets(final AstNode assignmentNode) {
    final List<AstNode> childNodes = assignmentNode.getChildren();
    int lastChevronIndex = -1;
    for (int index = 0; index < childNodes.size(); index++) {
      final AstNode childNode = childNodes.get(index);
      final String tokenValue = childNode.getTokenValue();
      if (tokenValue.equals("<<") || tokenValue.equals("^<<")) {
        lastChevronIndex = index;
      }
    }

    final int targetCount = Math.max(lastChevronIndex, 0);
    final List<AstNode> targetNodes = childNodes.subList(0, targetCount);
    for (final AstNode childNode : targetNodes) {
      final AstNode identifierNode =
          childNode.is(MagikGrammar.ATOM) ? childNode.getFirstChild(MagikGrammar.IDENTIFIER) : null;
      if (identifierNode != null) {
        this.addTarget(identifierNode);
      }
    }
  }

  private void addMultipleAssignmentTargets(final AstNode statementNode) {
    final AstNode assignablesNode =
        statementNode.getFirstChild(MagikGrammar.MULTIPLE_ASSIGNMENT_ASSIGNABLES);
    final List<AstNode> expressionNodes = assignablesNode.getChildren(MagikGrammar.EXPRESSION);
    for (final AstNode expressionNode : expressionNodes) {
      final AstNode identifierNode =
          AstQuery.getOnlyFromChain(expressionNode, MagikGrammar.ATOM, MagikGrammar.IDENTIFIER);
      if (identifierNode != null) {
        this.addTarget(identifierNode);
      }
    }
  }

  private void addTarget(final AstNode identifierNode) {
    final String identifier = identifierNode.getTokenValue();
    if (identifier.indexOf(':') != -1 || this.isDeclared(identifier)) {
      return;
    }

    this.implicitLocals.putIfAbsent(identifier, identifierNode);
  }

  private boolean isDeclared(final String identifier) {
    return this.declaredNames.stream().anyMatch(names -> names.contains(identifier));
  }
}
