package nl.ramsolutions.sw.checks.magiktyped;

import static nl.ramsolutions.sw.checks.magiktyped.MagikTypedCheckAssert.assertThat;

import java.util.Collections;
import nl.ramsolutions.sw.checks.MagikTypedCheck;
import nl.ramsolutions.sw.magik.analysis.definitions.DefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.ExemplarDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.SlotDefinition;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Test {@link TypeDocTypeExistsTypedCheck}. */
class TypeDocTypeExistsTypedCheckTest {

  @ParameterizedTest
  @ValueSource(
      strings = {
        """
        _method a.b(p1)
          ## @param {user:missing_type} p1
        _endmethod
        """,
        """
        _method a.b()
          ## @return {user:missing_type}
        _endmethod
        """,
        """
        _method a.b()
          ## @return {|sw:float} p1
        _endmethod
        """,
      })
  void testInvalid(final String code) {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final MagikTypedCheck check = new TypeDocTypeExistsTypedCheck();
    assertThat(check).reportsIssueCount(code, definitionKeeper, 1);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        """
        _method a.b(p1)
          ## @param {sw:float} p1
        _endmethod
        """,
        """
        _method a.b()
          ## @return {sw:float|sw:integer}
        _endmethod
        """,
        """
        _method a.b()
          ## @return {sw:integer...}
        _endmethod
        """,
        """
        _method a.b()
          ## @return {sw:symbol}
          ## @return {sw:integer|sw:unset...}
        _endmethod
        """,
      })
  void testValid(final String code) {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final MagikTypedCheck check = new TypeDocTypeExistsTypedCheck();
    assertThat(check).reportsNoIssues(code, definitionKeeper);
  }

  @Test
  void testValidParameterReference() {
    final String code =
        """
        _method a.b(p1)
          ## @return {_parameter(p1)}
        _endmethod
        """;
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final MagikTypedCheck check = new TypeDocTypeExistsTypedCheck();
    assertThat(check).reportsNoIssues(code, definitionKeeper);
  }

  @Test
  void testInvalidParameterReference() {
    final String code =
        """
        _method a.b(p1)
          ## @return {_parameter(nonexistent)}
        _endmethod
        """;
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final MagikTypedCheck check = new TypeDocTypeExistsTypedCheck();
    assertThat(check).reportsIssueCount(code, definitionKeeper, 1);
  }

  @Test
  void testValidSlotReference() {
    final String code =
        """
        _method a.b()
          ## @return {_slot(my_slot)}
        _endmethod
        """;
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    definitionKeeper.add(
        new ExemplarDefinition(
            null,
            null,
            null,
            null,
            null,
            ExemplarDefinition.Sort.SLOTTED,
            TypeString.ofIdentifier("a", "user"),
            Collections.emptyList(),
            null));
    definitionKeeper.add(
        new SlotDefinition(
            null,
            null,
            null,
            null,
            null,
            TypeString.ofIdentifier("a", "user"),
            "my_slot",
            TypeString.SW_INTEGER));
    final MagikTypedCheck check = new TypeDocTypeExistsTypedCheck();
    assertThat(check).reportsNoIssues(code, definitionKeeper);
  }

  @Test
  void testInvalidSlotReference() {
    final String code =
        """
        _method a.b()
          ## @return {_slot(nonexistent)}
        _endmethod
        """;
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    definitionKeeper.add(
        new ExemplarDefinition(
            null,
            null,
            null,
            null,
            null,
            ExemplarDefinition.Sort.SLOTTED,
            TypeString.ofIdentifier("a", "user"),
            Collections.emptyList(),
            null));
    final MagikTypedCheck check = new TypeDocTypeExistsTypedCheck();
    assertThat(check).reportsIssueCount(code, definitionKeeper, 1);
  }
}
