package nl.ramsolutions.sw.checks.magiktyped;

import static nl.ramsolutions.sw.checks.magiktyped.MagikTypedCheckAssert.assertThat;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import nl.ramsolutions.sw.checks.MagikTypedCheck;
import nl.ramsolutions.sw.magik.analysis.definitions.DefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.ExemplarDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodDefinition;
import nl.ramsolutions.sw.magik.analysis.typing.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import org.junit.jupiter.api.Test;

/** Tests for {@link AbstractMethodNotImplementedTypedCheck}. */
class AbstractMethodNotImplementedTypedCheckTest {

  private static final TypeString TYPE_PARENT = TypeString.ofIdentifier("parent", "sw");
  private static final TypeString TYPE_CHILD = TypeString.ofIdentifier("child", "sw");

  @Test
  void testAbstractMethodImplemented() {
    final String code =
        """
        _method child.do_something()
          _return 1
        _endmethod
        """;
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    definitionKeeper.add(
        new ExemplarDefinition(
            null,
            null,
            null,
            null,
            null,
            ExemplarDefinition.Sort.SLOTTED,
            TYPE_PARENT,
            Collections.emptyList(),
            Collections.emptyList(),
            null));
    definitionKeeper.add(
        new MethodDefinition(
            null,
            null,
            null,
            null,
            null,
            TYPE_PARENT,
            "do_something()",
            EnumSet.of(MethodDefinition.Modifier.ABSTRACT),
            Collections.emptyList(),
            null,
            null,
            ExpressionResultString.EMPTY,
            ExpressionResultString.EMPTY));
    definitionKeeper.add(
        new ExemplarDefinition(
            null,
            null,
            null,
            null,
            null,
            ExemplarDefinition.Sort.SLOTTED,
            TYPE_CHILD,
            Collections.emptyList(),
            List.of(TYPE_PARENT),
            null));
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
            new ExpressionResultString(TypeString.SW_INTEGER),
            ExpressionResultString.EMPTY));
    final MagikTypedCheck check = new AbstractMethodNotImplementedTypedCheck();
    assertThat(check).reportsNoIssues(code, definitionKeeper);
  }

  @Test
  void testAbstractMethodNotImplemented() {
    final String code =
        """
        _method child.other_method()
          _return 1
        _endmethod
        """;
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    definitionKeeper.add(
        new ExemplarDefinition(
            null,
            null,
            null,
            null,
            null,
            ExemplarDefinition.Sort.SLOTTED,
            TYPE_PARENT,
            Collections.emptyList(),
            Collections.emptyList(),
            null));
    definitionKeeper.add(
        new MethodDefinition(
            null,
            null,
            null,
            null,
            null,
            TYPE_PARENT,
            "do_something()",
            EnumSet.of(MethodDefinition.Modifier.ABSTRACT),
            Collections.emptyList(),
            null,
            null,
            ExpressionResultString.EMPTY,
            ExpressionResultString.EMPTY));
    definitionKeeper.add(
        new ExemplarDefinition(
            null,
            null,
            null,
            null,
            null,
            ExemplarDefinition.Sort.SLOTTED,
            TYPE_CHILD,
            Collections.emptyList(),
            List.of(TYPE_PARENT),
            null));
    definitionKeeper.add(
        new MethodDefinition(
            null,
            null,
            null,
            null,
            null,
            TYPE_CHILD,
            "other_method()",
            EnumSet.noneOf(MethodDefinition.Modifier.class),
            Collections.emptyList(),
            null,
            null,
            new ExpressionResultString(TypeString.SW_INTEGER),
            ExpressionResultString.EMPTY));
    final MagikTypedCheck check = new AbstractMethodNotImplementedTypedCheck();
    assertThat(check).reportsIssueCount(code, definitionKeeper, 1);
  }

  @Test
  void testNoParents() {
    final String code =
        """
        _method child.do_something()
          _return 1
        _endmethod
        """;
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    definitionKeeper.add(
        new ExemplarDefinition(
            null,
            null,
            null,
            null,
            null,
            ExemplarDefinition.Sort.SLOTTED,
            TYPE_CHILD,
            Collections.emptyList(),
            Collections.emptyList(),
            null));
    final MagikTypedCheck check = new AbstractMethodNotImplementedTypedCheck();
    assertThat(check).reportsNoIssues(code, definitionKeeper);
  }
}
