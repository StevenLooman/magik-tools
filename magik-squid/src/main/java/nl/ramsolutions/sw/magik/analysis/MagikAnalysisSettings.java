package nl.ramsolutions.sw.magik.analysis;

import nl.ramsolutions.sw.MagikToolsProperties;

/**
 * Settings for magik analysis.
 *
 * <p>Note that settings are settings for the language server, set by the language client (i.e.,
 * your IDE). Configuration is configuration read from files like `.magik-lint.properties`.
 */
public class MagikAnalysisSettings {

  private static final String INDEX_GLOBAL_USAGES = "magik.typing.indexGlobalUsages";
  private static final String INDEX_METHOD_USAGES = "magik.typing.indexMethodUsages";
  private static final String INDEX_SLOT_USAGES = "magik.typing.indexSlotUsages";
  private static final String INDEX_CONDITION_USAGES = "magik.typing.indexConditionUsages";
  private static final String CACHE_INDEXED_DEFINITIONS = "magik.typing.cacheIndexedDefinitions";

  private final MagikToolsProperties properties;

  /**
   * Constructor.
   *
   * @param properties The properties to use.
   */
  public MagikAnalysisSettings(final MagikToolsProperties properties) {
    this.properties = properties;
  }

  /**
   * Determine if global usages should be indexed.
   *
   * @return True if global usages should be indexed, false otherwise.
   */
  public boolean getTypingIndexGlobalUsages() {
    return this.properties.getPropertyBoolean(INDEX_GLOBAL_USAGES) != Boolean.FALSE;
  }

  /**
   * Determine if method usages should be indexed.
   *
   * @return True if method usages should be indexed, false otherwise.
   */
  public boolean getTypingIndexMethodUsages() {
    return this.properties.getPropertyBoolean(INDEX_METHOD_USAGES) == Boolean.TRUE;
  }

  /**
   * Determine if slot usages should be indexed.
   *
   * @return True if slot usages should be indexed, false otherwise.
   */
  public boolean getTypingIndexSlotUsages() {
    return this.properties.getPropertyBoolean(INDEX_SLOT_USAGES) != Boolean.FALSE;
  }

  /**
   * Determine if condition usages should be indexed.
   *
   * @return True if condition usages should be indexed, false otherwise.
   */
  public boolean getTypingIndexConditionUsages() {
    return this.properties.getPropertyBoolean(INDEX_CONDITION_USAGES) != Boolean.FALSE;
  }

  /**
   * Determine if indexed definitions should be cached for typing.
   *
   * @return True if indexed definitions should be cached, false otherwise.
   */
  public boolean getTypingCacheIndexedDefinitions() {
    return this.properties.getPropertyBoolean(CACHE_INDEXED_DEFINITIONS) != Boolean.FALSE;
  }

  /**
   * Determine if switching from this settings to the other settings requires reindexing.
   *
   * @param other The other settings.
   * @return True if reindexing is required, false otherwise.
   */
  public boolean requiresReindexing(final MagikAnalysisSettings other) {
    return this.getTypingIndexMethodUsages() != other.getTypingIndexMethodUsages()
        || this.getTypingIndexSlotUsages() != other.getTypingIndexSlotUsages()
        || this.getTypingIndexConditionUsages() != other.getTypingIndexConditionUsages()
        || this.getTypingCacheIndexedDefinitions() != other.getTypingCacheIndexedDefinitions();
  }

  /**
   * Determine if no settings are set.
   *
   * @return True if no settings are set, false otherwise.
   */
  public boolean isEmpty() {
    return this.properties.getPropertyBoolean(INDEX_METHOD_USAGES) == null
        && this.properties.getPropertyBoolean(INDEX_SLOT_USAGES) == null
        && this.properties.getPropertyBoolean(INDEX_CONDITION_USAGES) == null
        && this.properties.getPropertyBoolean(CACHE_INDEXED_DEFINITIONS) == null;
  }
}
