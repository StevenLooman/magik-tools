package nl.ramsolutions.sw.magik;

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
import nl.ramsolutions.sw.definitions.ModuleDefinition;
import nl.ramsolutions.sw.definitions.ModuleDefinitionParser;
import nl.ramsolutions.sw.definitions.ProductDefinition;
import nl.ramsolutions.sw.definitions.parser.SwModuleDefParser;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;

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
   * @throws IOException
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
   * @return
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
}
