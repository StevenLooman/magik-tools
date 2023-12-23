package nl.ramsolutions.sw.magik.languageserver.references;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.Position;
import nl.ramsolutions.sw.magik.analysis.definitions.DefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.GlobalUsage;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodUsage;
import nl.ramsolutions.sw.magik.analysis.typing.types.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.types.TypeString;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test ReferencesProvider.
 */
@SuppressWarnings("checkstyle:MagicNumber")
class ReferencesProviderTest {

    private static final Location EMPTY_LOCATION = Location.validLocation(null);

    private List<Location> getReferences(
                final String code,
                final Position position,
                final IDefinitionKeeper definitionKeeper) {
        final URI uri = EMPTY_LOCATION.getUri();
        final MagikTypedFile magikFile = new MagikTypedFile(uri, code, definitionKeeper);
        final ReferencesProvider provider = new ReferencesProvider();
        return provider.provideReferences(magikFile, position);
    }

    @Test
    void testProvideMethodReferenceFromMethodInvocation() {
        final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
        final TypeString integerRef = TypeString.ofIdentifier("integer", "sw");
        definitionKeeper.add(
            new MethodDefinition(
                EMPTY_LOCATION,
                null,
                null,
                null,
                integerRef,
                "refering",
                Collections.emptySet(),
                Collections.emptyList(),
                null,
                ExpressionResultString.UNDEFINED,
                ExpressionResultString.EMPTY,
                Collections.emptySet(),
                Set.of(
                    new MethodUsage(TypeString.UNDEFINED, "refering", EMPTY_LOCATION)),
                Collections.emptySet(),
                Collections.emptySet()));

        final String code = ""
            + "_method integer.refering\n"
            + "    _self.refering\n"
            + "_endmethod\n";
        final Position position = new Position(2, 12);  // On `refering`.
        final List<Location> references = this.getReferences(code, position, definitionKeeper);
        assertThat(references).hasSize(1);
    }

    @Test
    void testProvideMethodReferenceFromMethodDefintion() {
        final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
        final TypeString integerRef = TypeString.ofIdentifier("integer", "sw");
        definitionKeeper.add(
            new MethodDefinition(
                EMPTY_LOCATION,
                null,
                null,
                null,
                integerRef,
                "refering",
                Collections.emptySet(),
                Collections.emptyList(),
                null,
                ExpressionResultString.UNDEFINED,
                ExpressionResultString.EMPTY,
                Collections.emptySet(),
                Set.of(
                    new MethodUsage(TypeString.UNDEFINED, "refering", EMPTY_LOCATION)),
                Collections.emptySet(),
                Collections.emptySet()));

        final String code = ""
            + "_method integer.refering\n"
            + "    _self.refering\n"
            + "_endmethod\n";
        final Position position = new Position(1, 20);    // On `refering`.
        final List<Location> references = this.getReferences(code, position, definitionKeeper);
        assertThat(references).hasSize(1);
    }

    @Test
    void testProvideTypeReferenceFromAtom() {
        final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
        final TypeString integerRef = TypeString.ofIdentifier("integer", "sw");
        definitionKeeper.add(
            new MethodDefinition(
                EMPTY_LOCATION,
                null,
                null,
                null,
                integerRef,
                "refering",
                Collections.emptySet(),
                Collections.emptyList(),
                null,
                ExpressionResultString.UNDEFINED,
                ExpressionResultString.EMPTY,
                Set.of(
                    new GlobalUsage(integerRef, EMPTY_LOCATION)),
                Collections.emptySet(),
                Collections.emptySet(),
                Collections.emptySet()));

        final String code = ""
            + "_method integer.refering\n"
            + "    integer\n"
            + "_endmethod\n";
        final Position position = new Position(2, 4);    // On `integer`.
        final List<Location> references = this.getReferences(code, position, definitionKeeper);
        assertThat(references).hasSize(1);
    }

    @Test
    void testProvideTypeReferenceFromMethodDefinition() {
        final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
        final TypeString integerRef = TypeString.ofIdentifier("integer", "sw");
        definitionKeeper.add(
            new MethodDefinition(
                EMPTY_LOCATION,
                null,
                null,
                null,
                integerRef,
                "refering",
                Collections.emptySet(),
                Collections.emptyList(),
                null,
                ExpressionResultString.UNDEFINED,
                ExpressionResultString.EMPTY,
                Set.of(
                    new GlobalUsage(integerRef, EMPTY_LOCATION)),
                Collections.emptySet(),
                Collections.emptySet(),
                Collections.emptySet()));

        final String code = ""
            + "_method integer.refering\n"
            + "    print(integer)\n"
            + "_endmethod\n";
        final Position position = new Position(1, 10);    // On `integer`.
        final List<Location> references = this.getReferences(code, position, definitionKeeper);
        assertThat(references).hasSize(1);
    }

}
