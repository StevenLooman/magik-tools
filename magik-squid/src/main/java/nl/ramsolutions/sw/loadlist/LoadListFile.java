package nl.ramsolutions.sw.loadlist;

import com.sonar.sslr.api.AstNode;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import nl.ramsolutions.sw.FileCharsetDeterminer;
import nl.ramsolutions.sw.MagikToolsProperties;
import nl.ramsolutions.sw.OpenedFile;
import nl.ramsolutions.sw.SourceFileScanner;
import nl.ramsolutions.sw.loadlist.parser.LoadListParser;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.Range;

/** Load list file. */
public class LoadListFile extends OpenedFile {

  public static final URI DEFAULT_URI = URI.create("memory://load_list.txt");
  public static final Location DEFAULT_LOCATION = new Location(DEFAULT_URI, Range.DEFAULT_RANGE);

  private AstNode astNode;

  /**
   * Constructor.
   *
   * @param uri URI.
   * @param source Source.
   */
  public LoadListFile(final URI uri, final String source) {
    super(uri, source);
  }

  /**
   * Constructor.
   *
   * @param properties Properties.
   * @param uri URI.
   * @param source Source.
   */
  public LoadListFile(final MagikToolsProperties properties, final URI uri, final String source) {
    super(properties, uri, source);
  }

  /**
   * Constructor.
   *
   * @param path Path.
   * @throws IOException -
   */
  public LoadListFile(final Path path) throws IOException {
    super(path.toUri(), Files.readString(path, FileCharsetDeterminer.determineCharset(path)));
  }

  /**
   * Get the load list definition defined in this file.
   *
   * @return {@link LoadListDefinition} defined in this file.
   */
  public LoadListDefinition getLoadListDefinition() {
    final LoadListDefinitionParser parser = new LoadListDefinitionParser();
    return parser.parseDefinition(this);
  }

  /**
   * Parse the text for this file and return the top level {@link AstNode}.
   *
   * @return Top level {@link AstNode}.
   */
  public synchronized AstNode getTopNode() {
    if (this.astNode == null) {
      final LoadListParser parser = new LoadListParser();
      final String source = this.getSource();
      final URI uri = this.getUri();
      this.astNode = parser.parse(source, uri);
    }

    return this.astNode;
  }

  @Override
  public String getLanguageId() {
    return "sw-load-list";
  }

  /**
   * Get the {@link LoadListFile} for the given file-URI.
   *
   * @param uri URI to search from.
   * @return {@link LoadListFile} for the given URI, or null if no load_list.txt/patch_list.txt file
   *     was found.
   */
  @CheckForNull
  public static LoadListFile getLoadListFileForUri(final URI uri) {
    if (!uri.getScheme().equals("file")) {
      return null;
    }

    final Path path = Path.of(uri);
    // Try to find load_list.txt first
    Path loadListPath = SourceFileScanner.searchFileUpwards(path, "load_list.txt");
    if (loadListPath == null) {
      // Try to find patch_list.txt as fallback
      loadListPath = SourceFileScanner.searchFileUpwards(path, "patch_list.txt");
    }

    if (loadListPath == null) {
      return null;
    }

    final LoadListFile loadListFile;
    try {
      loadListFile = new LoadListFile(loadListPath);
    } catch (final IOException exception) {
      throw new IllegalStateException(exception);
    }

    return loadListFile;
  }

  @Override
  public String toString() {
    return "%s@%s(%s)"
        .formatted(this.getClass().getName(), Integer.toHexString(this.hashCode()), this.getUri());
  }
}
