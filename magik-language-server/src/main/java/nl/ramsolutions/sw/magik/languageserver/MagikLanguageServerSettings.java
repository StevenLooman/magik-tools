package nl.ramsolutions.sw.magik.languageserver;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import java.nio.file.Path;
import java.util.List;
import nl.ramsolutions.sw.MagikToolsProperties;

/** Magik settings. */
public final class MagikLanguageServerSettings {

  private static final String SMALLWORLD_GIS = "magik.smallworldGis";
  private static final String PRODUCT_DIRS = "magik.productDirs";
  private static final String OVERRIDE_CONFIG_FILE = "magik.lint.overrideConfigFile";
  private static final String TYPE_DATABASE_PATHS = "magik.typing.typeDatabasePaths";
  private static final String SHOW_TYPING_INLAY_HINTS = "magik.typing.showTypingInlayHints";
  private static final String SHOW_ARGUMENT_INLAY_HINTS = "magik.typing.showArgumentInlayHints";
  private static final String ENABLE_TYPING_CHECKS = "magik.typing.enableChecks";

  private final MagikToolsProperties properties;

  /** Constructor. */
  public MagikLanguageServerSettings(final MagikToolsProperties properties) {
    this.properties = properties;
  }

  /**
   * Get magik.smallworldGis.
   *
   * @return magik.smallworldGis.
   */
  @CheckForNull
  public String getSmallworldGis() {
    return this.properties.getPropertyString(SMALLWORLD_GIS);
  }

  /**
   * Get magik.libsDirs.
   *
   * @return magik.libsDirs.
   */
  public List<String> getProductDirs() {
    return this.properties.getPropertyList(PRODUCT_DIRS);
  }

  /**
   * Get magik.typing.typeDatabasePath.
   *
   * @return magik.typing.typeDatabasePath.
   */
  public List<String> getTypingTypeDatabasePaths() {
    return this.properties.getPropertyList(TYPE_DATABASE_PATHS);
  }

  /**
   * Get magik.typing.showAtomInlayHints.
   *
   * @return magik.typing.showAtomInlayHints
   */
  public boolean getTypingShowTypingInlayHints() {
    return this.properties.getPropertyBoolean(SHOW_TYPING_INLAY_HINTS) == Boolean.TRUE;
  }

  /**
   * Get magik.typing.showArgumentInlayHints.
   *
   * @return magik.typing.showArgumentInlayHints
   */
  public boolean getTypingShowArgumentInlayHints() {
    return this.properties.getPropertyBoolean(SHOW_ARGUMENT_INLAY_HINTS) == Boolean.TRUE;
  }

  /**
   * Get magik.typing.enableChecks, defaults to false if no config is provided.
   *
   * @return magik.typing.enableChecks.
   */
  @CheckForNull
  public Boolean getTypingEnableChecks() {
    return this.properties.getPropertyBoolean(ENABLE_TYPING_CHECKS) == Boolean.TRUE;
  }

  /**
   * Get magik.lint.overrideConfigFile.
   *
   * @return magik.lint.overrideConfigFile
   */
  @CheckForNull
  public Path getChecksOverrideSettingsPath() {
    final String overrideConfigFile = this.properties.getPropertyString(OVERRIDE_CONFIG_FILE);
    return overrideConfigFile != null ? Path.of(overrideConfigFile) : null;
  }
}
