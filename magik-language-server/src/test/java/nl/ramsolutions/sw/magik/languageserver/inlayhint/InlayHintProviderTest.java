package nl.ramsolutions.sw.magik.languageserver.inlayhint;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.analysis.definitions.DefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.ParameterDefinition;
import nl.ramsolutions.sw.magik.analysis.typing.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import org.eclipse.lsp4j.InlayHint;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.junit.jupiter.api.Test;

/** Tests for {@link InlayHintProvider}. */
class InlayHintProviderTest {

  private static final URI DEFAULT_URI = URI.create("memory://source.magik");

  @SuppressWarnings("checkstyle:MagicNumber")
  @Test
  void testProvideParameterHint() {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    definitionKeeper.add(
        new MethodDefinition(
            null,
            null,
            null,
            null,
            TypeString.SW_OBJECT,
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
            Collections.emptySet(),
            ExpressionResultString.UNDEFINED,
            ExpressionResultString.EMPTY));

    final String code = "object.method(_unset, :hello, var1)";
    final InlayHintProvider provider = new InlayHintProvider();
    final MagikTypedFile magikFile = new MagikTypedFile(DEFAULT_URI, code, definitionKeeper);

    final List<InlayHint> inlayHints =
        provider.provideInlayHints(magikFile, new Range(new Position(0, 0), new Position(2, 0)));
    assertThat(inlayHints)
        .isEqualTo(
            List.of(
                new InlayHint(new Position(0, 14), Either.forLeft("param1:")),
                new InlayHint(new Position(0, 22), Either.forLeft("param2:"))));
  }
}
