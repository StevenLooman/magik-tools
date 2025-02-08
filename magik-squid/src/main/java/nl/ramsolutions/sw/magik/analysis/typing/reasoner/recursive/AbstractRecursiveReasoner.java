package nl.ramsolutions.sw.magik.analysis.typing.reasoner.recursive;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import nl.ramsolutions.sw.FileCharsetDeterminer;
import nl.ramsolutions.sw.MagikToolsProperties;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.analysis.MagikAnalysisSettings;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;

/*
 * Abstract recursive reasoner.
 */
public abstract class AbstractRecursiveReasoner {

  protected final IDefinitionKeeper definitionKeeper;
  protected final int maxDepth;

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
   * @param maxDepth The maximum depth to reason.
   */
  protected AbstractRecursiveReasoner(
      final IDefinitionKeeper definitionKeeper, final int maxDepth) {
    this.definitionKeeper = definitionKeeper;
    this.maxDepth = maxDepth;
  }

  protected MagikTypedFile getMagikFile(final Location location) {
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
        AbstractRecursiveReasoner.MAGIK_FILE_PROPERTIES, uri, text, this.definitionKeeper);
  }
}
