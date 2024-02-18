package nl.ramsolutions.sw.magik.analysis;

import java.io.IOException;
import nl.ramsolutions.sw.MagikToolsProperties;

/** Configuration for magik analysis. */
public class MagikAnalysisConfiguration {

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final MagikAnalysisConfiguration DEFAULT_CONFIGURATION;

  private static final String KEY_MAGIK_INDEXER_INDEX_USAGES = "magik-indexer.index-usages";

  static {
    try {
      DEFAULT_CONFIGURATION = new MagikAnalysisConfiguration();
    } catch (final IOException e) {
      throw new IllegalStateException();
    }
  }

  private final MagikToolsProperties properties;

  /**
   * Constructor.
   *
   * @throws IOException
   */
  public MagikAnalysisConfiguration() throws IOException {
    this.properties = new MagikToolsProperties();
  }

  /**
   * Get Index usages setting for indexing.
   *
   * @return True if usages should be indexed, false otherwise.
   */
  public boolean getMagikIndexerIndexUsages() {
    final Boolean value = this.properties.getPropertyBoolean(KEY_MAGIK_INDEXER_INDEX_USAGES);
    if (value == null) {
      return false;
    }

    return value;
  }

  public void setMagikIndexerIndexUsages(final boolean value) {
    this.properties.setProperty(KEY_MAGIK_INDEXER_INDEX_USAGES, value);
  }
}
