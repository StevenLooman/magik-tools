package nl.ramsolutions.sw.magik.languageserver.inlayhint;

import java.net.URI;
import java.util.EnumSet;
import java.util.List;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.analysis.typing.ITypeKeeper;
import nl.ramsolutions.sw.magik.analysis.typing.TypeKeeper;
import nl.ramsolutions.sw.magik.analysis.typing.types.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.types.MagikType;
import nl.ramsolutions.sw.magik.analysis.typing.types.Method;
import nl.ramsolutions.sw.magik.analysis.typing.types.Parameter;
import nl.ramsolutions.sw.magik.analysis.typing.types.TypeString;
import org.eclipse.lsp4j.InlayHint;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link InlayHintProvider}.
 */
class InlayHintProviderTest {

    private static final URI TEST_URI = URI.create("tests://unittest");

    @SuppressWarnings("checkstyle:MagicNumber")
    @Test
    void testProvideParameterHint() {
        final ITypeKeeper typeKeeper = new TypeKeeper();
        final TypeString objectRef = TypeString.ofIdentifier("object", "sw");
        final MagikType objectType = (MagikType) typeKeeper.getType(objectRef);
        objectType.addMethod(
            null,
            EnumSet.noneOf(Method.Modifier.class),
            "method()",
            List.of(
                new Parameter("param1", Parameter.Modifier.NONE),
                new Parameter("param2", Parameter.Modifier.NONE),
                new Parameter("param3", Parameter.Modifier.NONE)),
            null,
            null,
            new ExpressionResultString(),
            new ExpressionResultString());

        final String code = "object.method(_unset, :hello, var1)";
        final InlayHintProvider provider = new InlayHintProvider();
        final MagikTypedFile magikFile = new MagikTypedFile(TEST_URI, code, typeKeeper);

        final List<InlayHint> inlayHints =
            provider.provideInlayHints(magikFile, new Range(new Position(0, 0), new Position(2, 0)));
        assertThat(inlayHints).isEqualTo(
            List.of(
                new InlayHint(
                    new Position(0, 14),
                    Either.forLeft("param1:")),
                new InlayHint(
                    new Position(0, 22),
                    Either.forLeft("param2:"))//,

                // new InlayHint(
                //     new Position(0, 0),
                //     Either.forLeft("sw:object")),
                // new InlayHint(
                //     new Position(0, 14),
                //     Either.forLeft("sw:unset")),
                // new InlayHint(
                //     new Position(0, 22),
                //     Either.forLeft("sw:symbol"))
                    ));
    }

}
