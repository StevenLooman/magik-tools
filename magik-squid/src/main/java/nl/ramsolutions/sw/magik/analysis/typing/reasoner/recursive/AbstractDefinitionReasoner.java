package nl.ramsolutions.sw.magik.analysis.typing.reasoner.recursive;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import nl.ramsolutions.sw.FileCharsetDeterminer;
import nl.ramsolutions.sw.MagikToolsProperties;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.analysis.MagikAnalysisSettings;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.MagikDefinition;

/** Abstract recursive reasoner. */
public abstract class AbstractDefinitionReasoner {

  protected final IDefinitionKeeper definitionKeeper;
  protected final MagikDefinition originalDefinition;

  private static final MagikToolsProperties MAGIK_FILE_PROPERTIES =
      new MagikToolsProperties(
          Map.of(
              MagikAnalysisSettings.INDEX_GLOBAL_USAGES, "true",
              MagikAnalysisSettings.INDEX_METHOD_USAGES, "true",
              MagikAnalysisSettings.INDEX_SLOT_USAGES, "true",
              MagikAnalysisSettings.INDEX_CONDITION_USAGES, "true"));

  /**
   * Constructor.
   *
   * @param definitionKeeper The definition keeper to use.
   * @param originalDefinition The definition to reason about.
   */
  protected AbstractDefinitionReasoner(
      final IDefinitionKeeper definitionKeeper, final MagikDefinition originalDefinition) {
    this.definitionKeeper = definitionKeeper;
    this.originalDefinition = originalDefinition;
  }

  /**
   * Get the original definition.
   *
   * @return The original definition.
   */
  public MagikDefinition getOriginalDefinition() {
    return this.originalDefinition;
  }

  /**
   * Check if the definition is reasonable.
   *
   * @return True if the definition is reasonable.
   */
  boolean isReasonable() {
    return this.originalDefinition.getLocation() != null;
  }

  /**
   * Process (reason) the definition, and return any definitions that are needed to completely
   * reason our definition.
   *
   * @return The definitions that are required to completely reason the definition.
   */
  abstract Collection<MagikDefinition> process();

  /**
   * Get the updated definition, after processing of self and any required definitions return by
   * process().
   *
   * @return The updated definition.
   */
  abstract MagikDefinition getUpdatedDefinition();

  /**
   * Check if the definition is reasoned (enough) for the required purpose.
   *
   * @return True if the definition is reasoned enough.
   */
  abstract boolean needsFurtherReasoning(final MagikDefinition definition);

  /**
   * Update the existing definition, replacing the original definition.
   *
   * @param definition The definition to store.
   */
  abstract void updateDefinition(final MagikDefinition definition);

  protected MagikTypedFile getMagikFile() {
    final Location location = this.originalDefinition.getLocation();
    final URI uri = location.getUri();
    final Path path = Path.of(uri);
    final Charset charset = FileCharsetDeterminer.determineCharset(path);
    final String text;
    try {
      text = Files.readString(path, charset);
    } catch (final IOException exception) {
      throw new IllegalStateException(exception);
    }
    return new MagikTypedFile(
        AbstractDefinitionReasoner.MAGIK_FILE_PROPERTIES, uri, text, this.definitionKeeper);
  }
}
