package nl.ramsolutions.sw.magik.typedchecks.checks;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import nl.ramsolutions.sw.magik.analysis.definitions.DefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodDefinition;
import nl.ramsolutions.sw.magik.analysis.typing.types.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.types.TypeString;
import nl.ramsolutions.sw.magik.checks.MagikIssue;
import nl.ramsolutions.sw.magik.typedchecks.MagikTypedCheck;
import org.junit.jupiter.api.Test;

/** Test {@link UndefinedMethodCallResultTypedCheck}. */
class UndefinedMethodCallResultTypedCheckTest extends MagikTypedCheckTestBase {

  @Test
  void testMethodInvocationUndefined() {
    final String code = "" + "_block\n" + "  object.m()\n" + "_endblock";
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final MagikTypedCheck check = new UndefinedMethodCallResultTypedCheck();
    final List<MagikIssue> issues = this.runCheck(code, definitionKeeper, check);
    assertThat(issues).hasSize(1);
  }

  @Test
  void testMethodInvocationDefined() {
    final String code = "" + "_block\n" + "  object.m()\n" + "_endblock";
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    definitionKeeper.add(
        new MethodDefinition(
            null,
            null,
            null,
            null,
            TypeString.SW_OBJECT,
            "m()",
            EnumSet.noneOf(MethodDefinition.Modifier.class),
            Collections.emptyList(),
            null,
            Collections.emptySet(),
            new ExpressionResultString(TypeString.SW_OBJECT),
            ExpressionResultString.EMPTY));

    final MagikTypedCheck check = new UndefinedMethodCallResultTypedCheck();
    final List<MagikIssue> issues = this.runCheck(code, definitionKeeper, check);
    assertThat(issues).isEmpty();
  }

  @Test
  void testMethodInvocationOnUndefined() {
    final String code = "" + "_block\n" + "  undefined.m()\n" + "_endblock";
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final MagikTypedCheck check = new UndefinedMethodCallResultTypedCheck();
    final List<MagikIssue> issues = this.runCheck(code, definitionKeeper, check);
    assertThat(issues).isEmpty();
  }
}
