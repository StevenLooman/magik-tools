package nl.ramsolutions.sw.magik.languageserver.definitions;

import java.net.URI;
import java.util.List;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.Position;
import nl.ramsolutions.sw.magik.Range;
import nl.ramsolutions.sw.magik.analysis.typing.ITypeKeeper;
import nl.ramsolutions.sw.magik.analysis.typing.TypeKeeper;
import nl.ramsolutions.sw.magik.analysis.typing.types.MagikType;
import nl.ramsolutions.sw.magik.analysis.typing.types.TypeString;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test DefinitionsProvider.
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class DefinitionsProviderTest {

    private static final URI TEST_URI = URI.create("tests://unittest");
    private static final Location EMPTY_LOCATION = new Location(
        URI.create("tests://unittest"),
        new Range(new Position(0, 0), new Position(0, 0)));

    private List<Location> getDefinitions(
                final String code, final Position position, final ITypeKeeper typeKeeper) {
        final MagikTypedFile magikFile = new MagikTypedFile(TEST_URI, code, typeKeeper);
        final DefinitionsProvider provider = new DefinitionsProvider();
        return provider.provideDefinitions(magikFile, position);
    }

    @Test
    void testProvideDefinitionsFromGlobal() {
        final ITypeKeeper typeKeeper = new TypeKeeper();
        final TypeString ropeRef = TypeString.ofIdentifier("rope", "sw");
        final MagikType ropeType = (MagikType) new MagikType(typeKeeper, MagikType.Sort.SLOTTED, ropeRef);
        ropeType.setLocation(EMPTY_LOCATION);

        final String code = ""
            + "_method object.method\n"
            + "    _return rope.new()\n"
            + "_endmethod\n";
        final Position position = new Position(2, 14);    // On `rope`.
        final List<Location> locations = this.getDefinitions(code, position, typeKeeper);
        assertThat(locations).hasSize(1);
        final Location location0 = locations.get(0);
        assertThat(location0).isEqualTo(EMPTY_LOCATION);
    }

    @Test
    void testProvideDefinitionsFromParameter() {
        final ITypeKeeper typeKeeper = new TypeKeeper();
        final String code = ""
            + "_method object.method(param1)\n"
            + "    _return param1\n"
            + "_endmethod\n";
        final Position position = new Position(2, 14);    // On `param1`.
        final List<Location> locations = this.getDefinitions(code, position, typeKeeper);
        assertThat(locations).hasSize(1);
        final Location location0 = locations.get(0);
        assertThat(location0).isEqualTo(
            new Location(
                TEST_URI,
                new Range(new Position(1, 22), new Position(1, 28))));
    }

    @Test
    void testProvideDefinitionsFromLocal() {
        final ITypeKeeper typeKeeper = new TypeKeeper();
        final String code = ""
            + "_method object.method\n"
            + "    _local local1 << 10\n"
            + "    _return local1\n"
            + "_endmethod\n";
        final Position position = new Position(3, 14);    // On `local1`.
        final List<Location> locations = this.getDefinitions(code, position, typeKeeper);
        assertThat(locations).hasSize(1);
        final Location location0 = locations.get(0);
        assertThat(location0).isEqualTo(
            new Location(
                TEST_URI,
                new Range(new Position(2, 11), new Position(2, 17))));
    }

}
