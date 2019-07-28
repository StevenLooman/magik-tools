package org.stevenlooman.sw.magik.checks;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.AstNodeType;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.stevenlooman.sw.magik.MagikCheck;
import org.stevenlooman.sw.magik.api.MagikGrammar;
import org.stevenlooman.sw.magik.metrics.ComplexityVisitor;

import java.util.Arrays;
import java.util.List;

@Rule(
    key = MethodComplexityCheck.CHECK_KEY,
    name = "MethodComplexity",
    description = "Check if the McCabe complexity of a method is too high")
public class MethodComplexityCheck extends MagikCheck {

  public static final String CHECK_KEY = "MethodComplexity";
  private static final int DEFAULT_MAXIMUM_COMPLEXITY = 10;
  private static final String MESSAGE =
      "Method has a complexity of %s which is greater than %s.";

  @RuleProperty(
      key = "maximum complexity",
      defaultValue = "" + DEFAULT_MAXIMUM_COMPLEXITY,
      description = "Maximum complexity of method by the McCabe definition")
  public int maximumComplexity = DEFAULT_MAXIMUM_COMPLEXITY;

  @Override
  public boolean isTemplatedCheck() {
    return false;
  }

  @Override
  public List<AstNodeType> subscribedTo() {
    return Arrays.asList(
        MagikGrammar.METHOD_DEFINITION,
        MagikGrammar.PROC_DEFINITION);
  }

  @Override
  public void visitNode(AstNode node) {
    ComplexityVisitor visitor = new ComplexityVisitor();
    visitor.scanNode(node);

    int complexity = visitor.getComplexity();
    if (complexity > maximumComplexity) {
      String message = String.format(MESSAGE, complexity, maximumComplexity);
      addIssue(message, node);
    }
  }
}
