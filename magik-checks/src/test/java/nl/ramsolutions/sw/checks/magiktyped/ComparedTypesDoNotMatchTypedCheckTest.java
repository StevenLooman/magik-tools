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
import org.junit.jupiter.api.Test;
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

  private void addExemplar(
      final IDefinitionKeeper definitionKeeper,
      final ExemplarDefinition.Sort sort,
      final TypeString typeRef) {
    definitionKeeper.add(new ExemplarDefinition(null, null, null, null, null, sort, typeRef, null));
  }

  private void addParent(
      final IDefinitionKeeper definitionKeeper,
      final TypeString childRef,
      final TypeString parentRef) {
    definitionKeeper.add(
        new InheritanceDefinition(null, null, null, null, null, childRef, parentRef));
  }

  /**
   * Slotted {@code zz_base}, {@code zz_sub} with parent {@code zz_base}, {@code zz_mid} with parent
   * {@code zz_sub}, {@code zz_p}, {@code zz_q}, {@code zz_r} with parents {@code zz_p} and {@code
   * zz_q}, {@code zz_s} with parents {@code zz_base} and {@code zz_m}, {@code zz_t} with parent
   * {@code zz_n}, and {@code zz_lone}; mixins {@code zz_m} and {@code zz_n}.
   */
  private IDefinitionKeeper createHierarchyKeeper() {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final TypeString zzBaseRef = TypeString.ofIdentifier("zz_base", "user");
    final TypeString zzSubRef = TypeString.ofIdentifier("zz_sub", "user");
    final TypeString zzMidRef = TypeString.ofIdentifier("zz_mid", "user");
    final TypeString zzPRef = TypeString.ofIdentifier("zz_p", "user");
    final TypeString zzQRef = TypeString.ofIdentifier("zz_q", "user");
    final TypeString zzRRef = TypeString.ofIdentifier("zz_r", "user");
    final TypeString zzSRef = TypeString.ofIdentifier("zz_s", "user");
    final TypeString zzTRef = TypeString.ofIdentifier("zz_t", "user");
    final TypeString zzLoneRef = TypeString.ofIdentifier("zz_lone", "user");
    final TypeString zzMRef = TypeString.ofIdentifier("zz_m", "user");
    final TypeString zzNRef = TypeString.ofIdentifier("zz_n", "user");
    for (final TypeString typeRef :
        List.of(zzBaseRef, zzSubRef, zzMidRef, zzPRef, zzQRef, zzRRef, zzSRef, zzTRef, zzLoneRef)) {
      this.addExemplar(definitionKeeper, ExemplarDefinition.Sort.SLOTTED, typeRef);
    }
    this.addExemplar(definitionKeeper, ExemplarDefinition.Sort.MIXIN, zzMRef);
    this.addExemplar(definitionKeeper, ExemplarDefinition.Sort.MIXIN, zzNRef);
    this.addParent(definitionKeeper, zzSubRef, zzBaseRef);
    this.addParent(definitionKeeper, zzMidRef, zzSubRef);
    this.addParent(definitionKeeper, zzRRef, zzPRef);
    this.addParent(definitionKeeper, zzRRef, zzQRef);
    this.addParent(definitionKeeper, zzSRef, zzBaseRef);
    this.addParent(definitionKeeper, zzSRef, zzMRef);
    this.addParent(definitionKeeper, zzTRef, zzNRef);
    return definitionKeeper;
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        // A downcast: x may hold a zz_sub.
        """
        _method zz_base.k(x)
          ## @param {user:zz_base} x
          _return x.is_kind_of?(zz_sub)
        _endmethod
        """,
        """
        _method zz_base.k()
          _return _self.is_kind_of?(zz_sub)
        _endmethod
        """,
        // A zz_r is a kind of both.
        """
        _method a.m(x)
          ## @param {user:zz_p} x
          _return x.is_kind_of?(zz_q)
        _endmethod
        """,
        // A zz_s is a zz_base including zz_m.
        """
        _method a.m(x)
          ## @param {user:zz_base} x
          _return x.is_kind_of?(zz_m)
        _endmethod
        """,
        // A zz_t is a slotted exemplar, so a kind of sw:slotted_format_mixin, including zz_n.
        """
        _method a.m(x)
          ## @param {sw:slotted_format_mixin} x
          _return x.is_kind_of?(zz_n)
        _endmethod
        """,
        // A zz_mid is a zz_base, two levels down.
        """
        _method a.m(x)
          ## @param {user:zz_base|sw:unset} x
          _return x.is_kind_of?(zz_mid)
        _endmethod
        """,
        // A class test: x may hold a zz_sub, the class of the exemplar zz_sub.
        """
        _method a.m(x)
          ## @param {user:zz_base} x
          _return x.is_class_of?(zz_sub)
        _endmethod
        """,
        // Both sides are values: each may hold a zz_mid.
        """
        _method a.m(x, y)
          ## @param {user:zz_sub} x
          ## @param {user:zz_base} y
          _return x.is_class_of?(y)
        _endmethod
        """,
      })
  void testDowncastCanMatch(final String code) {
    final IDefinitionKeeper definitionKeeper = this.createHierarchyKeeper();
    final MagikTypedCheck check = new ComparedTypesDoNotMatchTypedCheck();
    assertThat(check).reportsNoIssues(code, definitionKeeper);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        // No known type is a kind of both.
        """
        _method a.m(x)
          ## @param {user:zz_base} x
          _return x.is_kind_of?(zz_lone)
        _endmethod
        """,
        """
        _method a.m(x)
          ## @param {user:zz_p} x
          _return x.is_kind_of?(zz_m)
        _endmethod
        """,
        // The exemplar zz_base is of class zz_base, which is no zz_sub.
        """
        _method a.m(x)
          ## @param {user:zz_sub} x
          _return x.is_class_of?(zz_base)
        _endmethod
        """,
        """
        _method a.m(x, y)
          ## @param {user:zz_base} x
          ## @param {user:zz_lone} y
          _return x.is_class_of?(y)
        _endmethod
        """,
        // An exemplar read through its global is that exemplar, which is no zz_sub.
        """
        _method a.m()
          _return zz_base.is_kind_of?(zz_sub)
        _endmethod
        """,
        """
        _method a.m(x)
          ## @param {user:zz_sub} x
          _return zz_base.is_class_of?(x)
        _endmethod
        """,
        """
        _method a.m()
          _return zz_base.is_class_of?(zz_sub)
        _endmethod
        """,
      })
  void testNoJointSubtypeCannotMatch(final String code) {
    final IDefinitionKeeper definitionKeeper = this.createHierarchyKeeper();
    final MagikTypedCheck check = new ComparedTypesDoNotMatchTypedCheck();
    assertThat(check).reportsIssueCount(code, definitionKeeper, 1);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        // Both may hold one zz_sub.
        """
        _method a.m(x, y)
          ## @param {user:zz_base} x
          ## @param {user:zz_sub} y
          _return x _is y
        _endmethod
        """,
        """
        _method a.m(x, y)
          ## @param {user:zz_sub} x
          ## @param {user:zz_base} y
          _return x _isnt y
        _endmethod
        """,
        // Both may hold one zz_r.
        """
        _method a.m(x, y)
          ## @param {user:zz_p} x
          ## @param {user:zz_q} y
          _return x _is y
        _endmethod
        """,
        // The exemplar zz_sub is a zz_base.
        """
        _method a.m(x)
          ## @param {user:zz_base} x
          _return x _is zz_sub
        _endmethod
        """,
        // _self may be a zz_r, as may y.
        """
        _method zz_p.m(y)
          ## @param {user:zz_q} y
          _return _self _is y
        _endmethod
        """,
        // _self may be a zz_mid, as may y.
        """
        _method zz_sub.m(y)
          ## @param {user:zz_base} y
          _return y _is _self
        _endmethod
        """,
      })
  void testIdentityOfJointSubtypeCanMatch(final String code) {
    final IDefinitionKeeper definitionKeeper = this.createHierarchyKeeper();
    final MagikTypedCheck check = new ComparedTypesDoNotMatchTypedCheck();
    assertThat(check).reportsNoIssues(code, definitionKeeper);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        """
        _method a.m(x, y)
          ## @param {user:zz_base} x
          ## @param {user:zz_lone} y
          _return x _is y
        _endmethod
        """,
        // Two exemplars read through their globals are two objects.
        """
        _method a.m()
          _return zz_base _is zz_sub
        _endmethod
        """,
        """
        _method a.m()
          _return zz_sub _isnt zz_base
        _endmethod
        """,
        // The exemplar zz_base is no zz_sub.
        """
        _method a.m(x)
          ## @param {user:zz_sub} x
          _return x _is zz_base
        _endmethod
        """,
        """
        _method zz_sub.m()
          _return _self _is zz_base
        _endmethod
        """,
      })
  void testIdentityWithoutJointSubtypeCannotMatch(final String code) {
    final IDefinitionKeeper definitionKeeper = this.createHierarchyKeeper();
    final MagikTypedCheck check = new ComparedTypesDoNotMatchTypedCheck();
    assertThat(check).reportsIssueCount(code, definitionKeeper, 1);
  }

  @Test
  void testIdentityOfUnrelatedTypesWithoutJointSubtypeCannotMatch() {
    final String code =
        """
        _method a.m(x, y)
          ## @param {user:zz_p} x
          ## @param {user:zz_q} y
          _return x _is y
        _endmethod
        """;
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final TypeString zzPRef = TypeString.ofIdentifier("zz_p", "user");
    final TypeString zzQRef = TypeString.ofIdentifier("zz_q", "user");
    this.addExemplar(definitionKeeper, ExemplarDefinition.Sort.SLOTTED, zzPRef);
    this.addExemplar(definitionKeeper, ExemplarDefinition.Sort.SLOTTED, zzQRef);
    final MagikTypedCheck check = new ComparedTypesDoNotMatchTypedCheck();
    assertThat(check).reportsIssueCount(code, definitionKeeper, 1);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        """
        _method a.m(x)
          ## @param {<E>} x
          _return x.is_kind_of?(sw:integer)
        _endmethod
        """,
        """
        _method a.m(x)
          ## @param {<E>|sw:unset} x
          _return x.is_kind_of?(sw:integer)
        _endmethod
        """,
        """
        _method a.m(x)
          ## @param {<E>} x
          _return 1.is_kind_of?(x)
        _endmethod
        """,
        """
        _method a.m(x)
          ## @param {<E>} x
          _return x.is_class_of?(sw:integer)
        _endmethod
        """,
        """
        _method a.m(x)
          ## @param {<E>} x
          _return x _is 1
        _endmethod
        """,
      })
  void testUnboundGenericIsNotChecked(final String code) {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final MagikTypedCheck check = new ComparedTypesDoNotMatchTypedCheck();
    assertThat(check).reportsNoIssues(code, definitionKeeper);
  }
}
