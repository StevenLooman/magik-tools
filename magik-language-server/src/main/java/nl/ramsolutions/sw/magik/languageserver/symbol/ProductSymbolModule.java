package nl.ramsolutions.sw.magik.languageserver.symbol;

import java.util.stream.Stream;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.languageserver.Lsp4jConversion;
import nl.ramsolutions.sw.productdef.ProductDefinition;
import org.eclipse.lsp4j.SymbolKind;
import org.eclipse.lsp4j.WorkspaceSymbol;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

/** Provides symbols for matching products. */
public class ProductSymbolModule implements SymbolModule {

  @Override
  public Stream<WorkspaceSymbol> provideSymbols(final SymbolContext context) {
    final String query = context.query();
    return context.definitionKeeper().getProductDefinitions().stream()
        .filter(definition -> definition.getName().contains(query))
        .map(this::toSymbol);
  }

  private WorkspaceSymbol toSymbol(final ProductDefinition definition) {
    final Location productLocation = definition.getLocation();
    final Location location = Location.validLocation(productLocation);
    return new WorkspaceSymbol(
        "Product: " + definition.getName(),
        SymbolKind.Package,
        Either.forLeft(Lsp4jConversion.locationToLsp4j(location)));
  }
}
