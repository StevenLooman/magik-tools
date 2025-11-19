package nl.ramsolutions.sw.productdef;

import com.sonar.sslr.api.AstNode;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import nl.ramsolutions.sw.MagikToolsProperties;
import nl.ramsolutions.sw.OpenedFile;
import nl.ramsolutions.sw.SourceFileScanner;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.Range;
import nl.ramsolutions.sw.magik.analysis.definitions.DefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.productdef.parser.ProductDefParser;

/** Product definition file. */
public class ProductDefFile extends OpenedFile {

  public static final URI DEFAULT_URI = URI.create("memory://product.def");
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
   */
  public ProductDefFile(
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
  public ProductDefFile(
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
   * @throws IOException -
   */
  public ProductDefFile(
      final Path path,
      final IDefinitionKeeper definitionKeeper,
      final @Nullable ProductDefFile parentProductDefFile)
      throws IOException {
    super(MagikToolsProperties.DEFAULT_PROPERTIES, path);
    this.definitionKeeper = definitionKeeper;
    this.parentProductDefFile = parentProductDefFile;
  }

  @CheckForNull
  public ProductDefFile getParentProductDefFile() {
    return this.parentProductDefFile;
  }

  /**
   * Get the product definition defined in this file.
   *
   * <p>Note that this does not include the parent product!
   *
   * @return {@link ProductDefinition} defined in this file.
   * @throws IOException -
   */
  @CheckForNull
  public ProductDefinition getProductDefinition() {
    final ProductDefinitionParser parser = new ProductDefinitionParser();
    final ProductDefinition parentProductDefinition =
        this.parentProductDefFile != null ? this.parentProductDefFile.getProductDefinition() : null;
    return parser.parseDefinition(this, parentProductDefinition);
  }

  /**
   * Parse the text for this file and return the top level {@link AstNode}.
   *
   * @return Top level {@link AstNode}.
   */
  public synchronized AstNode getTopNode() {
    if (this.astNode == null) {
      final ProductDefParser parser = new ProductDefParser();
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
    return "sw-product-def";
  }

  /**
   * Get the {@link ProductDefFile} for the given URI.
   *
   * <p>Note that this method does not find/set the parent product.def file.
   *
   * @param uri URI.
   * @param definitionKeeper DefinitionKeeper.
   * @return {@link ProductDefFile} for the given URI, or null if no product.def file was found.
   */
  @CheckForNull
  public static ProductDefFile getProductDefFileForUri(
      final URI uri, final @Nullable IDefinitionKeeper definitionKeeper) {
    if (!uri.getScheme().equals("file")) {
      throw new IllegalStateException("Cannot get product.def for non-file URI");
    }

    final Path path = Path.of(uri);
    final Path productDefPath =
        SourceFileScanner.searchFileUpwards(path, SourceFileScanner.SW_PRODUCT_DEF);
    if (productDefPath == null) {
      return null;
    }

    final ProductDefFile productDefFile;
    final IDefinitionKeeper defKeeper =
        definitionKeeper != null
            ? definitionKeeper
            : new DefinitionKeeper(false); // Use empty definition keeper.
    try {
      productDefFile = new ProductDefFile(productDefPath, defKeeper, null);
    } catch (final IOException exception) {
      throw new IllegalStateException(exception);
    }

    return productDefFile;
  }

  @Override
  public String toString() {
    return "%s@%s(%s)"
        .formatted(this.getClass().getName(), Integer.toHexString(this.hashCode()), this.getUri());
  }
}
