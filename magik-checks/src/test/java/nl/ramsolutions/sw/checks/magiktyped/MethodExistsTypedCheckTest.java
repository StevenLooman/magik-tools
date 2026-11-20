package nl.ramsolutions.sw.checks.magiktyped;

import static nl.ramsolutions.sw.checks.magiktyped.MagikTypedCheckAssert.assertThat;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import nl.ramsolutions.sw.checks.Issue;
import nl.ramsolutions.sw.checks.MagikTypedCheck;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.analysis.definitions.DefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.ExemplarDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.InheritanceDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.PackageDefinition;
import nl.ramsolutions.sw.magik.analysis.typing.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import org.assertj.core.api.Assertions;
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

  @Test
  void testUnknownMethodMessage() {
    final String code =
        """
        _block
          object.m()
        _endblock""";
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final String message = this.runCheckForMessage(code, definitionKeeper);
    Assertions.assertThat(message).isEqualTo("Object sw:object does not understand message m()");
  }

  @Test
  void testUnknownIndexerMethodMessage() {
    final String code =
        """
        _block
          object[1]
        _endblock""";
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final String message = this.runCheckForMessage(code, definitionKeeper);
    Assertions.assertThat(message).isEqualTo("Object sw:object does not understand message []");
  }

  @Test
  void testUnknownIndexerAssignmentMethodMessage() {
    final String code =
        """
        _block
          object[1, 2] << 3
        _endblock""";
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final String message = this.runCheckForMessage(code, definitionKeeper);
    Assertions.assertThat(message).isEqualTo("Object sw:object does not understand message [,]<<");
  }

  private String runCheckForMessage(final String code, final IDefinitionKeeper definitionKeeper) {
    final MagikTypedFile magikFile =
        new MagikTypedFile(MagikTypedFile.DEFAULT_URI, code, definitionKeeper);
    final MagikTypedCheck check = new MethodExistsTypedCheck();
    final List<Issue> issues = check.scanFileForIssues(magikFile);
    Assertions.assertThat(issues).hasSize(1);

    final Issue issue = issues.get(0);
    return issue.message();
  }

  @Test
  void testMethodWrittenInUsingPackageKnown() {
    final String code =
        """
        _package sw
        _method a.caller()
          _self.m()
        _endmethod""";
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final TypeString swARef = TypeString.ofIdentifier("a", "sw");
    definitionKeeper.add(
        new ExemplarDefinition(
            null, null, null, null, null, ExemplarDefinition.Sort.SLOTTED, swARef, null));
    final TypeString userARef = TypeString.ofIdentifier("a", "user");
    definitionKeeper.add(
        new MethodDefinition(
            null,
            null,
            null,
            null,
            null,
            userARef,
            "m()",
            EnumSet.noneOf(MethodDefinition.Modifier.class),
            Collections.emptyList(),
            null,
            null,
            ExpressionResultString.EMPTY,
            ExpressionResultString.EMPTY));
    final MagikTypedCheck check = new MethodExistsTypedCheck();
    assertThat(check).reportsNoIssues(code, definitionKeeper);
  }

  @Test
  void testSuperSendIsLeftToSuperMethodExists() {
    final String code =
        """
        _package user
        _method host.run()
          _super.m()
        _endmethod""";
    final IDefinitionKeeper definitionKeeper = this.createHostWithBase();
    final MagikTypedCheck check = new MethodExistsTypedCheck();
    assertThat(check).reportsNoIssues(code, definitionKeeper);
  }

  @Test
  void testNamedSuperSendIsLeftToSuperMethodExists() {
    final String code =
        """
        _package user
        _method host.run()
          _super(base).m()
        _endmethod""";
    final IDefinitionKeeper definitionKeeper = this.createHostWithBase();
    final MagikTypedCheck check = new MethodExistsTypedCheck();
    assertThat(check).reportsNoIssues(code, definitionKeeper);
  }

  private IDefinitionKeeper createHostWithBase() {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final TypeString hostRef = TypeString.ofIdentifier("host", "user");
    this.addExemplar(definitionKeeper, ExemplarDefinition.Sort.SLOTTED, BASE_REF);
    this.addExemplar(definitionKeeper, ExemplarDefinition.Sort.SLOTTED, hostRef);
    definitionKeeper.add(
        new InheritanceDefinition(null, null, null, null, null, hostRef, BASE_REF));
    return definitionKeeper;
  }

  private static final TypeString ZZ_A_REF = TypeString.ofIdentifier("zz_a", "user");

  /** A lonely mixin {@code user:zz_a}, and {@code sw:object} answering {@code class_name}. */
  private IDefinitionKeeper createLonelyMixinKeeper() {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    this.addExemplar(definitionKeeper, ExemplarDefinition.Sort.OBJECT, TypeString.SW_OBJECT);
    final ExpressionResultString symbolResult = new ExpressionResultString(TypeString.SW_SYMBOL);
    this.addMethod(definitionKeeper, TypeString.SW_OBJECT, "class_name", symbolResult);
    this.addExemplar(definitionKeeper, ExemplarDefinition.Sort.MIXIN, ZZ_A_REF);
    return definitionKeeper;
  }

  @Test
  void testObjectMethodOnMixinTypedParameterIsNotReported() {
    // The value is an includer's instance, and every includer reaches sw:object.
    final String code =
        """
        _package user
        _method base.run(p)
          ## @param {user:zz_a} p
          _return p.class_name
        _endmethod""";
    final IDefinitionKeeper definitionKeeper = this.createLonelyMixinKeeper();
    final MagikTypedCheck check = new MethodExistsTypedCheck();
    assertThat(check).reportsNoIssues(code, definitionKeeper);
  }

  @Test
  void testObjectMethodOnSelfInLonelyMixinIsNotReported() {
    final String code =
        """
        _package user
        _method zz_a.run()
          _return _self.class_name
        _endmethod""";
    final IDefinitionKeeper definitionKeeper = this.createLonelyMixinKeeper();
    final MagikTypedCheck check = new MethodExistsTypedCheck();
    assertThat(check).reportsNoIssues(code, definitionKeeper);
  }

  @Test
  void testUnknownMethodOnMixinTypedParameterIsStillReported() {
    final String code =
        """
        _package user
        _method base.run(p)
          ## @param {user:zz_a} p
          _return p.no_such_method
        _endmethod""";
    final IDefinitionKeeper definitionKeeper = this.createLonelyMixinKeeper();
    final MagikTypedCheck check = new MethodExistsTypedCheck();
    assertThat(check).reportsIssueCount(code, definitionKeeper, 1);
  }
}
