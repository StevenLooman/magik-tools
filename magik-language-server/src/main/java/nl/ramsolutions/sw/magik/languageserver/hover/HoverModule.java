package nl.ramsolutions.sw.magik.languageserver.hover;

import java.util.Optional;
import nl.ramsolutions.sw.OpenedFile;

/**
 * A single hover module. Detects whether it applies to a context and, if so, provides its hover
 * text.
 *
 * @param <T> Type of {@link OpenedFile} this module hovers on.
 */
public interface HoverModule<T extends OpenedFile> {

  /**
   * Try to provide hover text for the given context.
   *
   * @param context Hover context.
   * @return {@code Optional.of} (possibly empty text) if this module claims the context; {@code
   *     Optional.empty()} if it does not apply.
   */
  Optional<String> tryHover(HoverContext<T> context);
}
