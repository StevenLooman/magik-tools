package nl.ramsolutions.sw.checks.magiktyped;

import static nl.ramsolutions.sw.checks.magiktyped.MagikTypedCheckAssert.assertThat;

import nl.ramsolutions.sw.checks.MagikTypedCheck;
import nl.ramsolutions.sw.magik.analysis.definitions.DefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import org.junit.jupiter.api.Test;

/** Tests for {@link VariadicOnlyOnReturnOrLoopTypedCheck}. */
class VariadicOnlyOnReturnOrLoopTypedCheckTest {

  @Test
  void testVariadicOnReturnIsFine() {
    final String code =
        """
        _method a.b()
          ## @return {sw:integer...}
        _endmethod
        """;
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final MagikTypedCheck check = new VariadicOnlyOnReturnOrLoopTypedCheck();
    assertThat(check).reportsNoIssues(code, definitionKeeper);
  }

  @Test
  void testVariadicOnLoopIsFine() {
    final String code =
        """
        _iter _method a.b()
          ## @loop {sw:integer...}
          _loopbody(1)
        _endmethod
        """;
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final MagikTypedCheck check = new VariadicOnlyOnReturnOrLoopTypedCheck();
    assertThat(check).reportsNoIssues(code, definitionKeeper);
  }

  @Test
  void testVariadicOnParamFlagged() {
    final String code =
        """
        _method a.b(_gather args)
          ## @param {sw:integer...} args
        _endmethod
        """;
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final MagikTypedCheck check = new VariadicOnlyOnReturnOrLoopTypedCheck();
    assertThat(check).reportsIssueCount(code, definitionKeeper, 1);
  }

  @Test
  void testVariadicOnSlotFlagged() {
    final String code =
        """
        _pragma(classify_level=basic, topic={demo})
        ## @slot {sw:integer...} slot1
        def_slotted_exemplar(
            :my_exemplar,
            {
                {:slot1, _unset}
            })
        $
        """;
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final MagikTypedCheck check = new VariadicOnlyOnReturnOrLoopTypedCheck();
    assertThat(check).reportsIssueCount(code, definitionKeeper, 1);
  }

  @Test
  void testMixedTagsFlagsOnlyParamSlot() {
    final String code =
        """
        _iter _method a.b(_gather args)
          ## @param {sw:integer...} args
          ## @loop {sw:symbol...}
          ## @return {sw:integer...}
          _loopbody(1)
        _endmethod
        """;
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final MagikTypedCheck check = new VariadicOnlyOnReturnOrLoopTypedCheck();
    assertThat(check).reportsIssueCount(code, definitionKeeper, 1);
  }

  @Test
  void testNoVariadicNoIssue() {
    final String code =
        """
        _method a.b(arg)
          ## @param {sw:integer} arg
          ## @return {sw:symbol}
        _endmethod
        """;
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final MagikTypedCheck check = new VariadicOnlyOnReturnOrLoopTypedCheck();
    assertThat(check).reportsNoIssues(code, definitionKeeper);
  }
}
