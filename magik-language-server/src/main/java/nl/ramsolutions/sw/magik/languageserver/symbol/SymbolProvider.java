package nl.ramsolutions.sw.magik.languageserver.symbol;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import nl.ramsolutions.sw.magik.analysis.Location;
import nl.ramsolutions.sw.magik.analysis.typing.ITypeKeeper;
import nl.ramsolutions.sw.magik.analysis.typing.types.AbstractType;
import nl.ramsolutions.sw.magik.analysis.typing.types.Condition;
import nl.ramsolutions.sw.magik.analysis.typing.types.Method;
import nl.ramsolutions.sw.magik.languageserver.Lsp4jConversion;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.SymbolKind;
import org.eclipse.lsp4j.WorkspaceSymbol;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Symbol provider.
 */
public class SymbolProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(SymbolProvider.class);
    private static final Location DUMMY_LOCATION = new Location(URI.create("file:///"));

    private final ITypeKeeper typeKeeper;

    public SymbolProvider(final ITypeKeeper typeKeeper) {
        this.typeKeeper = typeKeeper;
    }

    /**
     * Set server capabilities.
     * @param capabilities Server capabilities.
     */
    public void setCapabilities(final ServerCapabilities capabilities) {
        capabilities.setWorkspaceSymbolProvider(true);
    }

    /**
     * Get symbols matching {@code query}.
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
            this.gatherTypes(query, workspaceSymbols);
            this.gatherMethods(query, workspaceSymbols);
            this.gatherConditions(query, workspaceSymbols);
        } catch (PatternSyntaxException ex) {
            LOGGER.info("Ignoring caught exception: {}", ex.getMessage());
            return Collections.emptyList();
        }

        LOGGER.debug("Finished searching for: '{}', result count: {}", query, workspaceSymbols.size());
        return workspaceSymbols;
    }

    private void gatherTypes(final String query, final List<WorkspaceSymbol> workspaceSymbols) {
        final Predicate<AbstractType> typePredicate = this.buildTypePredicate(query);
        for (final AbstractType type : this.typeKeeper.getTypes()) {
            if (typePredicate.test(type)) {
                final Location location = Objects.requireNonNullElse(type.getLocation(), DUMMY_LOCATION);
                final WorkspaceSymbol symbol = new WorkspaceSymbol(
                    "Exemplar: " + type.getFullName(),
                    SymbolKind.Class,
                    Either.forLeft(Lsp4jConversion.locationToLsp4j(location)));
                workspaceSymbols.add(symbol);
            }
        }
    }

    private void gatherMethods(final String query, final List<WorkspaceSymbol> workspaceSymbols) {
        final Predicate<Method> methodPredicate = this.buildMethodPredicate(query);
        final BiPredicate<AbstractType, Method> typeMethodPredicate = this.buildTypeMethodPredicate(query);
        for (final AbstractType type : this.typeKeeper.getTypes()) {
            for (final Method method : type.getLocalMethods()) {
                if (methodPredicate.test(method)) {
                    final Location location = Objects.requireNonNullElse(method.getLocation(), DUMMY_LOCATION);
                    final WorkspaceSymbol symbol = new WorkspaceSymbol(
                        "Method: " + method.getSignature(),
                        SymbolKind.Method,
                        Either.forLeft(Lsp4jConversion.locationToLsp4j(location)));
                    workspaceSymbols.add(symbol);
                }

                if (typeMethodPredicate.test(type, method)) {
                    final Location location = Objects.requireNonNullElse(method.getLocation(), DUMMY_LOCATION);
                    final WorkspaceSymbol symbol = new WorkspaceSymbol(
                        "Method: " + method.getSignature(),
                        SymbolKind.Method,
                        Either.forLeft(Lsp4jConversion.locationToLsp4j(location)));
                    workspaceSymbols.add(symbol);
                }
            }
        }
    }

    /**
     * Gather {{@link WorkspaceSymbolsymbol}} for matching conditions.
     * @param query Query to run.
     * @param workspaceSymbols List to add results to.
     */
    private void gatherConditions(final String query, final List<WorkspaceSymbol> workspaceSymbols) {
        final Predicate<Condition> conditionPredicate = this.buildConditionPredicate(query);
        for (final Condition condition : this.typeKeeper.getConditions()) {
            if (conditionPredicate.test(condition)) {
                final Location location = Objects.requireNonNullElse(condition.getLocation(), DUMMY_LOCATION);
                final WorkspaceSymbol symbol = new WorkspaceSymbol(
                    "Condition: " + condition.getName(),
                    SymbolKind.Class,
                    Either.forLeft(Lsp4jConversion.locationToLsp4j(location)));
                workspaceSymbols.add(symbol);
            }
        }
    }

    /**
     * Build {@link Predicate} which matches {@link AbstractType}. This only gives a matchable
     * predicate if no '.' appears in the query.
     *
     * @param query Query string
     * @return Predicate to match {@link AbstractType}.
     */
    private Predicate<AbstractType> buildTypePredicate(final String query) {
        final int dotIndex = query.indexOf('.');
        if (dotIndex != -1) {
            return type -> false;
        }

        final String regexp = ".*" + query + ".*";
        return type -> type.getFullName().matches(regexp);
    }

    /**
     * Build {@link Predicate} which matches {@link Method}.
     * This only gives a matchable predicate if no '.' appears in the query.
     * @param query Query string
     * @return Predicate to match {@link Method}
     */
    private Predicate<Method> buildMethodPredicate(final String query) {
        final int dotIndex = query.indexOf('.');
        if (dotIndex != -1) {
            return method -> false;
        }

        final String regexp = ".*" + query + ".*";
        return method -> method.getName().matches(regexp);
    }

    /**
     * Build {@link BiPredicate} which matches {@link AbstractType} and {@link Method}.
     * This only gives a matchable predicate if '.' appears in the query.
     * @param query Query string
     * @return {@link BiPredicate} to match {@link AbstractType} and {@link Method}
     */
    private BiPredicate<AbstractType, Method> buildTypeMethodPredicate(final String query) {
        final int dotIndex = query.indexOf('.');
        if (dotIndex == -1) {
            // Only match if query contains a '.'.
            return (type, method) -> false;
        }

        final String typeQuery = query.substring(0, dotIndex);
        LOGGER.trace("Type query: {}", typeQuery);
        final String typeRegex = ".*" + Pattern.quote(typeQuery) + ".*";
        final Predicate<AbstractType> typePredicate = type -> type.getFullName().matches(typeRegex);

        final String methodQuery = query.substring(dotIndex + 1);
        final String methodRegexp = ".*" + Pattern.quote(methodQuery) + ".*";
        LOGGER.trace("Method query: {}", methodQuery);
        final Predicate<Method> methodPredicate = method -> method.getName().matches(methodRegexp);

        return (type, method) -> typePredicate.test(type)
            && methodPredicate.test(method);
    }

    /**
     * Build {@link Predicate} which matches {@link Condition}. This only gives a matchable
     * predicate if no '.' appears in the query.
     *
     * @param query Query string
     * @return {@link Predicate} to match {@link Condition}
     */
    private Predicate<Condition> buildConditionPredicate(final String query) {
        final int dotIndex = query.indexOf('.');
        if (dotIndex != -1) {
            return type -> false;
        }

        final String regexp = ".*" + query + ".*";
        return condition -> condition.getName().matches(regexp);
    }

}
