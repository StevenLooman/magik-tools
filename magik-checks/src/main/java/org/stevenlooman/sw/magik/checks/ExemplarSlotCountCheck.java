package org.stevenlooman.sw.magik.checks;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.AstNodeType;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.stevenlooman.sw.magik.MagikCheck;
import org.stevenlooman.sw.magik.api.MagikGrammar;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Rule(key = ExemplarSlotCountCheck.CHECK_KEY)
public class ExemplarSlotCountCheck extends MagikCheck {
  private static final String MESSAGE = "Exemplar has %s slots, more than allowed %s slots.";
  public static final String CHECK_KEY = "ExemplarSlotCount";
  private static final int DEFAULT_MAX_SLOT_COUNT = 10;

  @RuleProperty(
      key = "slot count",
      defaultValue = "" + DEFAULT_MAX_SLOT_COUNT,
      description = "Maximum number of slots for an exemplar")
  public int maxSlotCount = DEFAULT_MAX_SLOT_COUNT;

  @Override
  public boolean isTemplatedCheck() {
    return false;
  }

  @Override
  public List<AstNodeType> subscribedTo() {
    return Arrays.asList(MagikGrammar.PROCEDURE_INVOCATION);
  }

  @Override
  public void visitNode(AstNode node) {
    if (!isDefSlottedExemplar(node)) {
      return;
    }

    List<AstNode> slotDefinitions = getSlotDefinitions(node);
    int slotCount = slotDefinitions.size();
    if (slotCount > maxSlotCount) {
      String message = String.format(MESSAGE, slotCount, maxSlotCount);
      addIssue(message, node);
    }
  }

  private boolean isDefSlottedExemplar(AstNode node) {
    AstNode previousSibling = node.getPreviousSibling();
    if (previousSibling == null) {
      return false;
    }

    String tokenValue = previousSibling.getTokenValue();
    if (!"def_slotted_exemplar".equalsIgnoreCase(tokenValue)) {
      return false;
    }

    AstNode args = node.getFirstChild(MagikGrammar.ARGUMENTS);
    if (args == null) {
      return false;
    }
    AstNode arg = args.getFirstChild(MagikGrammar.ARGUMENT);
    if (arg == null) {
      return false;
    }
    AstNode symbol = arg.getFirstDescendant(MagikGrammar.SYMBOL);
    if (symbol == null) {
      return false;
    }
    return true;
  }

  private List<AstNode> getSlotDefinitions(AstNode node) {
    AstNode argumentsNode = node.getFirstChild(MagikGrammar.ARGUMENTS);
    List<AstNode> argumentNodes = argumentsNode.getChildren(MagikGrammar.ARGUMENT);
    if (argumentNodes.size() < 2) {
      return Collections.emptyList();
    }

    AstNode slotsArgumentNode = argumentNodes.get(1);
    AstNode slotsDefinitionNode = slotsArgumentNode.getFirstDescendant(MagikGrammar.SIMPLE_VECTOR);
    if (slotsDefinitionNode == null) {
      // vec() can also be used, but don't support that for now...
      return Collections.emptyList();
    }

    return slotsDefinitionNode.getChildren(MagikGrammar.EXPRESSION);
  }
}
