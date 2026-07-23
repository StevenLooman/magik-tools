package nl.ramsolutions.sw;

import edu.umd.cs.findbugs.annotations.CheckForNull;

/**
 * Settings for magik lint.
 *
 * <p>Note that these are used by both magik-lint and magik-typed-lint, and are read from
 * configuration files like `magik-lint.properties`.
 */
public class MagikLintSettings {

  public static final String KEY_MAX_INFRACTIONS = "magik.lint.max-infractions";
  public static final String KEY_COLUMN_OFFSET = "magik.lint.column-offset";
  public static final String KEY_MSG_TEMPLATE = "magik.lint.msg-template";
  public static final String KEY_OVERRIDE_CONFIG = "magik.lint.overrideConfigFile";

  private final MagikToolsProperties properties;

  /**
   * Constructor.
   *
   * @param properties The properties to use.
   */
  public MagikLintSettings(final MagikToolsProperties properties) {
    this.properties = properties;
  }

  /**
   * Get the maximum number of infractions to report.
   *
   * @return Maximum number of infractions, defaults to {@link Long#MAX_VALUE}.
   */
  public long getMaxInfractions() {
    return this.properties.getPropertyLong(KEY_MAX_INFRACTIONS, Long.MAX_VALUE);
  }

  /**
   * Get the offset to apply to reported columns.
   *
   * @return Column offset, defaults to 0.
   */
  public long getColumnOffset() {
    return this.properties.getPropertyLong(KEY_COLUMN_OFFSET, 0L);
  }

  /**
   * Get the template used to format reported issues.
   *
   * @return Message template, or null if unset.
   */
  @CheckForNull
  public String getMsgTemplate() {
    return this.properties.getPropertyString(KEY_MSG_TEMPLATE);
  }

  /**
   * Get the configuration file overriding the located configuration.
   *
   * @return Path to the overriding configuration file, or null if unset.
   */
  @CheckForNull
  public String getOverrideConfigFile() {
    return this.properties.getPropertyString(KEY_OVERRIDE_CONFIG);
  }
}
