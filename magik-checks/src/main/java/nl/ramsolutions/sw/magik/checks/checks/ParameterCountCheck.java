package nl.ramsolutions.sw.magik.checks.checks;

import com.sonar.sslr.api.AstNode;
import java.util.Map;
import nl.ramsolutions.sw.magik.analysis.helpers.MethodDefinitionNodeHelper;
import nl.ramsolutions.sw.magik.analysis.helpers.ProcedureDefinitionNodeHelper;
import nl.ramsolutions.sw.magik.checks.MagikCheck;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;

/** Check number of parameters in a method or procedure definition. */
@Rule(key = ParameterCountCheck.CHECK_KEY)
public class ParameterCountCheck extends MagikCheck {

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final String CHECK_KEY = "ParameterCount";

  private static final String MESSAGE = "Method or procedure has too many parameters (%s/%s).";
  private static final int DEFAULT_MAX_PARAMETER_COUNT = 6;

  /** Maximum number of slots for an exemplar. */
  @RuleProperty(
      key = "parameter count",
      defaultValue = "" + DEFAULT_MAX_PARAMETER_COUNT,
      description = "Maximum number of parameters for a method or procedure",
      type = "INTEGER")
  @SuppressWarnings("checkstyle:VisibilityModifier")
  public int maxParameterCount = DEFAULT_MAX_PARAMETER_COUNT;

  @Override
  protected void walkPostMethodDefinition(final AstNode node) {
    final MethodDefinitionNodeHelper helper = new MethodDefinitionNodeHelper(node);
    final Map<String, AstNode> parameterNodes = helper.getParameterNodes();
    final int parameterCount = parameterNodes.size();
    if (parameterCount > this.maxParameterCount) {
      final String message = String.format(MESSAGE, parameterCount, this.maxParameterCount);
      final AstNode markedNode = helper.getMethodNameNode();
      this.addIssue(markedNode, message);
    }
  }

  @Override
  protected void walkPostProcedureDefinition(final AstNode node) {
    final ProcedureDefinitionNodeHelper helper = new ProcedureDefinitionNodeHelper(node);
    final Map<String, AstNode> parameterNodes = helper.getParameterNodes();
    final int parameterCount = parameterNodes.size();
    if (parameterCount > this.maxParameterCount) {
      final String message = String.format(MESSAGE, parameterCount, this.maxParameterCount);
      final AstNode markedNode = helper.getProcedureNode();
      this.addIssue(markedNode, message);
    }
  }
}
