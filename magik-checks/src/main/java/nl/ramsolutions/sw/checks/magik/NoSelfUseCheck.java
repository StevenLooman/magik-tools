package nl.ramsolutions.sw.checks.magik;

import com.sonar.sslr.api.AstNode;
import nl.ramsolutions.sw.checks.DisabledByDefault;
import nl.ramsolutions.sw.checks.MagikCheck;
import nl.ramsolutions.sw.magik.analysis.helpers.MethodDefinitionNodeHelper;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import org.sonar.check.Rule;

/** Check for _self/_super/_clone/slot use in method. */
@DisabledByDefault
@Rule(key = NoSelfUseCheck.CHECK_KEY)
public class NoSelfUseCheck extends MagikCheck {

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final String CHECK_KEY = "NoSelfUse";

  private static final String MESSAGE = "No self use in method.";

  @Override
  protected void walkPreMethodDefinition(final AstNode node) {
    if (this.isAbstractMethod(node)) {
      return;
    }

    // _self/_clone/_super/slot
    final boolean anyDescendants =
        !node.getDescendants(
                MagikGrammar.SELF, MagikGrammar.CLONE, MagikGrammar.SUPER, MagikGrammar.SLOT)
            .isEmpty();
    if (anyDescendants) {
      return;
    }

    final MethodDefinitionNodeHelper helper = new MethodDefinitionNodeHelper(node);
    final AstNode methodNameNode = helper.getMethodNameNode();
    this.addIssue(methodNameNode, MESSAGE);
  }

  private boolean isAbstractMethod(final AstNode node) {
    final MethodDefinitionNodeHelper helper = new MethodDefinitionNodeHelper(node);
    return helper.isAbstractMethod();
  }
}
