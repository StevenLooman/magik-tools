package nl.ramsolutions.sw.checks.magiktyped;

import static nl.ramsolutions.sw.checks.magiktyped.MagikTypedCheckAssert.assertThat;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import nl.ramsolutions.sw.checks.MagikTypedCheck;
import nl.ramsolutions.sw.magik.analysis.definitions.DefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.ExemplarDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.InheritanceDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.PackageDefinition;
import nl.ramsolutions.sw.magik.analysis.typing.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import org.junit.jupiter.api.Test;

/** Test {@link MethodExistsTypedCheck}. */
class MethodExistsTypedCheckTest {

  @Test
  void testMethodUnknown() {
    final String code =
        """
        _block
          object.m()
        _endblock""";
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final MagikTypedCheck check = new MethodExistsTypedCheck();
    assertThat(check).reportsIssueCount(code, definitionKeeper, 1);
  }

  @Test
  void testMethodKnown() {
    final String code =
        """
        _block
          object.m()
        _endblock""";
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    definitionKeeper.add(
        new MethodDefinition(
            null,
            null,
            null,
            null,
            null,
            TypeString.SW_OBJECT,
            "m()",
            EnumSet.noneOf(MethodDefinition.Modifier.class),
            Collections.emptyList(),
            null,
            null,
            new ExpressionResultString(TypeString.SW_OBJECT),
            ExpressionResultString.EMPTY));
    final MagikTypedCheck check = new MethodExistsTypedCheck();
    assertThat(check).reportsNoIssues(code, definitionKeeper);
  }

  @Test
  void testMethodOnExemplarShadowingSameNameInUsedPackage() {
    final String code =
        """
        _package rs
        _block
          foo.m()
        _endblock""";
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    definitionKeeper.add(new PackageDefinition(null, null, null, null, null, "rs", List.of("sw")));

    // m() responds via the parent chain (sw:object).
    definitionKeeper.add(
        new MethodDefinition(
            null,
            null,
            null,
            null,
            null,
            TypeString.SW_OBJECT,
            "m()",
            EnumSet.noneOf(MethodDefinition.Modifier.class),
            Collections.emptyList(),
            null,
            null,
            new ExpressionResultString(TypeString.SW_OBJECT),
            ExpressionResultString.EMPTY));

    // The real exemplar in package rs, inheriting from sw:object.
    final TypeString rsFoo = TypeString.ofIdentifier("foo", "rs");
    definitionKeeper.add(
        new ExemplarDefinition(
            null, null, null, null, null, ExemplarDefinition.Sort.SLOTTED, rsFoo, null));
    definitionKeeper.add(
        new InheritanceDefinition(null, null, null, null, null, rsFoo, TypeString.SW_OBJECT));

    // A same-named phantom in the used package sw, with no useful parents.
    final TypeString swFoo = TypeString.ofIdentifier("foo", "sw");
    definitionKeeper.add(
        new ExemplarDefinition(
            null, null, null, null, null, ExemplarDefinition.Sort.UNDEFINED, swFoo, null));

    final MagikTypedCheck check = new MethodExistsTypedCheck();
    assertThat(check).reportsNoIssues(code, definitionKeeper);
  }

  @Test
  void testUndefinedUnionArmIsNotReported() {
    final String code =
        """
        _method object.test()
          _local a << _self.give_union()
          a.m()
        _endmethod""";
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final TypeString unionTypeStr = TypeString.combine(TypeString.SW_OBJECT, TypeString.UNDEFINED);
    definitionKeeper.add(
        new MethodDefinition(
            null,
            null,
            null,
            null,
            null,
            TypeString.SW_OBJECT,
            "give_union()",
            EnumSet.noneOf(MethodDefinition.Modifier.class),
            Collections.emptyList(),
            null,
            null,
            new ExpressionResultString(unionTypeStr),
            ExpressionResultString.EMPTY));
    definitionKeeper.add(
        new MethodDefinition(
            null,
            null,
            null,
            null,
            null,
            TypeString.SW_OBJECT,
            "m()",
            EnumSet.noneOf(MethodDefinition.Modifier.class),
            Collections.emptyList(),
            null,
            null,
            new ExpressionResultString(TypeString.SW_OBJECT),
            ExpressionResultString.EMPTY));
    final MagikTypedCheck check = new MethodExistsTypedCheck();
    assertThat(check).reportsNoIssues(code, definitionKeeper);
  }

  @Test
  void testUnsetUnionArmIsStillReported() {
    final String code =
        """
        _method object.test()
          _local a << _self.give_union()
          a.m()
        _endmethod""";
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final TypeString unionTypeStr = TypeString.combine(TypeString.SW_OBJECT, TypeString.SW_UNSET);
    definitionKeeper.add(
        new MethodDefinition(
            null,
            null,
            null,
            null,
            null,
            TypeString.SW_OBJECT,
            "give_union()",
            EnumSet.noneOf(MethodDefinition.Modifier.class),
            Collections.emptyList(),
            null,
            null,
            new ExpressionResultString(unionTypeStr),
            ExpressionResultString.EMPTY));
    definitionKeeper.add(
        new MethodDefinition(
            null,
            null,
            null,
            null,
            null,
            TypeString.SW_OBJECT,
            "m()",
            EnumSet.noneOf(MethodDefinition.Modifier.class),
            Collections.emptyList(),
            null,
            null,
            new ExpressionResultString(TypeString.SW_OBJECT),
            ExpressionResultString.EMPTY));
    final MagikTypedCheck check = new MethodExistsTypedCheck();
    assertThat(check).reportsIssueCount(code, definitionKeeper, 1);
  }

  private static final TypeString BASE_REF = TypeString.ofIdentifier("base", "user");

  private void addExemplar(
      final IDefinitionKeeper definitionKeeper,
      final ExemplarDefinition.Sort sort,
      final TypeString typeString) {
    definitionKeeper.add(
        new ExemplarDefinition(null, null, null, null, null, sort, typeString, null));
  }

  private void addPropsMethod(final IDefinitionKeeper definitionKeeper, final TypeString owner) {
    definitionKeeper.add(
        new MethodDefinition(
            null,
            null,
            null,
            null,
            null,
            owner,
            "props",
            EnumSet.noneOf(MethodDefinition.Modifier.class),
            Collections.emptyList(),
            null,
            null,
            new ExpressionResultString(TypeString.SW_INTEGER),
            ExpressionResultString.EMPTY));
  }

  private void addMethod(
      final IDefinitionKeeper definitionKeeper,
      final TypeString owner,
      final String methodName,
      final ExpressionResultString returnTypes) {
    definitionKeeper.add(
        new MethodDefinition(
            null,
            null,
            null,
            null,
            null,
            owner,
            methodName,
            EnumSet.noneOf(MethodDefinition.Modifier.class),
            Collections.emptyList(),
            null,
            null,
            returnTypes,
            ExpressionResultString.EMPTY));
  }

  @Test
  void testSelfUnionArmResolvesToTheMethodExemplar() {
    final String code =
        """
        _package user
        _method base.run()
          _self.a().props
        _endmethod""";
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    this.addExemplar(definitionKeeper, ExemplarDefinition.Sort.SLOTTED, BASE_REF);
    this.addMethod(definitionKeeper, BASE_REF, "a()", ExpressionResultString.UNDEFINED);
    final ExpressionResultString selfResult = new ExpressionResultString(TypeString.SELF);
    this.addMethod(definitionKeeper, BASE_REF, "a()", selfResult);
    this.addPropsMethod(definitionKeeper, BASE_REF);
    final MagikTypedCheck check = new MethodExistsTypedCheck();
    assertThat(check).reportsNoIssues(code, definitionKeeper);
  }

  @Test
  void testSelfInAProcResolvesToTheProcedure() {
    final String code =
        """
        _package user
        _method base.run()
          _proc()
            _self.invoke()
          _endproc
        _endmethod""";
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    this.addExemplar(definitionKeeper, ExemplarDefinition.Sort.SLOTTED, BASE_REF);
    this.addExemplar(definitionKeeper, ExemplarDefinition.Sort.SLOTTED, TypeString.SW_PROCEDURE);
    this.addMethod(
        definitionKeeper, TypeString.SW_PROCEDURE, "invoke()", ExpressionResultString.EMPTY);
    final MagikTypedCheck check = new MethodExistsTypedCheck();
    assertThat(check).reportsNoIssues(code, definitionKeeper);
  }
}
