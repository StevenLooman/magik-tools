package nl.ramsolutions.sw.magik.languageserver.symbol;

import java.util.function.Predicate;
import java.util.stream.Stream;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodDefinition;
import nl.ramsolutions.sw.magik.languageserver.Lsp4jConversion;
import org.eclipse.lsp4j.SymbolKind;
import org.eclipse.lsp4j.WorkspaceSymbol;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Provides symbols for matching methods. */
public class MethodSymbolModule implements SymbolModule {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodSymbolModule.class);

  @Override
  public Stream<WorkspaceSymbol> provideSymbols(final SymbolContext context) {
    final Predicate<MethodDefinition> predicate = this.buildMethodPredicate(context.query());
    return context.definitionKeeper().getMethodDefinitions().stream()
        .filter(predicate)
        .map(this::toSymbol);
  }

  private WorkspaceSymbol toSymbol(final MethodDefinition definition) {
    final Location methodLocation = definition.getLocation();
    final Location location = Location.validLocation(methodLocation);
    return new WorkspaceSymbol(
        "Method: " + definition.getName(),
        SymbolKind.Method,
        Either.forLeft(Lsp4jConversion.locationToLsp4j(location)));
  }

  /**
   * Build {@link Predicate} which matches {@link MethodDefinition}. This only gives a matchable
   * predicate if '.' appears in the query.
   *
   * @param query Query string
   * @return Predicate to match with.
   */
  private Predicate<MethodDefinition> buildMethodPredicate(final String query) {
    final int dotIndex = query.indexOf('.');
    if (dotIndex == -1) {
      // No `.`, match only based on method name.
      return definition -> definition.getMethodName().contains(query);
    }

    final String typeQuery = query.substring(0, dotIndex);
    LOGGER.trace("Type query: {}", typeQuery);

    final String methodQuery = query.substring(dotIndex + 1);
    LOGGER.trace("Method query: {}", methodQuery);

    return definition ->
        definition.getTypeName().getFullString().contains(typeQuery)
            && definition.getMethodName().contains(methodQuery);
  }
}
