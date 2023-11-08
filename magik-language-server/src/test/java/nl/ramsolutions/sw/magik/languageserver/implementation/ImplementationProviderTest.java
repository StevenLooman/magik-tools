package nl.ramsolutions.sw.magik.languageserver.implementation;

import java.net.URI;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.Position;
import nl.ramsolutions.sw.magik.Range;
import nl.ramsolutions.sw.magik.analysis.typing.ITypeKeeper;
import nl.ramsolutions.sw.magik.analysis.typing.TypeKeeper;
import nl.ramsolutions.sw.magik.analysis.typing.types.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.types.MagikType;
import nl.ramsolutions.sw.magik.analysis.typing.types.Method;
import nl.ramsolutions.sw.magik.analysis.typing.types.TypeString;
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
    void testProvideAbstractMethodImplementation() {
        final ITypeKeeper typeKeeper = new TypeKeeper();
        final TypeString objectRef = TypeString.ofIdentifier("object", "sw");
        final MagikType objectType = (MagikType) typeKeeper.getType(objectRef);
        objectType.addMethod(
            EMPTY_LOCATION,
            null,
            EnumSet.of(Method.Modifier.ABSTRACT),
            "abstract()",
            Collections.emptyList(),
            null,
            null,
            ExpressionResultString.UNDEFINED,
            new ExpressionResultString());
        final TypeString integerRef = TypeString.ofIdentifier("integer", "sw");
        final MagikType integerType = (MagikType) typeKeeper.getType(integerRef);
        integerType.addParent(objectRef);
        integerType.addMethod(
            EMPTY_LOCATION,
            null,
            EnumSet.noneOf(Method.Modifier.class),
            "abstract()",
            Collections.emptyList(),
            null,
            null,
            ExpressionResultString.UNDEFINED,
            new ExpressionResultString());

        final URI uri = URI.create("tests://unittest");
        final String code = ""
            + "_abstract _method object.abstract()\n"
            + "_endmethod";
        final MagikTypedFile magikFile = new MagikTypedFile(uri, code, typeKeeper);
        final Position position = new Position(1, 26);  // On `abstract`.

        final ImplementationProvider provider = new ImplementationProvider();
        final List<Location> implementations = provider.provideImplementations(magikFile, position);
        assertThat(implementations)
            .containsOnly(EMPTY_LOCATION);
    }

}
