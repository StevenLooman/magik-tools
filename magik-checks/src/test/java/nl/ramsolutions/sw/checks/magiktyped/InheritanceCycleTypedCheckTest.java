package nl.ramsolutions.sw.checks.magiktyped;

import static nl.ramsolutions.sw.checks.magiktyped.MagikTypedCheckAssert.assertThat;

import java.util.Collections;
import java.util.List;
import nl.ramsolutions.sw.checks.MagikTypedCheck;
import nl.ramsolutions.sw.magik.analysis.definitions.DefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.ExemplarDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.InheritanceDefinition;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import org.junit.jupiter.api.Test;

/** Tests for {@link InheritanceCycleTypedCheck}. */
class InheritanceCycleTypedCheckTest {

  private static final TypeString TYPE_CHILD = TypeString.ofIdentifier("child", "sw");
  private static final TypeString TYPE_PARENT = TypeString.ofIdentifier("parent", "sw");
  private static final TypeString TYPE_GRANDPARENT = TypeString.ofIdentifier("grandparent", "sw");
  private static final TypeString TYPE_BASE = TypeString.ofIdentifier("base", "sw");
  private static final TypeString TYPE_LEFT = TypeString.ofIdentifier("left", "sw");
  private static final TypeString TYPE_RIGHT = TypeString.ofIdentifier("right", "sw");

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

  @Test
  void testSelfCycleReported() {
    // A type declaring itself as its own parent.
    final String code = "def_slotted_exemplar(:child, {})\n";
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    this.addExemplar(
        definitionKeeper, TYPE_CHILD, ExemplarDefinition.Sort.SLOTTED, List.of(TYPE_CHILD));
    final MagikTypedCheck check = new InheritanceCycleTypedCheck();
    assertThat(check).reportsIssueCount(code, definitionKeeper, 1);
  }

  @Test
  void testTwoFileCycleReported() {
    // child extends parent (this file); parent extends child (declared elsewhere, i.e., in the
    // keeper only, representing a second file). The cycle is only visible via the keeper.
    final String code = "def_slotted_exemplar(:child, {}, :parent)\n";
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    this.addExemplar(
        definitionKeeper, TYPE_CHILD, ExemplarDefinition.Sort.SLOTTED, List.of(TYPE_PARENT));
    this.addExemplar(
        definitionKeeper, TYPE_PARENT, ExemplarDefinition.Sort.SLOTTED, List.of(TYPE_CHILD));
    final MagikTypedCheck check = new InheritanceCycleTypedCheck();
    assertThat(check).reportsIssueCount(code, definitionKeeper, 1);
  }

  @Test
  void testAcyclicHierarchyNotReported() {
    final String code = "def_slotted_exemplar(:child, {}, :parent)\n";
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    this.addExemplar(
        definitionKeeper,
        TYPE_GRANDPARENT,
        ExemplarDefinition.Sort.SLOTTED,
        Collections.emptyList());
    this.addExemplar(
        definitionKeeper, TYPE_PARENT, ExemplarDefinition.Sort.SLOTTED, List.of(TYPE_GRANDPARENT));
    this.addExemplar(
        definitionKeeper, TYPE_CHILD, ExemplarDefinition.Sort.SLOTTED, List.of(TYPE_PARENT));
    final MagikTypedCheck check = new InheritanceCycleTypedCheck();
    assertThat(check).reportsNoIssues(code, definitionKeeper);
  }

  @Test
  void testAcyclicDiamondNotReported() {
    // child -> {left, right}, left -> base, right -> base. `base` is legitimately reachable
    // twice via different paths, which must not be mistaken for a cycle.
    final String code = "def_slotted_exemplar(:child, {})\n";
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    this.addExemplar(
        definitionKeeper, TYPE_BASE, ExemplarDefinition.Sort.SLOTTED, Collections.emptyList());
    this.addExemplar(
        definitionKeeper, TYPE_LEFT, ExemplarDefinition.Sort.SLOTTED, List.of(TYPE_BASE));
    this.addExemplar(
        definitionKeeper, TYPE_RIGHT, ExemplarDefinition.Sort.SLOTTED, List.of(TYPE_BASE));
    this.addExemplar(
        definitionKeeper,
        TYPE_CHILD,
        ExemplarDefinition.Sort.SLOTTED,
        List.of(TYPE_LEFT, TYPE_RIGHT));
    final MagikTypedCheck check = new InheritanceCycleTypedCheck();
    assertThat(check).reportsNoIssues(code, definitionKeeper);
  }

  @Test
  void testNoParentsNotReported() {
    final String code = "def_slotted_exemplar(:child, {})\n";
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    this.addExemplar(
        definitionKeeper, TYPE_CHILD, ExemplarDefinition.Sort.SLOTTED, Collections.emptyList());
    final MagikTypedCheck check = new InheritanceCycleTypedCheck();
    assertThat(check).reportsNoIssues(code, definitionKeeper);
  }
}
