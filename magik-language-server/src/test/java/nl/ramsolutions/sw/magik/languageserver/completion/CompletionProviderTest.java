package nl.ramsolutions.sw.magik.languageserver.completion;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.analysis.definitions.DefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.ExemplarDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.SlotDefinition;
import nl.ramsolutions.sw.magik.analysis.typing.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import nl.ramsolutions.sw.magik.api.MagikKeyword;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionItemKind;
import org.eclipse.lsp4j.Position;
import org.junit.jupiter.api.Test;

/** Test CompletionProvider. */
@SuppressWarnings("checkstyle:MagicNumber")
class CompletionProviderTest {

  private static final URI DEFAULT_URI = URI.create("memory://source.magik");

  private List<CompletionItem> getCompletions(
      final String code, final IDefinitionKeeper definitionKeeper, final Position position) {
    final MagikTypedFile magikFile = new MagikTypedFile(DEFAULT_URI, code, definitionKeeper);
    final CompletionProvider provider = new CompletionProvider();
    return provider.provideCompletions(magikFile, position);
  }

  private List<CompletionItem> getCompletions(final String code, final Position position) {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    return this.getCompletions(code, definitionKeeper, position);
  }

  @Test
  void testKeywordCompletion() {
    final String code =
        """
        _method a.b
            _
        _endmethod""";
    final Position position = new Position(1, 5); // On '_'.
    final List<CompletionItem> completions = this.getCompletions(code, position);

    assertThat(completions).hasSize(MagikKeyword.keywordValues().length);

    final CompletionItem item = completions.get(0);
    assertThat(item.getKind()).isEqualTo(CompletionItemKind.Keyword);
    assertThat(item.getLabel()).startsWith("_");
  }

  @Test
  void testMethodCompletionBare() {
    final String code =
        """
        _method a.b
            1.
        _endmethod""";
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    definitionKeeper.add(
        new MethodDefinition(
            null,
            null,
            null,
            null,
            null,
            TypeString.SW_INTEGER,
            "find_me()",
            Collections.emptySet(),
            Collections.emptyList(),
            null,
            Collections.emptySet(),
            ExpressionResultString.UNDEFINED,
            ExpressionResultString.EMPTY));
    final Position position = new Position(1, 6); // On '.'.
    final List<CompletionItem> completions = this.getCompletions(code, definitionKeeper, position);

    assertThat(completions).hasSize(1);

    final CompletionItem item = completions.get(0);
    assertThat(item.getKind()).isEqualTo(CompletionItemKind.Method);
    assertThat(item.getInsertText()).isEqualTo("find_me()");
    assertThat(item.getLabel()).isEqualTo("find_me()");
    assertThat(item.getDetail()).isEqualTo("sw:integer");
  }

  @Test
  void testMethodCompletionSelf() {
    final String code =
        """
        _method a.b
            _self.
        _endmethod""";
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final TypeString aRef = TypeString.ofIdentifier("a", "user");
    definitionKeeper.add(
        new ExemplarDefinition(
            null,
            null,
            null,
            null,
            null,
            ExemplarDefinition.Sort.SLOTTED,
            aRef,
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptySet()));
    definitionKeeper.add(
        new MethodDefinition(
            null,
            null,
            null,
            null,
            null,
            aRef,
            "find_me()",
            Collections.emptySet(),
            Collections.emptyList(),
            null,
            Collections.emptySet(),
            ExpressionResultString.UNDEFINED,
            ExpressionResultString.EMPTY));
    final Position position = new Position(1, 10); // On '.'.
    final List<CompletionItem> completions = this.getCompletions(code, definitionKeeper, position);
    assertThat(completions).hasSize(1);
    final CompletionItem item = completions.get(0);
    assertThat(item.getKind()).isEqualTo(CompletionItemKind.Method);
    assertThat(item.getInsertText()).isEqualTo("find_me()");
    assertThat(item.getLabel()).isEqualTo("find_me()");
    assertThat(item.getDetail()).isEqualTo("user:a");
  }

  @Test
  void testMethodCompletionExisting() {
    final String code =
        """
        _method a.b
            1.fi
        _endmethod""";
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    definitionKeeper.add(
        new MethodDefinition(
            null,
            null,
            null,
            null,
            null,
            TypeString.SW_INTEGER,
            "find_me()",
            Collections.emptySet(),
            Collections.emptyList(),
            null,
            Collections.emptySet(),
            ExpressionResultString.UNDEFINED,
            ExpressionResultString.EMPTY));
    final Position position = new Position(1, 8); // On 'i'.
    final List<CompletionItem> completions = this.getCompletions(code, definitionKeeper, position);

    assertThat(completions).hasSize(1);

    final CompletionItem item = completions.get(0);
    assertThat(item.getKind()).isEqualTo(CompletionItemKind.Method);
    assertThat(item.getInsertText()).isEqualTo("find_me()");
    assertThat(item.getLabel()).isEqualTo("find_me()");
    assertThat(item.getDetail()).isEqualTo("sw:integer");
  }

  @Test
  void testGlobalCompletion() {
    final String code =
        """
        _method a.b
         \s\s\s
        _endmethod""";
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final Position position = new Position(1, 2); // On ''.
    final List<CompletionItem> completions = this.getCompletions(code, definitionKeeper, position);

    final Collection<ExemplarDefinition> defaultTypes = definitionKeeper.getExemplarDefinitions();
    assertThat(completions).hasSize(defaultTypes.size() + MagikKeyword.values().length);

    final Set<CompletionItemKind> itemKinds =
        completions.stream().map(item -> item.getKind()).collect(Collectors.toSet());
    assertThat(itemKinds)
        .containsExactlyInAnyOrder(CompletionItemKind.Class, CompletionItemKind.Keyword);
  }

  @Test
  void testGlobalCompletionVariable() {
    final String code =
        """
        _method a.b
            _local x << 10
         \s\s\s
        _endmethod""";
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final Position position = new Position(2, 2); // On ''.
    final List<CompletionItem> completions = this.getCompletions(code, definitionKeeper, position);

    final Collection<ExemplarDefinition> defaultTypes = definitionKeeper.getExemplarDefinitions();
    assertThat(completions)
        .hasSize(defaultTypes.size() + MagikKeyword.values().length + 1); // Local variable.

    final Set<CompletionItemKind> itemKinds =
        completions.stream().map(item -> item.getKind()).collect(Collectors.toSet());
    assertThat(itemKinds)
        .containsExactlyInAnyOrder(
            CompletionItemKind.Class, CompletionItemKind.Keyword, CompletionItemKind.Variable);
  }

  @Test
  void testGlobalCompletionSlot() {
    final String code =
        """
        _method a.b
         \s\s\s
        _endmethod""";
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final TypeString aRef = TypeString.ofIdentifier("a", "user");
    definitionKeeper.add(
        new ExemplarDefinition(
            null,
            null,
            null,
            null,
            null,
            ExemplarDefinition.Sort.SLOTTED,
            aRef,
            List.of(new SlotDefinition(null, null, code, code, null, code, aRef)),
            Collections.emptyList(),
            Collections.emptySet()));

    final Position position = new Position(1, 2); // On ''.
    final List<CompletionItem> completions = this.getCompletions(code, definitionKeeper, position);

    final Collection<ExemplarDefinition> defaultTypes = definitionKeeper.getExemplarDefinitions();
    assertThat(completions)
        .hasSize(defaultTypes.size() + MagikKeyword.values().length + 1); // Slot.

    final Set<CompletionItemKind> itemKinds =
        completions.stream().map(item -> item.getKind()).collect(Collectors.toSet());
    assertThat(itemKinds)
        .containsExactlyInAnyOrder(
            CompletionItemKind.Class, CompletionItemKind.Keyword, CompletionItemKind.Property);
  }

  @Test
  void testNoCompletionInComment() {
    final String code = "abc # ";
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final Position position = new Position(0, 5); // On ' ', in comment.
    final List<CompletionItem> completions = this.getCompletions(code, definitionKeeper, position);
    assertThat(completions).isEmpty();
  }
}
