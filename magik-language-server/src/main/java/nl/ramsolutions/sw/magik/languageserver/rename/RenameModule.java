package nl.ramsolutions.sw.magik.languageserver.rename;

import java.util.Optional;

/**
 * A single rename module. Detects whether it applies to a context and, if so, provides the {@link
 * Renamer} to use.
 */
interface RenameModule {

  /**
   * Try to provide a {@link Renamer} for the given context.
   *
   * @param context Rename context.
   * @return {@code Optional} containing the {@link Renamer} if this module claims the context;
   *     {@code Optional.empty()} if it does not apply.
   */
  Optional<Renamer> tryRenamer(RenameContext context);
}
