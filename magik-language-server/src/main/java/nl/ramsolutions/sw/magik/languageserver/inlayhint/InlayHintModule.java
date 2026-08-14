package nl.ramsolutions.sw.magik.languageserver.inlayhint;

import java.util.stream.Stream;
import org.eclipse.lsp4j.InlayHint;

/**
 * A single inlay hint module. Contributes zero or more {@link InlayHint}s for a given context.
 * Unlike {@code HoverModule}/{@code DefinitionModule}/{@code ReferencesModule}, results from all
 * modules are combined, not just the first module's.
 */
public interface InlayHintModule {

  /**
   * Provide inlay hints for the given context.
   *
   * @param context Inlay hint context.
   * @return Inlay hints contributed by this module.
   */
  Stream<InlayHint> provideInlayHints(InlayHintContext context);
}
