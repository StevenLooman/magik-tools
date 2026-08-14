package nl.ramsolutions.sw.magik.languageserver.references;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.analysis.definitions.ExemplarDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.GlobalUsage;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import nl.ramsolutions.sw.magik.analysis.typing.TypeStringResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Finds references to a type. Shared by {@link ExemplarNameReferencesModule} and {@link
 * AtomReferencesModule}, which both resolve to a type.
 */
final class TypeReferencesFinder {

  private static final Logger LOGGER = LoggerFactory.getLogger(TypeReferencesFinder.class);

  private TypeReferencesFinder() {}

  static List<Location> referencesToType(
      final IDefinitionKeeper definitionKeeper, final TypeString typeString) {
    LOGGER.debug("Finding references to type: {}", typeString);

    final TypeStringResolver resolver = new TypeStringResolver(definitionKeeper);
    final ExemplarDefinition exemplarDefinition = resolver.getExemplarDefinition(typeString);
    if (exemplarDefinition == null) {
      return Collections.emptyList();
    }

    // TODO: We need to resolve the referenced types, as the indexed globals might not have the
    // right (unresolved) package. I.e., We might need to match only on identifier, as the
    // usedGlobal might have a different package? This is because the ref might be stored with the
    // current package.
    final TypeString exemplarTypeString = exemplarDefinition.getTypeString();
    final Set<TypeString> searchedTypes = Set.of(exemplarTypeString);
    final Collection<GlobalUsage> wantedGlobalUsages =
        searchedTypes.stream()
            .map(wantedTypeRef -> new GlobalUsage(wantedTypeRef, null, null))
            .collect(Collectors.toSet());
    final Predicate<GlobalUsage> filterPredicate = wantedGlobalUsages::contains;

    // Find references.
    // TODO: Also parameters, return types of methods/procedures.
    // TODO: Also slots of methods.
    return Stream.of(
            definitionKeeper.getMethodDefinitions().stream()
                .flatMap(def -> def.getUsedGlobals().stream()),
            definitionKeeper.getProcedureDefinitions().stream()
                .flatMap(def -> def.getUsedGlobals().stream()))
        .flatMap(stream -> stream)
        .filter(filterPredicate::test)
        .map(GlobalUsage::getLocation)
        .map(Location::validLocation)
        .toList();
  }
}
