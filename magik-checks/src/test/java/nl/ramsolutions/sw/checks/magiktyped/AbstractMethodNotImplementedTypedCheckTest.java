package nl.ramsolutions.sw.checks.magiktyped;

import static nl.ramsolutions.sw.checks.magiktyped.MagikTypedCheckAssert.assertThat;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import nl.ramsolutions.sw.checks.MagikTypedCheck;
import nl.ramsolutions.sw.magik.analysis.definitions.DefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.ExemplarDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.InheritanceDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodDefinition;
import nl.ramsolutions.sw.magik.analysis.typing.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import org.junit.jupiter.api.Test;

/** Tests for {@link AbstractMethodNotImplementedTypedCheck}. */
class AbstractMethodNotImplementedTypedCheckTest {

  private static final TypeString TYPE_PARENT = TypeString.ofIdentifier("parent", "sw");
  private static final TypeString TYPE_CHILD = TypeString.ofIdentifier("child", "sw");

  private void addExemplar(
      final IDefinitionKeeper definitionKeeper,
      final TypeString typeStr,
      final ExemplarDefinition.Sort sort,
      final List<TypeString> parents) {
    definitionKeeper.add(new ExemplarDefinition(null, null, null, null, null, sort, typeStr, null));
    parents.forEach(
        parentTypeStr ->
            definitionKeeper.add(
                new InheritanceDefinition(null, null, null, null, null, typeStr, parentTypeStr)));
  }

  private void addMethod(
      final IDefinitionKeeper definitionKeeper,
      final TypeString typeStr,
      final String methodName,
      final Set<MethodDefinition.Modifier> modifiers) {
    definitionKeeper.add(
        new MethodDefinition(
            null,
            null,
            null,
            null,
            null,
            typeStr,
            methodName,
            modifiers,
            Collections.emptyList(),
            null,
            null,
            ExpressionResultString.EMPTY,
            ExpressionResultString.EMPTY));
  }

  @Test
  void testAbstractMethodImplemented() {
    final String code = "def_slotted_exemplar(:child, {}, :parent)\n";
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    this.addExemplar(
        definitionKeeper, TYPE_PARENT, ExemplarDefinition.Sort.SLOTTED, Collections.emptyList());
    this.addMethod(
        definitionKeeper,
        TYPE_PARENT,
        "do_something()",
        EnumSet.of(MethodDefinition.Modifier.ABSTRACT));
    this.addExemplar(
        definitionKeeper, TYPE_CHILD, ExemplarDefinition.Sort.SLOTTED, List.of(TYPE_PARENT));
    this.addMethod(
        definitionKeeper,
        TYPE_CHILD,
        "do_something()",
        EnumSet.noneOf(MethodDefinition.Modifier.class));
    final MagikTypedCheck check = new AbstractMethodNotImplementedTypedCheck();
    assertThat(check).reportsNoIssues(code, definitionKeeper);
  }

  @Test
  void testAbstractMethodNotImplemented() {
    final String code = "def_slotted_exemplar(:child, {}, :parent)\n";
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    this.addExemplar(
        definitionKeeper, TYPE_PARENT, ExemplarDefinition.Sort.SLOTTED, Collections.emptyList());
    this.addMethod(
        definitionKeeper,
        TYPE_PARENT,
        "do_something()",
        EnumSet.of(MethodDefinition.Modifier.ABSTRACT));
    this.addExemplar(
        definitionKeeper, TYPE_CHILD, ExemplarDefinition.Sort.SLOTTED, List.of(TYPE_PARENT));
    final MagikTypedCheck check = new AbstractMethodNotImplementedTypedCheck();
    assertThat(check).reportsIssueCount(code, definitionKeeper, 1);
  }

  @Test
  void testMixinAbstractMethodNotImplemented() {
    final String code = "def_mixin(:child, :parent)\n";
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    this.addExemplar(
        definitionKeeper, TYPE_PARENT, ExemplarDefinition.Sort.SLOTTED, Collections.emptyList());
    this.addMethod(
        definitionKeeper,
        TYPE_PARENT,
        "do_something()",
        EnumSet.of(MethodDefinition.Modifier.ABSTRACT));
    this.addExemplar(
        definitionKeeper, TYPE_CHILD, ExemplarDefinition.Sort.MIXIN, List.of(TYPE_PARENT));
    final MagikTypedCheck check = new AbstractMethodNotImplementedTypedCheck();
    assertThat(check).reportsIssueCount(code, definitionKeeper, 1);
  }

  @Test
  void testNoParents() {
    final String code = "def_slotted_exemplar(:child, {})\n";
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    this.addExemplar(
        definitionKeeper, TYPE_CHILD, ExemplarDefinition.Sort.SLOTTED, Collections.emptyList());
    final MagikTypedCheck check = new AbstractMethodNotImplementedTypedCheck();
    assertThat(check).reportsNoIssues(code, definitionKeeper);
  }
}
