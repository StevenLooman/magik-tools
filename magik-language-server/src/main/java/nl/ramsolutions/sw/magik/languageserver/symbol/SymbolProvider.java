package nl.ramsolutions.sw.magik.languageserver.symbol;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.analysis.definitions.ConditionDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.ExemplarDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.GlobalDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.ITypeStringDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.ProcedureDefinition;
import nl.ramsolutions.sw.magik.languageserver.Lsp4jConversion;
import nl.ramsolutions.sw.moduledef.ModuleDefinition;
import nl.ramsolutions.sw.productdef.ProductDefinition;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.SymbolKind;
import org.eclipse.lsp4j.WorkspaceSymbol;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Symbol provider. */
public class SymbolProvider {

  private static final Logger LOGGER = LoggerFactory.getLogger(SymbolProvider.class);

  private final IDefinitionKeeper definitionKeeper;

  public SymbolProvider(final IDefinitionKeeper definitionKeeper) {
    this.definitionKeeper = definitionKeeper;
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

    final List<WorkspaceSymbol> workspaceSymbols = new ArrayList<>();
    try {
      this.gatherProducts(query, workspaceSymbols);
      this.gatherModules(query, workspaceSymbols);
      this.gatherTypes(query, workspaceSymbols);
      this.gatherMethods(query, workspaceSymbols);
      this.gatherConditions(query, workspaceSymbols);
    } catch (final PatternSyntaxException exception) {
      LOGGER.info("Ignoring caught exception: {}", exception.getMessage());
      return Collections.emptyList();
    }

    LOGGER.debug("Finished searching for: '{}', result count: {}", query, workspaceSymbols.size());
    return workspaceSymbols;
  }

  private void gatherProducts(final String query, final List<WorkspaceSymbol> workspaceSymbols) {
    final Pattern pattern = Pattern.compile(".*" + query + ".*");
    final Predicate<ProductDefinition> predicate =
        definition -> pattern.matcher(definition.getName()).matches();
    for (final ProductDefinition definition : this.definitionKeeper.getProductDefinitions()) {
      if (predicate.test(definition)) {
        final Location conditionLocation = definition.getLocation();
        final Location location = Location.validLocation(conditionLocation);
        final WorkspaceSymbol symbol =
            new WorkspaceSymbol(
                "Product: " + definition.getName(),
                SymbolKind.Package,
                Either.forLeft(Lsp4jConversion.locationToLsp4j(location)));
        workspaceSymbols.add(symbol);
      }
    }
  }

  private void gatherModules(final String query, final List<WorkspaceSymbol> workspaceSymbols) {
    final Pattern pattern = Pattern.compile(".*" + query + ".*");
    final Predicate<ModuleDefinition> predicate =
        definition -> pattern.matcher(definition.getName()).matches();
    for (final ModuleDefinition definition : this.definitionKeeper.getModuleDefinitions()) {
      if (predicate.test(definition)) {
        final Location conditionLocation = definition.getLocation();
        final Location location = Location.validLocation(conditionLocation);
        final WorkspaceSymbol symbol =
            new WorkspaceSymbol(
                "Module: " + definition.getName(),
                SymbolKind.Module,
                Either.forLeft(Lsp4jConversion.locationToLsp4j(location)));
        workspaceSymbols.add(symbol);
      }
    }
  }

  private void gatherTypes(final String query, final List<WorkspaceSymbol> workspaceSymbols) {
    final Predicate<ITypeStringDefinition> predicate = this.buildTypePredicate(query);
    for (final ExemplarDefinition definition : this.definitionKeeper.getExemplarDefinitions()) {
      if (predicate.test(definition)) {
        final Location typeLocation = definition.getLocation();
        final Location location = Location.validLocation(typeLocation);
        final WorkspaceSymbol symbol =
            new WorkspaceSymbol(
                "Exemplar: " + definition.getTypeString().getFullString(),
                SymbolKind.Class,
                Either.forLeft(Lsp4jConversion.locationToLsp4j(location)));
        workspaceSymbols.add(symbol);
      }
    }
  }

  private void gatherMethods(final String query, final List<WorkspaceSymbol> workspaceSymbols) {
    final Predicate<MethodDefinition> predicate = this.buildMethodPredicate(query);
    for (final MethodDefinition definition : this.definitionKeeper.getMethodDefinitions()) {
      if (predicate.test(definition)) {
        final Location methodLocation = definition.getLocation();
        final Location location = Location.validLocation(methodLocation);
        final WorkspaceSymbol symbol =
            new WorkspaceSymbol(
                "Method: " + definition.getName(),
                SymbolKind.Method,
                Either.forLeft(Lsp4jConversion.locationToLsp4j(location)));
        workspaceSymbols.add(symbol);
      }
    }
  }

  /**
   * Gather {{@link WorkspaceSymbolsymbol}} for matching conditions.
   *
   * @param query Query to run.
   * @param workspaceSymbols List to add results to.
   */
  private void gatherConditions(final String query, final List<WorkspaceSymbol> workspaceSymbols) {
    final Predicate<ConditionDefinition> predicate = this.buildConditionPredicate(query);
    for (final ConditionDefinition definition : this.definitionKeeper.getConditionDefinitions()) {
      if (predicate.test(definition)) {
        final Location conditionLocation = definition.getLocation();
        final Location location = Location.validLocation(conditionLocation);
        final WorkspaceSymbol symbol =
            new WorkspaceSymbol(
                "Condition: " + definition.getName(),
                SymbolKind.Class,
                Either.forLeft(Lsp4jConversion.locationToLsp4j(location)));
        workspaceSymbols.add(symbol);
      }
    }
  }

  /**
   * Build {@link Predicate} which matches {@link ExemplarDefinition}/{@link ProcedureDefinition}/
   * {@link GlobalDefinition}.
   *
   * <p>This only gives a matchable * predicate if no '.' appears in the query.
   *
   * @param query Query string
   * @return Predicate to match with.
   */
  private Predicate<ITypeStringDefinition> buildTypePredicate(final String query) {
    final int dotIndex = query.indexOf('.');
    if (dotIndex != -1) {
      return type -> false;
    }

    final Pattern pattern = Pattern.compile(".*" + query + ".*");
    return definition -> pattern.matcher(definition.getTypeString().getFullString()).matches();
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
      final Pattern pattern = Pattern.compile(".*" + query + ".*");
      return definition -> pattern.matcher(definition.getMethodName()).matches();
    }

    final String typeQuery = query.substring(0, dotIndex);
    LOGGER.trace("Type query: {}", typeQuery);
    final Pattern typePattern = Pattern.compile(".*" + Pattern.quote(typeQuery) + ".*");

    final String methodQuery = query.substring(dotIndex + 1);
    LOGGER.trace("Method query: {}", methodQuery);
    final Pattern methodPattern = Pattern.compile(".*" + Pattern.quote(methodQuery) + ".*");

    return definition ->
        typePattern.matcher(definition.getTypeName().getFullString()).matches()
            && methodPattern.matcher(definition.getMethodName()).matches();
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

    final Pattern pattern = Pattern.compile(".*" + query + ".*");
    return definition -> pattern.matcher(definition.getName()).matches();
  }
}
