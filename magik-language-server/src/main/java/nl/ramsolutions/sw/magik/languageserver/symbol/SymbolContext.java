package nl.ramsolutions.sw.magik.languageserver.symbol;

import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;

/**
 * Shared context passed to each {@link SymbolModule}.
 *
 * @param definitionKeeper Definition keeper to search.
 * @param query Query to match against.
 */
public record SymbolContext(IDefinitionKeeper definitionKeeper, String query) {}
