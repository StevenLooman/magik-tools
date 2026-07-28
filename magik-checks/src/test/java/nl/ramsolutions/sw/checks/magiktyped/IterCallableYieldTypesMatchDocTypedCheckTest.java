package nl.ramsolutions.sw.checks.magiktyped;

import static nl.ramsolutions.sw.checks.magiktyped.MagikTypedCheckAssert.assertThat;

import nl.ramsolutions.sw.checks.MagikTypedCheck;
import nl.ramsolutions.sw.magik.analysis.definitions.DefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodDefinition;
import nl.ramsolutions.sw.magik.analysis.typing.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
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

  @Test
  void testVariadicLoopDocMatchingVariadicYield() {
    final String code =
        """
        _iter _method a.b()
          ## @loop {sw:integer...}
          _local vec << {1, 2, 3}
          _loopbody(_scatter vec)
        _endmethod
        """;
    final IDefinitionKeeper definitionKeeper = withSimpleVectorForScatter();
    final MagikTypedCheck check = new IterCallableYieldTypesMatchDocTypedCheck();
    assertThat(check).reportsNoIssues(code, definitionKeeper);
  }

  @Test
  void testVariadicLoopDocMismatchedVariadicYield() {
    final String code =
        """
        _iter _method a.b()
          ## @loop {sw:integer...}
          _local vec << {:a, :b, :c}
          _loopbody(_scatter vec)
        _endmethod
        """;
    final IDefinitionKeeper definitionKeeper = withSimpleVectorForScatter();
    final MagikTypedCheck check = new IterCallableYieldTypesMatchDocTypedCheck();
    assertThat(check).reportsIssueCount(code, definitionKeeper, 1);
  }

  @Test
  void testLeadingPlusVariadicLoopDocMatchesLoopbody() {
    final String code =
        """
        _iter _method a.b()
          ## @loop {sw:integer}
          ## @loop {sw:symbol...}
          _local vec << {:a, :b}
          _loopbody(10, _scatter vec)
        _endmethod
        """;
    final IDefinitionKeeper definitionKeeper = withSimpleVectorForScatter();
    final MagikTypedCheck check = new IterCallableYieldTypesMatchDocTypedCheck();
    assertThat(check).reportsNoIssues(code, definitionKeeper);
  }

  private IDefinitionKeeper withSimpleVectorForScatter() {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    definitionKeeper.add(
        new MethodDefinition(
            null,
            null,
            null,
            null,
            null,
            TypeString.SW_SIMPLE_VECTOR,
            "for_scatter()",
            java.util.EnumSet.noneOf(MethodDefinition.Modifier.class),
            java.util.Collections.emptyList(),
            null,
            null,
            new ExpressionResultString(TypeString.ofVariadic(TypeString.ofGenericReference("E"))),
            ExpressionResultString.EMPTY));
    return definitionKeeper;
  }

  @Test
  void testUntypedLoopDocAlongsideTypedLoopDoc() {
    final String code =
        """
        _iter _method a.b()
          ## Yields integers. The declared
          ## @loop must survive reasoning.
          ## @loop {integer}
          _loopbody(1)
        _endmethod
        """;
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final MagikTypedCheck check = new IterCallableYieldTypesMatchDocTypedCheck();
    assertThat(check).reportsIssueCount(code, definitionKeeper, 1);
  }

  @Test
  void testUntypedLoopDocOnly() {
    final String code =
        """
        _iter _method a.b()
          ## @loop Description, but no type.
          _loopbody(1)
        _endmethod
        """;
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final MagikTypedCheck check = new IterCallableYieldTypesMatchDocTypedCheck();
    assertThat(check).reportsIssueCount(code, definitionKeeper, 1);
  }

  @Test
  void testUntypedLoopDocOnNonIterMethod() {
    final String code =
        """
        _method a.b()
          ## @loop Description, but no type.
        _endmethod
        """;
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final MagikTypedCheck check = new IterCallableYieldTypesMatchDocTypedCheck();
    assertThat(check).reportsIssueCount(code, definitionKeeper, 1);
  }

  @Test
  void testUntypedLoopDocOnAbstractIterMethod() {
    final String code =
        """
        _abstract _iter _method a.b()
          ## @loop Description, but no type.
        _endmethod
        """;
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final MagikTypedCheck check = new IterCallableYieldTypesMatchDocTypedCheck();
    assertThat(check).reportsIssueCount(code, definitionKeeper, 1);
  }
}
