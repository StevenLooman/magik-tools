package org.stevenlooman.sw.magik.checks;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.AstNodeType;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.stevenlooman.sw.magik.MagikCheck;
import org.stevenlooman.sw.magik.api.MagikGrammar;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Rule(key = WarnedCallCheck.CHECK_KEY)
public class WarnedCallCheck extends MagikCheck {
  public static final String CHECK_KEY = "WarnedCall";
  private static final String MESSAGE = "Call is warned.";
  private static final String DEFAULT_WARNED_CALLS = "write()";

  @RuleProperty(
      key = "Warned calls",
      defaultValue = "" + DEFAULT_WARNED_CALLS,
      description = "List of Warned calls, separated by ','")
  public String warnedCalls = DEFAULT_WARNED_CALLS;

  private Set<String> getWarnedCalls() {
    return Arrays.stream(warnedCalls.split(","))
        .map(s -> s.trim())
        .collect(Collectors.toSet());
  }

  @Override
  public List<AstNodeType> subscribedTo() {
    return Arrays.asList(
        MagikGrammar.METHOD_INVOCATION,
        MagikGrammar.PROCEDURE_INVOCATION);
  }

  @Override
  public void visitNode(AstNode node) {
    if (node.getType() == MagikGrammar.METHOD_INVOCATION) {
      visitNodeMethodInvocation(node);
    } else if (node.getType() == MagikGrammar.PROCEDURE_INVOCATION) {
      visitNodeProcedureInvocation(node);
    }
  }

  private void visitNodeMethodInvocation(AstNode node) {
    AstNode identifierNode = node.getFirstChild(MagikGrammar.IDENTIFIER);
    if (identifierNode == null) {
      return;
    }

    String identifier = "." + identifierNode.getTokenValue();
    if (!getWarnedCalls().contains(identifier)) {
      return;
    }

    addIssue(MESSAGE, node);
  }

  private void visitNodeProcedureInvocation(AstNode node) {
    AstNode parentNode = node.getParent();
    if (parentNode.getType() != MagikGrammar.POSTFIX_EXPRESSION) {
      return;
    }

    String identifier = parentNode.getTokenValue() + "()";
    if (!getWarnedCalls().contains(identifier)) {
      return;
    }

    addIssue(MESSAGE, node);
  }

}
