package nl.ramsolutions.sw.magik.analysis;

import java.io.IOException;
import nl.ramsolutions.sw.MagikToolsProperties;

/** Configuration for magik analysis. */
public class MagikAnalysisConfiguration {

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final MagikAnalysisConfiguration DEFAULT_CONFIGURATION;

  private static final String KEY_MAGIK_INDEXER_INDEX_GLOBAL_USAGES =
      "magik-indexer.index-global-usages";
  private static final String KEY_MAGIK_INDEXER_INDEX_METHOD_USAGES =
      "magik-indexer.index-method-usages";
  private static final String KEY_MAGIK_INDEXER_INDEX_SLOT_USAGES =
      "magik-indexer.index-slot-usages";
  private static final String KEY_MAGIK_INDEXER_INDEX_CONDITION_USAGES =
      "magik-indexer.index-condition-usages";

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
   * @throws IOException -
   */
  public MagikAnalysisConfiguration() throws IOException {
    this.properties = new MagikToolsProperties();
  }

  /**
   * Get Index global usages setting for indexing.
   *
   * @return True if usages should be indexed, false otherwise.
   */
  public boolean getMagikIndexerIndexGlobalUsages() {
    final Boolean value = this.properties.getPropertyBoolean(KEY_MAGIK_INDEXER_INDEX_GLOBAL_USAGES);
    if (value == null) {
      return false;
    }

    return value;
  }

  /**
   * Get Index method usages setting for indexing.
   *
   * @return True if usages should be indexed, false otherwise.
   */
  public boolean getMagikIndexerIndexMethodUsages() {
    final Boolean value = this.properties.getPropertyBoolean(KEY_MAGIK_INDEXER_INDEX_METHOD_USAGES);
    if (value == null) {
      return false;
    }

    return value;
  }

  /**
   * Get Index slot usages setting for indexing.
   *
   * @return True if usages should be indexed, false otherwise.
   */
  public boolean getMagikIndexerIndexSlotUsages() {
    final Boolean value = this.properties.getPropertyBoolean(KEY_MAGIK_INDEXER_INDEX_SLOT_USAGES);
    if (value == null) {
      return false;
    }

    return value;
  }

  /**
   * Get Index condition usages setting for indexing.
   *
   * @return True if usages should be indexed, false otherwise.
   */
  public boolean getMagikIndexerIndexConditionUsages() {
    final Boolean value =
        this.properties.getPropertyBoolean(KEY_MAGIK_INDEXER_INDEX_CONDITION_USAGES);
    if (value == null) {
      return false;
    }

    return value;
  }

  public void setMagikIndexerIndexGlobalUsages(final boolean value) {
    this.properties.setProperty(KEY_MAGIK_INDEXER_INDEX_GLOBAL_USAGES, value);
  }

  public void setMagikIndexerIndexMethodUsages(final boolean value) {
    this.properties.setProperty(KEY_MAGIK_INDEXER_INDEX_METHOD_USAGES, value);
  }

  public void setMagikIndexerIndexSlotUsages(final boolean value) {
    this.properties.setProperty(KEY_MAGIK_INDEXER_INDEX_SLOT_USAGES, value);
  }

  public void setMagikIndexerIndexConditionUsages(final boolean value) {
    this.properties.setProperty(KEY_MAGIK_INDEXER_INDEX_CONDITION_USAGES, value);
  }
}
