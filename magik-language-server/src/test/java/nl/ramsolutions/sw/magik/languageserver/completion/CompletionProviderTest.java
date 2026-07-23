package nl.ramsolutions.sw.magik.languageserver.completion;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.analysis.definitions.DefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.ExemplarDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.Pragma;
import nl.ramsolutions.sw.magik.analysis.definitions.SlotDefinition;
import nl.ramsolutions.sw.magik.analysis.typing.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import nl.ramsolutions.sw.magik.api.MagikKeyword;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionItemKind;
import org.eclipse.lsp4j.CompletionItemTag;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.TextEdit;
import org.junit.jupiter.api.Test;

/** Test CompletionProvider. */
@SuppressWarnings("checkstyle:MagicNumber")
class CompletionProviderTest {

  private List<CompletionItem> getCompletions(
      final String code, final IDefinitionKeeper definitionKeeper, final Position position) {
    final MagikTypedFile magikFile =
        new MagikTypedFile(MagikTypedFile.DEFAULT_URI, code, definitionKeeper);
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
    final Position position = new Position(1, 5); // After '_'.
    final List<CompletionItem> completions = this.getCompletions(code, position);

    assertThat(completions).hasSize(MagikKeyword.keywordValues().length);
    completions.forEach(
        item -> {
          assertThat(item.getKind()).isEqualTo(CompletionItemKind.Keyword);
          assertThat(item.getLabel()).startsWith("_");
        });
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
            null,
            ExpressionResultString.UNDEFINED,
            ExpressionResultString.EMPTY));
    final Position position = new Position(1, 6); // After '.'.
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
            null, null, null, null, null, ExemplarDefinition.Sort.SLOTTED, aRef, null));
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
            null,
            ExpressionResultString.UNDEFINED,
            ExpressionResultString.EMPTY));
    final Position position = new Position(1, 9); // On '.'.
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
            null,
            ExpressionResultString.UNDEFINED,
            ExpressionResultString.EMPTY));
    final Position position = new Position(1, 7); // On 'i'.
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
          \s
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
            null, null, null, null, null, ExemplarDefinition.Sort.SLOTTED, aRef, null));
    definitionKeeper.add(new SlotDefinition(null, null, code, code, null, aRef, code, aRef));

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

  private static final class ExtendedProvider extends CompletionProvider {
    @Override
    protected List<CompletionModule> createModules() {
      final List<CompletionModule> modules = new ArrayList<>(super.createModules());
      modules.add(context -> Optional.of(List.of(new CompletionItem("DUMMY"))));
      return modules;
    }
  }

  private static final class TriggerProvider extends CompletionProvider {
    @Override
    protected List<CompletionModule> createModules() {
      final List<CompletionModule> modules = new ArrayList<>(super.createModules());
      modules.add(
          new CompletionModule() {
            @Override
            public Optional<List<CompletionItem>> tryComplete(final CompletionContext context) {
              return Optional.empty();
            }

            @Override
            public List<String> getTriggerCharacters() {
              return List.of("!");
            }
          });
      return modules;
    }
  }

  @Test
  void testTriggerCharactersAggregatedFromModules() {
    final ServerCapabilities capabilities = new ServerCapabilities();
    new TriggerProvider().setCapabilities(capabilities);
    assertThat(capabilities.getCompletionProvider().getTriggerCharacters()).contains(".", "!");
  }

  @Test
  void testCustomModuleAggregates() {
    final String code =
        """
        _method a.b
            _
        _endmethod""";
    final Position position = new Position(1, 5); // After '_'.
    final MagikTypedFile magikFile =
        new MagikTypedFile(MagikTypedFile.DEFAULT_URI, code, new DefinitionKeeper());
    final CompletionProvider provider = new ExtendedProvider();
    final List<CompletionItem> completions = provider.provideCompletions(magikFile, position);

    assertThat(completions).extracting(CompletionItem::getLabel).contains("DUMMY");
    assertThat(completions).hasSizeGreaterThan(1);
  }

  private String applyEdit(final CompletionItem item, final String code) {
    final TextEdit edit = item.getTextEdit().getLeft();
    final String[] lines = code.split("\n", -1);
    final int line = edit.getRange().getStart().getLine();
    final int start = edit.getRange().getStart().getCharacter();
    final int end = edit.getRange().getEnd().getCharacter();
    lines[line] = lines[line].substring(0, start) + edit.getNewText() + lines[line].substring(end);
    return String.join("\n", lines);
  }

  @Test
  void testGlobalTypeCompletionReplacesPackagePrefix() {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    definitionKeeper.add(
        new ExemplarDefinition(
            null,
            null,
            null,
            null,
            null,
            ExemplarDefinition.Sort.SLOTTED,
            TypeString.ofIdentifier("rope", "sw"),
            null));
    final String code =
        """
        _method a.b
            sw:rop
        _endmethod""";
    final int col = "    sw:rop".length(); // cursor right after 'sw:rop'
    final Position position = new Position(1, col);
    final CompletionItem item =
        this.getCompletions(code, definitionKeeper, position).stream()
            .filter(i -> "sw:rope".equals(i.getLabel()))
            .findFirst()
            .orElseThrow();
    assertThat(this.applyEdit(item, code).split("\n")[1]).isEqualTo("    sw:rope");
  }

  @Test
  void testGlobalCompletionDeprecatedExemplar() {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    definitionKeeper.add(
        new ExemplarDefinition(
            null,
            null,
            null,
            null,
            null,
            ExemplarDefinition.Sort.SLOTTED,
            TypeString.ofIdentifier("rope", "sw"),
            new Pragma(null, Set.of(), Set.of("deprecated"), Set.of())));
    final String code =
        """
        _method a.b
            sw:rop
        _endmethod""";
    final Position position = new Position(1, "    sw:rop".length());
    final CompletionItem item =
        this.getCompletions(code, definitionKeeper, position).stream()
            .filter(i -> "sw:rope".equals(i.getLabel()))
            .findFirst()
            .orElseThrow();
    assertThat(item.getTags()).contains(CompletionItemTag.Deprecated);
  }

  @Test
  void testMethodCompletionDeprecated() {
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
            new Pragma(null, Set.of(), Set.of("deprecated"), Set.of()),
            ExpressionResultString.UNDEFINED,
            ExpressionResultString.EMPTY));
    final String code =
        """
        _method a.b
            1.
        _endmethod""";
    final Position position = new Position(1, 6); // After '.'.
    final CompletionItem item =
        this.getCompletions(code, definitionKeeper, position).stream()
            .filter(i -> "find_me()".equals(i.getLabel()))
            .findFirst()
            .orElseThrow();
    assertThat(item.getTags()).contains(CompletionItemTag.Deprecated);
  }
}
