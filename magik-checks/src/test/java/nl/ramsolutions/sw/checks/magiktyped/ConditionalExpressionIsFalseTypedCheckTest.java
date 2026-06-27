package nl.ramsolutions.sw.checks.magiktyped;

import static nl.ramsolutions.sw.checks.magiktyped.MagikTypedCheckAssert.assertThat;

import java.util.Collections;
import java.util.EnumSet;
import nl.ramsolutions.sw.checks.MagikTypedCheck;
import nl.ramsolutions.sw.magik.analysis.definitions.DefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodDefinition;
import nl.ramsolutions.sw.magik.analysis.typing.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import org.junit.jupiter.api.Test;
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

  @Test
  void testRespondsToShouldBeBoolean() {
    final String code =
        """
        _if :a
        _then
        _endif
        """;
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    definitionKeeper.add(
        new MethodDefinition(
            null,
            null,
            null,
            null,
            null,
            TypeString.SW_SYMBOL,
            "should_be_boolean()",
            EnumSet.noneOf(MethodDefinition.Modifier.class),
            Collections.emptyList(),
            null,
            null,
            new ExpressionResultString(TypeString.SW_FALSE),
            ExpressionResultString.EMPTY));
    final MagikTypedCheck check = new ConditionalExpressionIsFalseTypedCheck();
    assertThat(check).reportsNoIssues(code, definitionKeeper);
  }
}
