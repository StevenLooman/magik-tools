package nl.ramsolutions.sw.checks.magiktyped;

import com.sonar.sslr.api.AstNode;
import java.util.Collection;
import java.util.List;
import nl.ramsolutions.sw.checks.MagikTypedCheck;
import nl.ramsolutions.sw.magik.analysis.definitions.ArgumentRange;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.ParameterDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.ProcedureDefinition;
import nl.ramsolutions.sw.magik.analysis.helpers.ArgumentsNodeHelper;
import nl.ramsolutions.sw.magik.analysis.helpers.MethodInvocationNodeHelper;
import nl.ramsolutions.sw.magik.analysis.helpers.ProcedureInvocationDefinitionHelper;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import nl.ramsolutions.sw.magik.analysis.typing.TypeStringResolver;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import org.sonar.check.Rule;

/** Check if argument-count for invocation matches. */
@Rule(key = InvocationArgumentCountMatchesParameterCountTypedCheck.CHECK_KEY)
public class InvocationArgumentCountMatchesParameterCountTypedCheck extends MagikTypedCheck {

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final String CHECK_KEY = "InvocationArgumentCountMatchesParameterCount";

  private static final String MESSAGE_METHOD =
      "Object %s sent message %s with too few arguments (requires %d, given %d)";
  // A session names the procedure object; a check knows its type.
  private static final String MESSAGE_PROCEDURE =
      "Procedure %s invoked with too few arguments (requires %d, given %d)";

  @Override
  protected void walkPostMethodInvocation(final AstNode node) {
    // Ensure there are arguments to check.
    final AstNode argumentsNode = node.getFirstChild(MagikGrammar.ARGUMENTS);
    if (argumentsNode == null) {
      return;
    }

    // Don't bother checking scatter.
    final ArgumentsNodeHelper argumentsHelper = new ArgumentsNodeHelper(argumentsNode);
    if (argumentsHelper.hasScatterArgument()) {
      return;
    }

    // Get type.
    final TypeString calledTypeStr = this.getTypeInvokedOn(node);
    if (calledTypeStr.isUndefined()) {
      // Cannot give any useful information, so abort.
      return;
    }

    // Get methods.
    final MethodInvocationNodeHelper helper = new MethodInvocationNodeHelper(node);
    final String methodName = helper.getMethodName();
    final boolean superReceiver = helper.isSuperSend();
    final TypeStringResolver resolver = this.getTypeStringResolver();
    final Collection<MethodDefinition> methodDefs =
        resolver.getRespondingMethodDefinitions(calledTypeStr, methodName, superReceiver);
    for (final MethodDefinition methodDef : methodDefs) {
      final List<ParameterDefinition> parameterDefs = methodDef.getParameters();
      if (parameterDefs.isEmpty()) {
        continue;
      }

      // Match arguments against method.parameters.
      final List<AstNode> argumentNodes = argumentsHelper.getArgumentNodes();
      final ArgumentRange argumentRange = ArgumentRange.of(parameterDefs);
      final int required = argumentRange.required();
      final int given = argumentNodes.size();
      if (required > given) {
        final String typeName = calledTypeStr.getFullString();
        final String message = MESSAGE_METHOD.formatted(typeName, methodName, required, given);
        this.addIssue(node, message);
      }
    }
  }

  @Override
  protected void walkPostProcedureInvocation(final AstNode node) {
    // Ensure there are arguments to check.
    final AstNode argumentsNode = node.getFirstChild(MagikGrammar.ARGUMENTS);
    if (argumentsNode == null) {
      return;
    }

    // Don't bother checking scatter.
    final ArgumentsNodeHelper argumentsHelper = new ArgumentsNodeHelper(argumentsNode);
    if (argumentsHelper.hasScatterArgument()) {
      return;
    }

    // Get type.
    final TypeString calledTypeStr =
        ProcedureInvocationDefinitionHelper.getProcedureTypeInvokedOn(
            node, this.getTypeReasonerState(), this.getTypeStringResolver());

    // Get procedures.
    final Collection<ProcedureDefinition> procedureDefs =
        ProcedureInvocationDefinitionHelper.getRespondingProcedureDefinitions(
            node, this.getTypeReasonerState(), this.getTypeStringResolver(), this.getMagikFile());
    if (procedureDefs.isEmpty()) {
      // Cannot give any useful information, so abort.
      return;
    }
    for (final ProcedureDefinition procedureDef : procedureDefs) {
      final List<ParameterDefinition> parameterDefs = procedureDef.getParameters();
      if (parameterDefs.isEmpty()) {
        continue;
      }

      // Match arguments against procedure parameters.
      final List<AstNode> argumentNodes = argumentsHelper.getArgumentNodes();
      final ArgumentRange argumentRange = ArgumentRange.of(parameterDefs);
      final int required = argumentRange.required();
      final int given = argumentNodes.size();
      if (required > given) {
        // An anonymous procedure's own type has an internal name, so name it a sw:procedure.
        final TypeString procedureTypeStr =
            calledTypeStr.isAnonymous() ? TypeString.SW_PROCEDURE : calledTypeStr;
        final String typeName = procedureTypeStr.getFullString();
        final String message = MESSAGE_PROCEDURE.formatted(typeName, required, given);
        this.addIssue(node, message);
      }
    }
  }
}
