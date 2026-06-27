package nl.ramsolutions.sw.magik.analysis.typing;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collection;
import java.util.List;
import nl.ramsolutions.sw.magik.analysis.definitions.DefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.ExemplarDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.ITypeStringDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.PackageDefinition;
import org.junit.jupiter.api.Test;

/** Tests for {@link TypeStringResolver}. */
class TypeStringResolverTest {

  private static ExemplarDefinition exemplar(final TypeString typeString) {
    return new ExemplarDefinition(
        null,
        null,
        null,
        null,
        null,
        ExemplarDefinition.Sort.SLOTTED,
        typeString,
        List.of(),
        List.of(),
        null);
  }

  @Test
  void testResolveShadowsSameNameInUsedPackage() {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    // Package `rs` uses `sw`.
    definitionKeeper.add(new PackageDefinition(null, null, null, null, null, "rs", List.of("sw")));

    // The same identifier is defined in both `rs` and the used package `sw`.
    final TypeString rsFoo = TypeString.ofIdentifier("foo", "rs");
    final TypeString swFoo = TypeString.ofIdentifier("foo", "sw");
    final ExemplarDefinition exemplarRsFoo = TypeStringResolverTest.exemplar(rsFoo);
    definitionKeeper.add(exemplarRsFoo);
    final ExemplarDefinition exemplarSwFoo = TypeStringResolverTest.exemplar(swFoo);
    definitionKeeper.add(exemplarSwFoo);

    // A reference to `rs:foo` must resolve to the nearest package's definition only,
    // i.e. `rs:foo` shadows `sw:foo` -- not a union of both.
    final TypeStringResolver resolver = new TypeStringResolver(definitionKeeper);
    final Collection<ITypeStringDefinition> resolved = resolver.resolve(rsFoo);
    assertThat(resolved).extracting(ITypeStringDefinition::getTypeString).containsExactly(rsFoo);
  }
}
