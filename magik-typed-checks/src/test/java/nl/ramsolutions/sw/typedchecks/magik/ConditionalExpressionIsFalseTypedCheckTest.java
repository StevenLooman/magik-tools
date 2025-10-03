package nl.ramsolutions.sw.typedchecks.magik;

import static nl.ramsolutions.sw.typedchecks.magik.MagikTypedCheckAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

import nl.ramsolutions.sw.magik.analysis.definitions.DefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.typedchecks.MagikTypedCheck;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Tests for {@link ConditionalExpressionIsFalseTypedCheck}. */
class ConditionalExpressionIsFalseTypedCheckTest {

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
    assertThat(check).reportsNoIssues(code, definitionKeeper);
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
    assertThat(check).reportsIssueCount(code, definitionKeeper, 1);
  }
}
