package nl.ramsolutions.sw.magik.analysis;

import nl.ramsolutions.sw.MagikToolsProperties;

/**
 * Settings for magik analysis.
 *
 * <p>Note that settings are settings for the language server, set by the langauage client (i.e.,
 * your IDE). Configuration is configuration read from files like `.magik-lint.properties`.
 */
public class MagikAnalysisSettings {

  private static final String KEY_MAGIK_INDEXER_INDEX_GLOBAL_USAGES =
      "magik-indexer.index-global-usages";
  private static final String KEY_MAGIK_INDEXER_INDEX_METHOD_USAGES =
      "magik-indexer.index-method-usages";
  private static final String KEY_MAGIK_INDEXER_INDEX_SLOT_USAGES =
      "magik-indexer.index-slot-usages";
  private static final String KEY_MAGIK_INDEXER_INDEX_CONDITION_USAGES =
      "magik-indexer.index-condition-usages";

  private final MagikToolsProperties properties;

  /** Constructor. */
  public MagikAnalysisSettings(final MagikToolsProperties properties) {
    this.properties = properties;
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
