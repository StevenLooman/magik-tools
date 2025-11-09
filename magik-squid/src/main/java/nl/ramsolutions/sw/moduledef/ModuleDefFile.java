package nl.ramsolutions.sw.moduledef;

import com.sonar.sslr.api.AstNode;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import nl.ramsolutions.sw.FileCharsetDeterminer;
import nl.ramsolutions.sw.MagikToolsProperties;
import nl.ramsolutions.sw.OpenedFile;
import nl.ramsolutions.sw.SourceFileScanner;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.Range;
import nl.ramsolutions.sw.magik.analysis.definitions.DefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.moduledef.parser.ModuleDefParser;
import nl.ramsolutions.sw.productdef.ProductDefFile;
import nl.ramsolutions.sw.productdef.ProductDefinition;

/** Module definition file. */
public class ModuleDefFile extends OpenedFile {

  public static final URI DEFAULT_URI = URI.create("memory://module.def");
  public static final Location DEFAULT_LOCATION = new Location(DEFAULT_URI, Range.DEFAULT_RANGE);

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
    this.definitionKeeper = definitionKeeper;
    this.parentProductDefFile = parentProductDefFile;
  }

  /**
   * Constructor.
   *
   * @param properties Properties.
   * @param uri URI.
   * @param source Source.
   * @param definitionKeeper DefinitionKeeper.
   */
  public ModuleDefFile(
      final MagikToolsProperties properties,
      final URI uri,
      final String source,
      final IDefinitionKeeper definitionKeeper,
      final @Nullable ProductDefFile parentProductDefFile) {
    super(properties, uri, source);
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
    this.definitionKeeper = definitionKeeper;
    this.parentProductDefFile = parentProductDefFile;
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
      final ModuleDefParser parser = new ModuleDefParser();
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
    return "sw-module-def";
  }

  /**
   * Get the {@link ModuleDefFile} for the given file-URI.
   *
   * <p>Note that this method does not find/set the parent product.def file.
   *
   * @param uri
   * @param definitionKeeper
   * @return {@link ModuleDefFile} for the given URI, or null if no module.def file was found.
   */
  @CheckForNull
  public static ModuleDefFile getModuleDefFileForUri(
      final URI uri, final @Nullable IDefinitionKeeper definitionKeeper) {
    if (!uri.getScheme().equals("file")) {
      return null;
    }

    final Path path = Path.of(uri);
    final Path moduleDefPath =
        SourceFileScanner.searchFileUpwards(path, SourceFileScanner.SW_MODULE_DEF);
    if (moduleDefPath == null) {
      return null;
    }

    final ModuleDefFile moduleDefFile;
    final IDefinitionKeeper defKeeper =
        definitionKeeper != null
            ? definitionKeeper
            : new DefinitionKeeper(false); // Use empty definition keeper.
    try {
      moduleDefFile = new ModuleDefFile(moduleDefPath, defKeeper, null);
    } catch (final IOException exception) {
      throw new IllegalStateException(exception);
    }

    return moduleDefFile;
  }

  @Override
  public String toString() {
    return "%s@%s(%s)"
        .formatted(this.getClass().getName(), Integer.toHexString(this.hashCode()), this.getUri());
  }
}
