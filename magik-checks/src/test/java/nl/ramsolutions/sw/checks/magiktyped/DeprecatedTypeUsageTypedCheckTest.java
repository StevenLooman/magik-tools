package nl.ramsolutions.sw.checks.magiktyped;

import static nl.ramsolutions.sw.checks.magiktyped.MagikTypedCheckAssert.assertThat;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import nl.ramsolutions.sw.checks.Issue;
import nl.ramsolutions.sw.checks.MagikTypedCheck;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.analysis.definitions.DefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.ExemplarDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.GlobalDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.Pragma;
import nl.ramsolutions.sw.magik.analysis.definitions.ProcedureDefinition;
import nl.ramsolutions.sw.magik.analysis.typing.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

/** Test {@link DeprecatedTypeUsageTypedCheck}. */
class DeprecatedTypeUsageTypedCheckTest {

  private static final TypeString VALUE_EXEMPLAR =
      TypeString.ofIdentifier("value_exemplar", "user");
  private static final TypeString GLOBAL = TypeString.ofIdentifier("!a_global!", "user");
  private static final TypeString PROCEDURE = TypeString.ofIdentifier("a_procedure", "user");

  private void addExemplarDefinition(
      final IDefinitionKeeper definitionKeeper,
      final TypeString typeName,
      final String classifyLevel) {
    this.addExemplarDefinition(definitionKeeper, typeName, classifyLevel, null);
  }

  private void addExemplarDefinition(
      final IDefinitionKeeper definitionKeeper,
      final TypeString typeName,
      final String classifyLevel,
      final String moduleName) {
    definitionKeeper.add(
        new ExemplarDefinition(
            null,
            null,
            moduleName,
            null,
            null,
            ExemplarDefinition.Sort.SLOTTED,
            typeName,
            new Pragma(null, Arrays.asList(classifyLevel), Set.of(), Set.of())));
  }

  private void addGlobalDefinition(
      final IDefinitionKeeper definitionKeeper, final String classifyLevel) {
    definitionKeeper.add(
        new GlobalDefinition(
            null,
            null,
            null,
            null,
            null,
            GLOBAL,
            VALUE_EXEMPLAR,
            new Pragma(null, Arrays.asList(classifyLevel), Set.of(), Set.of())));
  }

  private void addProcedureDefinition(
      final IDefinitionKeeper definitionKeeper, final String classifyLevel) {
    definitionKeeper.add(
        new ProcedureDefinition(
            null,
            null,
            null,
            null,
            null,
            Set.of(),
            PROCEDURE,
            "a_procedure",
            Collections.emptyList(),
            new Pragma(null, Arrays.asList(classifyLevel), Set.of(), Set.of()),
            ExpressionResultString.EMPTY,
            ExpressionResultString.EMPTY));
  }

  private List<Issue> runCheck(final String code, final IDefinitionKeeper definitionKeeper) {
    final MagikTypedFile magikFile =
        new MagikTypedFile(MagikTypedFile.DEFAULT_URI, code, definitionKeeper);
    final MagikTypedCheck check = new DeprecatedTypeUsageTypedCheck();
    return check.scanFileForIssues(magikFile);
  }

  @Test
  void testTypeDeprecated() {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final TypeString typeStr = TypeString.ofIdentifier("test", "user");
    this.addExemplarDefinition(definitionKeeper, typeStr, "deprecated");
    final String code =
        """
        _block
          user:test.m()
        _endblock""";
    final MagikTypedCheck check = new DeprecatedTypeUsageTypedCheck();
    assertThat(check).reportsIssueCount(code, definitionKeeper, 1);
  }

  @Test
  void testTypeNotDeprecated() {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final TypeString typeStr = TypeString.ofIdentifier("test", "user");
    this.addExemplarDefinition(definitionKeeper, typeStr, "basic");
    final String code =
        """
        _block
          user:test.m()
        _endblock""";
    final MagikTypedCheck check = new DeprecatedTypeUsageTypedCheck();
    assertThat(check).reportsNoIssues(code, definitionKeeper);
  }

  @Test
  void testTypeDeprecatedByEveryDefinition() {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final TypeString typeStr = TypeString.ofIdentifier("test", "user");
    this.addExemplarDefinition(definitionKeeper, typeStr, "deprecated", "module_a");
    this.addExemplarDefinition(definitionKeeper, typeStr, "deprecated", "module_b");
    final String code =
        """
        _block
          user:test.m()
        _endblock""";
    final MagikTypedCheck check = new DeprecatedTypeUsageTypedCheck();
    assertThat(check).reportsIssueCount(code, definitionKeeper, 1);
  }

  @Test
  void testTypeDeprecatedByOneDefinitionOnly() {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final TypeString typeStr = TypeString.ofIdentifier("test", "user");
    this.addExemplarDefinition(definitionKeeper, typeStr, "deprecated", "module_a");
    this.addExemplarDefinition(definitionKeeper, typeStr, "basic", "module_b");
    final String code =
        """
        _block
          user:test.m()
        _endblock""";
    final MagikTypedCheck check = new DeprecatedTypeUsageTypedCheck();
    assertThat(check).reportsNoIssues(code, definitionKeeper);
  }

  @Test
  void testGlobalDeprecated() {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    this.addExemplarDefinition(definitionKeeper, VALUE_EXEMPLAR, "basic");
    this.addGlobalDefinition(definitionKeeper, "deprecated");
    final String code =
        """
        _block
          _return !a_global!
        _endblock""";
    final List<Issue> issues = this.runCheck(code, definitionKeeper);
    Assertions.assertThat(issues).hasSize(1);

    final Issue issue = issues.get(0);
    final String message = issue.message();
    Assertions.assertThat(message).isEqualTo("Used global 'user:!a_global!' is deprecated");
  }

  @Test
  void testGlobalHoldingADeprecatedExemplarIsNotReported() {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    this.addExemplarDefinition(definitionKeeper, VALUE_EXEMPLAR, "deprecated");
    this.addGlobalDefinition(definitionKeeper, "basic");
    final String code =
        """
        _block
          _return !a_global!
        _endblock""";
    final MagikTypedCheck check = new DeprecatedTypeUsageTypedCheck();
    assertThat(check).reportsNoIssues(code, definitionKeeper);
  }

  @Test
  void testProcedureDeprecated() {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    this.addProcedureDefinition(definitionKeeper, "deprecated");
    final String code =
        """
        _block
          _return a_procedure()
        _endblock""";
    final List<Issue> issues = this.runCheck(code, definitionKeeper);
    Assertions.assertThat(issues).hasSize(1);

    final Issue issue = issues.get(0);
    final String message = issue.message();
    Assertions.assertThat(message).isEqualTo("Used procedure 'user:a_procedure' is deprecated");
  }

  @Test
  void testProcedureNotDeprecated() {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    this.addProcedureDefinition(definitionKeeper, "basic");
    final String code =
        """
        _block
          _return a_procedure()
        _endblock""";
    final MagikTypedCheck check = new DeprecatedTypeUsageTypedCheck();
    assertThat(check).reportsNoIssues(code, definitionKeeper);
  }

  @Test
  void testLocalOfADeprecatedTypeIsNotReported() {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    this.addExemplarDefinition(definitionKeeper, VALUE_EXEMPLAR, "deprecated");
    final String code =
        """
        _method a.b(p)
          ## @param {user:value_exemplar} p
          _return p
        _endmethod""";
    final MagikTypedCheck check = new DeprecatedTypeUsageTypedCheck();
    assertThat(check).reportsNoIssues(code, definitionKeeper);
  }
}
