package nl.ramsolutions.sw.checks.magik;

import com.sonar.sslr.api.AstNode;
import nl.ramsolutions.sw.checks.MagikCheck;
import nl.ramsolutions.sw.magik.analysis.helpers.ProcedureDefinitionNodeHelper;
import org.sonar.check.Rule;

/** Check if a procedure has a label (@name). */
@Rule(key = UnnamedProcedureCheck.CHECK_KEY)
public class UnnamedProcedureCheck extends MagikCheck {

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final String CHECK_KEY = "UnnamedProcedure";

  private static final String MESSAGE = "Procedure has no name.";

  @Override
  protected void walkPreProcedureDefinition(final AstNode node) {
    final ProcedureDefinitionNodeHelper helper = new ProcedureDefinitionNodeHelper(node);
    if (helper.getProcedureName() == null) {
      this.addIssue(helper.getProcedureNode(), MESSAGE);
    }
  }
}
