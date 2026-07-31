package nl.ramsolutions.sw.magik.analysis;

import java.util.function.Function;
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
  private static final String SMALLWORLD_GIS = "magik.smallworldGis";

  /**
   * Name of the Smallworld logical, as it appears in dumped source paths ({@code $SMALLWORLD_GIS}).
   */
  private static final String SMALLWORLD_GIS_LOGICAL = "SMALLWORLD_GIS";

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
   * Get the configured Smallworld gis (installation) directory, used to expand {@code
   * $SMALLWORLD_GIS} in dumped definition source paths.
   *
   * @return The Smallworld gis directory, or {@code null} when not set.
   */
  public String getSmallworldGis() {
    return this.properties.getPropertyString(SMALLWORLD_GIS);
  }

  /**
   * Build an environment-variable resolver for expanding {@code $NAME} in dumped definition source
   * paths. Overlays the {@code magik.smallworldGis} setting on the {@code SMALLWORLD_GIS} logical
   * (setting wins when set), falling back to the process environment for it and every other name.
   *
   * @return Resolver mapping a variable name to its value (or {@code null} when unset).
   */
  public Function<String, String> getEnvironment() {
    final String gis = this.getSmallworldGis();
    if (gis == null) {
      return System::getenv;
    }

    return name -> SMALLWORLD_GIS_LOGICAL.equals(name) ? gis : System.getenv(name);
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
