package nl.ramsolutions.sw.magik.typedchecks.checks;

import com.sonar.sslr.api.AstNode;
import nl.ramsolutions.sw.magik.analysis.helpers.SlotNodeHelper;
import nl.ramsolutions.sw.magik.analysis.typing.types.AbstractType;
import nl.ramsolutions.sw.magik.analysis.typing.types.Slot;
import nl.ramsolutions.sw.magik.analysis.typing.types.UndefinedType;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.typedchecks.MagikTypedCheck;
import org.sonar.check.Rule;

/** Check to check if the type the method is defined on has the used slot. */
@Rule(key = SlotExistsTypedCheck.CHECK_KEY)
public class SlotExistsTypedCheck extends MagikTypedCheck {

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final String CHECK_KEY = "SlotExists";

  private static final String MESSAGE = "Unknown slot: %s";

  @Override
  protected void walkPostSlot(final AstNode node) {
    final AstNode methodDefNode = node.getFirstAncestor(MagikGrammar.METHOD_DEFINITION);
    final AbstractType type = this.getTypeOfMethodDefinition(methodDefNode);
    if (type == UndefinedType.INSTANCE) {
      // Cannot give any useful information, so abort.
      return;
    }

    final SlotNodeHelper helper = new SlotNodeHelper(node);
    final String slotName = helper.getSlotName();
    final Slot slot = type.getSlot(slotName);
    if (slot == null) {
      final String message = String.format(MESSAGE, slotName);
      final AstNode identifierNode = node.getFirstChild(MagikGrammar.IDENTIFIER);
      this.addIssue(identifierNode, message);
    }
  }
}
