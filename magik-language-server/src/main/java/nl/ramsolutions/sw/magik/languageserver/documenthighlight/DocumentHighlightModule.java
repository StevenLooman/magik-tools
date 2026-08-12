package nl.ramsolutions.sw.magik.languageserver.documenthighlight;

import java.util.List;
import java.util.Optional;
import org.eclipse.lsp4j.DocumentHighlight;

/**
 * A single document highlight module. Detects whether it applies to a context and, if so, provides
 * its document highlights.
 */
public interface DocumentHighlightModule {

  /**
   * Try to provide document highlights for the given context.
   *
   * @param context Document highlight context.
   * @return {@code Optional.of} (possibly empty highlights) if this module claims the context;
   *     {@code Optional.empty()} if it does not apply.
   */
  Optional<List<DocumentHighlight>> tryHighlights(DocumentHighlightContext context);
}
