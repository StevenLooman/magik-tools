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

/** Tests for {@link CallableReturnTypesMatchDocTypedCheck}. */
class CallableReturnTypesMatchDocTypedCheckTest {

  @Test
  void testTypesMatches() {
    final String code =
        """
        _method a.b
          ## @return {integer}
          _return 1
        _endmethod
        """;
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final MagikTypedCheck check = new CallableReturnTypesMatchDocTypedCheck();
    assertThat(check).reportsNoIssues(code, definitionKeeper);
  }

  @Test
  void testNoReturnAndNoDoc() {
    final String code =
        """
        _method a.b
        _endmethod
        """;
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final MagikTypedCheck check = new CallableReturnTypesMatchDocTypedCheck();
    assertThat(check).reportsNoIssues(code, definitionKeeper);
  }

  @Test
  void testIgnoreAbstractMethod() {
    final String code =
        """
        _abstract _method a.b
          ## @return {integer}
        _endmethod
        """;
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final MagikTypedCheck check = new CallableReturnTypesMatchDocTypedCheck();
    assertThat(check).reportsNoIssues(code, definitionKeeper);
  }

  @Test
  void testTypesDiffer() {
    final String code =
        """
        _method a.b
          ## @return {float}
          _return 1
        _endmethod
        """;
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final MagikTypedCheck check = new CallableReturnTypesMatchDocTypedCheck();
    assertThat(check).reportsIssueCount(code, definitionKeeper, 1);
  }

  @Test
  void testMissingTypeDoc() {
    final String code =
        """
        _method a.b
          _return 1
        _endmethod
        """;
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final MagikTypedCheck check = new CallableReturnTypesMatchDocTypedCheck();
    assertThat(check).reportsIssueCount(code, definitionKeeper, 1);
  }

  @Test
  void testUnexpectedReturnDoc() {
    final String code =
        """
        _method a.b
          ## @return {integer}
        _endmethod
        """;
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final MagikTypedCheck check = new CallableReturnTypesMatchDocTypedCheck();
    assertThat(check).reportsIssueCount(code, definitionKeeper, 1);
  }

  @Test
  void testMultipleReturnValuesMatch() {
    final String code =
        """
        _method a.b
          ## @return {integer}
          ## @return {symbol}
          _return 1, :a
        _endmethod
        """;
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final MagikTypedCheck check = new CallableReturnTypesMatchDocTypedCheck();
    assertThat(check).reportsNoIssues(code, definitionKeeper);
  }

  @Test
  void testMultipleReturnValuesMissingTypeDoc() {
    final String code =
        """
        _method a.b
          ## @return {integer}
          _return 1, :a
        _endmethod
        """;
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final MagikTypedCheck check = new CallableReturnTypesMatchDocTypedCheck();
    assertThat(check).reportsIssueCount(code, definitionKeeper, 1);
  }

  @Test
  void testMultipleReturnValuesTypeDiffer() {
    final String code =
        """
        _method a.b
          ## @return {integer}
          ## @return {float}
          _return 1, :a
        _endmethod
        """;
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final MagikTypedCheck check = new CallableReturnTypesMatchDocTypedCheck();
    assertThat(check).reportsIssueCount(code, definitionKeeper, 1);
  }

  @Test
  void testMultipleReturnValuesOrderMismatch() {
    final String code =
        """
        _method a.b
          ## @return {symbol}
          ## @return {integer}
          _return 1, :a
        _endmethod
        """;
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final MagikTypedCheck check = new CallableReturnTypesMatchDocTypedCheck();
    assertThat(check).reportsIssueCount(code, definitionKeeper, 2);
  }

  @Test
  void testNoReturnDocAndUndefinedMethodInvocationReturn() {
    final String code =
        """
        _method a.b
          _return object.m()
        _endmethod
        """;
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final MagikTypedCheck check = new CallableReturnTypesMatchDocTypedCheck();
    // Deferred to UndefinedMethodCallResultTypedCheck; nothing actionable here.
    assertThat(check).reportsNoIssues(code, definitionKeeper);
  }

  @Test
  void testReturnDocAndUndefinedMethodInvocationReturn() {
    final String code =
        """
        _method a.b
          ## @return {integer}
          _return object.m()
        _endmethod
        """;
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final MagikTypedCheck check = new CallableReturnTypesMatchDocTypedCheck();
    assertThat(check).reportsIssueCount(code, definitionKeeper, 1);
  }

  @Test
  void testMissingTypeDocForUndefinedReturnedVariable() {
    final String code =
        """
        _method a.b
          _local value << object.m()
          _return value
        _endmethod
        """;
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    definitionKeeper.add(
        new MethodDefinition(
            null,
            null,
            null,
            null,
            null,
            TypeString.SW_OBJECT,
            "m()",
            EnumSet.noneOf(MethodDefinition.Modifier.class),
            Collections.emptyList(),
            null,
            null,
            ExpressionResultString.UNDEFINED,
            ExpressionResultString.EMPTY));

    final MagikTypedCheck check = new CallableReturnTypesMatchDocTypedCheck();
    assertThat(check).reportsIssueCount(code, definitionKeeper, 1);
  }

  @Test
  void testMultipleUndefinedReturnEntriesDoNotExplode() {
    final String code =
        """
        _method a.b
          _local value << object.m()
          _return value, :done
        _endmethod
        """;
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    definitionKeeper.add(
        new MethodDefinition(
            null,
            null,
            null,
            null,
            null,
            TypeString.SW_OBJECT,
            "m()",
            EnumSet.noneOf(MethodDefinition.Modifier.class),
            Collections.emptyList(),
            null,
            null,
            ExpressionResultString.UNDEFINED,
            ExpressionResultString.EMPTY));

    final MagikTypedCheck check = new CallableReturnTypesMatchDocTypedCheck();
    assertThat(check).reportsIssueCount(code, definitionKeeper, 2);
  }

  @Test
  void testProcedureReturnTypesMatch() {
    final String code =
        """
        _proc()
          ## @return {integer}
          _return 1
        _endproc
        """;

    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final MagikTypedCheck check = new CallableReturnTypesMatchDocTypedCheck();
    assertThat(check).reportsNoIssues(code, definitionKeeper);
  }

  @Test
  void testProcedureReturnTypesDiffer() {
    final String code =
        """
        _proc()
          ## @return {float}
          _return 1
        _endproc
        """;

    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final MagikTypedCheck check = new CallableReturnTypesMatchDocTypedCheck();
    assertThat(check).reportsIssueCount(code, definitionKeeper, 1);
  }

  @Test
  void testEmitReturnTypeMatches() {
    final String code =
        """
        _method a.b
          ## @return {integer}
          >> 1
        _endmethod
        """;

    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final MagikTypedCheck check = new CallableReturnTypesMatchDocTypedCheck();
    assertThat(check).reportsNoIssues(code, definitionKeeper);
  }

  @Test
  void testEmitReturnTypeDiffer() {
    final String code =
        """
        _method a.b
          ## @return {float}
          >> 1
        _endmethod
        """;

    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final MagikTypedCheck check = new CallableReturnTypesMatchDocTypedCheck();
    assertThat(check).reportsIssueCount(code, definitionKeeper, 1);
  }
}
