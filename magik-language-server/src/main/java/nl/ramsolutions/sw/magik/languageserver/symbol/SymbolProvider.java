package nl.ramsolutions.sw.magik.languageserver.symbol;

import java.util.Collections;
import java.util.List;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.WorkspaceSymbol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Symbol provider. Runs a set of {@link SymbolModule}s and combines all of their results, like
 * {@code InlayHintProvider}, rather than using only the first module that claims the context.
 */
public class SymbolProvider {

  private static final Logger LOGGER = LoggerFactory.getLogger(SymbolProvider.class);

  private static final int MAX_RESULTS = 200;

  private final IDefinitionKeeper definitionKeeper;
  private final List<SymbolModule> modules;

  public SymbolProvider(final IDefinitionKeeper definitionKeeper) {
    this.definitionKeeper = definitionKeeper;
    this.modules = this.createModules();
  }

  /**
   * Create the ordered modules. Override to add or remove symbol modules.
   *
   * <p>Gatherer order (products, modules, types, methods, conditions) determines which results
   * survive the shared {@link #MAX_RESULTS} cap applied once the modules' results are combined.
   *
   * @return Ordered modules.
   */
  protected List<SymbolModule> createModules() {
    return List.of(
        new ProductSymbolModule(),
        new ModuleSymbolModule(),
        new TypeSymbolModule(),
        new MethodSymbolModule(),
        new ConditionSymbolModule());
  }

  /**
   * Set server capabilities.
   *
   * @param capabilities Server capabilities.
   */
  public void setCapabilities(final ServerCapabilities capabilities) {
    capabilities.setWorkspaceSymbolProvider(true);
  }

  /**
   * Get symbols matching {@code query}.
   *
   * @param query Query to match against.
   * @return {@link WorkspaceSymbol}s with query results.
   */
  public List<WorkspaceSymbol> getSymbols(final String query) {
    LOGGER.debug("Searching for: '{}'", query);

    if (query.trim().isEmpty()) {
      return Collections.emptyList();
    }

    final SymbolContext context = new SymbolContext(this.definitionKeeper, query);
    final List<WorkspaceSymbol> workspaceSymbols =
        this.modules.stream()
            .flatMap(module -> module.provideSymbols(context))
            .limit(MAX_RESULTS)
            .toList();

    LOGGER.debug("Finished searching for: '{}', result count: {}", query, workspaceSymbols.size());
    return workspaceSymbols;
  }
}
