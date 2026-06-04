package nl.ramsolutions.sw.checks.magik;

import com.sonar.sslr.api.AstNode;
import nl.ramsolutions.sw.checks.MagikCheck;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import org.sonar.check.Rule;

/** Check for emit or leave-with statements whose value is discarded. */
@Rule(key = EmitOrLeaveResultUnusedCheck.CHECK_KEY)
public class EmitOrLeaveResultUnusedCheck extends MagikCheck {

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final String CHECK_KEY = "EmitOrLeaveResultUnused";

  private static final String MESSAGE = "Result is not used.";

  @Override
  protected void walkPreEmitStatement(final AstNode node) {
    final AstNode bodyNode = node.getFirstAncestor(MagikGrammar.BODY);
    if (bodyNode == null) {
      return;
    }

    final AstNode bodyParent = bodyNode.getParent();
    if (bodyParent.is(
        MagikGrammar.METHOD_DEFINITION, MagikGrammar.PROCEDURE_DEFINITION, MagikGrammar.LOOP)) {
      return;
    }

    final AstNode rootNode = EmitOrLeaveResultUnusedCheck.getRoot(bodyParent);
    if (EmitOrLeaveResultUnusedCheck.isStandaloneStatement(rootNode)) {
      this.addIssue(node, MESSAGE);
    }
  }

  @Override
  protected void walkPreLeaveStatement(final AstNode node) {
    // Only flag _leave _with val.
    if (node.getFirstChild(MagikGrammar.TUPLE) == null) {
      return;
    }

    // Skip labeled _leave; it may target an outer loop that cannot be resolved here.
    if (node.getFirstChild(MagikGrammar.LABEL) != null) {
      return;
    }

    // A _leave targets the nearest enclosing loop or block; its _with value becomes that
    // construct's value. Flag only when that construct's value is itself discarded.
    final AstNode targetNode = node.getFirstAncestor(MagikGrammar.LOOP, MagikGrammar.BLOCK);
    if (targetNode == null) {
      return;
    }

    if (EmitOrLeaveResultUnusedCheck.isStandaloneStatement(targetNode)) {
      this.addIssue(node, MESSAGE);
    }
  }

  /**
   * Walk up through sub-nodes to find the root control structure node.
   *
   * <p>ELIF/ELSE are sub-nodes of IF; WHEN is a sub-node of TRY; PROTECTION is a sub-node of
   * PROTECT; FINALLY is a sub-node of LOOP which is itself inside OVER/WHILE/FOR.
   *
   * @param bodyParentNode The parent node of the BODY containing the emit/leave statement.
   * @return The root control structure node.
   */
  private static AstNode getRoot(final AstNode bodyParentNode) {
    if (bodyParentNode.is(MagikGrammar.ELIF, MagikGrammar.ELSE)) {
      return bodyParentNode.getParent();
    }

    if (bodyParentNode.is(MagikGrammar.WHEN)) {
      return bodyParentNode.getParent();
    }

    if (bodyParentNode.is(MagikGrammar.PROTECTION)) {
      return bodyParentNode.getParent();
    }

    if (bodyParentNode.is(MagikGrammar.FINALLY)) {
      final AstNode loopNode = bodyParentNode.getParent();
      return loopNode.getParent();
    }

    return bodyParentNode;
  }

  /**
   * Check if the control structure is used as a standalone expression statement.
   *
   * @param rootNode The root control structure node.
   * @return True if the control structure is a standalone statement, false otherwise.
   */
  private static boolean isStandaloneStatement(final AstNode rootNode) {
    final AstNode atomNode =
        rootNode.is(MagikGrammar.ATOM) ? rootNode : rootNode.getFirstAncestor(MagikGrammar.ATOM);
    if (atomNode == null) {
      return false;
    }

    AstNode parentNode = atomNode.getParent();
    while (parentNode != null && parentNode.is(MagikGrammar.ATOM, MagikGrammar.EXPRESSION)) {
      parentNode = parentNode.getParent();
    }
    return parentNode != null && parentNode.is(MagikGrammar.EXPRESSION_STATEMENT);
  }
}
