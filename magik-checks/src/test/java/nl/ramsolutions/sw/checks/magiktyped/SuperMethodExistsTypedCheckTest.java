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
import nl.ramsolutions.sw.magik.analysis.typing.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

/** Tests for {@link SuperMethodExistsTypedCheck}. */
class SuperMethodExistsTypedCheckTest {

  private static final TypeString TYPE_PARENT = TypeString.ofIdentifier("parent", "sw");
  private static final TypeString TYPE_CHILD = TypeString.ofIdentifier("child", "sw");

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
}
