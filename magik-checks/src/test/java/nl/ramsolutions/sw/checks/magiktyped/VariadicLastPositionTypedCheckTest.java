package nl.ramsolutions.sw.checks.magiktyped;

import static nl.ramsolutions.sw.checks.magiktyped.MagikTypedCheckAssert.assertThat;

import nl.ramsolutions.sw.checks.MagikTypedCheck;
import nl.ramsolutions.sw.magik.analysis.definitions.DefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import org.junit.jupiter.api.Test;

/** Tests for {@link VariadicLastPositionTypedCheck}. */
class VariadicLastPositionTypedCheckTest {

  @Test
  void testNoIssueWhenVariadicIsLast() {
    final String code =
        """
        _method object.test
            ## @return {sw:symbol} description
            ## @return {sw:integer...} variadic tail
        _endmethod
        """;
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final MagikTypedCheck check = new VariadicLastPositionTypedCheck();
    assertThat(check).reportsNoIssues(code, definitionKeeper);
  }

  @Test
  void testIssueWhenVariadicIsNotLast() {
    final String code =
        """
        _method object.test
            ## @return {sw:integer...} variadic
            ## @return {sw:symbol} after variadic
        _endmethod
        """;
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final MagikTypedCheck check = new VariadicLastPositionTypedCheck();
    assertThat(check).reportsIssueCount(code, definitionKeeper, 1);
  }

  @Test
  void testNoIssueWhenNoVariadic() {
    final String code =
        """
        _method object.test
            ## @return {sw:symbol} just one
        _endmethod
        """;
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final MagikTypedCheck check = new VariadicLastPositionTypedCheck();
    assertThat(check).reportsNoIssues(code, definitionKeeper);
  }

  @Test
  void testNoIssueWhenVariadicLoopIsLast() {
    final String code =
        """
        _iter _method object.test
            ## @loop {sw:symbol} description
            ## @loop {sw:integer...} variadic tail
            _loopbody(:a, 1)
        _endmethod
        """;
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final MagikTypedCheck check = new VariadicLastPositionTypedCheck();
    assertThat(check).reportsNoIssues(code, definitionKeeper);
  }

  @Test
  void testIssueWhenVariadicLoopIsNotLast() {
    final String code =
        """
        _iter _method object.test
            ## @loop {sw:integer...} variadic
            ## @loop {sw:symbol} after variadic
            _loopbody(1, :a)
        _endmethod
        """;
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final MagikTypedCheck check = new VariadicLastPositionTypedCheck();
    assertThat(check).reportsIssueCount(code, definitionKeeper, 1);
  }

  @Test
  void testIndependentReturnAndLoopFlagging() {
    // Both @return and @loop have a non-last variadic; should report 2 issues.
    final String code =
        """
        _iter _method object.test
            ## @return {sw:integer...} non-last variadic
            ## @return {sw:symbol}
            ## @loop {sw:integer...} non-last variadic
            ## @loop {sw:symbol}
            _loopbody(1, :a)
        _endmethod
        """;
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final MagikTypedCheck check = new VariadicLastPositionTypedCheck();
    assertThat(check).reportsIssueCount(code, definitionKeeper, 2);
  }
}
