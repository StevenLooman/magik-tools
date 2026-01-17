package nl.ramsolutions.sw.checks.magiktyped;

import static nl.ramsolutions.sw.checks.magiktyped.MagikTypedCheckAssert.assertThat;

import java.util.Collections;
import nl.ramsolutions.sw.checks.MagikTypedCheck;
import nl.ramsolutions.sw.magik.analysis.definitions.DefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.ExemplarDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Tests for {@link SystemCommandUseSimpleVectorTypedCheck}. */
class SystemCommandUseSimpleVectorTypedCheckTest {

  private static final ExemplarDefinition SYSTEM_EXEMPLAR_DEFINITION =
      new ExemplarDefinition(
          null,
          null,
          null,
          null,
          null,
          ExemplarDefinition.Sort.SLOTTED,
          TypeString.ofIdentifier("system", "sw"),
          Collections.emptyList(),
          Collections.emptyList(),
          null);

  private IDefinitionKeeper getDefinitionKeeper() {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    definitionKeeper.add(SYSTEM_EXEMPLAR_DEFINITION);
    return definitionKeeper;
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "system.do_command('ls -la')",
        "system.input_from_command('grep pattern')",
        "system.output_to_command('echo Hello World')",
        "system.start_command('date')",
        "system.start_command_with_io('whoami')"
      })
  void testInvalid(final String code) {
    final IDefinitionKeeper definitionKeeper = this.getDefinitionKeeper();
    final MagikTypedCheck check = new SystemCommandUseSimpleVectorTypedCheck();
    assertThat(check).reportsIssueCount(code, definitionKeeper, 1);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "system.do_command({'ls', '-la'})",
        "system.input_from_command({'grep', 'pattern'})",
        "system.output_to_command({'echo', 'Hello World'})",
        "system.start_command({'date'})",
        "system.start_command_with_io({'whoami'})"
      })
  void testValid(final String code) {
    final IDefinitionKeeper definitionKeeper = this.getDefinitionKeeper();
    final MagikTypedCheck check = new SystemCommandUseSimpleVectorTypedCheck();
    assertThat(check).reportsNoIssues(code, definitionKeeper);
  }
}
