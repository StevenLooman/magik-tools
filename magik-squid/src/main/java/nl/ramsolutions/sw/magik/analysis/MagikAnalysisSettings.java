package nl.ramsolutions.sw.magik.analysis;

import nl.ramsolutions.sw.MagikToolsProperties;

/**
 * Settings for magik analysis.
 *
 * <p>Note that settings are settings for the language server, set by the langauage client (i.e.,
 * your IDE). Configuration is configuration read from files like `.magik-lint.properties`.
 */
public class MagikAnalysisSettings {

  public static final String INDEX_GLOBAL_USAGES = "magik.typing.indexGlobalUsages";
  public static final String INDEX_METHOD_USAGES = "magik.typing.indexMethodUsages";
  public static final String INDEX_SLOT_USAGES = "magik.typing.indexSlotUsages";
  public static final String INDEX_CONDITION_USAGES = "magik.typing.indexConditionUsages";
  public static final String INDEX_UNARY_OPERATOR_USAGES = "magik.typing.indexUnaryOperatorUsages";
  public static final String INDEX_BINARY_OPERATOR_USAGES =
      "magik.typing.indexBinaryOperatorUsages";
  public static final String CACHE_INDEXED_DEFINITIONS = "magik.typing.cacheIndexedDefinitions";

  private final MagikToolsProperties properties;

  /** Constructor. */
  public MagikAnalysisSettings(final MagikToolsProperties properties) {
    this.properties = properties;
  }

  public boolean getTypingIndexGlobalUsages() {
    return this.properties.getPropertyBoolean(INDEX_GLOBAL_USAGES) != Boolean.FALSE;
  }

  public boolean getTypingIndexMethodUsages() {
    return this.properties.getPropertyBoolean(INDEX_METHOD_USAGES) == Boolean.TRUE;
  }

  public boolean getTypingIndexSlotUsages() {
    return this.properties.getPropertyBoolean(INDEX_SLOT_USAGES) != Boolean.FALSE;
  }

  public boolean getTypingIndexConditionUsages() {
    return this.properties.getPropertyBoolean(INDEX_CONDITION_USAGES) != Boolean.FALSE;
  }

  public boolean getTypingIndexUnaryOperatorUsages() {
    return this.properties.getPropertyBoolean(INDEX_UNARY_OPERATOR_USAGES) != Boolean.FALSE;
  }

  public boolean getTypingIndexBinaryOperatorUsages() {
    return this.properties.getPropertyBoolean(INDEX_BINARY_OPERATOR_USAGES) != Boolean.FALSE;
  }

  public boolean getTypingCacheIndexedDefinitions() {
    return this.properties.getPropertyBoolean(CACHE_INDEXED_DEFINITIONS) != Boolean.FALSE;
  }
}
