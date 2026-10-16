package nl.ramsolutions.sw.checks.magiktyped;

import static nl.ramsolutions.sw.checks.magiktyped.MagikTypedCheckAssert.assertThat;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import nl.ramsolutions.sw.checks.Issue;
import nl.ramsolutions.sw.checks.MagikTypedCheck;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.analysis.definitions.DefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodDefinition;
import nl.ramsolutions.sw.magik.analysis.typing.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

/** Test {@link UndefinedMethodCallResultTypedCheck}. */
class UndefinedMethodCallResultTypedCheckTest {

  @Test
  void testMethodInvocationOnUndefined() {
    final String code =
        """
        _block
          undefined.m()
        _endblock
        """;
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final MagikTypedCheck check = new UndefinedMethodCallResultTypedCheck();
    assertThat(check).reportsNoIssues(code, definitionKeeper);
  }

  @Test
  void testMethodInvocationUndefined() {
    final String code =
        """
        _block
          object.m()
        _endblock
        """;
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final MagikTypedCheck check = new UndefinedMethodCallResultTypedCheck();
    assertThat(check).reportsNoIssues(code, definitionKeeper);
  }

  @Test
  void testMethodInvocationDefined() {
    final String code =
        """
        _block
          object.m()
        _endblock
        """;
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

    final MagikTypedCheck check = new UndefinedMethodCallResultTypedCheck();
    assertThat(check).reportsNoIssues(code, definitionKeeper);
  }

  @Test
  void testMethodInvocationUndefinedResults() {
    final String code =
        """
        _block
          object.m()
        _endblock
        """;
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
            ExpressionResultString.UNDEFINED,
            ExpressionResultString.EMPTY));

    final MagikTypedCheck check = new UndefinedMethodCallResultTypedCheck();
    assertThat(check).reportsIssueCount(code, definitionKeeper, 1);
  }

  @Test
  void testIndexerMethodInvocationUndefinedResultsMessage() {
    final String code =
        """
        _block
          object[1]
        _endblock
        """;
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
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
            Collections.emptyList(),
            null,
            null,
            ExpressionResultString.UNDEFINED,
            ExpressionResultString.EMPTY));
    final String message = this.runCheckForMessage(code, definitionKeeper);
    Assertions.assertThat(message).isEqualTo("UNDEFINED results for method: sw:object[]");
  }

  private String runCheckForMessage(final String code, final IDefinitionKeeper definitionKeeper) {
    final MagikTypedFile magikFile =
        new MagikTypedFile(MagikTypedFile.DEFAULT_URI, code, definitionKeeper);
    final MagikTypedCheck check = new UndefinedMethodCallResultTypedCheck();
    final List<Issue> issues = check.scanFileForIssues(magikFile);
    Assertions.assertThat(issues).hasSize(1);

    final Issue issue = issues.get(0);
    return issue.message();
  }

  @Test
  void testMethodInvocationUndefinedResultsMessage() {
    final String code =
        """
        _block
          object.m()
        _endblock
        """;
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
            ExpressionResultString.UNDEFINED,
            ExpressionResultString.EMPTY));
    final String message = this.runCheckForMessage(code, definitionKeeper);
    Assertions.assertThat(message).isEqualTo("UNDEFINED results for method: sw:object.m()");
  }
}
