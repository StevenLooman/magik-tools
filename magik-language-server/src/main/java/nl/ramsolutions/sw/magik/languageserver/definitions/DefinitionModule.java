package nl.ramsolutions.sw.magik.languageserver.definitions;

import java.util.List;
import java.util.Optional;
import nl.ramsolutions.sw.OpenedFile;
import nl.ramsolutions.sw.magik.Location;

/**
 * A single definitions module. Detects whether it applies to a context and, if so, provides its
 * definition locations.
 *
 * @param <T> Type of {@link OpenedFile} this module provides definitions for.
 */
public interface DefinitionModule<T extends OpenedFile> {

  /**
   * Try to provide definitions for the given context.
   *
   * @param context Definitions context.
   * @return {@code Optional.of} (possibly empty locations) if this module claims the context;
   *     {@code Optional.empty()} if it does not apply.
   */
  Optional<List<Location>> tryDefinitions(DefinitionContext<T> context);
}
