package nl.ramsolutions.sw.magik.analysis.typing;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.util.Collection;
import java.util.List;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.Position;
import nl.ramsolutions.sw.magik.Range;
import nl.ramsolutions.sw.magik.analysis.definitions.DefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.ExemplarDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.ITypeStringDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.InheritanceDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.PackageDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.SlotDefinition;
import org.junit.jupiter.api.Test;

/** Tests for {@link TypeStringResolver}. */
class TypeStringResolverTest {

  private static ExemplarDefinition createExemplar(final TypeString typeString) {
    return new ExemplarDefinition(
        null, null, null, null, null, ExemplarDefinition.Sort.SLOTTED, typeString, null);
  }

  @Test
  void testResolveShadowsSameNameInUsedPackage() {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    // Package `rs` uses `sw`.
    definitionKeeper.add(new PackageDefinition(null, null, null, null, null, "rs", List.of("sw")));

    // The same identifier is defined in both `rs` and the used package `sw`.
    final TypeString rsFoo = TypeString.ofIdentifier("foo", "rs");
    final TypeString swFoo = TypeString.ofIdentifier("foo", "sw");
    final ExemplarDefinition exemplarRsFoo = TypeStringResolverTest.createExemplar(rsFoo);
    definitionKeeper.add(exemplarRsFoo);
    final ExemplarDefinition exemplarSwFoo = TypeStringResolverTest.createExemplar(swFoo);
    definitionKeeper.add(exemplarSwFoo);

    // A reference to `rs:foo` must resolve to the nearest package's definition only,
    // i.e. `rs:foo` shadows `sw:foo` -- not a union of both.
    final TypeStringResolver resolver = new TypeStringResolver(definitionKeeper);
    final Collection<ITypeStringDefinition> resolved = resolver.resolve(rsFoo);
    assertThat(resolved).extracting(ITypeStringDefinition::getTypeString).containsExactly(rsFoo);
  }

  @Test
  void testGetSlotDefinitionsFromKeeper() {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final TypeString typeA = TypeString.ofIdentifier("a", "user");
    definitionKeeper.add(
        new ExemplarDefinition(
            null, null, null, null, null, ExemplarDefinition.Sort.SLOTTED, typeA, null));
    definitionKeeper.add(
        new SlotDefinition(null, null, null, null, null, typeA, "slot1", TypeString.SW_INTEGER));

    final TypeStringResolver resolver = new TypeStringResolver(definitionKeeper);
    assertThat(resolver.getSlotDefinitions(typeA))
        .extracting(SlotDefinition::getName)
        .containsExactly("slot1");
  }

  @Test
  void testGetSlotDefinitionsByName() {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final TypeString typeA = TypeString.ofIdentifier("a", "user");
    definitionKeeper.add(
        new ExemplarDefinition(
            null, null, null, null, null, ExemplarDefinition.Sort.SLOTTED, typeA, null));
    definitionKeeper.add(
        new SlotDefinition(null, null, null, null, null, typeA, "slot1", TypeString.SW_INTEGER));
    definitionKeeper.add(
        new SlotDefinition(null, null, null, null, null, typeA, "slot2", TypeString.SW_FLOAT));

    final TypeStringResolver resolver = new TypeStringResolver(definitionKeeper);
    assertThat(resolver.getSlotDefinitions(typeA, "slot2"))
        .extracting(SlotDefinition::getName)
        .containsExactly("slot2");
  }

  @Test
  void testGetParentsAggregatesCrossFileEdges() {
    final IDefinitionKeeper keeper = new DefinitionKeeper();
    final TypeString child = TypeString.ofIdentifier("child", "user");
    final TypeString parentA = TypeString.ofIdentifier("parent_a", "user");
    final TypeString parentB = TypeString.ofIdentifier("parent_b", "user");
    keeper.add(TypeStringResolverTest.createExemplar(child));
    keeper.add(new InheritanceDefinition(null, null, "m1", null, null, child, parentA));
    keeper.add(new InheritanceDefinition(null, null, "m2", null, null, child, parentB));
    final TypeStringResolver resolver = new TypeStringResolver(keeper);
    assertThat(resolver.getParents(child)).contains(parentA, parentB);
  }

  @Test
  void testIsKindOfAcrossCrossFileMixin() {
    final IDefinitionKeeper keeper = new DefinitionKeeper();
    final TypeString child = TypeString.ofIdentifier("child", "user");
    final TypeString parentA = TypeString.ofIdentifier("parent_a", "user");
    final TypeString parentB = TypeString.ofIdentifier("parent_b", "user");
    keeper.add(TypeStringResolverTest.createExemplar(child));
    keeper.add(TypeStringResolverTest.createExemplar(parentA));
    keeper.add(TypeStringResolverTest.createExemplar(parentB));
    // The two edges come from separate modules, aggregated on the same child.
    keeper.add(new InheritanceDefinition(null, null, "m1", null, null, child, parentA));
    keeper.add(new InheritanceDefinition(null, null, "m2", null, null, child, parentB));
    final TypeStringResolver resolver = new TypeStringResolver(keeper);
    assertThat(resolver.isKindOf(child, parentB)).isTrue();
  }

  @Test
  void testImplicitFormatMixinStillApplied() {
    final IDefinitionKeeper keeper = new DefinitionKeeper();
    final TypeString child = TypeString.ofIdentifier("child", "user");
    // A SLOTTED exemplar with no non-mixin parent implicitly inherits the slotted format mixin.
    keeper.add(TypeStringResolverTest.createExemplar(child));
    final TypeStringResolver resolver = new TypeStringResolver(keeper);
    assertThat(resolver.getParents(child)).contains(TypeString.SW_SLOTTED_FORMAT_MIXIN);
  }

  /**
   * Pins a deliberately-preserved quirk: the private {@code getParents(ITypeStringDefinition)} used
   * by {@link TypeStringResolver#isKindOf} reads raw edge parents WITHOUT implicit format-mixin
   * parents. So a SLOTTED exemplar is NOT kind-of {@code sw:slotted_format_mixin} even though its
   * public parents include it implicitly. A follow-up prompt (see docs/superpowers/prompts/)
   * inverts this; until then this test must keep asserting false.
   */
  @Test
  void testIsKindOfDoesNotSeeImplicitParent() {
    final IDefinitionKeeper keeper = new DefinitionKeeper();
    final TypeString child = TypeString.ofIdentifier("child", "user");
    final TypeString parentA = TypeString.ofIdentifier("parent_a", "user");
    keeper.add(TypeStringResolverTest.createExemplar(child));
    keeper.add(TypeStringResolverTest.createExemplar(parentA));
    // A real edge so the isKindOf walk has depth; it still must not reach the implicit mixin.
    keeper.add(new InheritanceDefinition(null, null, "m1", null, null, child, parentA));
    final TypeStringResolver resolver = new TypeStringResolver(keeper);
    assertThat(resolver.isKindOf(child, TypeString.SW_SLOTTED_FORMAT_MIXIN)).isFalse();
  }

  @Test
  void testAncestorWalkTerminatesOnCycle() {
    final IDefinitionKeeper keeper = new DefinitionKeeper();
    final TypeString typeA = TypeString.ofIdentifier("a", "user");
    final TypeString typeB = TypeString.ofIdentifier("b", "user");
    keeper.add(TypeStringResolverTest.createExemplar(typeA));
    keeper.add(TypeStringResolverTest.createExemplar(typeB));
    // A cycle: A -> B and B -> A.
    keeper.add(new InheritanceDefinition(null, null, "m1", null, null, typeA, typeB));
    keeper.add(new InheritanceDefinition(null, null, "m2", null, null, typeB, typeA));
    final TypeStringResolver resolver = new TypeStringResolver(keeper);
    // Must terminate (no StackOverflowError) and still contain the reachable ancestor.
    assertThat(resolver.getAllAncestors(typeA)).contains(typeB);
  }

  @Test
  void testSlotResolutionWalksParents() {
    final IDefinitionKeeper keeper = new DefinitionKeeper();
    final TypeString child = TypeString.ofIdentifier("child", "user");
    final TypeString parent = TypeString.ofIdentifier("parent", "user");
    keeper.add(
        new ExemplarDefinition(
            null, null, null, null, null, ExemplarDefinition.Sort.SLOTTED, child, null));
    keeper.add(
        new ExemplarDefinition(
            null, null, null, null, null, ExemplarDefinition.Sort.SLOTTED, parent, null));
    keeper.add(new InheritanceDefinition(null, null, null, null, null, child, parent));
    keeper.add(
        new SlotDefinition(
            null, null, null, null, null, parent, "inherited", TypeString.SW_INTEGER));
    final TypeStringResolver resolver = new TypeStringResolver(keeper);
    assertThat(resolver.getSlotDefinitions(child))
        .extracting(SlotDefinition::getName)
        .contains("inherited");
  }

  @Test
  void testChildSlotShadowsAncestorSlot() {
    final IDefinitionKeeper keeper = new DefinitionKeeper();
    final TypeString child = TypeString.ofIdentifier("child", "user");
    final TypeString parent = TypeString.ofIdentifier("parent", "user");
    keeper.add(
        new ExemplarDefinition(
            null, null, null, null, null, ExemplarDefinition.Sort.SLOTTED, child, null));
    keeper.add(
        new ExemplarDefinition(
            null, null, null, null, null, ExemplarDefinition.Sort.SLOTTED, parent, null));
    keeper.add(new InheritanceDefinition(null, null, null, null, null, child, parent));
    keeper.add(new SlotDefinition(null, null, null, null, null, child, "s", TypeString.SW_INTEGER));
    keeper.add(new SlotDefinition(null, null, null, null, null, parent, "s", TypeString.SW_FLOAT));
    final TypeStringResolver resolver = new TypeStringResolver(keeper);
    assertThat(resolver.getSlotDefinitions(child, "s"))
        .extracting(SlotDefinition::getTypeName)
        .containsOnly(TypeString.SW_INTEGER); // child wins
  }

  @Test
  void testSameNameSlotsAtDepthZeroBothSurvive() {
    // Two files contribute a same-named slot to the SAME type (both depth 0): both kept, so the
    // cross-file conflict surfaces rather than one silently winning. Pins the Stage 1 decision.
    final IDefinitionKeeper keeper = new DefinitionKeeper();
    final TypeString child = TypeString.ofIdentifier("child", "user");
    keeper.add(
        new ExemplarDefinition(
            null, null, null, null, null, ExemplarDefinition.Sort.SLOTTED, child, null));
    final Location locationA =
        new Location(
            URI.create("memory:///a.magik"), new Range(new Position(1, 1), new Position(1, 2)));
    final Location locationB =
        new Location(
            URI.create("memory:///b.magik"), new Range(new Position(1, 1), new Position(1, 2)));
    keeper.add(
        new SlotDefinition(locationA, null, "m1", null, null, child, "s", TypeString.SW_INTEGER));
    keeper.add(
        new SlotDefinition(locationB, null, "m2", null, null, child, "s", TypeString.SW_FLOAT));
    final TypeStringResolver resolver = new TypeStringResolver(keeper);
    assertThat(resolver.getSlotDefinitions(child, "s")).hasSize(2);
  }

  @Test
  void testSlotWalkTerminatesOnCycle() {
    final IDefinitionKeeper keeper = new DefinitionKeeper();
    final TypeString typeA = TypeString.ofIdentifier("a", "user");
    final TypeString typeB = TypeString.ofIdentifier("b", "user");
    keeper.add(
        new ExemplarDefinition(
            null, null, null, null, null, ExemplarDefinition.Sort.SLOTTED, typeA, null));
    keeper.add(
        new ExemplarDefinition(
            null, null, null, null, null, ExemplarDefinition.Sort.SLOTTED, typeB, null));
    keeper.add(new InheritanceDefinition(null, null, null, null, null, typeA, typeB));
    keeper.add(new InheritanceDefinition(null, null, null, null, null, typeB, typeA));
    keeper.add(
        new SlotDefinition(null, null, null, null, null, typeA, "slot_a", TypeString.SW_INTEGER));
    keeper.add(
        new SlotDefinition(null, null, null, null, null, typeB, "slot_b", TypeString.SW_INTEGER));
    final TypeStringResolver resolver = new TypeStringResolver(keeper);
    // Terminates (no stack overflow / infinite loop) AND resolves both slots across the cycle.
    assertThat(resolver.getSlotDefinitions(typeA))
        .extracting(SlotDefinition::getName)
        .containsExactlyInAnyOrder("slot_a", "slot_b");
  }
}
