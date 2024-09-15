package nl.ramsolutions.sw.moduledef;

import com.sonar.sslr.api.AstNode;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import nl.ramsolutions.sw.FileCharsetDeterminer;
import nl.ramsolutions.sw.OpenedFile;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.Range;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.moduledef.parser.SwModuleDefParser;
import nl.ramsolutions.sw.productdef.ProductDefFile;
import nl.ramsolutions.sw.productdef.ProductDefinition;

/** Module definition file. */
public class ModuleDefFile extends OpenedFile {

  private static final URI DEFAULT_URI = URI.create("memory://module.def");
  public static final Location DEFAULT_LOCATION = new Location(DEFAULT_URI, Range.DEFAULT_RANGE);

  private final @Nullable Instant timestamp;
  private final IDefinitionKeeper definitionKeeper;
  private final @Nullable ProductDefFile parentProductDefFile;
  private AstNode astNode;

  /**
   * Constructor.
   *
   * @param uri URI.
   * @param source Source.
   * @param definitionKeeper DefinitionKeeper.
   * @param parentProductDefFile Parent {@link ProductDefFile}.
   */
  public ModuleDefFile(
      final URI uri,
      final String source,
      final IDefinitionKeeper definitionKeeper,
      final @Nullable ProductDefFile parentProductDefFile) {
    super(uri, source);
    this.timestamp = null;
    this.definitionKeeper = definitionKeeper;
    this.parentProductDefFile = parentProductDefFile;
  }

  /**
   * Constructor.
   *
   * @param path Path.
   * @param definitionKeeper DefinitionKeeper.
   * @param parentProductDefFile Parent {@link ProductDefFile}.
   * @throws IOException -
   */
  public ModuleDefFile(
      final Path path,
      final IDefinitionKeeper definitionKeeper,
      final @Nullable ProductDefFile parentProductDefFile)
      throws IOException {
    super(path.toUri(), Files.readString(path, FileCharsetDeterminer.determineCharset(path)));
    this.timestamp = Files.getLastModifiedTime(path).toInstant();
    this.definitionKeeper = definitionKeeper;
    this.parentProductDefFile = parentProductDefFile;
  }

  /**
   * Get the timestamp for this file.
   *
   * @return Timestamp for this file.
   */
  @CheckForNull
  public Instant getTimestamp() {
    return this.timestamp;
  }

  /**
   * Get the parent {@link ProductDefFile}.
   *
   * @return Parent {@link ProductDefFile}.
   */
  @CheckForNull
  public ProductDefFile getParentProductDefFile() {
    return this.parentProductDefFile;
  }

  /**
   * Get the product definition defined in this file.
   *
   * @return {@link ProductDefinition} defined in this file.
   * @throws IOException -
   */
  public ModuleDefinition getModuleDefinition() {
    final ModuleDefinitionParser parser = new ModuleDefinitionParser();
    final ProductDefinition productDefinition =
        this.parentProductDefFile != null ? this.parentProductDefFile.getProductDefinition() : null;
    return parser.parseDefinition(this, productDefinition);
  }

  /**
   * Parse the text for this file and return the top level {@link AstNode}.
   *
   * @return Top level {@link AstNode}.
   */
  public synchronized AstNode getTopNode() {
    if (this.astNode == null) {
      final SwModuleDefParser parser = new SwModuleDefParser();
      final String source = this.getSource();
      final URI uri = this.getUri();
      this.astNode = parser.parse(source, uri);
    }

    return this.astNode;
  }

  /** Get the {@link IDefinitionKeeper}. */
  public IDefinitionKeeper getDefinitionKeeper() {
    return this.definitionKeeper;
  }

  @Override
  public String getLanguageId() {
    return "module.def";
  }

  /**
   * Get the module name for the given URI.
   *
   * <p>Scans upwards from the given URI to find the module definition file and extracts the module
   * name from the `module.def` file..
   *
   * @param uri URI to start searching from.
   * @return Module name, or null if no module was found.
   */
  @CheckForNull
  public static String getModuleNameForUri(final URI uri) {
    if (!uri.getScheme().equals("file")) {
      return null;
    }

    final Path path = Path.of(uri);
    final ModuleDefFile moduleDefFile;
    try {
      final Path moduleDefPath = ModuleDefFileScanner.getModuleDefFileForPath(path);
      if (moduleDefPath == null) {
        return null;
      }

      moduleDefFile = new ModuleDefFile(moduleDefPath, null, null);
    } catch (final IOException exception) {
      throw new IllegalStateException(exception);
    }

    final ModuleDefinition moduleDefinition = moduleDefFile.getModuleDefinition();
    return moduleDefinition.getName();
  }
}
