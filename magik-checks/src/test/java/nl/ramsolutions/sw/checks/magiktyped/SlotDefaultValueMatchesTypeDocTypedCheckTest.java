package nl.ramsolutions.sw.checks.magiktyped;

import static nl.ramsolutions.sw.checks.magiktyped.MagikTypedCheckAssert.assertThat;

import nl.ramsolutions.sw.checks.MagikTypedCheck;
import nl.ramsolutions.sw.magik.analysis.definitions.DefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Tests for {@link SlotDefaultValueMatchesTypeDocTypedCheck}. */
class SlotDefaultValueMatchesTypeDocTypedCheckTest {

  @ParameterizedTest
  @ValueSource(
      strings = {
        """
        ## @slot {sw:integer} slot1
        def_slotted_exemplar(:test_exemplar, {{:slot1, 1}})
        """,
        """
        ## @slot {sw:integer|sw:unset} slot1
        def_slotted_exemplar(:test_exemplar, {{:slot1, _unset}})
        """,
        """
        def_slotted_exemplar(:test_exemplar, {{:slot1, _unset}})
        """,
        """
        ## @slot {sw:integer} slot1
        def_slotted_exemplar(:test_exemplar, {{:slot1}})
        """,
        """
        ## @slot {sw:integer} slot1
        ## @slot {sw:char16_vector} slot2
        def_slotted_exemplar(:test_exemplar, {{:slot1, 1}, {:slot2, "abc"}})
        """,
      })
  void testValid(final String code) {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final MagikTypedCheck check = new SlotDefaultValueMatchesTypeDocTypedCheck();
    assertThat(check).reportsNoIssues(code, definitionKeeper);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        """
        ## @slot {sw:integer} slot1
        def_slotted_exemplar(:test_exemplar, {{:slot1, _unset}})
        """,
        """
        ## @slot {sw:integer} slot1
        def_slotted_exemplar(:test_exemplar, {{:slot1, "abc"}})
        """,
      })
  void testInvalid(final String code) {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final MagikTypedCheck check = new SlotDefaultValueMatchesTypeDocTypedCheck();
    assertThat(check).reportsIssueCount(code, definitionKeeper, 1);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        """
        ## @slot {sw:integer} slot1
        ## @slot {sw:integer} slot2
        def_slotted_exemplar(:test_exemplar, {{:slot1, 1}, {:slot2, "abc"}})
        """,
      })
  void testMixed(final String code) {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final MagikTypedCheck check = new SlotDefaultValueMatchesTypeDocTypedCheck();
    assertThat(check).reportsIssueCount(code, definitionKeeper, 1);
  }
}
