package nl.ramsolutions.sw.magik.languageserver.typehierarchy;

import java.util.List;
import java.util.Optional;
import nl.ramsolutions.sw.magik.analysis.definitions.ExemplarDefinition;

/**
 * A single type hierarchy module. Detects whether it applies to a context and, if so, provides the
 * {@link ExemplarDefinition} to build a type hierarchy item for.
 */
public interface TypeHierarchyModule {

  /**
   * Try to provide the exemplar definition for the given context.
   *
   * @param context Type hierarchy context.
   * @return {@code Optional.of} (possibly empty, meaning the definition could not be resolved) if
   *     this module claims the context; {@code Optional.empty()} if it does not apply.
   */
  Optional<List<ExemplarDefinition>> tryPrepareTypeHierarchy(TypeHierarchyContext context);
}
