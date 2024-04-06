package nl.ramsolutions.sw.magik.typedchecks.checks;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.List;
import nl.ramsolutions.sw.magik.analysis.definitions.DefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.ExemplarDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.typing.types.TypeString;
import nl.ramsolutions.sw.magik.checks.MagikIssue;
import nl.ramsolutions.sw.magik.typedchecks.MagikTypedCheck;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Tests for {@link ComparedTypesDoNotMatchTypedCheck}. */
class ComparedTypesDoNotMatchTypedCheckTest extends MagikTypedCheckTestBase {

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
    final List<MagikIssue> checkResults = this.runCheck(code, definitionKeeper, check);
    assertThat(checkResults).isEmpty();
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
            ExemplarDefinition.Sort.SLOTTED,
            TypeString.ofIdentifier("parent", "user"),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptySet()));
    definitionKeeper.add(
        new ExemplarDefinition(
            null,
            null,
            null,
            null,
            ExemplarDefinition.Sort.SLOTTED,
            TypeString.ofIdentifier("child", "user"),
            Collections.emptyList(),
            List.of(TypeString.ofIdentifier("parent", "user")),
            Collections.emptySet()));
    final MagikTypedCheck check = new ComparedTypesDoNotMatchTypedCheck();
    final List<MagikIssue> checkResults = this.runCheck(code, definitionKeeper, check);
    assertThat(checkResults).isEmpty();
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
    final List<MagikIssue> checkResults = this.runCheck(code, definitionKeeper, check);
    assertThat(checkResults).hasSize(1);
  }
}
