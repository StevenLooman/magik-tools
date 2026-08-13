package nl.ramsolutions.sw.magik.languageserver.references;

import java.util.List;
import java.util.Optional;
import nl.ramsolutions.sw.OpenedFile;
import nl.ramsolutions.sw.magik.Location;

/**
 * A single references module. Detects whether it applies to a context and, if so, provides its
 * reference locations.
 *
 * @param <T> Type of {@link OpenedFile} this module provides references for.
 */
public interface ReferencesModule<T extends OpenedFile> {

  /**
   * Try to provide references for the given context.
   *
   * @param context References context.
   * @return {@code Optional.of} (possibly empty locations) if this module claims the context;
   *     {@code Optional.empty()} if it does not apply.
   */
  Optional<List<Location>> tryReferences(ReferencesContext<T> context);
}
