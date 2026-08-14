package nl.ramsolutions.sw.magik.languageserver.symbol;

import java.util.stream.Stream;
import org.eclipse.lsp4j.WorkspaceSymbol;

/**
 * A single symbol module. Contributes zero or more {@link WorkspaceSymbol}s for a given context.
 * Like {@code InlayHintModule}, results from all modules are combined, not just the first module's.
 */
public interface SymbolModule {

  /**
   * Provide symbols for the given context.
   *
   * @param context Symbol context.
   * @return Symbols contributed by this module.
   */
  Stream<WorkspaceSymbol> provideSymbols(SymbolContext context);
}
