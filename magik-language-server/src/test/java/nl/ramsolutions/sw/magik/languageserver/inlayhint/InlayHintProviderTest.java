package nl.ramsolutions.sw.magik.languageserver.inlayhint;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.analysis.definitions.DefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.ParameterDefinition;
import nl.ramsolutions.sw.magik.analysis.typing.types.ExpressionResultString;
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
        final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
        final TypeString objectRef = TypeString.ofIdentifier("object", "sw");
        definitionKeeper.add(
            new MethodDefinition(
                null,
                null,
                null,
                null,
                objectRef,
                "method()",
                Collections.emptySet(),
                List.of(
                    new ParameterDefinition(
                        null,
                        null,
                        null,
                        null,
                        "param1",
                        ParameterDefinition.Modifier.NONE,
                        TypeString.UNDEFINED),
                    new ParameterDefinition(
                        null,
                        null,
                        null,
                        null,
                        "param2",
                        ParameterDefinition.Modifier.NONE,
                        TypeString.UNDEFINED),
                    new ParameterDefinition(
                        null,
                        null,
                        null,
                        null,
                        "param3",
                        ParameterDefinition.Modifier.NONE,
                        TypeString.UNDEFINED)),
                null,
                ExpressionResultString.UNDEFINED,
                ExpressionResultString.EMPTY));

        final String code = "object.method(_unset, :hello, var1)";
        final InlayHintProvider provider = new InlayHintProvider();
        final MagikTypedFile magikFile = new MagikTypedFile(TEST_URI, code, definitionKeeper);

        final List<InlayHint> inlayHints =
            provider.provideInlayHints(magikFile, new Range(new Position(0, 0), new Position(2, 0)));
        assertThat(inlayHints).hasSize(2);

        final InlayHint inlayHint0 = inlayHints.get(0);
        assertThat(inlayHint0)
            .isEqualTo(
                new InlayHint(
                    new Position(0, 14),
                    Either.forLeft("param1:")));

        final InlayHint inlayHint1 = inlayHints.get(1);
        assertThat(inlayHint1)
            .isEqualTo(
                new InlayHint(
                    new Position(0, 22),
                    Either.forLeft("param2:")));
    }

}
