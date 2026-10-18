package nl.ramsolutions.sw;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** An opened file, with its URI and source text. */
public abstract class OpenedFile {

  private static final Logger LOGGER = LoggerFactory.getLogger(OpenedFile.class);

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
  protected OpenedFile(final Path path) throws IOException {
    this(MagikToolsProperties.DEFAULT_PROPERTIES, path);
  }

  /**
   * Constructor. Read file at path.
   *
   * @param properties Properties.
   * @param path File to read.
   * @throws IOException -
   */
  protected OpenedFile(final MagikToolsProperties properties, final Path path) throws IOException {
    this.timestamp = Files.getLastModifiedTime(path).toInstant();
    this.properties = properties;
    this.uri = path.toUri();
    this.source = OpenedFile.readSource(path);
  }

  /**
   * Read the file, using the encoding it declares.
   *
   * @param path File to read.
   * @return Contents of the file.
   * @throws IOException -
   */
  private static String readSource(final Path path) throws IOException {
    final Charset charset = FileCharsetDeterminer.determineCharset(path);
    try {
      return Files.readString(path, charset);
    } catch (final CharacterCodingException exception) {
      LOGGER.warn("File: {}, contents do not match encoding: {}", path, charset);
      return new String(Files.readAllBytes(path), charset);
    }
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
