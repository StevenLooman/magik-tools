package nl.ramsolutions.sw;

import java.net.URI;

/** An opened file, with its URI and source text. */
public abstract class OpenedFile {

  private final URI uri;
  private final String source;

  /**
   * Constructor.
   *
   * @param uri URI.
   * @param source Source.
   */
  protected OpenedFile(final URI uri, final String source) {
    this.uri = uri;
    this.source = source;
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
   * Get the language ID for this file.
   *
   * @return Language ID.
   */
  public abstract String getLanguageId();
}
