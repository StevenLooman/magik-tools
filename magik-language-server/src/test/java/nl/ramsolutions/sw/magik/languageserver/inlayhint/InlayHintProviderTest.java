package nl.ramsolutions.sw.magik.languageserver.inlayhint;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import nl.ramsolutions.sw.MagikToolsProperties;
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
                    null,
                    "param1",
                    ParameterDefinition.Modifier.NONE,
                    TypeString.UNDEFINED),
                new ParameterDefinition(
                    null,
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
                    null,
                    "param3",
                    ParameterDefinition.Modifier.NONE,
                    TypeString.UNDEFINED)),
            null,
            null,
            ExpressionResultString.UNDEFINED,
            ExpressionResultString.EMPTY));

    final String code = "object.method(_unset, :hello, var1)";
    final MagikToolsProperties properties =
        new MagikToolsProperties(Map.of("magik.typing.showArgumentInlayHints", "true"));
    final InlayHintProvider provider = new InlayHintProvider(properties);
    final MagikTypedFile magikFile =
        new MagikTypedFile(MagikTypedFile.DEFAULT_URI, code, definitionKeeper);

    final List<InlayHint> inlayHints =
        provider.provideInlayHints(magikFile, new Range(new Position(0, 0), new Position(2, 0)));
    assertThat(inlayHints)
        .isEqualTo(
            List.of(
                new InlayHint(new Position(0, 14), Either.forLeft("param1:")),
                new InlayHint(new Position(0, 22), Either.forLeft("param2:"))));
  }

  @SuppressWarnings("checkstyle:MagicNumber")
  @Test
  void testProvideAtomTypingHint() {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final String code =
        """
        _method a.b
            :symbol
        _endmethod
        """;
    final MagikToolsProperties properties =
        new MagikToolsProperties(Map.of("magik.typing.showTypingInlayHints", "true"));
    final InlayHintProvider provider = new InlayHintProvider(properties);
    final MagikTypedFile magikFile =
        new MagikTypedFile(MagikTypedFile.DEFAULT_URI, code, definitionKeeper);

    final List<InlayHint> inlayHints =
        provider.provideInlayHints(magikFile, new Range(new Position(0, 0), new Position(3, 0)));
    assertThat(inlayHints)
        .isEqualTo(List.of(new InlayHint(new Position(1, 4), Either.forLeft("sw:symbol"))));
  }

  @SuppressWarnings("checkstyle:MagicNumber")
  @Test
  void testProvideAtomAndInvocationTypingHints() {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    definitionKeeper.add(
        new MethodDefinition(
            null,
            null,
            null,
            null,
            null,
            TypeString.SW_INTEGER,
            "hover_me()",
            Collections.emptySet(),
            Collections.emptyList(),
            null,
            null,
            new ExpressionResultString(TypeString.SW_SYMBOL),
            ExpressionResultString.EMPTY));

    final String code =
        """
        _method a.b
            _local var << 1
            var.hover_me()
        _endmethod
        """;
    final MagikToolsProperties properties =
        new MagikToolsProperties(Map.of("magik.typing.showTypingInlayHints", "true"));
    final InlayHintProvider provider = new InlayHintProvider(properties);
    final MagikTypedFile magikFile =
        new MagikTypedFile(MagikTypedFile.DEFAULT_URI, code, definitionKeeper);

    final List<InlayHint> inlayHints =
        provider.provideInlayHints(magikFile, new Range(new Position(0, 0), new Position(4, 0)));
    assertThat(inlayHints)
        .isEqualTo(
            List.of(
                new InlayHint(new Position(1, 18), Either.forLeft("sw:integer")),
                new InlayHint(new Position(2, 4), Either.forLeft("sw:integer")),
                new InlayHint(new Position(2, 18), Either.forLeft("->sw:symbol"))));
  }

  @SuppressWarnings("checkstyle:MagicNumber")
  @Test
  void testProvideTypingHintsDisabledByDefault() {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final String code =
        """
        _method a.b
            :symbol
        _endmethod
        """;
    final MagikToolsProperties properties = new MagikToolsProperties(Collections.emptyMap());
    final InlayHintProvider provider = new InlayHintProvider(properties);
    final MagikTypedFile magikFile =
        new MagikTypedFile(MagikTypedFile.DEFAULT_URI, code, definitionKeeper);

    final List<InlayHint> inlayHints =
        provider.provideInlayHints(magikFile, new Range(new Position(0, 0), new Position(3, 0)));
    assertThat(inlayHints).isEmpty();
  }
}
