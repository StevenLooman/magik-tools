package nl.ramsolutions.sw.magik.languageserver.completion;

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
import nl.ramsolutions.sw.magik.analysis.typing.types.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.types.TypeString;
import nl.ramsolutions.sw.magik.api.MagikKeyword;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionItemKind;
import org.eclipse.lsp4j.Position;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test CompletionProvider.
 */
@SuppressWarnings("checkstyle:MagicNumber")
class CompletionProviderTest {

    private List<CompletionItem> getCompletions(
            final String code,
            final IDefinitionKeeper definitionKeeper,
            final Position position) {
        final URI uri = URI.create("tests://unittest");
        final MagikTypedFile magikFile = new MagikTypedFile(uri, code, definitionKeeper);
        final CompletionProvider provider = new CompletionProvider();
        return provider.provideCompletions(magikFile, position);
    }

    private List<CompletionItem> getCompletions(final String code, final Position position) {
        final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
        return this.getCompletions(code, definitionKeeper, position);
    }

    @Test
    void testKeywordCompletion() {
        final String code = ""
            + "_method a.b\n"
            + "    _\n"
            + "_endmethod";
        final Position position = new Position(1, 5);    // On '_'.
        final List<CompletionItem> completions = this.getCompletions(code, position);

        assertThat(completions).hasSize(MagikKeyword.keywordValues().length);

        final CompletionItem item = completions.get(0);
        assertThat(item.getKind()).isEqualTo(CompletionItemKind.Keyword);
        assertThat(item.getLabel()).startsWith("_");
    }

    @Test
    void testMethodCompletionBare() {
        final String code = ""
            + "_method a.b\n"
            + "    1.\n"
            + "_endmethod";
        final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
        definitionKeeper.add(
            new MethodDefinition(
                null,
                null,
                null,
                null,
                TypeString.SW_INTEGER,
                "find_me()",
                Collections.emptySet(),
                Collections.emptyList(),
                null,
                ExpressionResultString.UNDEFINED,
                ExpressionResultString.EMPTY));
        final Position position = new Position(1, 6);    // On '.'.
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
        final String code = ""
            + "_method a.b\n"
            + "    _self.\n"
            + "_endmethod";
        final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
        final TypeString aRef = TypeString.ofIdentifier("a", "user");
        definitionKeeper.add(
            new ExemplarDefinition(
                null,
                null,
                null,
                null,
                ExemplarDefinition.Sort.SLOTTED,
                aRef,
                Collections.emptyList(),
                Collections.emptyList()));
        definitionKeeper.add(
            new MethodDefinition(
                null,
                null,
                null,
                null,
                aRef,
                "find_me()",
                Collections.emptySet(),
                Collections.emptyList(),
                null,
                ExpressionResultString.UNDEFINED,
                ExpressionResultString.EMPTY));
        final Position position = new Position(1, 10);    // On '.'.
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
        final String code = ""
            + "_method a.b\n"
            + "    1.fi\n"
            + "_endmethod";
        final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
        definitionKeeper.add(
            new MethodDefinition(
                null,
                null,
                null,
                null,
                TypeString.SW_INTEGER,
                "find_me()",
                Collections.emptySet(),
                Collections.emptyList(),
                null,
                ExpressionResultString.UNDEFINED,
                ExpressionResultString.EMPTY));
        final Position position = new Position(1, 8);    // On 'i'.
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
        final String code = ""
            + "_method a.b\n"
            + "    \n"
            + "_endmethod";
        final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
        final Position position = new Position(1, 2);    // On ''.
        final List<CompletionItem> completions = this.getCompletions(code, definitionKeeper, position);

        final Collection<ExemplarDefinition> defaultTypes = definitionKeeper.getExemplarDefinitions();
        assertThat(completions).hasSize(defaultTypes.size() + MagikKeyword.values().length);

        final Set<CompletionItemKind> itemKinds = completions.stream()
            .map(item -> item.getKind())
            .collect(Collectors.toSet());
        assertThat(itemKinds).containsExactlyInAnyOrder(
            CompletionItemKind.Class,
            CompletionItemKind.Keyword);
    }

    @Test
    void testGlobalCompletionVariable() {
        final String code = ""
            + "_method a.b\n"
            + "    _local x << 10\n"
            + "    \n"
            + "_endmethod";
        final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
        final Position position = new Position(2, 2);    // On ''.
        final List<CompletionItem> completions = this.getCompletions(code, definitionKeeper, position);

        final Collection<ExemplarDefinition> defaultTypes = definitionKeeper.getExemplarDefinitions();
        assertThat(completions).hasSize(
            defaultTypes.size()
            + MagikKeyword.values().length
            + 1);  // Local variable.

        final Set<CompletionItemKind> itemKinds = completions.stream()
            .map(item -> item.getKind())
            .collect(Collectors.toSet());
        assertThat(itemKinds).containsExactlyInAnyOrder(
            CompletionItemKind.Class,
            CompletionItemKind.Keyword,
            CompletionItemKind.Variable);
    }

    @Test
    void testGlobalCompletionSlot() {
        final String code = ""
            + "_method a.b\n"
            + "    \n"
            + "_endmethod";
        final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
        final TypeString aRef = TypeString.ofIdentifier("a", "user");
        definitionKeeper.add(
            new ExemplarDefinition(
                null,
                null,
                null,
                null,
                ExemplarDefinition.Sort.SLOTTED,
                aRef,
                List.of(
                    new SlotDefinition(
                        null,
                        code,
                        code,
                        null,
                        code,
                        aRef)),
                Collections.emptyList()));

        final Position position = new Position(1, 2);    // On ''.
        final List<CompletionItem> completions = this.getCompletions(code, definitionKeeper, position);

        final Collection<ExemplarDefinition> defaultTypes = definitionKeeper.getExemplarDefinitions();
        assertThat(completions).hasSize(
            defaultTypes.size()
            + MagikKeyword.values().length
            + 1);  // Slot.

        final Set<CompletionItemKind> itemKinds = completions.stream()
            .map(item -> item.getKind())
            .collect(Collectors.toSet());
        assertThat(itemKinds).containsExactlyInAnyOrder(
            CompletionItemKind.Class,
            CompletionItemKind.Keyword,
            CompletionItemKind.Property);
    }

    @Test
    void testNoCompletionInComment() {
        final String code = ""
            + "abc # ";
        final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
        final Position position = new Position(0, 5);    // On ' ', in comment.
        final List<CompletionItem> completions = this.getCompletions(code, definitionKeeper, position);
        assertThat(completions).isEmpty();
    }

}
