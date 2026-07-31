package nl.ramsolutions.sw.magik.languageserver.references;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.Position;
import nl.ramsolutions.sw.magik.analysis.definitions.DefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.GlobalUsage;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodUsage;
import nl.ramsolutions.sw.magik.analysis.typing.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import org.junit.jupiter.api.Test;

/** Test ReferencesProvider. */
@SuppressWarnings("checkstyle:MagicNumber")
class ReferencesProviderTest {

  private static final Location EMPTY_LOCATION = Location.validLocation(null);

  private List<Location> getReferences(
      final String code, final Position position, final IDefinitionKeeper definitionKeeper) {
    final URI uri = EMPTY_LOCATION.getUri();
    final MagikTypedFile magikFile = new MagikTypedFile(uri, code, definitionKeeper);
    final ReferencesProvider provider = new ReferencesProvider();
    return provider.provideReferences(magikFile, position);
  }

  @Test
  void testProvideMethodReferenceFromMethodInvocation() {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    definitionKeeper.add(
        new MethodDefinition(
            EMPTY_LOCATION,
            null,
            null,
            null,
            null,
            TypeString.SW_INTEGER,
            "referring",
            Collections.emptySet(),
            Collections.emptyList(),
            null,
            null,
            ExpressionResultString.UNDEFINED,
            ExpressionResultString.EMPTY,
            Collections.emptyList(),
            List.of(new MethodUsage(TypeString.UNDEFINED, "referring", EMPTY_LOCATION, null)),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList()));

    final String code =
        """
        _method integer.referring
            _self.referring
        _endmethod
        """;
    final Position position = new Position(2, 12); // On `referring`.
    final List<Location> references = this.getReferences(code, position, definitionKeeper);
    assertThat(references).hasSize(1);
  }

  @Test
  void testProvideMethodReferenceFromMethodDefinition() {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    definitionKeeper.add(
        new MethodDefinition(
            EMPTY_LOCATION,
            null,
            null,
            null,
            null,
            TypeString.SW_INTEGER,
            "referring",
            Collections.emptySet(),
            Collections.emptyList(),
            null,
            null,
            ExpressionResultString.UNDEFINED,
            ExpressionResultString.EMPTY,
            Collections.emptyList(),
            List.of(new MethodUsage(TypeString.UNDEFINED, "referring", EMPTY_LOCATION, null)),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList()));

    final String code =
        """
        _method integer.referring
            _self.referring
        _endmethod
        """;
    final Position position = new Position(1, 20); // On `referring`.
    final List<Location> references = this.getReferences(code, position, definitionKeeper);
    assertThat(references).hasSize(1);
  }

  @Test
  void testProvideTypeReferenceFromAtom() {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    definitionKeeper.add(
        new MethodDefinition(
            EMPTY_LOCATION,
            null,
            null,
            null,
            null,
            TypeString.SW_INTEGER,
            "referring",
            Collections.emptySet(),
            Collections.emptyList(),
            null,
            null,
            ExpressionResultString.UNDEFINED,
            ExpressionResultString.EMPTY,
            List.of(new GlobalUsage(TypeString.SW_INTEGER, EMPTY_LOCATION, null)),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList()));

    final String code =
        """
        _method integer.referring
            integer
        _endmethod
        """;
    final Position position = new Position(2, 4); // On `integer`.
    final List<Location> references = this.getReferences(code, position, definitionKeeper);
    assertThat(references).hasSize(1);
  }

  @Test
  void testProvideTypeReferenceFromMethodDefinition() {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    definitionKeeper.add(
        new MethodDefinition(
            EMPTY_LOCATION,
            null,
            null,
            null,
            null,
            TypeString.SW_INTEGER,
            "referring",
            Collections.emptySet(),
            Collections.emptyList(),
            null,
            null,
            ExpressionResultString.UNDEFINED,
            ExpressionResultString.EMPTY,
            List.of(new GlobalUsage(TypeString.SW_INTEGER, EMPTY_LOCATION, null)),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList()));

    final String code =
        """
        _method integer.referring
            print(integer)
        _endmethod
        """;
    final Position position = new Position(1, 10); // On `integer`.
    final List<Location> references = this.getReferences(code, position, definitionKeeper);
    assertThat(references).hasSize(1);
  }
}
