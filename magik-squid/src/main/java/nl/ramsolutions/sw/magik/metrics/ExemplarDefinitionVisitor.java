package nl.ramsolutions.sw.magik.metrics;

import com.sonar.sslr.api.AstNode;
import nl.ramsolutions.sw.magik.MagikVisitor;
import nl.ramsolutions.sw.magik.analysis.helpers.ArgumentsNodeHelper;
import nl.ramsolutions.sw.magik.analysis.helpers.ProcedureInvocationNodeHelper;
import nl.ramsolutions.sw.magik.api.MagikGrammar;

/** Exemplar definition visitor. */
public class ExemplarDefinitionVisitor extends MagikVisitor {

  private int count;

  public int getCount() {
    return this.count;
  }

  @Override
  protected void walkPreMagik(final AstNode node) {
    this.count = 0;
  }

  @Override
  public void walkPreProcedureInvocation(final AstNode node) {
    final ProcedureInvocationNodeHelper invocationHelper = new ProcedureInvocationNodeHelper(node);
    if (!invocationHelper.isProcedureInvocationOf("def_slotted_exemplar")
        && !invocationHelper.isProcedureInvocationOf("def_indexed_exemplar")
        && !invocationHelper.isProcedureInvocationOf("def_enumeration")
        && !invocationHelper.isProcedureInvocationOf("def_enumeration_from")) {
      return;
    }

    final AstNode argumentsNode = node.getFirstChild(MagikGrammar.ARGUMENTS);
    final ArgumentsNodeHelper argumentsHelper = new ArgumentsNodeHelper(argumentsNode);
    final AstNode symbolNode = argumentsHelper.getArgument(0, MagikGrammar.SYMBOL);
    if (symbolNode == null) {
      return;
    }

    // exemplar found
    this.count++;
  }
}
