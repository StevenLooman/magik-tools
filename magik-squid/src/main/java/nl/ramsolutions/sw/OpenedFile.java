package nl.ramsolutions.sw;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

/** An opened file, with its URI and source text. */
public abstract class OpenedFile {

  private final @Nullable Instant timestamp;
  private final MagikToolsProperties properties;
  private final URI uri;
  private final String source;

  /**
   * Constructor.
   *
   * @param uri URI.
   * @param source Source.
   */
  protected OpenedFile(final URI uri, final String source) {
    this(MagikToolsProperties.DEFAULT_PROPERTIES, uri, source);
  }

  /**
   * Constructor.
   *
   * @param properties Properties.
   * @param uri URI.
   * @param source Source.
   */
  protected OpenedFile(final MagikToolsProperties properties, final URI uri, final String source) {
    this.timestamp = null;
    this.properties = properties;
    this.uri = uri;
    this.source = source;
  }

  /**
   * Constructor. Read file at path.
   *
   * @param properties Properties.
   * @param path File to read.
   * @throws IOException -
   */
  protected OpenedFile(final MagikToolsProperties properties, final Path path) throws IOException {
    this.uri = path.toUri();
    this.source = Files.readString(path, FileCharsetDeterminer.determineCharset(path));
    this.timestamp = Files.getLastModifiedTime(path).toInstant();
    this.properties = properties;
  }

  /**
   * Get the timestamp of when the file was last modified, or null if not known.
   *
   * @return Timestamp, or null if not known.
   */
  @CheckForNull
  public Instant getTimestamp() {
    return this.timestamp;
  }

  /**
   * Get the properties for this file.
   *
   * @return Properties.
   */
  public MagikToolsProperties getProperties() {
    return this.properties;
  }

  /**
   * Get the URI.
   *
   * @return The URI.
   */
  public URI getUri() {
    return this.uri;
  }

  /**
   * Get the source text.
   *
   * @return Source text.
   */
  public String getSource() {
    return this.source;
  }

  /**
   * Get the source lines.
   *
   * @return Source lines.
   */
  public String[] getSourceLines() {
    final String source = this.getSource();
    return source.split("\r\n|\n|\r");
  }

  /**
   * Get the language ID for this file.
   *
   * @return Language ID.
   */
  public abstract String getLanguageId();
}
