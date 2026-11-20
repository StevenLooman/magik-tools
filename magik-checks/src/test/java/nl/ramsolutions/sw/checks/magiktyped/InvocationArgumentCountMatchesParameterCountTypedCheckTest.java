package nl.ramsolutions.sw.checks.magiktyped;

import static nl.ramsolutions.sw.checks.magiktyped.MagikTypedCheckAssert.assertThat;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import nl.ramsolutions.sw.checks.Issue;
import nl.ramsolutions.sw.checks.MagikTypedCheck;
import nl.ramsolutions.sw.magik.MagikFile;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.analysis.definitions.DefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.ExemplarDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.InheritanceDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.MagikDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.ParameterDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.ProcedureDefinition;
import nl.ramsolutions.sw.magik.analysis.typing.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

/** Test {@link InvocationArgumentCountMatchesParameterCountTypedCheck}. */
class InvocationArgumentCountMatchesParameterCountTypedCheckTest {

  @Test
  void testMethodUnknown() {
    final String code =
        """
        _block
          object.m()
        _endblock""";
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final MagikTypedCheck check = new InvocationArgumentCountMatchesParameterCountTypedCheck();
    assertThat(check).reportsNoIssues(code, definitionKeeper);
  }

  @Test
  void testArgumentCountMatches() {
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
            List.of(
                new ParameterDefinition(
                    null,
                    null,
                    null,
                    null,
                    null,
                    "p1",
                    ParameterDefinition.Modifier.NONE,
                    TypeString.SW_OBJECT),
                new ParameterDefinition(
                    null,
                    null,
                    null,
                    null,
                    null,
                    "p2",
                    ParameterDefinition.Modifier.NONE,
                    TypeString.SW_OBJECT)),
            null,
            null,
            ExpressionResultString.EMPTY,
            ExpressionResultString.EMPTY));

    final String code =
        """
        _block
          object.m(object, object)
        _endblock""";
    final MagikTypedCheck check = new InvocationArgumentCountMatchesParameterCountTypedCheck();
    assertThat(check).reportsNoIssues(code, definitionKeeper);
  }

  @Test
  void testArgumentMissing() {
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
            List.of(
                new ParameterDefinition(
                    null,
                    null,
                    null,
                    null,
                    null,
                    "p1",
                    ParameterDefinition.Modifier.NONE,
                    TypeString.SW_OBJECT),
                new ParameterDefinition(
                    null,
                    null,
                    null,
                    null,
                    null,
                    "p2",
                    ParameterDefinition.Modifier.NONE,
                    TypeString.SW_OBJECT)),
            null,
            null,
            ExpressionResultString.EMPTY,
            ExpressionResultString.EMPTY));

    final String code =
        """
        _block
          object.m(object)
        _endblock""";
    final MagikTypedCheck check = new InvocationArgumentCountMatchesParameterCountTypedCheck();
    assertThat(check).reportsIssueCount(code, definitionKeeper, 1);
  }

  @Test
  void testScatterArgumentIsNotCounted() {
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
            List.of(
                new ParameterDefinition(
                    null,
                    null,
                    null,
                    null,
                    null,
                    "p1",
                    ParameterDefinition.Modifier.NONE,
                    TypeString.SW_OBJECT),
                new ParameterDefinition(
                    null,
                    null,
                    null,
                    null,
                    null,
                    "p2",
                    ParameterDefinition.Modifier.NONE,
                    TypeString.SW_OBJECT)),
            null,
            null,
            ExpressionResultString.EMPTY,
            ExpressionResultString.EMPTY));

    final String code =
        """
        _block
          object.m(_scatter args)
        _endblock""";
    final MagikTypedCheck check = new InvocationArgumentCountMatchesParameterCountTypedCheck();
    assertThat(check).reportsNoIssues(code, definitionKeeper);
  }

  @Test
  void testProcedureArgumentCountMatches() {
    final String code =
        """
        _block
          (_proc(p1, p2)
          _endproc)(object, object)
        _endblock
        """;
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final MagikTypedCheck check = new InvocationArgumentCountMatchesParameterCountTypedCheck();
    assertThat(check).reportsNoIssues(code, definitionKeeper);
  }

  @Test
  void testProcedureScatterArgumentIsNotCounted() {
    final String code =
        """
        _block
          (_proc(p1, p2)
          _endproc)(_scatter args)
        _endblock
        """;
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final MagikTypedCheck check = new InvocationArgumentCountMatchesParameterCountTypedCheck();
    assertThat(check).reportsNoIssues(code, definitionKeeper);
  }

  @Test
  void testProcedureArgumentMissing() {
    final String code =
        """
        _block
          (_proc(p1, p2)
          _endproc)(object)
        _endblock
        """;
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final MagikTypedCheck check = new InvocationArgumentCountMatchesParameterCountTypedCheck();
    assertThat(check).reportsIssueCount(code, definitionKeeper, 1);
  }

  @Test
  void testProcedureArgumentMissingMessageNamesTheProcedureType() {
    // A session says: Procedure proc unnamed(a) invoked with too few arguments (requires 1, given
    // 0). A check knows the procedure by its type.
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final TypeString procedureRef = TypeString.ofIdentifier("zz_pr", "user");
    final ParameterDefinition parameter =
        new ParameterDefinition(
            null,
            null,
            null,
            null,
            null,
            "a",
            ParameterDefinition.Modifier.NONE,
            TypeString.UNDEFINED);
    definitionKeeper.add(
        new ProcedureDefinition(
            null,
            null,
            null,
            null,
            null,
            Set.of(),
            procedureRef,
            null,
            List.of(parameter),
            null,
            ExpressionResultString.EMPTY,
            ExpressionResultString.EMPTY));
    final String code =
        """
        _package user
        _block
          zz_pr()
        _endblock""";
    final MagikTypedCheck check = new InvocationArgumentCountMatchesParameterCountTypedCheck();
    assertThat(check)
        .reportsIssueMessages(
            code,
            definitionKeeper,
            "Procedure user:zz_pr invoked with too few arguments (requires 1, given 0)");
  }

  private void addProcedureWithOneParameter(
      final IDefinitionKeeper definitionKeeper, final String procedureName) {
    final TypeString procedureRef = TypeString.ofIdentifier(procedureName, "user");
    final ParameterDefinition parameter =
        new ParameterDefinition(
            null,
            null,
            null,
            null,
            null,
            "a",
            ParameterDefinition.Modifier.NONE,
            TypeString.UNDEFINED);
    definitionKeeper.add(
        new ProcedureDefinition(
            null,
            null,
            null,
            null,
            null,
            Set.of(),
            procedureRef,
            null,
            List.of(parameter),
            null,
            ExpressionResultString.EMPTY,
            ExpressionResultString.EMPTY));
  }

  @Test
  void testProcedureMemberOfAUnionArgumentMissingMessage() {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    this.addProcedureWithOneParameter(definitionKeeper, "zz_pr");
    final String code =
        """
        _package user
        _method object.m()
          _local f << _self.y()  # type: _undefined|user:zz_pr
          f()
        _endmethod""";
    final MagikTypedCheck check = new InvocationArgumentCountMatchesParameterCountTypedCheck();
    assertThat(check)
        .reportsIssueMessages(
            code,
            definitionKeeper,
            "Procedure user:zz_pr invoked with too few arguments (requires 1, given 0)");
  }

  @Test
  void testEachProcedureMemberOfAUnionIsChecked() {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    this.addProcedureWithOneParameter(definitionKeeper, "zz_pr");
    this.addProcedureWithOneParameter(definitionKeeper, "zz_pr2");
    final String code =
        """
        _package user
        _method object.m()
          _local f << _self.y()  # type: user:zz_pr|user:zz_pr2
          f()
        _endmethod""";
    final MagikTypedCheck check = new InvocationArgumentCountMatchesParameterCountTypedCheck();
    assertThat(check).reportsIssueCount(code, definitionKeeper, 2);
  }

  @Test
  void testProcedureMemberOfAUnionArgumentCountMatches() {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    this.addProcedureWithOneParameter(definitionKeeper, "zz_pr");
    final String code =
        """
        _package user
        _method object.m()
          _local f << _self.y()  # type: _undefined|user:zz_pr
          f(1)
        _endmethod""";
    final MagikTypedCheck check = new InvocationArgumentCountMatchesParameterCountTypedCheck();
    assertThat(check).reportsNoIssues(code, definitionKeeper);
  }

  @Test
  void testAnonymousProcedureArgumentMissingMessage() {
    // A procedure of no type of its own is a sw:procedure.
    final String code =
        """
        _block
          (_proc(p1, p2)
          _endproc)(object)
        _endblock
        """;
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final MagikTypedCheck check = new InvocationArgumentCountMatchesParameterCountTypedCheck();
    assertThat(check)
        .reportsIssueMessages(
            code,
            definitionKeeper,
            "Procedure sw:procedure invoked with too few arguments (requires 2, given 1)");
  }

  @Test
  void testProcedureInALocalArgumentMissingMessage() {
    // An anonymous procedure's own type is internal, so the message names a sw:procedure.
    final String code =
        """
        _method object.m()
          _local p << _proc(a)
                      _endproc
          p()
        _endmethod
        """;
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final MagikFile magikFile = new MagikFile(MagikTypedFile.DEFAULT_URI, code);
    final List<MagikDefinition> definitions = magikFile.getMagikDefinitions();
    definitions.forEach(definitionKeeper::add);
    final MagikTypedCheck check = new InvocationArgumentCountMatchesParameterCountTypedCheck();
    assertThat(check)
        .reportsIssueMessages(
            code,
            definitionKeeper,
            "Procedure sw:procedure invoked with too few arguments (requires 1, given 0)");
  }

  @Test
  void testIndexerArgumentCountMismatchMessage() {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final ParameterDefinition parameter1 =
        new ParameterDefinition(
            null,
            null,
            null,
            null,
            null,
            "p1",
            ParameterDefinition.Modifier.NONE,
            TypeString.SW_OBJECT);
    final ParameterDefinition parameter2 =
        new ParameterDefinition(
            null,
            null,
            null,
            null,
            null,
            "p2",
            ParameterDefinition.Modifier.NONE,
            TypeString.SW_OBJECT);
    final List<ParameterDefinition> parameters = List.of(parameter1, parameter2);
    definitionKeeper.add(
        new MethodDefinition(
            null,
            null,
            null,
            null,
            null,
            TypeString.SW_OBJECT,
            "[]",
            EnumSet.noneOf(MethodDefinition.Modifier.class),
            parameters,
            null,
            null,
            ExpressionResultString.EMPTY,
            ExpressionResultString.EMPTY));
    final String code =
        """
        _block
          object[1]
        _endblock""";
    final String message = this.runCheckForMessage(code, definitionKeeper);
    Assertions.assertThat(message)
        .isEqualTo("Object sw:object sent message [] with too few arguments (requires 2, given 1)");
  }

  private String runCheckForMessage(final String code, final IDefinitionKeeper definitionKeeper) {
    final MagikTypedFile magikFile =
        new MagikTypedFile(MagikTypedFile.DEFAULT_URI, code, definitionKeeper);
    final MagikTypedCheck check = new InvocationArgumentCountMatchesParameterCountTypedCheck();
    final List<Issue> issues = check.scanFileForIssues(magikFile);
    Assertions.assertThat(issues).hasSize(1);

    final Issue issue = issues.get(0);
    return issue.message();
  }

  @Test
  void testArgumentCountMismatchMessage() {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final ParameterDefinition parameter1 =
        new ParameterDefinition(
            null,
            null,
            null,
            null,
            null,
            "p1",
            ParameterDefinition.Modifier.NONE,
            TypeString.SW_OBJECT);
    final ParameterDefinition parameter2 =
        new ParameterDefinition(
            null,
            null,
            null,
            null,
            null,
            "p2",
            ParameterDefinition.Modifier.NONE,
            TypeString.SW_OBJECT);
    final List<ParameterDefinition> parameters = List.of(parameter1, parameter2);
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
            parameters,
            null,
            null,
            ExpressionResultString.EMPTY,
            ExpressionResultString.EMPTY));
    final String code =
        """
        _block
          object.m(object)
        _endblock""";
    final String message = this.runCheckForMessage(code, definitionKeeper);
    Assertions.assertThat(message)
        .isEqualTo(
            "Object sw:object sent message m() with too few arguments (requires 2, given 1)");
  }

  /** A slotted {@code user:zz_e} over the lonely mixin {@code user:zz_m}. */
  private void addExemplarOverMixin(final IDefinitionKeeper definitionKeeper) {
    final TypeString mixinRef = TypeString.ofIdentifier("zz_m", "user");
    final TypeString exemplarRef = TypeString.ofIdentifier("zz_e", "user");
    definitionKeeper.add(
        new ExemplarDefinition(
            null, null, null, null, null, ExemplarDefinition.Sort.MIXIN, mixinRef, null));
    definitionKeeper.add(
        new ExemplarDefinition(
            null, null, null, null, null, ExemplarDefinition.Sort.SLOTTED, exemplarRef, null));
    definitionKeeper.add(
        new InheritanceDefinition(null, null, null, null, null, exemplarRef, mixinRef));
  }

  @Test
  void testObjectMethodOnNamedMixinSuperIsNotReported() {
    // A mixin's own table has no parents, so sw:object's method does not answer.
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
            List.of(
                new ParameterDefinition(
                    null,
                    null,
                    null,
                    null,
                    null,
                    "p1",
                    ParameterDefinition.Modifier.NONE,
                    TypeString.SW_OBJECT),
                new ParameterDefinition(
                    null,
                    null,
                    null,
                    null,
                    null,
                    "p2",
                    ParameterDefinition.Modifier.NONE,
                    TypeString.SW_OBJECT)),
            null,
            null,
            ExpressionResultString.EMPTY,
            ExpressionResultString.EMPTY));
    this.addExemplarOverMixin(definitionKeeper);
    final String code =
        """
        _package user
        _method zz_e.run()
          _super(zz_m).m(1)
        _endmethod""";
    final MagikTypedCheck check = new InvocationArgumentCountMatchesParameterCountTypedCheck();
    assertThat(check).reportsNoIssues(code, definitionKeeper);
  }
}
