package nl.ramsolutions.sw.magik.languageserver.implementation;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.Position;
import nl.ramsolutions.sw.magik.Range;
import nl.ramsolutions.sw.magik.analysis.definitions.DefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.ExemplarDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodDefinition;
import nl.ramsolutions.sw.magik.analysis.typing.types.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.types.TypeString;
import org.junit.jupiter.api.Test;

/** Test ImplementationProvider. */
@SuppressWarnings("checkstyle:MagicNumber")
class ImplementationProviderTest {

  private static final Location EMPTY_LOCATION =
      new Location(
          URI.create("tests://unittest"), new Range(new Position(0, 0), new Position(0, 0)));

  @Test
  void testProvideAbstractMethodImplementation() {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final TypeString aRef = TypeString.ofIdentifier("a", "user");
    definitionKeeper.add(
        new ExemplarDefinition(
            EMPTY_LOCATION,
            null,
            null,
            null,
            ExemplarDefinition.Sort.SLOTTED,
            aRef,
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptySet()));
    definitionKeeper.add(
        new MethodDefinition(
            new Location(
                URI.create("tests://unittest"), new Range(new Position(0, 0), new Position(0, 10))),
            null,
            null,
            null,
            aRef,
            "abstract()",
            Set.of(MethodDefinition.Modifier.ABSTRACT),
            Collections.emptyList(),
            null,
            Collections.emptySet(),
            ExpressionResultString.UNDEFINED,
            ExpressionResultString.EMPTY));
    final TypeString bRef = TypeString.ofIdentifier("b", "user");
    definitionKeeper.add(
        new ExemplarDefinition(
            EMPTY_LOCATION,
            null,
            null,
            null,
            ExemplarDefinition.Sort.SLOTTED,
            bRef,
            Collections.emptyList(),
            List.of(aRef),
            Collections.emptySet()));
    definitionKeeper.add(
        new MethodDefinition(
            new Location(
                URI.create("tests://unittest"),
                new Range(new Position(50, 0), new Position(50, 10))),
            null,
            null,
            null,
            bRef,
            "abstract()",
            Collections.emptySet(), // Concrete.
            Collections.emptyList(),
            null,
            Collections.emptySet(),
            ExpressionResultString.UNDEFINED,
            ExpressionResultString.EMPTY));

    final URI uri = URI.create("tests://unittest");
    final String code = "" + "_abstract _method a.abstract()\n" + "_endmethod";
    final MagikTypedFile magikFile = new MagikTypedFile(uri, code, definitionKeeper);
    final Position position = new Position(1, 26); // On `abstract()`.

    final ImplementationProvider provider = new ImplementationProvider();
    final List<Location> implementations = provider.provideImplementations(magikFile, position);
    assertThat(implementations)
        .containsOnly(
            new Location(
                URI.create("tests://unittest"),
                new Range(new Position(50, 0), new Position(50, 10))));
  }
}
