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

/** Tests for {@link SuperMethodExistsTypedCheck}. */
class SuperMethodExistsTypedCheckTest {

  private static final TypeString TYPE_PARENT = TypeString.ofIdentifier("parent", "sw");
  private static final TypeString TYPE_CHILD = TypeString.ofIdentifier("child", "sw");
  private static final TypeString ZZ_A = TypeString.ofIdentifier("zz_a", "user");
  private static final TypeString ZZ_B = TypeString.ofIdentifier("zz_b", "user");
  private static final TypeString ZZ_C = TypeString.ofIdentifier("zz_c", "user");
  private static final TypeString ZZ_D = TypeString.ofIdentifier("zz_d", "user");

  @Test
  void testSuperMethodExists() {
    final String code =
        """
        _method child.do_something()
          _super.do_something()
        _endmethod
        """;
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    definitionKeeper.add(
        new ExemplarDefinition(
            null, null, null, null, null, ExemplarDefinition.Sort.SLOTTED, TYPE_PARENT, null));
    definitionKeeper.add(
        new MethodDefinition(
            null,
            null,
            null,
            null,
            null,
            TYPE_PARENT,
            "do_something()",
            EnumSet.noneOf(MethodDefinition.Modifier.class),
            Collections.emptyList(),
            null,
            null,
            ExpressionResultString.EMPTY,
            ExpressionResultString.EMPTY));
    definitionKeeper.add(
        new ExemplarDefinition(
            null, null, null, null, null, ExemplarDefinition.Sort.SLOTTED, TYPE_CHILD, null));
    definitionKeeper.add(
        new InheritanceDefinition(null, null, null, null, null, TYPE_CHILD, TYPE_PARENT));
    final MagikTypedCheck check = new SuperMethodExistsTypedCheck();
    assertThat(check).reportsNoIssues(code, definitionKeeper);
  }

  @Test
  void testSuperMethodDoesNotExist() {
    final String code =
        """
        _method child.do_something()
          _super.nonexistent()
        _endmethod
        """;
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    definitionKeeper.add(
        new ExemplarDefinition(
            null, null, null, null, null, ExemplarDefinition.Sort.SLOTTED, TYPE_PARENT, null));
    definitionKeeper.add(
        new ExemplarDefinition(
            null, null, null, null, null, ExemplarDefinition.Sort.SLOTTED, TYPE_CHILD, null));
    definitionKeeper.add(
        new InheritanceDefinition(null, null, null, null, null, TYPE_CHILD, TYPE_PARENT));
    final MagikTypedCheck check = new SuperMethodExistsTypedCheck();
    assertThat(check).reportsIssueCount(code, definitionKeeper, 1);
  }

  @Test
  void testNonSuperMethodInvocation() {
    final String code =
        """
        _method child.do_something()
          _self.do_something()
        _endmethod
        """;
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    definitionKeeper.add(
        new ExemplarDefinition(
            null, null, null, null, null, ExemplarDefinition.Sort.SLOTTED, TYPE_CHILD, null));
    definitionKeeper.add(
        new MethodDefinition(
            null,
            null,
            null,
            null,
            null,
            TYPE_CHILD,
            "do_something()",
            EnumSet.noneOf(MethodDefinition.Modifier.class),
            Collections.emptyList(),
            null,
            null,
            ExpressionResultString.EMPTY,
            ExpressionResultString.EMPTY));
    final MagikTypedCheck check = new SuperMethodExistsTypedCheck();
    assertThat(check).reportsNoIssues(code, definitionKeeper);
  }

  @Test
  void testUndefinedSuperArmIsNotReported() {
    final String code =
        """
        _method child.do_something()
          _super.m()
        _endmethod
        """;
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    definitionKeeper.add(
        new ExemplarDefinition(
            null, null, null, null, null, ExemplarDefinition.Sort.SLOTTED, TYPE_PARENT, null));
    definitionKeeper.add(
        new MethodDefinition(
            null,
            null,
            null,
            null,
            null,
            TYPE_PARENT,
            "m()",
            EnumSet.noneOf(MethodDefinition.Modifier.class),
            Collections.emptyList(),
            null,
            null,
            ExpressionResultString.EMPTY,
            ExpressionResultString.EMPTY));
    definitionKeeper.add(
        new ExemplarDefinition(
            null, null, null, null, null, ExemplarDefinition.Sort.SLOTTED, TYPE_CHILD, null));
    definitionKeeper.add(
        new InheritanceDefinition(null, null, null, null, null, TYPE_CHILD, TYPE_PARENT));
    definitionKeeper.add(
        new InheritanceDefinition(null, null, null, null, null, TYPE_CHILD, TypeString.UNDEFINED));
    final MagikTypedCheck check = new SuperMethodExistsTypedCheck();
    assertThat(check).reportsNoIssues(code, definitionKeeper);
  }

  @Test
  void testSuperIndexerMethodDoesNotExistMessage() {
    final String code =
        """
        _method child.do_something()
          _super[1]
        _endmethod
        """;
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    definitionKeeper.add(
        new ExemplarDefinition(
            null, null, null, null, null, ExemplarDefinition.Sort.SLOTTED, TYPE_PARENT, null));
    definitionKeeper.add(
        new ExemplarDefinition(
            null, null, null, null, null, ExemplarDefinition.Sort.SLOTTED, TYPE_CHILD, null));
    definitionKeeper.add(
        new InheritanceDefinition(null, null, null, null, null, TYPE_CHILD, TYPE_PARENT));
    final String message = this.runCheckForMessage(code, definitionKeeper);
    Assertions.assertThat(message).isEqualTo("Unknown method on super: sw:parent[]");
  }

  private String runCheckForMessage(final String code, final IDefinitionKeeper definitionKeeper) {
    final MagikTypedFile magikFile =
        new MagikTypedFile(MagikTypedFile.DEFAULT_URI, code, definitionKeeper);
    final MagikTypedCheck check = new SuperMethodExistsTypedCheck();
    final List<Issue> issues = check.scanFileForIssues(magikFile);
    Assertions.assertThat(issues).hasSize(1);

    final Issue issue = issues.get(0);
    return issue.message();
  }

  @Test
  void testSuperMethodDoesNotExistMessage() {
    final String code =
        """
        _method child.do_something()
          _super.nonexistent()
        _endmethod
        """;
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    definitionKeeper.add(
        new ExemplarDefinition(
            null, null, null, null, null, ExemplarDefinition.Sort.SLOTTED, TYPE_PARENT, null));
    definitionKeeper.add(
        new ExemplarDefinition(
            null, null, null, null, null, ExemplarDefinition.Sort.SLOTTED, TYPE_CHILD, null));
    definitionKeeper.add(
        new InheritanceDefinition(null, null, null, null, null, TYPE_CHILD, TYPE_PARENT));
    final String message = this.runCheckForMessage(code, definitionKeeper);
    Assertions.assertThat(message).isEqualTo("Unknown method on super: sw:parent.nonexistent()");
  }

  @Test
  void testOneOfSeveralParentsAnswers() {
    final String code =
        """
        _package user
        $
        _method zz_c.t()
          _super.only_a()
        _endmethod
        """;
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    this.addExemplar(definitionKeeper, ExemplarDefinition.Sort.MIXIN, ZZ_A);
    this.addExemplar(definitionKeeper, ExemplarDefinition.Sort.MIXIN, ZZ_B);
    this.addExemplar(definitionKeeper, ExemplarDefinition.Sort.SLOTTED, ZZ_C, ZZ_A, ZZ_B);
    this.addMethod(definitionKeeper, ZZ_A, "only_a()");
    final List<String> messages = this.runCheckForMessages(code, definitionKeeper);
    Assertions.assertThat(messages).isEmpty();
  }

  @Test
  void testTwoParentsAnswerIsTooMany() {
    final String code =
        """
        _package user
        $
        _method zz_c.t()
          _super.fp()
        _endmethod
        """;
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    this.addExemplar(definitionKeeper, ExemplarDefinition.Sort.MIXIN, ZZ_A);
    this.addExemplar(definitionKeeper, ExemplarDefinition.Sort.MIXIN, ZZ_B);
    this.addExemplar(definitionKeeper, ExemplarDefinition.Sort.SLOTTED, ZZ_C, ZZ_A, ZZ_B);
    this.addMethod(definitionKeeper, ZZ_A, "fp()");
    this.addMethod(definitionKeeper, ZZ_B, "fp()");
    final List<String> messages = this.runCheckForMessages(code, definitionKeeper);
    Assertions.assertThat(messages).containsExactly("Too many super implementations of fp()");
  }

  @Test
  void testTwoParentsReachingOneAncestorMethodIsTooMany() {
    // The session counts the parents that answer, so a diamond fails to load as well.
    final String code =
        """
        _package user
        $
        _method zz_c.t()
          _super.dm()
        _endmethod
        """;
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    this.addExemplar(definitionKeeper, ExemplarDefinition.Sort.MIXIN, ZZ_D);
    this.addExemplar(definitionKeeper, ExemplarDefinition.Sort.MIXIN, ZZ_A, ZZ_D);
    this.addExemplar(definitionKeeper, ExemplarDefinition.Sort.MIXIN, ZZ_B, ZZ_D);
    this.addExemplar(definitionKeeper, ExemplarDefinition.Sort.SLOTTED, ZZ_C, ZZ_A, ZZ_B);
    this.addMethod(definitionKeeper, ZZ_D, "dm()");
    final List<String> messages = this.runCheckForMessages(code, definitionKeeper);
    Assertions.assertThat(messages).containsExactly("Too many super implementations of dm()");
  }

  @Test
  void testAbstractAndConcreteParentsAreTooMany() {
    final String code =
        """
        _package user
        $
        _method zz_c.t()
          _super.am()
        _endmethod
        """;
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    this.addExemplar(definitionKeeper, ExemplarDefinition.Sort.MIXIN, ZZ_A);
    this.addExemplar(definitionKeeper, ExemplarDefinition.Sort.MIXIN, ZZ_B);
    this.addExemplar(definitionKeeper, ExemplarDefinition.Sort.SLOTTED, ZZ_C, ZZ_A, ZZ_B);
    this.addMethod(definitionKeeper, ZZ_A, "am()", MethodDefinition.Modifier.ABSTRACT);
    this.addMethod(definitionKeeper, ZZ_B, "am()");
    final List<String> messages = this.runCheckForMessages(code, definitionKeeper);
    Assertions.assertThat(messages).containsExactly("Too many super implementations of am()");
  }

  @Test
  void testPrivateAndPublicParentsAreTooMany() {
    final String code =
        """
        _package user
        $
        _method zz_c.t()
          _super.pm()
        _endmethod
        """;
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    this.addExemplar(definitionKeeper, ExemplarDefinition.Sort.MIXIN, ZZ_A);
    this.addExemplar(definitionKeeper, ExemplarDefinition.Sort.MIXIN, ZZ_B);
    this.addExemplar(definitionKeeper, ExemplarDefinition.Sort.SLOTTED, ZZ_C, ZZ_A, ZZ_B);
    this.addMethod(definitionKeeper, ZZ_A, "pm()", MethodDefinition.Modifier.PRIVATE);
    this.addMethod(definitionKeeper, ZZ_B, "pm()");
    final List<String> messages = this.runCheckForMessages(code, definitionKeeper);
    Assertions.assertThat(messages).containsExactly("Too many super implementations of pm()");
  }

  @Test
  void testParentAnswersThroughObject() {
    // A mixin parent does not reach sw:object; the slotted parent does.
    final String code =
        """
        _package user
        $
        _method zz_c.t()
          _super.zz_object_m
        _endmethod
        """;
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    this.addObjectMethod(definitionKeeper);
    this.addExemplar(definitionKeeper, ExemplarDefinition.Sort.MIXIN, ZZ_A);
    this.addExemplar(definitionKeeper, ExemplarDefinition.Sort.SLOTTED, ZZ_B);
    this.addExemplar(definitionKeeper, ExemplarDefinition.Sort.SLOTTED, ZZ_C, ZZ_A, ZZ_B);
    final List<String> messages = this.runCheckForMessages(code, definitionKeeper);
    Assertions.assertThat(messages).isEmpty();
  }

  @Test
  void testMixinParentAndImplicitFormatMixinAreTooMany() {
    // The implicit format mixin answers through sw:object, beside the mixin's own method.
    final String code =
        """
        _package user
        $
        _method zz_c.t()
          _super.zz_object_m
        _endmethod
        """;
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    this.addObjectMethod(definitionKeeper);
    this.addExemplar(definitionKeeper, ExemplarDefinition.Sort.MIXIN, ZZ_A);
    this.addExemplar(definitionKeeper, ExemplarDefinition.Sort.SLOTTED, ZZ_C, ZZ_A);
    this.addMethod(definitionKeeper, ZZ_A, "zz_object_m");
    final List<String> messages = this.runCheckForMessages(code, definitionKeeper);
    Assertions.assertThat(messages)
        .containsExactly("Too many super implementations of zz_object_m");
  }

  @Test
  void testNoParentAnswersIsReportedPerParent() {
    final String code =
        """
        _package user
        $
        _method zz_c.t()
          _super.nonexistent()
        _endmethod
        """;
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    this.addExemplar(definitionKeeper, ExemplarDefinition.Sort.MIXIN, ZZ_A);
    this.addExemplar(definitionKeeper, ExemplarDefinition.Sort.SLOTTED, ZZ_B);
    this.addExemplar(definitionKeeper, ExemplarDefinition.Sort.SLOTTED, ZZ_C, ZZ_A, ZZ_B);
    final List<String> messages = this.runCheckForMessages(code, definitionKeeper);
    Assertions.assertThat(messages)
        .containsExactlyInAnyOrder(
            "Unknown method on super: user:zz_a.nonexistent()",
            "Unknown method on super: user:zz_b.nonexistent()");
  }

  @Test
  void testUndefinedParentWithoutAnsweringParentIsNotReported() {
    final String code =
        """
        _package user
        $
        _method zz_c.t()
          _super.nonexistent()
        _endmethod
        """;
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    this.addExemplar(definitionKeeper, ExemplarDefinition.Sort.SLOTTED, ZZ_B);
    this.addExemplar(
        definitionKeeper, ExemplarDefinition.Sort.SLOTTED, ZZ_C, ZZ_B, TypeString.UNDEFINED);
    final List<String> messages = this.runCheckForMessages(code, definitionKeeper);
    Assertions.assertThat(messages).isEmpty();
  }

  @Test
  void testParentUnderTwoSpellingsIsOneParent() {
    // `{:zz_a}` read in package user is filed as user:zz_a, the type being sw:zz_a.
    final String code =
        """
        _package user
        $
        _method zz_c.t()
          _super.m()
        _endmethod
        """;
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    definitionKeeper.add(
        new PackageDefinition(
            null, null, null, null, null, TypeString.USER_PACKAGE, List.of("sw")));
    final TypeString swZzA = TypeString.ofIdentifier("zz_a", "sw");
    this.addExemplar(definitionKeeper, ExemplarDefinition.Sort.SLOTTED, swZzA);
    this.addMethod(definitionKeeper, swZzA, "m()");
    this.addExemplar(definitionKeeper, ExemplarDefinition.Sort.SLOTTED, ZZ_C, ZZ_A, swZzA);
    final List<String> messages = this.runCheckForMessages(code, definitionKeeper);
    Assertions.assertThat(messages).isEmpty();
  }

  @Test
  void testNamedSuperStillReportsTheNamedParent() {
    final String code =
        """
        _package user
        $
        _method zz_c.t()
          _super(zz_b).only_a()
        _endmethod
        """;
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    this.addExemplar(definitionKeeper, ExemplarDefinition.Sort.MIXIN, ZZ_A);
    this.addExemplar(definitionKeeper, ExemplarDefinition.Sort.MIXIN, ZZ_B);
    this.addExemplar(definitionKeeper, ExemplarDefinition.Sort.SLOTTED, ZZ_C, ZZ_A, ZZ_B);
    this.addMethod(definitionKeeper, ZZ_A, "only_a()");
    final List<String> messages = this.runCheckForMessages(code, definitionKeeper);
    Assertions.assertThat(messages).containsExactly("Unknown method on super: user:zz_b.only_a()");
  }

  private List<String> runCheckForMessages(
      final String code, final IDefinitionKeeper definitionKeeper) {
    final MagikTypedFile magikFile =
        new MagikTypedFile(MagikTypedFile.DEFAULT_URI, code, definitionKeeper);
    final MagikTypedCheck check = new SuperMethodExistsTypedCheck();
    final List<Issue> issues = check.scanFileForIssues(magikFile);
    return issues.stream().map(Issue::message).toList();
  }

  private void addExemplar(
      final IDefinitionKeeper definitionKeeper,
      final ExemplarDefinition.Sort sort,
      final TypeString typeString,
      final TypeString... parents) {
    definitionKeeper.add(
        new ExemplarDefinition(null, null, null, null, null, sort, typeString, null));
    for (final TypeString parent : parents) {
      definitionKeeper.add(
          new InheritanceDefinition(null, null, null, null, null, typeString, parent));
    }
  }

  private void addMethod(
      final IDefinitionKeeper definitionKeeper,
      final TypeString typeString,
      final String methodName,
      final MethodDefinition.Modifier... modifiers) {
    final EnumSet<MethodDefinition.Modifier> modifierSet =
        EnumSet.noneOf(MethodDefinition.Modifier.class);
    final List<MethodDefinition.Modifier> modifierList = List.of(modifiers);
    modifierSet.addAll(modifierList);
    definitionKeeper.add(
        new MethodDefinition(
            null,
            null,
            null,
            null,
            null,
            typeString,
            methodName,
            modifierSet,
            Collections.emptyList(),
            null,
            null,
            ExpressionResultString.EMPTY,
            ExpressionResultString.EMPTY));
  }

  /** A method on sw:object, which the slotted format mixin inherits. */
  private void addObjectMethod(final IDefinitionKeeper definitionKeeper) {
    definitionKeeper.add(
        new InheritanceDefinition(
            null,
            null,
            null,
            null,
            null,
            TypeString.SW_SLOTTED_FORMAT_MIXIN,
            TypeString.SW_OBJECT));
    this.addMethod(definitionKeeper, TypeString.SW_OBJECT, "zz_object_m");
  }
}
