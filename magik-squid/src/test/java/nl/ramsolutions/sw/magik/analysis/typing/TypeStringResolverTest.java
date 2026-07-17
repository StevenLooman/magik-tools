package nl.ramsolutions.sw.magik.analysis.typing;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import nl.ramsolutions.sw.magik.analysis.definitions.DefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.ExemplarDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.ITypeStringDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.PackageDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.SlotDefinition;
import org.junit.jupiter.api.Test;

/** Tests for {@link TypeStringResolver}. */
class TypeStringResolverTest {

  private static ExemplarDefinition createExemplar(final TypeString typeString) {
    return new ExemplarDefinition(
        null, null, null, null, null, ExemplarDefinition.Sort.SLOTTED, typeString, List.of(), null);
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
            null,
            null,
            null,
            null,
            null,
            ExemplarDefinition.Sort.SLOTTED,
            typeA,
            Collections.emptyList(),
            null));
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
            null,
            null,
            null,
            null,
            null,
            ExemplarDefinition.Sort.SLOTTED,
            typeA,
            Collections.emptyList(),
            null));
    definitionKeeper.add(
        new SlotDefinition(null, null, null, null, null, typeA, "slot1", TypeString.SW_INTEGER));
    definitionKeeper.add(
        new SlotDefinition(null, null, null, null, null, typeA, "slot2", TypeString.SW_FLOAT));

    final TypeStringResolver resolver = new TypeStringResolver(definitionKeeper);
    assertThat(resolver.getSlotDefinitions(typeA, "slot2"))
        .extracting(SlotDefinition::getName)
        .containsExactly("slot2");
  }
}
