package nl.ramsolutions.sw.magik.languageserver.implementation;

import java.net.URI;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.analysis.Location;
import nl.ramsolutions.sw.magik.analysis.Position;
import nl.ramsolutions.sw.magik.analysis.Range;
import nl.ramsolutions.sw.magik.analysis.typing.ITypeKeeper;
import nl.ramsolutions.sw.magik.analysis.typing.TypeKeeper;
import nl.ramsolutions.sw.magik.analysis.typing.types.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.types.MagikType;
import nl.ramsolutions.sw.magik.analysis.typing.types.Method;
import nl.ramsolutions.sw.magik.analysis.typing.types.TypeString;
import nl.ramsolutions.sw.magik.languageserver.Lsp4jConversion;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test ImplementationProvider.
 */
@SuppressWarnings("checkstyle:MagicNumber")
class ImplementationProviderTest {

    private static final Location EMPTY_LOCATION = new Location(
        URI.create("tests://unittest"),
        new Range(new Position(0, 0), new Position(0, 0)));

    @Test
    void testProvideImplementation() {
        final ITypeKeeper typeKeeper = new TypeKeeper();
        final TypeString integerRef = TypeString.ofIdentifier("integer", "sw");
        final MagikType integerType = (MagikType) typeKeeper.getType(integerRef);
        integerType.addMethod(
            EMPTY_LOCATION,
            EnumSet.noneOf(Method.Modifier.class),
            "implementation()",
            Collections.emptyList(),
            null,
            null,
            ExpressionResultString.UNDEFINED,
            new ExpressionResultString());

        final URI uri = URI.create("tests://unittest");
        final String code = ""
            + "_method object.b\n"
            + "    1.implementation()\n"
            + "_endmethod";
        final MagikTypedFile magikFile = new MagikTypedFile(uri, code, typeKeeper);
        final org.eclipse.lsp4j.Position position = new org.eclipse.lsp4j.Position(1, 10);

        final ImplementationProvider provider = new ImplementationProvider();
        final List<org.eclipse.lsp4j.Location> implementations = provider.provideImplementations(magikFile, position);
        assertThat(implementations)
            .containsOnly(Lsp4jConversion.locationToLsp4j(EMPTY_LOCATION));
    }

}
