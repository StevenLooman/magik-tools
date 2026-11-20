package nl.ramsolutions.sw.magik.analysis.typing;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.Position;
import nl.ramsolutions.sw.magik.Range;
import nl.ramsolutions.sw.magik.analysis.definitions.DefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.ExemplarDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.GlobalDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.ITypeStringDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.InheritanceDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.PackageDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.ProcedureDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.SlotDefinition;
import org.junit.jupiter.api.Test;

/** Tests for {@link TypeStringResolver}. */
class TypeStringResolverTest {

  private static final TypeString SW_A_REF = TypeString.ofIdentifier("a", "sw");
  private static final TypeString USER_A_REF = TypeString.ofIdentifier("a", "user");
  private static final TypeString SW_B_REF = TypeString.ofIdentifier("b", "sw");

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

  private IDefinitionKeeper createGrandParentChainKeeper(
      final TypeString exemplarRef, final TypeString parentRef, final TypeString grandParentRef) {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    this.addExemplarWithParents(definitionKeeper, grandParentRef);
    this.addExemplarWithParents(definitionKeeper, parentRef, grandParentRef);
    this.addExemplarWithParents(definitionKeeper, exemplarRef, parentRef);
    return definitionKeeper;
  }

  @Test
  void testAncestorWalkOfExemplarHoldsItsParentsNearestFirst() {
    final TypeString exemplarRef = TypeString.ofIdentifier("an_exemplar", "user");
    final TypeString parentRef = TypeString.ofIdentifier("a_parent", "user");
    final TypeString grandParentRef = TypeString.ofIdentifier("a_grand_parent", "user");
    final IDefinitionKeeper definitionKeeper =
        this.createGrandParentChainKeeper(exemplarRef, parentRef, grandParentRef);
    final TypeStringResolver resolver = new TypeStringResolver(definitionKeeper);

    final Collection<TypeString> ancestors = resolver.getAllAncestors(exemplarRef);
    final Collection<TypeString> selfAndAncestors = resolver.getSelfAndAncestors(exemplarRef);

    assertThat(ancestors)
        .containsExactly(parentRef, grandParentRef, TypeString.SW_SLOTTED_FORMAT_MIXIN);
    assertThat(selfAndAncestors)
        .containsExactlyInAnyOrder(
            exemplarRef, parentRef, grandParentRef, TypeString.SW_SLOTTED_FORMAT_MIXIN);
  }

  @Test
  void testAncestorWalkDoesNotFollowImplicitParent() {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final TypeString exemplarRef = TypeString.ofIdentifier("an_exemplar", "user");
    this.addExemplarWithParents(definitionKeeper, exemplarRef);
    definitionKeeper.add(
        new InheritanceDefinition(
            null,
            null,
            null,
            null,
            null,
            TypeString.SW_SLOTTED_FORMAT_MIXIN,
            TypeString.SW_OBJECT));
    final TypeStringResolver resolver = new TypeStringResolver(definitionKeeper);

    final Collection<TypeString> ancestors = resolver.getAllAncestors(exemplarRef);

    assertThat(ancestors).containsExactly(TypeString.SW_SLOTTED_FORMAT_MIXIN);
  }

  @Test
  void testAncestorWalkKeepsParentAsWrittenWhenDefinedInUsedPackage() {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final TypeString exemplarRef = TypeString.ofIdentifier("an_exemplar", "user");
    final TypeString writtenParentRef = TypeString.ofIdentifier("a_parent", "user");
    final TypeString parentRef = TypeString.ofIdentifier("a_parent", "sw");
    final TypeString grandParentRef = TypeString.ofIdentifier("a_grand_parent", "sw");
    this.addExemplarWithParents(definitionKeeper, grandParentRef);
    this.addExemplarWithParents(definitionKeeper, parentRef, grandParentRef);
    this.addExemplarWithParents(definitionKeeper, exemplarRef, writtenParentRef);
    final TypeStringResolver resolver = new TypeStringResolver(definitionKeeper);

    final Collection<TypeString> ancestors = resolver.getAllAncestors(exemplarRef);

    assertThat(ancestors)
        .containsExactly(writtenParentRef, grandParentRef, TypeString.SW_SLOTTED_FORMAT_MIXIN);
  }

  @Test
  void testAncestorWalkOfGlobalAliasHoldsItsExemplarAndItsAncestors() {
    final TypeString globalRef = TypeString.ofIdentifier("a_global", "user");
    final TypeString exemplarRef = TypeString.ofIdentifier("an_exemplar", "user");
    final TypeString parentRef = TypeString.ofIdentifier("a_parent", "user");
    final TypeString grandParentRef = TypeString.ofIdentifier("a_grand_parent", "user");
    final IDefinitionKeeper definitionKeeper =
        this.createGrandParentChainKeeper(exemplarRef, parentRef, grandParentRef);
    definitionKeeper.add(this.createGlobalDefinition(globalRef, exemplarRef));
    final TypeStringResolver resolver = new TypeStringResolver(definitionKeeper);

    final Collection<TypeString> ancestors = resolver.getAllAncestors(globalRef);
    final Collection<TypeString> selfAndAncestors = resolver.getSelfAndAncestors(globalRef);

    assertThat(ancestors)
        .containsExactly(
            exemplarRef, parentRef, grandParentRef, TypeString.SW_SLOTTED_FORMAT_MIXIN);
    assertThat(selfAndAncestors)
        .containsExactlyInAnyOrder(
            globalRef, exemplarRef, parentRef, grandParentRef, TypeString.SW_SLOTTED_FORMAT_MIXIN);
  }

  @Test
  void testAncestorWalkFollowsGlobalAliasChain() {
    final TypeString globalRef = TypeString.ofIdentifier("a_global", "user");
    final TypeString otherGlobalRef = TypeString.ofIdentifier("another_global", "user");
    final TypeString exemplarRef = TypeString.ofIdentifier("an_exemplar", "user");
    final TypeString parentRef = TypeString.ofIdentifier("a_parent", "user");
    final TypeString grandParentRef = TypeString.ofIdentifier("a_grand_parent", "user");
    final IDefinitionKeeper definitionKeeper =
        this.createGrandParentChainKeeper(exemplarRef, parentRef, grandParentRef);
    definitionKeeper.add(this.createGlobalDefinition(globalRef, otherGlobalRef));
    definitionKeeper.add(this.createGlobalDefinition(otherGlobalRef, exemplarRef));
    final TypeStringResolver resolver = new TypeStringResolver(definitionKeeper);

    final Collection<TypeString> ancestors = resolver.getAllAncestors(globalRef);

    assertThat(ancestors)
        .containsExactly(
            exemplarRef, parentRef, grandParentRef, TypeString.SW_SLOTTED_FORMAT_MIXIN);
  }

  @Test
  void testAncestorWalkOfParentNamedByGlobalAliasReachesGrandParents() {
    final TypeString childRef = TypeString.ofIdentifier("a_child", "user");
    final TypeString globalRef = TypeString.ofIdentifier("a_global", "user");
    final TypeString exemplarRef = TypeString.ofIdentifier("an_exemplar", "user");
    final TypeString parentRef = TypeString.ofIdentifier("a_parent", "user");
    final TypeString grandParentRef = TypeString.ofIdentifier("a_grand_parent", "user");
    final IDefinitionKeeper definitionKeeper =
        this.createGrandParentChainKeeper(exemplarRef, parentRef, grandParentRef);
    definitionKeeper.add(this.createGlobalDefinition(globalRef, exemplarRef));
    this.addExemplarWithParents(definitionKeeper, childRef, globalRef);
    final TypeStringResolver resolver = new TypeStringResolver(definitionKeeper);

    final Collection<TypeString> ancestors = resolver.getAllAncestors(childRef);

    assertThat(ancestors)
        .containsExactly(
            globalRef, exemplarRef, parentRef, grandParentRef, TypeString.SW_SLOTTED_FORMAT_MIXIN);
  }

  @Test
  void testAncestorWalkOfAliasCycleIsEmpty() {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final TypeString selfAliasRef = TypeString.ofIdentifier("a_self_alias", "user");
    final TypeString globalRef = TypeString.ofIdentifier("a_global", "user");
    final TypeString otherGlobalRef = TypeString.ofIdentifier("another_global", "user");
    definitionKeeper.add(this.createGlobalDefinition(selfAliasRef, selfAliasRef));
    definitionKeeper.add(this.createGlobalDefinition(globalRef, otherGlobalRef));
    definitionKeeper.add(this.createGlobalDefinition(otherGlobalRef, globalRef));
    final TypeStringResolver resolver = new TypeStringResolver(definitionKeeper);

    final Collection<TypeString> selfAliasAncestors = resolver.getAllAncestors(selfAliasRef);
    final Collection<TypeString> cycleAncestors = resolver.getAllAncestors(globalRef);

    assertThat(selfAliasAncestors).isEmpty();
    assertThat(cycleAncestors).isEmpty();
  }

  @Test
  void testAncestorWalkOfExemplarWithSameNamedSelfAliasIsUnchanged() {
    final TypeString exemplarRef = TypeString.ofIdentifier("an_exemplar", "user");
    final TypeString parentRef = TypeString.ofIdentifier("a_parent", "user");
    final TypeString grandParentRef = TypeString.ofIdentifier("a_grand_parent", "user");
    final IDefinitionKeeper definitionKeeper =
        this.createGrandParentChainKeeper(exemplarRef, parentRef, grandParentRef);
    definitionKeeper.add(this.createGlobalDefinition(exemplarRef, exemplarRef));
    final TypeStringResolver resolver = new TypeStringResolver(definitionKeeper);

    final Collection<TypeString> ancestors = resolver.getAllAncestors(exemplarRef);

    assertThat(ancestors)
        .containsExactly(parentRef, grandParentRef, TypeString.SW_SLOTTED_FORMAT_MIXIN);
  }

  @Test
  void testAncestorWalkOfProcedureHoldsProcedureExemplarAndItsAncestors() {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final TypeString procedureRef = TypeString.ofIdentifier("a_procedure", "user");
    final TypeString parentRef = TypeString.ofIdentifier("a_parent", "user");
    final TypeString grandParentRef = TypeString.ofIdentifier("a_grand_parent", "user");
    this.addExemplarWithParents(definitionKeeper, grandParentRef);
    this.addExemplarWithParents(definitionKeeper, parentRef, grandParentRef);
    definitionKeeper.add(
        new InheritanceDefinition(
            null, null, null, null, null, TypeString.SW_PROCEDURE, parentRef));
    definitionKeeper.add(this.createProcedureDefinition(procedureRef));
    final TypeStringResolver resolver = new TypeStringResolver(definitionKeeper);

    final Collection<TypeString> ancestors = resolver.getAllAncestors(procedureRef);

    assertThat(ancestors)
        .containsExactly(
            TypeString.SW_PROCEDURE, parentRef, grandParentRef, TypeString.SW_SLOTTED_FORMAT_MIXIN);
  }

  @Test
  void testAncestorWalkOfExemplarWithSameNamedProcedureHoldsProcedureAncestors() {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final TypeString sharedRef = TypeString.ofIdentifier("a_shared_name", "user");
    final TypeString parentRef = TypeString.ofIdentifier("a_parent", "user");
    final TypeString grandParentRef = TypeString.ofIdentifier("a_grand_parent", "user");
    this.addExemplarWithParents(definitionKeeper, grandParentRef);
    this.addExemplarWithParents(definitionKeeper, parentRef, grandParentRef);
    definitionKeeper.add(
        new InheritanceDefinition(
            null, null, null, null, null, TypeString.SW_PROCEDURE, parentRef));
    this.addExemplarWithParents(definitionKeeper, sharedRef);
    definitionKeeper.add(this.createProcedureDefinition(sharedRef));
    final TypeStringResolver resolver = new TypeStringResolver(definitionKeeper);

    final Collection<TypeString> ancestors = resolver.getAllAncestors(sharedRef);

    assertThat(ancestors)
        .contains(
            TypeString.SW_PROCEDURE, parentRef, grandParentRef, TypeString.SW_SLOTTED_FORMAT_MIXIN);
  }

  private ProcedureDefinition createProcedureDefinition(final TypeString typeString) {
    return new ProcedureDefinition(
        null,
        null,
        null,
        null,
        null,
        EnumSet.noneOf(ProcedureDefinition.Modifier.class),
        typeString,
        typeString.getIdentifier(),
        Collections.emptyList(),
        null,
        ExpressionResultString.UNDEFINED,
        ExpressionResultString.UNDEFINED);
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

  private static void addExemplarWithParents(
      final IDefinitionKeeper definitionKeeper,
      final TypeString typeString,
      final TypeString... parents) {
    definitionKeeper.add(TypeStringResolverTest.createExemplar(typeString));
    for (final TypeString parent : parents) {
      definitionKeeper.add(
          new InheritanceDefinition(null, null, null, null, null, typeString, parent));
    }
  }

  private static MethodDefinition createMethodDefinition(
      final TypeString typeName,
      final String methodName,
      final TypeString returnType,
      final MethodDefinition.Modifier... modifiers) {
    return new MethodDefinition(
        null,
        null,
        null,
        null,
        null,
        typeName,
        methodName,
        Set.of(modifiers),
        Collections.emptyList(),
        null,
        null,
        new ExpressionResultString(returnType),
        ExpressionResultString.EMPTY);
  }

  @Test
  void testRespondingMethodFromTwoConcreteParentsReturnsBoth() {
    final IDefinitionKeeper keeper = new DefinitionKeeper();
    final TypeString base1 = TypeString.ofIdentifier("base_1", "sw");
    final TypeString base2 = TypeString.ofIdentifier("base_2", "sw");
    final TypeString concrete = TypeString.ofIdentifier("concrete", "sw");
    TypeStringResolverTest.addExemplarWithParents(keeper, base1);
    TypeStringResolverTest.addExemplarWithParents(keeper, base2);
    TypeStringResolverTest.addExemplarWithParents(keeper, concrete, base1, base2);
    final MethodDefinition method1 =
        TypeStringResolverTest.createMethodDefinition(base1, "nnn()", TypeString.SW_INTEGER);
    keeper.add(method1);
    final MethodDefinition method2 =
        TypeStringResolverTest.createMethodDefinition(base2, "nnn()", TypeString.SW_SYMBOL);
    keeper.add(method2);

    final TypeStringResolver resolver = new TypeStringResolver(keeper);
    final Collection<MethodDefinition> definitions =
        resolver.getRespondingMethodDefinitions(concrete, "nnn()");
    assertThat(definitions)
        .extracting(MethodDefinition::getTypeName)
        .containsExactlyInAnyOrder(base1, base2);
  }

  @Test
  void testRespondingMethodPrefersConcreteOverAbstractParent() {
    final IDefinitionKeeper keeper = new DefinitionKeeper();
    final TypeString base1 = TypeString.ofIdentifier("base_1", "sw");
    final TypeString base2 = TypeString.ofIdentifier("base_2", "sw");
    final TypeString concrete = TypeString.ofIdentifier("concrete", "sw");
    TypeStringResolverTest.addExemplarWithParents(keeper, base1);
    TypeStringResolverTest.addExemplarWithParents(keeper, base2);
    TypeStringResolverTest.addExemplarWithParents(keeper, concrete, base1, base2);
    final MethodDefinition abstractMethod =
        TypeStringResolverTest.createMethodDefinition(
            base1, "mmm()", TypeString.SW_INTEGER, MethodDefinition.Modifier.ABSTRACT);
    keeper.add(abstractMethod);
    final MethodDefinition concreteMethod =
        TypeStringResolverTest.createMethodDefinition(base2, "mmm()", TypeString.SW_SYMBOL);
    keeper.add(concreteMethod);

    final TypeStringResolver resolver = new TypeStringResolver(keeper);
    final Collection<MethodDefinition> definitions =
        resolver.getRespondingMethodDefinitions(concrete, "mmm()");
    assertThat(definitions).extracting(MethodDefinition::getTypeName).containsExactly(base2);
  }

  @Test
  void testRespondingMethodKeepsAbstractWhenNoConcreteResponder() {
    final IDefinitionKeeper keeper = new DefinitionKeeper();
    final TypeString base1 = TypeString.ofIdentifier("base_1", "sw");
    final TypeString concrete = TypeString.ofIdentifier("concrete", "sw");
    TypeStringResolverTest.addExemplarWithParents(keeper, base1);
    TypeStringResolverTest.addExemplarWithParents(keeper, concrete, base1);
    final MethodDefinition abstractMethod =
        TypeStringResolverTest.createMethodDefinition(
            base1, "mmm()", TypeString.SW_INTEGER, MethodDefinition.Modifier.ABSTRACT);
    keeper.add(abstractMethod);

    final TypeStringResolver resolver = new TypeStringResolver(keeper);
    final Collection<MethodDefinition> definitions =
        resolver.getRespondingMethodDefinitions(concrete, "mmm()");
    assertThat(definitions).extracting(MethodDefinition::getTypeName).containsExactly(base1);
  }

  @Test
  void testRespondingMethodOwnConcreteShadowsParents() {
    final IDefinitionKeeper keeper = new DefinitionKeeper();
    final TypeString base1 = TypeString.ofIdentifier("base_1", "sw");
    final TypeString base2 = TypeString.ofIdentifier("base_2", "sw");
    final TypeString concrete = TypeString.ofIdentifier("concrete", "sw");
    TypeStringResolverTest.addExemplarWithParents(keeper, base1);
    TypeStringResolverTest.addExemplarWithParents(keeper, base2);
    TypeStringResolverTest.addExemplarWithParents(keeper, concrete, base1, base2);
    final MethodDefinition method1 =
        TypeStringResolverTest.createMethodDefinition(base1, "nnn()", TypeString.SW_INTEGER);
    keeper.add(method1);
    final MethodDefinition method2 =
        TypeStringResolverTest.createMethodDefinition(base2, "nnn()", TypeString.SW_SYMBOL);
    keeper.add(method2);
    final MethodDefinition ownMethod =
        TypeStringResolverTest.createMethodDefinition(
            concrete, "nnn()", TypeString.SW_CHAR16_VECTOR);
    keeper.add(ownMethod);

    final TypeStringResolver resolver = new TypeStringResolver(keeper);
    final Collection<MethodDefinition> definitions =
        resolver.getRespondingMethodDefinitions(concrete, "nnn()");
    assertThat(definitions).extracting(MethodDefinition::getTypeName).containsExactly(concrete);
  }

  private static GlobalDefinition createGlobalDefinition(
      final TypeString typeString, final TypeString aliasedTypeString) {
    return new GlobalDefinition(null, null, null, null, null, typeString, aliasedTypeString);
  }

  @Test
  void testGetExemplarDefinitionsFollowsGlobalAliasChain() {
    final IDefinitionKeeper keeper = new DefinitionKeeper();
    final TypeString typeA = TypeString.ofIdentifier("a", "user");
    final TypeString typeB = TypeString.ofIdentifier("b", "user");
    final TypeString typeC = TypeString.ofIdentifier("c", "user");
    final GlobalDefinition globalA = TypeStringResolverTest.createGlobalDefinition(typeA, typeB);
    keeper.add(globalA);
    final GlobalDefinition globalB = TypeStringResolverTest.createGlobalDefinition(typeB, typeC);
    keeper.add(globalB);
    final ExemplarDefinition exemplarC = TypeStringResolverTest.createExemplar(typeC);
    keeper.add(exemplarC);

    final TypeStringResolver resolver = new TypeStringResolver(keeper);
    final Collection<ExemplarDefinition> definitions = resolver.getExemplarDefinitions(typeA);
    assertThat(definitions).containsExactly(exemplarC);
  }

  @Test
  void testGetExemplarDefinitionsSelfAliasedGlobalIsUnresolvable() {
    final IDefinitionKeeper keeper = new DefinitionKeeper();
    final TypeString typeFoo = TypeString.ofIdentifier("foo", "user");
    final GlobalDefinition globalFoo =
        TypeStringResolverTest.createGlobalDefinition(typeFoo, typeFoo);
    keeper.add(globalFoo);

    final TypeStringResolver resolver = new TypeStringResolver(keeper);
    final Collection<ExemplarDefinition> definitions = resolver.getExemplarDefinitions(typeFoo);
    assertThat(definitions).isEmpty();
  }

  @Test
  void testGetExemplarDefinitionsGlobalAliasCycleIsUnresolvable() {
    final IDefinitionKeeper keeper = new DefinitionKeeper();
    final TypeString typeA = TypeString.ofIdentifier("a", "user");
    final TypeString typeB = TypeString.ofIdentifier("b", "user");
    final GlobalDefinition globalA = TypeStringResolverTest.createGlobalDefinition(typeA, typeB);
    keeper.add(globalA);
    final GlobalDefinition globalB = TypeStringResolverTest.createGlobalDefinition(typeB, typeA);
    keeper.add(globalB);

    final TypeStringResolver resolver = new TypeStringResolver(keeper);
    final Collection<ExemplarDefinition> definitions = resolver.getExemplarDefinitions(typeA);
    assertThat(definitions).isEmpty();
  }

  @Test
  void testIsKindOfSelfAliasedGlobalIsNotKindOfObject() {
    final IDefinitionKeeper keeper = new DefinitionKeeper();
    final TypeString typeFoo = TypeString.ofIdentifier("foo", "user");
    final GlobalDefinition globalFoo =
        TypeStringResolverTest.createGlobalDefinition(typeFoo, typeFoo);
    keeper.add(globalFoo);

    final TypeStringResolver resolver = new TypeStringResolver(keeper);
    final boolean isKindOf = resolver.isKindOf(typeFoo, TypeString.SW_OBJECT);
    assertThat(isKindOf).isFalse();
  }

  @Test
  void testIsKindOfGlobalAliasCycleIsNotKindOfObject() {
    final IDefinitionKeeper keeper = new DefinitionKeeper();
    final TypeString typeA = TypeString.ofIdentifier("a", "user");
    final TypeString typeB = TypeString.ofIdentifier("b", "user");
    final GlobalDefinition globalA = TypeStringResolverTest.createGlobalDefinition(typeA, typeB);
    keeper.add(globalA);
    final GlobalDefinition globalB = TypeStringResolverTest.createGlobalDefinition(typeB, typeA);
    keeper.add(globalB);

    final TypeStringResolver resolver = new TypeStringResolver(keeper);
    final boolean isKindOf = resolver.isKindOf(typeA, TypeString.SW_OBJECT);
    assertThat(isKindOf).isFalse();
  }

  private static GlobalDefinition createUntypedGlobalDefinition(
      final TypeString typeString, final String checkout, final int line) {
    return TypeStringResolverTest.createGlobalDefinition(
        typeString, TypeString.UNDEFINED, checkout, line);
  }

  private static GlobalDefinition createGlobalDefinition(
      final TypeString typeString,
      final TypeString aliasedTypeString,
      final String checkout,
      final int line) {
    final URI uri = URI.create("file:///" + checkout + "/test_runner_model.magik");
    final Position position = new Position(line, 0);
    final Range range = new Range(position, position);
    final Location location = new Location(uri, range);
    return new GlobalDefinition(location, null, null, null, null, typeString, aliasedTypeString);
  }

  private static ProcedureDefinition createProcedureDefinition(
      final TypeString typeString, final String moduleName) {
    return new ProcedureDefinition(
        null,
        null,
        moduleName,
        null,
        null,
        EnumSet.noneOf(ProcedureDefinition.Modifier.class),
        typeString,
        typeString.getIdentifier(),
        Collections.emptyList(),
        null,
        ExpressionResultString.UNDEFINED,
        ExpressionResultString.UNDEFINED);
  }

  @Test
  void testRespondingMethodOfAProcedureDoesNotDependOnWhereItsUntypedGlobalsSit() {
    final TypeString loadFileRef = TypeString.ofIdentifier("load_file", "sw");
    final MethodDefinition copyDefinition =
        TypeStringResolverTest.createMethodDefinition(
            TypeString.SW_OBJECT, "copy()", TypeString.SELF);
    final List<String> unresolvedCheckouts = new ArrayList<>();
    for (int checkout = 0; checkout < 32; checkout++) {
      final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
      definitionKeeper.add(copyDefinition);
      definitionKeeper.add(
          new InheritanceDefinition(
              null, null, null, null, null, TypeString.SW_PROCEDURE, TypeString.SW_OBJECT));
      definitionKeeper.add(TypeStringResolverTest.createProcedureDefinition(loadFileRef, null));
      final String checkoutPath = "checkout" + checkout;
      for (final int line : List.of(149, 166, 178)) {
        definitionKeeper.add(
            TypeStringResolverTest.createUntypedGlobalDefinition(loadFileRef, checkoutPath, line));
      }

      final TypeStringResolver resolver = new TypeStringResolver(definitionKeeper);
      final Collection<MethodDefinition> methodDefinitions =
          resolver.getRespondingMethodDefinitions(loadFileRef, "copy()");
      if (!methodDefinitions.equals(Set.of(copyDefinition))) {
        unresolvedCheckouts.add(checkoutPath);
      }
    }
    assertThat(unresolvedCheckouts).isEmpty();
  }

  @Test
  void testExemplarOfAGlobalIsNotHiddenByAnUntypedGlobalOfTheSameName() {
    final TypeString globalRef = TypeString.ofIdentifier("a_global", "sw");
    final TypeString exemplarRef = TypeString.ofIdentifier("an_exemplar", "sw");
    final ExemplarDefinition exemplarDefinition =
        TypeStringResolverTest.createExemplar(exemplarRef);
    final List<String> unresolvedCheckouts = new ArrayList<>();
    for (int checkout = 0; checkout < 32; checkout++) {
      final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
      definitionKeeper.add(exemplarDefinition);
      final String checkoutPath = "checkout" + checkout;
      definitionKeeper.add(
          TypeStringResolverTest.createUntypedGlobalDefinition(globalRef, checkoutPath, 1));
      definitionKeeper.add(
          TypeStringResolverTest.createGlobalDefinition(globalRef, exemplarRef, checkoutPath, 2));

      final TypeStringResolver resolver = new TypeStringResolver(definitionKeeper);
      final Collection<ExemplarDefinition> resolved = resolver.getExemplarDefinitions(globalRef);
      if (!resolved.equals(Set.of(exemplarDefinition))) {
        unresolvedCheckouts.add(checkoutPath);
      }
    }
    assertThat(unresolvedCheckouts).isEmpty();
  }

  /** A keeper where {@code sw:a_global} is assigned twice, once to each of two exemplars. */
  private static IDefinitionKeeper createCollidingGlobalKeeper(final String checkoutPath) {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final TypeString globalRef = TypeString.ofIdentifier("a_global", "sw");
    final TypeString firstRef = TypeString.ofIdentifier("a_first", "sw");
    final TypeString secondRef = TypeString.ofIdentifier("a_second", "sw");
    final TypeString firstParentRef = TypeString.ofIdentifier("a_first_parent", "sw");
    final TypeString secondParentRef = TypeString.ofIdentifier("a_second_parent", "sw");
    TypeStringResolverTest.addExemplarWithParents(definitionKeeper, firstParentRef);
    TypeStringResolverTest.addExemplarWithParents(definitionKeeper, secondParentRef);
    TypeStringResolverTest.addExemplarWithParents(definitionKeeper, firstRef, firstParentRef);
    TypeStringResolverTest.addExemplarWithParents(definitionKeeper, secondRef, secondParentRef);
    definitionKeeper.add(
        TypeStringResolverTest.createMethodDefinition(
            firstParentRef, "size", TypeString.SW_INTEGER));
    definitionKeeper.add(
        TypeStringResolverTest.createMethodDefinition(
            secondParentRef, "size", TypeString.SW_SYMBOL));
    definitionKeeper.add(
        TypeStringResolverTest.createGlobalDefinition(globalRef, secondRef, checkoutPath, 1));
    definitionKeeper.add(
        TypeStringResolverTest.createGlobalDefinition(globalRef, firstRef, checkoutPath, 2));
    return definitionKeeper;
  }

  @Test
  void testExemplarsOfACollidingGlobalAreAllItsAliases() {
    final TypeString globalRef = TypeString.ofIdentifier("a_global", "sw");
    final TypeString firstRef = TypeString.ofIdentifier("a_first", "sw");
    final TypeString secondRef = TypeString.ofIdentifier("a_second", "sw");
    final List<String> otherCheckouts = new ArrayList<>();
    for (int checkout = 0; checkout < 32; checkout++) {
      final String checkoutPath = "checkout" + checkout;
      final IDefinitionKeeper definitionKeeper =
          TypeStringResolverTest.createCollidingGlobalKeeper(checkoutPath);

      final TypeStringResolver resolver = new TypeStringResolver(definitionKeeper);
      final Collection<ExemplarDefinition> exemplarDefinitions =
          resolver.getExemplarDefinitions(globalRef);
      final Set<TypeString> exemplarTypeStrings =
          exemplarDefinitions.stream()
              .map(ExemplarDefinition::getTypeString)
              .collect(Collectors.toSet());
      if (!exemplarTypeStrings.equals(Set.of(firstRef, secondRef))) {
        otherCheckouts.add(checkoutPath);
      }
    }
    assertThat(otherCheckouts).isEmpty();
  }

  @Test
  void testParentsOfACollidingGlobalAreThoseOfAllItsAliases() {
    final TypeString globalRef = TypeString.ofIdentifier("a_global", "sw");
    final TypeString firstParentRef = TypeString.ofIdentifier("a_first_parent", "sw");
    final TypeString secondParentRef = TypeString.ofIdentifier("a_second_parent", "sw");
    final IDefinitionKeeper definitionKeeper =
        TypeStringResolverTest.createCollidingGlobalKeeper("checkout");

    final TypeStringResolver resolver = new TypeStringResolver(definitionKeeper);
    final Collection<TypeString> parents = resolver.getParents(globalRef);
    assertThat(parents).contains(firstParentRef, secondParentRef);
  }

  @Test
  void testCollidingGlobalIsKindOfWhatAnyOfItsAliasesIs() {
    final TypeString globalRef = TypeString.ofIdentifier("a_global", "sw");
    final TypeString firstParentRef = TypeString.ofIdentifier("a_first_parent", "sw");
    final TypeString secondParentRef = TypeString.ofIdentifier("a_second_parent", "sw");
    final IDefinitionKeeper definitionKeeper =
        TypeStringResolverTest.createCollidingGlobalKeeper("checkout");

    final TypeStringResolver resolver = new TypeStringResolver(definitionKeeper);
    final boolean kindOfFirst = resolver.isKindOf(globalRef, firstParentRef);
    final boolean kindOfSecond = resolver.isKindOf(globalRef, secondParentRef);
    assertThat(kindOfFirst).isTrue();
    assertThat(kindOfSecond).isTrue();
  }

  @Test
  void testCollidingGlobalRespondsWithTheMethodsOfAllItsAliases() {
    final TypeString globalRef = TypeString.ofIdentifier("a_global", "sw");
    final IDefinitionKeeper definitionKeeper =
        TypeStringResolverTest.createCollidingGlobalKeeper("checkout");

    final TypeStringResolver resolver = new TypeStringResolver(definitionKeeper);
    final Collection<MethodDefinition> methodDefinitions =
        resolver.getRespondingMethodDefinitions(globalRef, "size");
    final Set<ExpressionResultString> returnTypes =
        methodDefinitions.stream()
            .map(MethodDefinition::getReturnTypes)
            .collect(Collectors.toSet());
    final ExpressionResultString integerResult = new ExpressionResultString(TypeString.SW_INTEGER);
    final ExpressionResultString symbolResult = new ExpressionResultString(TypeString.SW_SYMBOL);
    assertThat(returnTypes).containsExactlyInAnyOrder(integerResult, symbolResult);
  }

  private static MethodDefinition createModuleMethodDefinition(
      final TypeString typeString,
      final String methodName,
      final String moduleName,
      final TypeString returnType) {
    return new MethodDefinition(
        null,
        null,
        moduleName,
        null,
        null,
        typeString,
        methodName,
        EnumSet.noneOf(MethodDefinition.Modifier.class),
        Collections.emptyList(),
        null,
        null,
        new ExpressionResultString(returnType),
        ExpressionResultString.EMPTY);
  }

  @Test
  void testAllRespondingMethodsIncludeEveryCollidingDefinition() {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final TypeString thingRef = TypeString.ofIdentifier("thing", "sw");
    TypeStringResolverTest.addExemplarWithParents(definitionKeeper, thingRef);
    final MethodDefinition firstDefinition =
        TypeStringResolverTest.createModuleMethodDefinition(
            thingRef, "size", "module_a", TypeString.SW_INTEGER);
    final MethodDefinition secondDefinition =
        TypeStringResolverTest.createModuleMethodDefinition(
            thingRef, "size", "module_b", TypeString.SW_SYMBOL);
    definitionKeeper.add(firstDefinition);
    definitionKeeper.add(secondDefinition);

    final TypeStringResolver resolver = new TypeStringResolver(definitionKeeper);
    final Collection<MethodDefinition> methodDefinitions =
        resolver.getRespondingMethodDefinitions(thingRef);
    assertThat(methodDefinitions).contains(firstDefinition, secondDefinition);
  }

  @Test
  void testAllRespondingMethodsLetAnOwnMethodShadowItsParents() {
    final List<String> shadowedTypes = new ArrayList<>();
    for (int index = 0; index < 32; index++) {
      // Vary the names, so the ancestor set iterates in a different order each time.
      final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
      final TypeString parentRef = TypeString.ofIdentifier("parent" + index, "sw");
      final TypeString childRef = TypeString.ofIdentifier("child" + index, "sw");
      TypeStringResolverTest.addExemplarWithParents(definitionKeeper, parentRef);
      TypeStringResolverTest.addExemplarWithParents(definitionKeeper, childRef, parentRef);
      final MethodDefinition parentDefinition =
          TypeStringResolverTest.createModuleMethodDefinition(
              parentRef, "size", "module", TypeString.SW_SYMBOL);
      final MethodDefinition childDefinition =
          TypeStringResolverTest.createModuleMethodDefinition(
              childRef, "size", "module", TypeString.SW_INTEGER);
      definitionKeeper.add(parentDefinition);
      definitionKeeper.add(childDefinition);

      final TypeStringResolver resolver = new TypeStringResolver(definitionKeeper);
      final Collection<MethodDefinition> respondingDefinitions =
          resolver.getRespondingMethodDefinitions(childRef);
      final List<MethodDefinition> sizeDefinitions =
          respondingDefinitions.stream()
              .filter(definition -> definition.getMethodName().equals("size"))
              .toList();
      if (!sizeDefinitions.equals(List.of(childDefinition))) {
        shadowedTypes.add(childRef.getFullString());
      }
    }
    assertThat(shadowedTypes).isEmpty();
  }

  @Test
  void testRespondingProceduresIncludeEveryCollidingDefinition() {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final TypeString procedureRef = TypeString.ofIdentifier("a_procedure", "sw");
    final List<ProcedureDefinition> procedureDefinitions = new ArrayList<>();
    for (final String moduleName : List.of("module_a", "module_b")) {
      final ProcedureDefinition procedureDefinition =
          TypeStringResolverTest.createProcedureDefinition(procedureRef, moduleName);
      definitionKeeper.add(procedureDefinition);
      procedureDefinitions.add(procedureDefinition);
    }

    final TypeStringResolver resolver = new TypeStringResolver(definitionKeeper);
    final Collection<ProcedureDefinition> respondingDefinitions =
        resolver.getRespondingProcedureDefinitions(procedureRef);
    assertThat(respondingDefinitions).containsExactlyInAnyOrderElementsOf(procedureDefinitions);
  }

  @Test
  void testResolvedTypeStringOfDefinitionsDifferingInGenericsIsTheBareName() {
    final TypeString elementRef = TypeString.ofGenericReference("E");
    final List<String> otherNames = new ArrayList<>();
    for (int index = 0; index < 16; index++) {
      // Vary the name, so the resolved set iterates in a different order each time.
      final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
      final String identifier = "box" + index;
      final TypeString boxRef = TypeString.ofIdentifier(identifier, "sw");
      final TypeString genericBoxRef = TypeString.ofIdentifier(identifier, "sw", elementRef);
      definitionKeeper.add(TypeStringResolverTest.createModuleExemplar(genericBoxRef, "module_a"));
      definitionKeeper.add(
          new GlobalDefinition(null, null, "module_b", null, null, boxRef, boxRef));

      final TypeStringResolver resolver = new TypeStringResolver(definitionKeeper);
      final TypeString resolvedTypeString = resolver.getResolvedTypeString(boxRef);
      if (!resolvedTypeString.equals(boxRef)) {
        otherNames.add(identifier);
      }
    }
    assertThat(otherNames).isEmpty();
  }

  private static ExemplarDefinition createModuleExemplar(
      final TypeString typeString, final String moduleName) {
    return new ExemplarDefinition(
        null, null, moduleName, null, null, ExemplarDefinition.Sort.SLOTTED, typeString, null);
  }

  @Test
  void testExemplarTypeStringOfAnAliasToATypeDefinedTwiceIsThatType() {
    final IDefinitionKeeper keeper = new DefinitionKeeper();
    final TypeString aliasRef = TypeString.ofIdentifier("an_alias", "user");
    final TypeString thingRef = TypeString.ofIdentifier("thing", "user");
    keeper.add(TypeStringResolverTest.createGlobalDefinition(aliasRef, thingRef));
    keeper.add(TypeStringResolverTest.createModuleExemplar(thingRef, "module_a"));
    keeper.add(TypeStringResolverTest.createModuleExemplar(thingRef, "module_b"));

    final TypeStringResolver resolver = new TypeStringResolver(keeper);
    final TypeString exemplarTypeString = resolver.getExemplarTypeString(aliasRef);
    assertThat(exemplarTypeString).isEqualTo(thingRef);
  }

  @Test
  void testExemplarTypeStringOfATypeDefinedTwiceWithOtherGenericsIsTheBareName() {
    final IDefinitionKeeper keeper = new DefinitionKeeper();
    final TypeString thingRef = TypeString.ofIdentifier("thing", "user");
    final TypeString elementRef = TypeString.ofGenericReference("E");
    final TypeString genericThingRef = TypeString.ofIdentifier("thing", "user", elementRef);
    keeper.add(TypeStringResolverTest.createModuleExemplar(thingRef, "module_a"));
    keeper.add(TypeStringResolverTest.createModuleExemplar(genericThingRef, "module_b"));

    final TypeStringResolver resolver = new TypeStringResolver(keeper);
    final TypeString exemplarTypeString = resolver.getExemplarTypeString(thingRef);
    assertThat(exemplarTypeString).isEqualTo(thingRef);
  }

  @Test
  void testExemplarTypeStringOfAGlobalAliasingTwoExemplarsIsTheReference() {
    final IDefinitionKeeper keeper = TypeStringResolverTest.createCollidingGlobalKeeper("checkout");
    final TypeString globalRef = TypeString.ofIdentifier("a_global", "sw");

    final TypeStringResolver resolver = new TypeStringResolver(keeper);
    final TypeString exemplarTypeString = resolver.getExemplarTypeString(globalRef);
    assertThat(exemplarTypeString).isEqualTo(globalRef);
  }

  @Test
  void testExemplarTypeStringOfAnUnknownTypeIsTheReference() {
    final IDefinitionKeeper keeper = new DefinitionKeeper();
    final TypeString unknownRef = TypeString.ofIdentifier("unknown", "user");

    final TypeStringResolver resolver = new TypeStringResolver(keeper);
    final TypeString exemplarTypeString = resolver.getExemplarTypeString(unknownRef);
    assertThat(exemplarTypeString).isEqualTo(unknownRef);
  }

  /** Keeper with exemplar {@code sw:a} and {@code m()} written as {@code user:a.m()}. */
  private static IDefinitionKeeper createSpelledMethodKeeper() {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    TypeStringResolverTest.addExemplarWithParents(definitionKeeper, SW_A_REF);
    final MethodDefinition methodDefinition =
        TypeStringResolverTest.createMethodDefinition(USER_A_REF, "m()", TypeString.SW_INTEGER);
    definitionKeeper.add(methodDefinition);
    return definitionKeeper;
  }

  @Test
  void testRespondingMethodDefinitionsIncludeMethodWrittenInUsingPackage() {
    final IDefinitionKeeper definitionKeeper = TypeStringResolverTest.createSpelledMethodKeeper();

    final TypeStringResolver resolver = new TypeStringResolver(definitionKeeper);
    final Collection<MethodDefinition> methodDefinitions =
        resolver.getRespondingMethodDefinitions(SW_A_REF, "m()");
    final Set<TypeString> ownerTypeStrs =
        methodDefinitions.stream().map(MethodDefinition::getTypeName).collect(Collectors.toSet());
    assertThat(ownerTypeStrs).containsExactly(USER_A_REF);

    final Collection<MethodDefinition> allMethodDefinitions =
        resolver.getRespondingMethodDefinitions(SW_A_REF);
    assertThat(allMethodDefinitions).containsAll(methodDefinitions);
  }

  @Test
  void testRespondingMethodDefinitionsOfSpellingIncludeMethodWrittenUnderIt() {
    final IDefinitionKeeper definitionKeeper = TypeStringResolverTest.createSpelledMethodKeeper();

    final TypeStringResolver resolver = new TypeStringResolver(definitionKeeper);
    final Collection<MethodDefinition> methodDefinitions =
        resolver.getRespondingMethodDefinitions(USER_A_REF, "m()");
    assertThat(methodDefinitions).hasSize(1);
  }

  @Test
  void testRespondingMethodDefinitionsOfChildIncludeMethodWrittenInUsingPackage() {
    final IDefinitionKeeper definitionKeeper = TypeStringResolverTest.createSpelledMethodKeeper();
    TypeStringResolverTest.addExemplarWithParents(definitionKeeper, SW_B_REF, SW_A_REF);

    final TypeStringResolver resolver = new TypeStringResolver(definitionKeeper);
    final Collection<MethodDefinition> methodDefinitions =
        resolver.getRespondingMethodDefinitions(SW_B_REF, "m()");
    assertThat(methodDefinitions).hasSize(1);

    final Collection<MethodDefinition> allMethodDefinitions =
        resolver.getRespondingMethodDefinitions(SW_B_REF);
    assertThat(allMethodDefinitions).containsAll(methodDefinitions);
  }

  @Test
  void testRespondingMethodDefinitionsSkipSpellingWithItsOwnExemplar() {
    final IDefinitionKeeper definitionKeeper = TypeStringResolverTest.createSpelledMethodKeeper();
    TypeStringResolverTest.addExemplarWithParents(definitionKeeper, USER_A_REF);

    final TypeStringResolver resolver = new TypeStringResolver(definitionKeeper);
    final Collection<MethodDefinition> methodDefinitions =
        resolver.getRespondingMethodDefinitions(SW_A_REF, "m()");
    assertThat(methodDefinitions).isEmpty();

    final Collection<MethodDefinition> shadowingDefinitions =
        resolver.getRespondingMethodDefinitions(USER_A_REF, "m()");
    assertThat(shadowingDefinitions).hasSize(1);
  }

  @Test
  void testOwnMethodDefinitionsIncludeMethodWrittenInUsingPackage() {
    final IDefinitionKeeper definitionKeeper = TypeStringResolverTest.createSpelledMethodKeeper();

    final TypeStringResolver resolver = new TypeStringResolver(definitionKeeper);
    final Collection<MethodDefinition> methodDefinitions =
        resolver.getOwnMethodDefinitions(SW_A_REF);
    final Set<TypeString> ownerTypeStrs =
        methodDefinitions.stream().map(MethodDefinition::getTypeName).collect(Collectors.toSet());
    assertThat(ownerTypeStrs).containsExactly(USER_A_REF);
  }

  @Test
  void testOwnMethodDefinitionsExcludeInheritedMethods() {
    final IDefinitionKeeper definitionKeeper = TypeStringResolverTest.createSpelledMethodKeeper();
    TypeStringResolverTest.addExemplarWithParents(definitionKeeper, SW_B_REF, SW_A_REF);

    final TypeStringResolver resolver = new TypeStringResolver(definitionKeeper);
    final Collection<MethodDefinition> methodDefinitions =
        resolver.getOwnMethodDefinitions(SW_B_REF);
    assertThat(methodDefinitions).isEmpty();
  }

  @Test
  void testIsSameTypeThroughUsingPackage() {
    final IDefinitionKeeper definitionKeeper = TypeStringResolverTest.createSpelledMethodKeeper();

    final TypeStringResolver resolver = new TypeStringResolver(definitionKeeper);
    final boolean sameType = resolver.isSameType(USER_A_REF, SW_A_REF);
    assertThat(sameType).isTrue();
    final boolean otherType = resolver.isSameType(USER_A_REF, SW_B_REF);
    assertThat(otherType).isFalse();
  }

  @Test
  void testIsSameTypeNotForSpellingWithItsOwnExemplar() {
    final IDefinitionKeeper definitionKeeper = TypeStringResolverTest.createSpelledMethodKeeper();
    TypeStringResolverTest.addExemplarWithParents(definitionKeeper, USER_A_REF);

    final TypeStringResolver resolver = new TypeStringResolver(definitionKeeper);
    final boolean sameType = resolver.isSameType(USER_A_REF, SW_A_REF);
    assertThat(sameType).isFalse();
  }
}
