package nl.ramsolutions.sw.magik.typedchecks.checks;

import static nl.ramsolutions.sw.magik.typedchecks.checks.MagikTypedCheckAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import nl.ramsolutions.sw.magik.analysis.definitions.DefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.ExemplarDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.Pragma;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import nl.ramsolutions.sw.magik.typedchecks.MagikTypedCheck;
import org.junit.jupiter.api.Test;

/** Test {@link DeprecatedTypeUsageTypedCheck}. */
class DeprecatedTypeUsageTypedCheckTest {

  private void addExemplarDefinition(
      final IDefinitionKeeper definitionKeeper,
      final TypeString typeName,
      final String classifyLevel) {
    definitionKeeper.add(
        new ExemplarDefinition(
            null,
            null,
            null,
            null,
            null,
            ExemplarDefinition.Sort.SLOTTED,
            typeName,
            Collections.emptyList(),
            Collections.emptyList(),
            new Pragma(null, Arrays.asList(classifyLevel), Set.of(), Set.of())));
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
    assertThat(check).reportsIssueCount(code, definitionKeeper, 1);
  }

  @Test
  void testTypeNotDeprecated() {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final TypeString typeStr = TypeString.ofIdentifier("test", "user");
    this.addExemplarDefinition(definitionKeeper, typeStr, "basic");
    final String code =
        """
        _block
          user:test.m()
        _endblock""";
    final MagikTypedCheck check = new DeprecatedTypeUsageTypedCheck();
    assertThat(check).reportsNoIssues(code, definitionKeeper);
  }
}
