package nl.ramsolutions.sw.magik.languageserver.symbol;

import java.util.function.Predicate;
import java.util.stream.Stream;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.analysis.definitions.ExemplarDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.GlobalDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.ITypeStringDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.ProcedureDefinition;
import nl.ramsolutions.sw.magik.languageserver.Lsp4jConversion;
import org.eclipse.lsp4j.SymbolKind;
import org.eclipse.lsp4j.WorkspaceSymbol;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

/** Provides symbols for matching types. */
public class TypeSymbolModule implements SymbolModule {

  @Override
  public Stream<WorkspaceSymbol> provideSymbols(final SymbolContext context) {
    final Predicate<ITypeStringDefinition> predicate = this.buildTypePredicate(context.query());
    return context.definitionKeeper().getExemplarDefinitions().stream()
        .filter(predicate)
        .map(this::toSymbol);
  }

  private WorkspaceSymbol toSymbol(final ExemplarDefinition definition) {
    final Location typeLocation = definition.getLocation();
    final Location location = Location.validLocation(typeLocation);
    return new WorkspaceSymbol(
        "Exemplar: " + definition.getTypeString().getFullString(),
        SymbolKind.Class,
        Either.forLeft(Lsp4jConversion.locationToLsp4j(location)));
  }

  /**
   * Build {@link Predicate} which matches {@link ExemplarDefinition}/{@link ProcedureDefinition}/
   * {@link GlobalDefinition}.
   *
   * <p>This only gives a matchable predicate if no '.' appears in the query.
   *
   * @param query Query string
   * @return Predicate to match with.
   */
  private Predicate<ITypeStringDefinition> buildTypePredicate(final String query) {
    final int dotIndex = query.indexOf('.');
    if (dotIndex != -1) {
      return type -> false;
    }

    return definition -> definition.getTypeString().getFullString().contains(query);
  }
}
