package nl.ramsolutions.sw.magik.languageserver.completion;

import edu.umd.cs.findbugs.annotations.Nullable;
import java.util.List;
import nl.ramsolutions.sw.magik.Range;
import nl.ramsolutions.sw.magik.TextEdit;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.languageserver.Lsp4jConversion;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionItemKind;
import org.eclipse.lsp4j.CompletionItemTag;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

/** Builds exemplar-name completion items shared by completion modules. */
public final class ExemplarCompletionItems {

  private static final String TOPIC_DEPRECATED = "deprecated";

  private ExemplarCompletionItems() {}

  /**
   * Build exemplar-name completion items filtered by a substring.
   *
   * @param keeper Definition keeper.
   * @param filterPart Substring the full type name must contain (may be empty).
   * @param replaceRange Range the completion should replace, or {@code null} to leave it to the
   *     client's default (the item then only carries {@code insertText}).
   * @return Completion items (kind {@code Class}).
   */
  public static List<CompletionItem> build(
      final IDefinitionKeeper keeper, final String filterPart, final @Nullable Range replaceRange) {
    return keeper.getExemplarDefinitions().stream()
        .filter(exemplarDef -> exemplarDef.getTypeString().getFullString().contains(filterPart))
        .map(
            exemplarDef -> {
              final String name = exemplarDef.getTypeString().getFullString();
              final CompletionItem item = new CompletionItem(name);
              item.setInsertText(name);
              if (replaceRange != null) {
                item.setTextEdit(
                    Either.forLeft(
                        Lsp4jConversion.textEditToLsp4j(new TextEdit(replaceRange, name))));
              }
              item.setDetail(name);
              item.setDocumentation(exemplarDef.getDoc());
              item.setKind(CompletionItemKind.Class);
              if (exemplarDef.getPragma() != null
                  && exemplarDef.getPragma().getTopics().contains(TOPIC_DEPRECATED)) {
                item.setTags(List.of(CompletionItemTag.Deprecated));
              }
              return item;
            })
        .toList();
  }
}
