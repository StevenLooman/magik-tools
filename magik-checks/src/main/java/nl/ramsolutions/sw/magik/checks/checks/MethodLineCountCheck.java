package nl.ramsolutions.sw.magik.checks.checks;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.Token;
import java.util.stream.Collectors;
import nl.ramsolutions.sw.magik.analysis.helpers.MethodDefinitionNodeHelper;
import nl.ramsolutions.sw.magik.analysis.helpers.ProcedureDefinitionNodeHelper;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.checks.MagikCheck;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;

/** Check length of method/procedure. */
@Rule(key = MethodLineCountCheck.CHECK_KEY)
public class MethodLineCountCheck extends MagikCheck {

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final String CHECK_KEY = "MethodLineCount";

  private static final int DEFAULT_MAXIMUM_LENGTH = 35;
  private static final String MESSAGE = "Method is longer than permitted (%s/%s).";

  /** Maximum length of method in lines, without whitelines and comment lines. */
  @RuleProperty(
      key = "maximum length",
      defaultValue = "" + DEFAULT_MAXIMUM_LENGTH,
      description = "Maximum length of method in lines without white lines and comment lines",
      type = "INTEGER")
  @SuppressWarnings("checkstyle:VisibilityModifier")
  public int maximumLineCount = DEFAULT_MAXIMUM_LENGTH;

  @Override
  protected void walkPreMethodDefinition(final AstNode node) {
    this.checkDefinition(node);
  }

  @Override
  protected void walkPreProcedureDefinition(final AstNode node) {
    this.checkDefinition(node);
  }

  private void checkDefinition(final AstNode node) {
    final AstNode bodyNode = node.getFirstChild(MagikGrammar.BODY);
    if (bodyNode == null) {
      // In case of a SYNTAX_ERROR.
      return;
    }

    final long lineCount =
        bodyNode.getTokens().stream().map(Token::getLine).distinct().collect(Collectors.counting());
    if (lineCount > this.maximumLineCount) {
      final String message = String.format(MESSAGE, lineCount, this.maximumLineCount);
      final AstNode markedNode;
      if (node.is(MagikGrammar.METHOD_DEFINITION)) {
        final MethodDefinitionNodeHelper helper = new MethodDefinitionNodeHelper(node);
        markedNode = helper.getMethodNameNode();
      } else {
        final ProcedureDefinitionNodeHelper helper = new ProcedureDefinitionNodeHelper(node);
        markedNode = helper.getProcedureNode();
      }
      this.addIssue(markedNode, message);
    }
  }
}
