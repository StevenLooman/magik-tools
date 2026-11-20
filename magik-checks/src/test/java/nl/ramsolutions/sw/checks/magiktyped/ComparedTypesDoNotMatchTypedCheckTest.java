package nl.ramsolutions.sw.checks.magiktyped;

import static nl.ramsolutions.sw.checks.magiktyped.MagikTypedCheckAssert.assertThat;

import java.util.List;
import nl.ramsolutions.sw.checks.MagikTypedCheck;
import nl.ramsolutions.sw.magik.analysis.definitions.DefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.ExemplarDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.GlobalDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.InheritanceDefinition;
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
    final TypeString parentRef = TypeString.ofIdentifier("parent", "user");
    final TypeString childRef = TypeString.ofIdentifier("child", "user");
    definitionKeeper.add(
        new ExemplarDefinition(
            null, null, null, null, null, ExemplarDefinition.Sort.SLOTTED, parentRef, null));
    definitionKeeper.add(
        new ExemplarDefinition(
            null, null, null, null, null, ExemplarDefinition.Sort.SLOTTED, childRef, null));
    definitionKeeper.add(
        new InheritanceDefinition(null, null, null, null, null, childRef, parentRef));
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

  private IDefinitionKeeper createKeeperWithExemplars() {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final TypeString aRef = TypeString.ofIdentifier("a", "user");
    final TypeString bRef = TypeString.ofIdentifier("b", "user");
    final TypeString cRef = TypeString.ofIdentifier("c", "user");
    for (final TypeString typeRef : List.of(aRef, bRef, cRef)) {
      definitionKeeper.add(
          new ExemplarDefinition(
              null, null, null, null, null, ExemplarDefinition.Sort.SLOTTED, typeRef, null));
    }
    definitionKeeper.add(new InheritanceDefinition(null, null, null, null, null, bRef, aRef));
    final TypeString dynamicRef = TypeString.ofIdentifier("!x!", "user");
    definitionKeeper.add(
        new GlobalDefinition(
            null, null, null, null, null, dynamicRef, TypeString.SW_INTEGER, null));
    return definitionKeeper;
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        """
        _method a.m()
          _if _self _is a
          _then
          _endif
        _endmethod
        """,
        """
        _method a.m()
          _if _self _is b
          _then
          _endif
        _endmethod
        """,
        """
        _method a.m()
          _if a _isnt _unset
          _then
          _endif
        _endmethod
        """,
        """
        _method a.m()
          _if _unset _is a
          _then
          _endif
        _endmethod
        """,
        """
        _method a.m()
          _dynamic !x!
          _if !x! _is _unset
          _then
          _endif
        _endmethod
        """,
      })
  void testSelfMatchesItsExemplarAndGlobalMatchesUnset(final String code) {
    // On the exemplar `_self` is the exemplar; a global or inherited dynamic can be unset.
    final IDefinitionKeeper definitionKeeper = this.createKeeperWithExemplars();
    final MagikTypedCheck check = new ComparedTypesDoNotMatchTypedCheck();
    assertThat(check).reportsNoIssues(code, definitionKeeper);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        """
        _method a.m()
          _if _self _is _unset
          _then
          _endif
        _endmethod
        """,
        """
        _method a.m()
          _if _self _is 1
          _then
          _endif
        _endmethod
        """,
        """
        _method a.m()
          _if a _is 1
          _then
          _endif
        _endmethod
        """,
        """
        _method a.m()
          _if a _is c
          _then
          _endif
        _endmethod
        """,
        """
        _method a.m()
          _local x << 1
          _if x _is _unset
          _then
          _endif
        _endmethod
        """,
        """
        _method a.m()
          _dynamic !x! << 1
          _if !x! _is _unset
          _then
          _endif
        _endmethod
        """,
      })
  void testSelfAndGlobalAgainstOtherTypes(final String code) {
    final IDefinitionKeeper definitionKeeper = this.createKeeperWithExemplars();
    final MagikTypedCheck check = new ComparedTypesDoNotMatchTypedCheck();
    assertThat(check).reportsIssueCount(code, definitionKeeper, 1);
  }

  @ParameterizedTest
  @ValueSource(strings = {"sw:object", "sw:slotted_format_mixin"})
  void testKindTestOfAPlainSlottedExemplarAgainstItsImplicitAncestryCanMatch(
      final String kindName) {
    final String code =
        """
        _method a.m(p)
          ## @param {user:zz_lp} p
          _if p.is_kind_of?(%s)
          _then
          _endif
        _endmethod
        """
            .formatted(kindName);
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final TypeString exemplarRef = TypeString.ofIdentifier("zz_lp", "user");
    definitionKeeper.add(
        new ExemplarDefinition(
            null, null, null, null, null, ExemplarDefinition.Sort.SLOTTED, exemplarRef, null));
    definitionKeeper.add(
        new InheritanceDefinition(
            null,
            null,
            null,
            null,
            null,
            TypeString.SW_SLOTTED_FORMAT_MIXIN,
            TypeString.SW_OBJECT));
    final MagikTypedCheck check = new ComparedTypesDoNotMatchTypedCheck();
    assertThat(check).reportsNoIssues(code, definitionKeeper);
  }
}
