package nl.ramsolutions.sw.checks.magiktyped;

import static nl.ramsolutions.sw.checks.magiktyped.MagikTypedCheckAssert.assertThat;

import java.util.EnumSet;
import java.util.List;
import nl.ramsolutions.sw.checks.MagikTypedCheck;
import nl.ramsolutions.sw.magik.analysis.definitions.DefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.ParameterDefinition;
import nl.ramsolutions.sw.magik.analysis.typing.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
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
}
