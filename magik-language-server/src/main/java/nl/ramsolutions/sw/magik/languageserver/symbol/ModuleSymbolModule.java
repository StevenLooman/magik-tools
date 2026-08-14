package nl.ramsolutions.sw.magik.languageserver.symbol;

import java.util.stream.Stream;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.languageserver.Lsp4jConversion;
import nl.ramsolutions.sw.moduledef.ModuleDefinition;
import org.eclipse.lsp4j.SymbolKind;
import org.eclipse.lsp4j.WorkspaceSymbol;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

/** Provides symbols for matching modules. */
public class ModuleSymbolModule implements SymbolModule {

  @Override
  public Stream<WorkspaceSymbol> provideSymbols(final SymbolContext context) {
    final String query = context.query();
    return context.definitionKeeper().getModuleDefinitions().stream()
        .filter(definition -> definition.getName().contains(query))
        .map(this::toSymbol);
  }

  private WorkspaceSymbol toSymbol(final ModuleDefinition definition) {
    final Location moduleLocation = definition.getLocation();
    final Location location = Location.validLocation(moduleLocation);
    return new WorkspaceSymbol(
        "Module: " + definition.getName(),
        SymbolKind.Module,
        Either.forLeft(Lsp4jConversion.locationToLsp4j(location)));
  }
}
