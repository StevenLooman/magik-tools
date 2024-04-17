package nl.ramsolutions.sw.magik.typedchecks.checks;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import nl.ramsolutions.sw.magik.analysis.definitions.DefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodDefinition;
import nl.ramsolutions.sw.magik.analysis.typing.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import nl.ramsolutions.sw.magik.checks.MagikIssue;
import nl.ramsolutions.sw.magik.typedchecks.MagikTypedCheck;
import org.junit.jupiter.api.Test;

/** Test {@link DeprecatedMethodUsageTypedCheck}. */
public class DeprecatedMethodUsageTypedCheckTest extends MagikTypedCheckTestBase {

  private void addMethodDefinition(
      final IDefinitionKeeper definitionKeeper, final String... topics) {
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
            Set.of(topics),
            ExpressionResultString.EMPTY,
            ExpressionResultString.EMPTY));
  }

  @Test
  void testMethodDeprecated() {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    this.addMethodDefinition(definitionKeeper, "deprecated");
    final String code =
        """
        _block
          object.m()
        _endblock""";
    final MagikTypedCheck check = new DeprecatedMethodUsageTypedCheck();
    final List<MagikIssue> issues = this.runCheck(code, definitionKeeper, check);
    assertThat(issues).hasSize(1);
  }

  @Test
  void testMethodNotDeprecated() {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    this.addMethodDefinition(definitionKeeper, "not_deprecated");
    final String code =
        """
        _block
          object.m()
        _endblock""";
    final MagikTypedCheck check = new DeprecatedMethodUsageTypedCheck();
    final List<MagikIssue> issues = this.runCheck(code, definitionKeeper, check);
    assertThat(issues).isEmpty();
  }
}
