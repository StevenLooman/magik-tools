package nl.ramsolutions.sw.magik.languageserver.completion;

import java.util.List;
import java.util.Optional;
import org.eclipse.lsp4j.CompletionItem;

/**
 * A single completion module. Detects whether it applies to a context and, if so, provides its
 * completion items.
 */
public interface CompletionModule {

  /**
   * Try to provide completions for the given context.
   *
   * @param context Completion context.
   * @return {@code Optional.of} (possibly empty) if this module claims the context; {@code
   *     Optional.empty()} if it does not apply.
   */
  Optional<List<CompletionItem>> tryComplete(CompletionContext context);

  /**
   * The characters that should trigger this module's completion. The language server registers the
   * union of all modules' trigger characters as the completion trigger characters.
   *
   * @return Trigger characters (empty by default).
   */
  default List<String> getTriggerCharacters() {
    return List.of();
  }
}
