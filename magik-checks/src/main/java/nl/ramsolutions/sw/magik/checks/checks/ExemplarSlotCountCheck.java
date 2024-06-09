package nl.ramsolutions.sw.magik.checks.checks;

import com.sonar.sslr.api.AstNode;
import java.util.List;
import nl.ramsolutions.sw.magik.MagikFile;
import nl.ramsolutions.sw.magik.analysis.definitions.ExemplarDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.MagikDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.parsers.DefSlottedExemplarParser;
import nl.ramsolutions.sw.magik.checks.MagikCheck;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;

/** Check number of slots in slotted exemplar. */
@Rule(key = ExemplarSlotCountCheck.CHECK_KEY)
public class ExemplarSlotCountCheck extends MagikCheck {

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final String CHECK_KEY = "ExemplarSlotCount";

  private static final String MESSAGE = "Exemplar has too many slots (%s/%s).";
  private static final int DEFAULT_MAX_SLOT_COUNT = 10;

  /** Maximum number of slots for an exemplar. */
  @RuleProperty(
      key = "slot count",
      defaultValue = "" + DEFAULT_MAX_SLOT_COUNT,
      description = "Maximum number of slots for an exemplar",
      type = "INTEGER")
  @SuppressWarnings("checkstyle:VisibilityModifier")
  public int maxSlotCount = DEFAULT_MAX_SLOT_COUNT;

  @Override
  protected void walkPreProcedureInvocation(final AstNode node) {
    if (!DefSlottedExemplarParser.isDefSlottedExemplar(node)) {
      return;
    }

    final MagikFile magikFile = this.getMagikFile();
    final DefSlottedExemplarParser parser = new DefSlottedExemplarParser(magikFile, node);
    final List<MagikDefinition> parsedDefinitions = parser.parseDefinitions();
    final ExemplarDefinition definition = (ExemplarDefinition) parsedDefinitions.get(0);
    final int slotCount = definition.getSlots().size();
    if (slotCount > this.maxSlotCount) {
      final String message = String.format(MESSAGE, slotCount, this.maxSlotCount);
      this.addIssue(node, message);
    }
  }
}
