package nl.ramsolutions.sw.checks.magiktyped;

import static nl.ramsolutions.sw.checks.magiktyped.MagikTypedCheckAssert.assertThat;

import nl.ramsolutions.sw.checks.MagikTypedCheck;
import nl.ramsolutions.sw.magik.analysis.definitions.DefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import org.junit.jupiter.api.Test;

/** Tests for {@link IterCallableYieldTypesMatchDocTypedCheck}. */
class IterCallableYieldTypesMatchDocTypedCheckTest {

  @Test
  void testLoopTypesMatch() {
    final String code =
        """
        _iter _method a.b()
          ## @loop {integer}
          _loopbody(1)
        _endmethod
        """;
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final MagikTypedCheck check = new IterCallableYieldTypesMatchDocTypedCheck();
    assertThat(check).reportsNoIssues(code, definitionKeeper);
  }

  @Test
  void testLoopTypesDiffer() {
    final String code =
        """
        _iter _method a.b()
          ## @loop {float}
          _loopbody(1)
        _endmethod
        """;
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final MagikTypedCheck check = new IterCallableYieldTypesMatchDocTypedCheck();
    assertThat(check).reportsIssueCount(code, definitionKeeper, 1);
  }

  @Test
  void testNonIterMethodWithLoopDoc() {
    final String code =
        """
        _method a.b()
          ## @loop {integer}
        _endmethod
        """;
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final MagikTypedCheck check = new IterCallableYieldTypesMatchDocTypedCheck();
    assertThat(check).reportsIssueCount(code, definitionKeeper, 1);
  }

  @Test
  void testIgnoreAbstractIterMethod() {
    final String code =
        """
        _abstract _iter _method a.b()
          ## @loop {integer}
        _endmethod
        """;
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final MagikTypedCheck check = new IterCallableYieldTypesMatchDocTypedCheck();
    assertThat(check).reportsNoIssues(code, definitionKeeper);
  }

  @Test
  void testNoLoopDoc() {
    final String code =
        """
        _iter _method a.b()
          _loopbody(1)
        _endmethod
        """;
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final MagikTypedCheck check = new IterCallableYieldTypesMatchDocTypedCheck();
    assertThat(check).reportsIssueCount(code, definitionKeeper, 1);
  }

  @Test
  void testNoLoopDocUnknownYieldType() {
    final String code =
        """
        _iter _method a.b()
          _loopbody(_self.something())
        _endmethod
        """;
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final MagikTypedCheck check = new IterCallableYieldTypesMatchDocTypedCheck();
    assertThat(check).reportsIssueCount(code, definitionKeeper, 1);
  }

  @Test
  void testIterProcedureNoLoopDoc() {
    final String code =
        """
        _iter _proc()
          _loopbody(1)
        _endproc
        """;
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final MagikTypedCheck check = new IterCallableYieldTypesMatchDocTypedCheck();
    assertThat(check).reportsIssueCount(code, definitionKeeper, 1);
  }

  @Test
  void testIterProcedureLoopTypesDiffer() {
    final String code =
        """
        _iter _proc()
          ## @loop {float}
          _loopbody(1)
        _endproc
        """;
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final MagikTypedCheck check = new IterCallableYieldTypesMatchDocTypedCheck();
    assertThat(check).reportsIssueCount(code, definitionKeeper, 1);
  }

  @Test
  void testLoopDocWithUndefinedYieldTypeNoFalsePositive() {
    final String code =
        """
        _iter _method a.b()
          ## @loop {integer}
          _loopbody(_self.something())
        _endmethod
        """;
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final MagikTypedCheck check = new IterCallableYieldTypesMatchDocTypedCheck();
    // UNDEFINED methods are deferred to UndefinedMethodCallResultTypedCheck.
    assertThat(check).reportsNoIssues(code, definitionKeeper);
  }

  @Test
  void testNonIterProcedureWithLoopDoc() {
    final String code =
        """
        _proc()
          ## @loop {integer}
        _endproc
        """;
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final MagikTypedCheck check = new IterCallableYieldTypesMatchDocTypedCheck();
    assertThat(check).reportsIssueCount(code, definitionKeeper, 1);
  }
}
