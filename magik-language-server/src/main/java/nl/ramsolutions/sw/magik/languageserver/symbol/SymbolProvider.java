package nl.ramsolutions.sw.magik.languageserver.symbol;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import nl.ramsolutions.sw.magik.analysis.Location;
import nl.ramsolutions.sw.magik.analysis.typing.ITypeKeeper;
import nl.ramsolutions.sw.magik.analysis.typing.types.AbstractType;
import nl.ramsolutions.sw.magik.analysis.typing.types.Method;
import nl.ramsolutions.sw.magik.languageserver.Lsp4jConversion;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.SymbolKind;
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
     * Get symbols matching {{query}}.
     * @param query Query to match against.
     * @return {{SymbolInformation}}s with query results.
     */
    public List<SymbolInformation> getSymbols(final String query) {
        LOGGER.debug("Searching for: '{}'", query);

        if (query.trim().isEmpty()) {
            return Collections.emptyList();
        }

        final Predicate<AbstractType> typePredicate;
        final Predicate<Method> methodPredicate;
        final BiPredicate<AbstractType, Method> typeMethodPredicate;
        try {
            typePredicate = this.buildTypePredicate(query);
            methodPredicate = this.buildMethodPredicate(query);
            typeMethodPredicate = this.buildTypeMethodPredicate(query);
        } catch (PatternSyntaxException ex) {
            LOGGER.info("Ignoring caught exception: {}", ex.getMessage());
            return Collections.emptyList();
        }

        final List<SymbolInformation> symbolInformations = new ArrayList<>();
        for (final AbstractType type : this.typeKeeper.getTypes()) {
            if (typePredicate.test(type)) {
                final Location location = type.getLocation() != null
                    ? type.getLocation()
                    : DUMMY_LOCATION;
                final SymbolInformation information = new SymbolInformation(
                    "Exemplar: " + type.getFullName(),
                    SymbolKind.Class,
                    Lsp4jConversion.locationToLsp4j(location));
                symbolInformations.add(information);
            }

            for (final Method method : type.getLocalMethods()) {
                if (methodPredicate.test(method)) {
                    final Location location = method.getLocation() != null
                        ? method.getLocation()
                        : DUMMY_LOCATION;
                    final SymbolInformation information = new SymbolInformation(
                        "Method: " + method.getSignature(),
                        SymbolKind.Method,
                        Lsp4jConversion.locationToLsp4j(location));
                    symbolInformations.add(information);
                }

                if (typeMethodPredicate.test(type, method)) {
                    final Location location = method.getLocation() != null
                        ? method.getLocation()
                        : DUMMY_LOCATION;
                    final SymbolInformation information = new SymbolInformation(
                        "Method: " + method.getSignature(),
                        SymbolKind.Method,
                        Lsp4jConversion.locationToLsp4j(location));
                    symbolInformations.add(information);
                }
            }
        }

        LOGGER.debug("Finished searching for: '{}', result count: {}", query, symbolInformations.size());
        return symbolInformations;
    }

    /**
     * Build {{Predicate}} which matches {{AbstractType}}. This only gives a matchable
     * predicate if no '.' appears in the query.
     *
     * @param query Query string
     * @return Predicate to match {{AbstractType}}
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
     * Build {{Predicate}} which matches {{Method}}.
     * This only gives a matchable predicate if no '.' appears in the query.
     * @param query Query string
     * @return Predicate to match {{Method}}
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
     * Build {{BiPredicate}} which matches {{AbstractType}} and {{Method}}.
     * This only gives a matchable predicate if '.' appears in the query.
     * @param query Query string
     * @return BiPredicate to match {{AbstractType}} and {{Method}}
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

}
