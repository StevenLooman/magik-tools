package nl.ramsolutions.sw.magik.typedchecks.checks;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import nl.ramsolutions.sw.magik.analysis.definitions.DefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.checks.MagikIssue;
import nl.ramsolutions.sw.magik.typedchecks.MagikTypedCheck;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Tests for {@link ConditionalExpressionIsFalseTypedCheck}. */
class ConditionalExpressionIsFalseTypedCheckTest extends MagikTypedCheckTestBase {

  @ParameterizedTest
  @ValueSource(
      strings = {
        """
        _if _true
        _then
        _endif
        """,
        """
        _if :a _is :b
        _then
        _endif
        """,
        """
        _if :a _is :b _orif :a _is :c
        _then
        _endif
        """,
      })
  void testOk(final String code) {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final MagikTypedCheck check = new ConditionalExpressionIsFalseTypedCheck();
    final List<MagikIssue> checkResults = this.runCheck(code, definitionKeeper, check);
    assertThat(checkResults).isEmpty();
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        """
        _if :a
        _then
        _endif
        """,
        """
        _if _maybe
        _then
        _endif
        """,
      })
  void testFail(final String code) {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final MagikTypedCheck check = new ConditionalExpressionIsFalseTypedCheck();
    final List<MagikIssue> checkResults = this.runCheck(code, definitionKeeper, check);
    assertThat(checkResults).hasSize(1);
  }
}
