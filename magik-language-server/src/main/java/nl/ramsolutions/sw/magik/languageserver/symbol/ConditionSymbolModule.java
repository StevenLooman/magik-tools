package nl.ramsolutions.sw.magik.languageserver.symbol;

import java.util.function.Predicate;
import java.util.stream.Stream;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.analysis.definitions.ConditionDefinition;
import nl.ramsolutions.sw.magik.languageserver.Lsp4jConversion;
import org.eclipse.lsp4j.SymbolKind;
import org.eclipse.lsp4j.WorkspaceSymbol;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

/** Provides symbols for matching conditions. */
public class ConditionSymbolModule implements SymbolModule {

  @Override
  public Stream<WorkspaceSymbol> provideSymbols(final SymbolContext context) {
    final Predicate<ConditionDefinition> predicate = this.buildConditionPredicate(context.query());
    return context.definitionKeeper().getConditionDefinitions().stream()
        .filter(predicate)
        .map(this::toSymbol);
  }

  private WorkspaceSymbol toSymbol(final ConditionDefinition definition) {
    final Location conditionLocation = definition.getLocation();
    final Location location = Location.validLocation(conditionLocation);
    return new WorkspaceSymbol(
        "Condition: " + definition.getName(),
        SymbolKind.Class,
        Either.forLeft(Lsp4jConversion.locationToLsp4j(location)));
  }

  /**
   * Build {@link Predicate} which matches {@link ConditionDefinition}. This only gives a matchable
   * predicate if no '.' appears in the query.
   *
   * @param query Query string
   * @return Predicate to match with.
   */
  private Predicate<ConditionDefinition> buildConditionPredicate(final String query) {
    final int dotIndex = query.indexOf('.');
    if (dotIndex != -1) {
      return type -> false;
    }

    return definition -> definition.getName().contains(query);
  }
}
