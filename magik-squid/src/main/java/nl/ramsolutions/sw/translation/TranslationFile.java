package nl.ramsolutions.sw.translation;

import com.sonar.sslr.api.AstNode;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import nl.ramsolutions.sw.MagikToolsProperties;
import nl.ramsolutions.sw.OpenedFile;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.Range;
import nl.ramsolutions.sw.translation.parser.TranslationParser;

/** Translation (.trn) file. */
public class TranslationFile extends OpenedFile {

  public static final String LANGUAGE_ID = "sw-translation";
  public static final URI DEFAULT_URI = URI.create("memory://source.trn");
  public static final Location DEFAULT_LOCATION = new Location(DEFAULT_URI, Range.DEFAULT_RANGE);

  private AstNode astNode;

  /**
   * Constructor.
   *
   * @param uri URI.
   * @param source Source.
   */
  public TranslationFile(final URI uri, final String source) {
    super(uri, source);
  }

  /**
   * Constructor.
   *
   * @param properties Properties.
   * @param uri URI.
   * @param source Source.
   */
  public TranslationFile(
      final MagikToolsProperties properties, final URI uri, final String source) {
    super(properties, uri, source);
  }

  /**
   * Constructor.
   *
   * @param path Path.
   * @throws IOException -
   */
  public TranslationFile(final Path path) throws IOException {
    super(path);
  }

  /**
   * Parse the text for this file and return the top level {@link AstNode}.
   *
   * @return Top level {@link AstNode}.
   */
  public synchronized AstNode getTopNode() {
    if (this.astNode == null) {
      final TranslationParser parser = new TranslationParser();
      final String source = this.getSource();
      final URI uri = this.getUri();
      this.astNode = parser.parse(source, uri);
    }

    return this.astNode;
  }

  @Override
  public String getLanguageId() {
    return LANGUAGE_ID;
  }

  @Override
  public String toString() {
    return "%s@%s(%s)"
        .formatted(this.getClass().getName(), Integer.toHexString(this.hashCode()), this.getUri());
  }
}
