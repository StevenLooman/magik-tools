package nl.ramsolutions.sw.magik.languageserver.definitions;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.Position;
import nl.ramsolutions.sw.magik.Range;
import nl.ramsolutions.sw.magik.analysis.definitions.DefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.ExemplarDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.typing.types.TypeString;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test DefinitionsProvider.
 */
@SuppressWarnings("checkstyle:MagicNumber")
class DefinitionsProviderTest {

    private static final URI TEST_URI = URI.create("tests://unittest");
    private static final Location EMPTY_LOCATION = new Location(
        URI.create("tests://unittest"),
        new Range(new Position(0, 0), new Position(0, 0)));

    private List<Location> getDefinitions(
                final String code,
                final Position position,
                final IDefinitionKeeper definitionKeeper) {
        final MagikTypedFile magikFile = new MagikTypedFile(TEST_URI, code, definitionKeeper);
        final DefinitionsProvider provider = new DefinitionsProvider();
        return provider.provideDefinitions(magikFile, position);
    }

    @Test
    void testProvideDefinitionsFromGlobal() {
        final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
        final TypeString ropeRef = TypeString.ofIdentifier("rope", "sw");
        definitionKeeper.add(
            new ExemplarDefinition(
                EMPTY_LOCATION,
                null,
                null,
                null,
                ExemplarDefinition.Sort.SLOTTED,
                ropeRef,
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList()));

        final String code = ""
            + "_method object.method\n"
            + "    _return rope.new()\n"
            + "_endmethod\n";
        final Position position = new Position(2, 14);    // On `rope`.
        final List<Location> locations = this.getDefinitions(code, position, definitionKeeper);
        assertThat(locations).hasSize(1);
        final Location location0 = locations.get(0);
        assertThat(location0).isEqualTo(EMPTY_LOCATION);
    }

    @Test
    void testProvideDefinitionsFromParameter() {
        final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
        final String code = ""
            + "_method object.method(param1)\n"
            + "    _return param1\n"
            + "_endmethod\n";
        final Position position = new Position(2, 14);    // On `param1`.
        final List<Location> locations = this.getDefinitions(code, position, definitionKeeper);
        assertThat(locations).hasSize(1);
        final Location location0 = locations.get(0);
        assertThat(location0).isEqualTo(
            new Location(
                TEST_URI,
                new Range(new Position(1, 22), new Position(1, 28))));
    }

    @Test
    void testProvideDefinitionsFromLocal() {
        final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
        final String code = ""
            + "_method object.method\n"
            + "    _local local1 << 10\n"
            + "    _return local1\n"
            + "_endmethod\n";
        final Position position = new Position(3, 14);    // On `local1`.
        final List<Location> locations = this.getDefinitions(code, position, definitionKeeper);
        assertThat(locations).hasSize(1);
        final Location location0 = locations.get(0);
        assertThat(location0).isEqualTo(
            new Location(
                TEST_URI,
                new Range(new Position(2, 11), new Position(2, 17))));
    }

}
