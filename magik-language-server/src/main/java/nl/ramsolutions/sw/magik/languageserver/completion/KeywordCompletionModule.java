package nl.ramsolutions.sw.magik.languageserver.completion;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import nl.ramsolutions.sw.magik.api.MagikKeyword;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionItemKind;

/** Completes Magik keywords when the current token starts with {@code '_'}. */
public class KeywordCompletionModule implements CompletionModule {

  @Override
  public Optional<List<CompletionItem>> tryComplete(final CompletionContext context) {
    if (!context.removedPart().startsWith("_")) {
      return Optional.empty();
    }
    return Optional.of(this.keywordItems());
  }

  private List<CompletionItem> keywordItems() {
    return Arrays.stream(MagikKeyword.values())
        .map(MagikKeyword::getValue)
        .map(
            value -> {
              final CompletionItem item = new CompletionItem(value);
              item.setKind(CompletionItemKind.Keyword);
              return item;
            })
        .toList();
  }
}
