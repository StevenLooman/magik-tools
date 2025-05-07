package nl.ramsolutions.sw.magik.checks.checks;

import com.sonar.sslr.api.AstNode;
import nl.ramsolutions.sw.magik.analysis.AstQuery;
import nl.ramsolutions.sw.magik.analysis.definitions.ConditionDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.ExemplarDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.GlobalDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.MagikDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodDefinition;
import nl.ramsolutions.sw.magik.analysis.helpers.MethodInvocationNodeHelper;
import nl.ramsolutions.sw.magik.analysis.helpers.ProcedureInvocationNodeHelper;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.checks.MagikCheck;
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
@Rule(key = HasPragmaCheck.CHECK_KEY)
public class HasPragmaCheck extends MagikCheck {

  private static final String DEF_SLOTTED_EXEMPLAR = "def_slotted_exemplar";
  private static final String SW_DEF_SLOTTED_EXEMPLAR = "sw:def_slotted_exemplar";
  private static final String DEFINE_SHARED_VARIABLE = "define_shared_variable()";

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final String CHECK_KEY = "HasPragma";

  private static final String MESSAGE = "Definition does not have a pragma.";

  @Override
  protected void walkPostMagik(final AstNode node) {
    this.getMagikFile().getMagikDefinitions().stream()
        .filter(this::requiresPragma)
        .filter(this::isPrimaryDefinition)
        .filter(this::missingPragma)
        .forEach(
            definition -> {
              final AstNode definitionNode = definition.getNode();
              this.addIssue(definitionNode, MESSAGE);
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
      final AstNode node = methodDefinition.getNode();

      if (node.is(MagikGrammar.PROCEDURE_INVOCATION)) {
        // Skip methods which were generated via a exemplar definition.
        // The exemplar definition itself will get flagged in this case.
        final ProcedureInvocationNodeHelper helper = new ProcedureInvocationNodeHelper(node);
        return helper.isProcedureInvocationOf(DEF_SLOTTED_EXEMPLAR)
            || helper.isProcedureInvocationOf(SW_DEF_SLOTTED_EXEMPLAR);
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

        // Keep only one method generated via a shared variable.
        // The shared variable definition itself will get flagged in this case.
        final MethodInvocationNodeHelper helper = new MethodInvocationNodeHelper(invocationNode);
        if (helper.isMethodInvocationOf(DEFINE_SHARED_VARIABLE)
            && (methodDefinition.getMethodName().endsWith("<<")
                || methodDefinition.getMethodName().endsWith("^<<"))) {
          return false;
        }
      }
    }

    return true;
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
}
