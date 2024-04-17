package nl.ramsolutions.sw.magik.typedchecks.checks;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import nl.ramsolutions.sw.magik.analysis.definitions.DefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.ExemplarDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import nl.ramsolutions.sw.magik.checks.MagikIssue;
import nl.ramsolutions.sw.magik.typedchecks.MagikTypedCheck;
import org.junit.jupiter.api.Test;

/** Test {@link DeprecatedTypeUsageTypedCheck}. */
public class DeprecatedTypeUsageTypedCheckTest extends MagikTypedCheckTestBase {

  private void addExemplarDefinition(
      final IDefinitionKeeper definitionKeeper, final TypeString typeName, final String... topics) {
    definitionKeeper.add(
        new ExemplarDefinition(
            null,
            null,
            null,
            null,
            ExemplarDefinition.Sort.SLOTTED,
            typeName,
            Collections.emptyList(),
            Collections.emptyList(),
            Set.of(topics)));
  }

  @Test
  void testTypeDeprecated() {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final TypeString typeStr = TypeString.ofIdentifier("test", "user");
    this.addExemplarDefinition(definitionKeeper, typeStr, "deprecated");
    final String code =
        """
        _block
          user:test.m()
        _endblock""";
    final MagikTypedCheck check = new DeprecatedTypeUsageTypedCheck();
    final List<MagikIssue> issues = this.runCheck(code, definitionKeeper, check);
    assertThat(issues).hasSize(1);
  }

  @Test
  void testTypeNotDeprecated() {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final TypeString typeStr = TypeString.ofIdentifier("test", "user");
    this.addExemplarDefinition(definitionKeeper, typeStr, "not_deprecated");
    final String code =
        """
        _block
          user:test.m()
        _endblock""";
    final MagikTypedCheck check = new DeprecatedTypeUsageTypedCheck();
    final List<MagikIssue> issues = this.runCheck(code, definitionKeeper, check);
    assertThat(issues).isEmpty();
  }
}
