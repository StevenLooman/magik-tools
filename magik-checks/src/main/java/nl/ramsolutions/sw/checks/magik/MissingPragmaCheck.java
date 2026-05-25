package nl.ramsolutions.sw.checks.magik;

import com.sonar.sslr.api.AstNode;
import java.util.Set;
import nl.ramsolutions.sw.checks.MagikCheck;
import nl.ramsolutions.sw.magik.analysis.AstQuery;
import nl.ramsolutions.sw.magik.analysis.definitions.ConditionDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.ExemplarDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.GlobalDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.MagikDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodDefinition;
import nl.ramsolutions.sw.magik.analysis.helpers.ArgumentsNodeHelper;
import nl.ramsolutions.sw.magik.analysis.helpers.MethodDefinitionNodeHelper;
import nl.ramsolutions.sw.magik.analysis.helpers.MethodInvocationNodeHelper;
import nl.ramsolutions.sw.magik.analysis.helpers.PragmaNodeHelper;
import nl.ramsolutions.sw.magik.analysis.helpers.ProcedureInvocationNodeHelper;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import org.sonar.check.Rule;

/**
 * Check if each defined thing has a valid pragma definition.
 *
 * <ul>
 *   <li>exemplars
 *   <li>method definition (including slot accessors, shared constants, shared variables)
 *   <li>globals
 * </ul>
 */
@Rule(key = MissingPragmaCheck.CHECK_KEY)
public class MissingPragmaCheck extends MagikCheck {

  private static final Set<String> EXEMPLAR_DEFINITION_PROCEDURES =
      Set.of(
          "def_slotted_exemplar",
          "sw:def_slotted_exemplar",
          "def_indexed_exemplar",
          "sw:def_indexed_exemplar",
          "def_mixin",
          "sw:def_mixin",
          "def_enumeration",
          "sw:def_enumeration",
          "def_enumeration_from",
          "sw:def_enumeration_from");
  private static final String DEFINE_SHARED_VARIABLE = "define_shared_variable()";
  private static final String DEFINE_SHARED_CONSTANT = "define_shared_constant()";
  private static final String ADD_CHILD = "add_child()";
  private static final Set<String> DEFINITIONAL_METHOD_INVOCATIONS =
      Set.of(ADD_CHILD, DEFINE_SHARED_CONSTANT, DEFINE_SHARED_VARIABLE);

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final String CHECK_KEY = "MissingPragma";

  private static final String MESSAGE = "Definition does not have a pragma.";

  @Override
  protected void walkPostMagik(final AstNode node) {
    this.getMagikFile().getMagikDefinitions().stream()
        .filter(this::requiresPragma)
        .filter(this::isPrimaryDefinition)
        .filter(this::missingPragma)
        .map(this::getIssueNode)
        .forEach(
            issueNode -> {
              this.addIssue(issueNode, MESSAGE);
            });
  }

  private boolean requiresPragma(final MagikDefinition definition) {
    return ExemplarDefinition.class.isInstance(definition)
        || MethodDefinition.class.isInstance(definition)
        || GlobalDefinition.class.isInstance(definition)
        || ConditionDefinition.class.isInstance(definition);
  }

  private boolean isPrimaryDefinition(final MagikDefinition definition) {
    if (definition instanceof MethodDefinition methodDefinition) {
      return isPrimaryMethodDefinition(methodDefinition);
    } else if (definition instanceof GlobalDefinition globalDefinition) {
      return isPrimaryGlobalDefinition(globalDefinition);
    }

    return true;
  }

  private boolean isPrimaryMethodDefinition(MethodDefinition methodDefinition) {
    final AstNode node = methodDefinition.getNode();

    if (node.is(MagikGrammar.PROCEDURE_INVOCATION)) {
      // Methods synthesized by a def_*_exemplar/def_enumeration/def_mixin call (e.g., slot
      // accessors) are reported in addition to the exemplar definition itself.
      final ProcedureInvocationNodeHelper helper = new ProcedureInvocationNodeHelper(node);
      return EXEMPLAR_DEFINITION_PROCEDURES.stream().anyMatch(helper::isProcedureInvocationOf);
    } else if (node.is(MagikGrammar.STATEMENT)) {
      final AstNode invocationNode =
          AstQuery.getFirstChildFromChain(
              node,
              MagikGrammar.EXPRESSION_STATEMENT,
              MagikGrammar.EXPRESSION,
              MagikGrammar.POSTFIX_EXPRESSION,
              MagikGrammar.METHOD_INVOCATION);
      if (invocationNode == null) {
        return true;
      }

      // Skip methods synthesized by define_shared_variable()/define_shared_constant();
      // these are flagged at the invocation site by walkPostMethodInvocation.
      final MethodInvocationNodeHelper helper = new MethodInvocationNodeHelper(invocationNode);
      if (helper.isMethodInvocationOf(DEFINE_SHARED_VARIABLE)
          || helper.isMethodInvocationOf(DEFINE_SHARED_CONSTANT)) {
        return false;
      }
    }
    return true;
  }

  @Override
  protected void walkPostMethodInvocation(final AstNode node) {
    final MethodInvocationNodeHelper helper = new MethodInvocationNodeHelper(node);
    final String methodName = helper.getMethodName();
    if (!DEFINITIONAL_METHOD_INVOCATIONS.contains(methodName)) {
      return;
    }

    // Only flag top-level invocations; nested calls (e.g. `tree.add_child(node)` inside a method
    // body) are regular method calls, not definitions.
    final AstNode statementNode = node.getFirstAncestor(MagikGrammar.STATEMENT);
    if (statementNode == null || !statementNode.getParent().is(MagikGrammar.MAGIK)) {
      return;
    }

    if (PragmaNodeHelper.getPragmaNode(node) != null) {
      return;
    }

    this.addIssue(helper.getMethodNameNode(), MESSAGE);
  }

  private boolean isPrimaryGlobalDefinition(GlobalDefinition globalDefinition) {
    final AstNode node = globalDefinition.getNode();
    if (!node.is(MagikGrammar.VARIABLE_DEFINITION_STATEMENT)) {
      return false;
    }

    // Only from top level.
    final AstNode chainNode =
        AstQuery.getParentFromChain(node, MagikGrammar.STATEMENT, MagikGrammar.MAGIK);
    return chainNode != null;
  }

  private boolean missingPragma(final MagikDefinition definition) {
    if (definition instanceof ExemplarDefinition exemplarDefinition) {
      return exemplarDefinition.getPragma() == null;
    } else if (definition instanceof MethodDefinition methodDefinition) {
      return methodDefinition.getPragma() == null;
    } else if (definition instanceof GlobalDefinition globalDefinition) {
      return globalDefinition.getPragma() == null;
    } else if (definition instanceof ConditionDefinition conditionDefinition) {
      return conditionDefinition.getPragma() == null;
    }

    throw new IllegalStateException();
  }

  private AstNode getIssueNode(final MagikDefinition definition) {
    if (definition instanceof ExemplarDefinition exemplarDefinition) {
      final AstNode definitionNode = exemplarDefinition.getNode();
      final AstNode argumentsNode = definitionNode.getFirstDescendant(MagikGrammar.ARGUMENTS);
      final ArgumentsNodeHelper helper = new ArgumentsNodeHelper(argumentsNode);
      return helper.getArgument(0);
    } else if (definition instanceof MethodDefinition methodDefinition) {
      final AstNode definitionNode = methodDefinition.getNode();
      final AstNode argumentsNode = definitionNode.getFirstDescendant(MagikGrammar.ARGUMENTS);
      if (definitionNode.is(MagikGrammar.METHOD_DEFINITION)) {
        final MethodDefinitionNodeHelper helper = new MethodDefinitionNodeHelper(definitionNode);
        return helper.getMethodNameNode();
      } else if (argumentsNode != null) {
        final ArgumentsNodeHelper helper = new ArgumentsNodeHelper(argumentsNode);
        return helper.getArgument(0);
      } else {
        return definitionNode;
      }
    } else if (definition instanceof GlobalDefinition globalDefinition) {
      final AstNode definitionNode = globalDefinition.getNode();
      if (definitionNode.is(MagikGrammar.VARIABLE_DEFINITION_STATEMENT)) {
        return definitionNode.getFirstDescendant(MagikGrammar.IDENTIFIER);
      }

      return definitionNode;
    } else if (definition instanceof ConditionDefinition conditionDefinition) {
      final AstNode definitionNode = conditionDefinition.getNode();
      final AstNode argumentsNode = definitionNode.getFirstDescendant(MagikGrammar.ARGUMENTS);
      final ArgumentsNodeHelper helper = new ArgumentsNodeHelper(argumentsNode);
      return helper.getArgument(0);
    }

    throw new IllegalStateException();
  }
}
