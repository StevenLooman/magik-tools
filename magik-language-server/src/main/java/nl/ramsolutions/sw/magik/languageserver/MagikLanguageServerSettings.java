package nl.ramsolutions.sw.magik.languageserver;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import nl.ramsolutions.sw.MagikToolsProperties;
import nl.ramsolutions.sw.magik.PathMapping;

/** Magik settings. */
public final class MagikLanguageServerSettings {

  private static final String PRODUCT_DIRS = "magik.productDirs";
  private static final String OVERRIDE_CONFIG_FILE = "magik.lint.overrideConfigFile";
  private static final String TYPE_DATABASE_PATHS = "magik.typing.typeDatabasePaths";
  private static final String SHOW_TYPING_INLAY_HINTS = "magik.typing.showTypingInlayHints";
  private static final String SHOW_ARGUMENT_INLAY_HINTS = "magik.typing.showArgumentInlayHints";
  private static final String ENABLE_TYPING_CHECKS = "magik.typing.enableChecks";
  public static final String SMALLWORLD_GIS = "magik.smallworldGis";
  public static final String PATH_MAPPING = "magik.pathMapping";

  private static List<PathMapping> pathMappings = new CopyOnWriteArrayList<>();
  private static String pathMappingsStr = "";

  private final MagikToolsProperties properties;

  /** Constructor. */
  public MagikLanguageServerSettings(final MagikToolsProperties properties) {
    this.properties = properties;
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

  @CheckForNull
  public String getSmallworldGis() {
    return this.properties.getPropertyString(SMALLWORLD_GIS);
  }

  /**
   * Get magik.pathMapping, defaults to an empty list if not defined
   *
   * @return the path mappings
   */
  public List<PathMapping> getPathMappings() {
    // get cached path mappings for performance
    String unparsedMappingsStr = this.properties.getPropertyString(PATH_MAPPING);
    if (unparsedMappingsStr == null) {
      return Collections.emptyList();
    }
    if (unparsedMappingsStr.equals(MagikLanguageServerSettings.pathMappingsStr)) {
      return MagikLanguageServerSettings.pathMappings;
    }

    List<PathMapping> mappings =
        new CopyOnWriteArrayList<>(
            this.properties.getPropertyList(PATH_MAPPING, null, PathMapping.class));

    MagikLanguageServerSettings.pathMappings = mappings;
    MagikLanguageServerSettings.pathMappingsStr = unparsedMappingsStr;

    return mappings;
  }
}
