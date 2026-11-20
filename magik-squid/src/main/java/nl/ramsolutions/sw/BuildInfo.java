package nl.ramsolutions.sw;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The magik-tools build this code belongs to: its version and the git commit it was built from,
 * baked in at build time. Two snapshot builds share a version, so only the commit tells them apart.
 *
 * @param version Version, {@link #UNKNOWN_VERSION} when the build recorded none.
 * @param commit Abbreviated commit, suffixed {@code -dirty} for a modified checkout, or {@code
 *     null} when built without git.
 */
public record BuildInfo(String version, @CheckForNull String commit) {

  /** Version of a build that recorded none, such as a run from an IDE. */
  public static final String UNKNOWN_VERSION = "unknown";

  /** Resource the build writes, relative to this class. */
  static final String RESOURCE = "magik-tools-build.properties";

  private static final Logger LOGGER = LoggerFactory.getLogger(BuildInfo.class);
  private static final BuildInfo CURRENT = BuildInfo.readCurrent();

  /**
   * Get the build this code belongs to.
   *
   * @return The build.
   */
  public static BuildInfo getCurrent() {
    return BuildInfo.CURRENT;
  }

  /**
   * Describe this build as the tools report it: the version, followed by the commit when known.
   *
   * @return Description, such as {@code 0.13.0-SNAPSHOT (commit 6ce81b1c290a)}.
   */
  public String describeVersion() {
    return this.commit != null ? this.version + " (commit " + this.commit + ")" : this.version;
  }

  /**
   * Read build info from the properties the build wrote.
   *
   * @param properties Properties holding {@code version} and, when known, {@code commit}.
   * @return The build info.
   */
  static BuildInfo createFromProperties(final Properties properties) {
    final String version = properties.getProperty("version", BuildInfo.UNKNOWN_VERSION);
    final String commit = properties.getProperty("commit");
    final String recordedCommit = commit == null || commit.isBlank() ? null : commit;
    return new BuildInfo(version, recordedCommit);
  }

  private static BuildInfo readCurrent() {
    final Properties properties = new Properties();
    try (InputStream stream = BuildInfo.class.getResourceAsStream(BuildInfo.RESOURCE)) {
      if (stream != null) {
        properties.load(stream);
      }
    } catch (final IOException exception) {
      LOGGER.debug("Cannot read build info", exception);
    }
    return BuildInfo.createFromProperties(properties);
  }
}
