package nl.ramsolutions.sw.checks.magiktyped;

import static nl.ramsolutions.sw.checks.magiktyped.MagikTypedCheckAssert.assertThat;

import java.util.Collections;
import java.util.List;
import nl.ramsolutions.sw.checks.MagikTypedCheck;
import nl.ramsolutions.sw.magik.analysis.definitions.DefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.ExemplarDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Tests for {@link ComparedTypesDoNotMatchTypedCheck}. */
class ComparedTypesDoNotMatchTypedCheckTest {

  @ParameterizedTest
  @ValueSource(
      strings = {
        """
        _method a.m(param1)
          _if param1 _is _unset
          _then
          _endif
        _endmethod
        """,
        """
        _method a.m(param1)
          _if param1.is_class_of?(sw:float)
          _then
          _endif
        _endmethod
        """,
        """
        _method a.m(param1)
          _if param1.is_kind_of?(sw:float)
          _then
          _endif
        _endmethod
        """,
      })
  void testDoesNotCheckUndefined(final String code) {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final MagikTypedCheck check = new ComparedTypesDoNotMatchTypedCheck();
    assertThat(check).reportsNoIssues(code, definitionKeeper);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        """
        _method a.m(_optional param1)
          ## @param {sw:integer} param1
          _if param1 _is _unset
          _then
          _endif
        _endmethod
        """,
        """
        _method a.m(param1, param2)
          ## @param {sw:integer|sw:float} param1
          ## @param {sw:symbol|sw:float} param2
          _if param1 _is param2
          _then
          _endif
        _endmethod
        """,
        """
        _method a.m(param1)
          ## @param {sw:integer} param1
          _if param1.is_class_of?(sw:integer)
          _then
          _endif
        _endmethod
        """,
        """
        _method a.m(param1, param2)
          ## @param {sw:integer|sw:float} param1
          ## @param {sw:integer|sw:float} param2
          _if param1.is_class_of?(param2)
          _then
          _endif
        _endmethod
        """,
        """
        _method a.m(param1)
          ## @param {user:child} param1
          _if param1.is_kind_of?(user:parent)
          _then
          _endif
        _endmethod
        """,
      })
  void testTypeMatchable(final String code) {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    definitionKeeper.add(
        new ExemplarDefinition(
            null,
            null,
            null,
            null,
            null,
            ExemplarDefinition.Sort.SLOTTED,
            TypeString.ofIdentifier("parent", "user"),
            Collections.emptyList(),
            null));
    definitionKeeper.add(
        new ExemplarDefinition(
            null,
            null,
            null,
            null,
            null,
            ExemplarDefinition.Sort.SLOTTED,
            TypeString.ofIdentifier("child", "user"),
            List.of(TypeString.ofIdentifier("parent", "user")),
            null));
    final MagikTypedCheck check = new ComparedTypesDoNotMatchTypedCheck();
    assertThat(check).reportsNoIssues(code, definitionKeeper);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        """
        _method a.m(param1)
          ## @param {sw:integer} param1
          _if param1 _is _unset
          _then
          _endif
        _endmethod
        """,
        """
        _method a.m(param1, param2)
          ## @param {sw:integer|sw:float} param1
          ## @param {sw:symbol|sw:char16_vector} param2
          _if param1 _is param2
          _then
          _endif
        _endmethod
        """,
        """
        _method a.m(param1)
          ## @param {sw:integer} param1
          _if param1.is_class_of?(sw:character)
          _then
          _endif
        _endmethod
        """,
        """
        _method a.m(param1)
          ## @param {sw:integer} param1
          _if param1.is_kind_of?(sw:symbol)
          _then
          _endif
        _endmethod
        """,
      })
  void testTypeNotMatchable(final String code) {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final MagikTypedCheck check = new ComparedTypesDoNotMatchTypedCheck();
    assertThat(check).reportsIssueCount(code, definitionKeeper, 1);
  }
}
