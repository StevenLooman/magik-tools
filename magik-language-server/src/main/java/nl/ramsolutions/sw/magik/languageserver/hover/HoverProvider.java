package nl.ramsolutions.sw.magik.languageserver.hover;

import com.sonar.sslr.api.AstNode;
import java.util.List;
import java.util.Optional;
import nl.ramsolutions.sw.OpenedFile;
import nl.ramsolutions.sw.loadlist.LoadListFile;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.Range;
import nl.ramsolutions.sw.magik.analysis.AstQuery;
import nl.ramsolutions.sw.magik.languageserver.Lsp4jConversion;
import nl.ramsolutions.sw.moduledef.ModuleDefFile;
import nl.ramsolutions.sw.productdef.ProductDefFile;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.MarkupContent;
import org.eclipse.lsp4j.MarkupKind;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.ServerCapabilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Hover provider. Runs a set of {@link HoverModule}s and uses the first claimed hover text. */
public class HoverProvider {

  private static final Logger LOGGER = LoggerFactory.getLogger(HoverProvider.class);

  private final List<HoverModule<MagikTypedFile>> magikModules;
  private final List<HoverModule<ProductDefFile>> productDefModules;
  private final List<HoverModule<ModuleDefFile>> moduleDefModules;

  /** Constructor. */
  public HoverProvider() {
    this.magikModules = this.createMagikModules();
    this.productDefModules = this.createProductDefModules();
    this.moduleDefModules = this.createModuleDefModules();
  }

  /**
   * Create the ordered modules for Magik files. Override to add or remove hover modules.
   *
   * @return Ordered modules.
   */
  protected List<HoverModule<MagikTypedFile>> createMagikModules() {
    return List.of(
        new PackageHoverModule(),
        new MethodDefinitionHoverModule(),
        new MethodInvocationHoverModule(),
        new ProcedureInvocationHoverModule(),
        new AtomHoverModule(),
        new ExpressionHoverModule(),
        new ConditionHoverModule());
  }

  /**
   * Create the ordered modules for product.def files. Override to add or remove hover modules.
   *
   * @return Ordered modules.
   */
  protected List<HoverModule<ProductDefFile>> createProductDefModules() {
    return List.of(new ProductNameHoverModule());
  }

  /**
   * Create the ordered modules for module.def files. Override to add or remove hover modules.
   *
   * @return Ordered modules.
   */
  protected List<HoverModule<ModuleDefFile>> createModuleDefModules() {
    return List.of(new ModuleNameHoverModule());
  }

  /**
   * Set server capabilities.
   *
   * @param capabilities Server capabilities.
   */
  public void setCapabilities(final ServerCapabilities capabilities) {
    capabilities.setHoverProvider(true);
  }

  /**
   * Provide a hover at the given position.
   *
   * @param productDefFile Product.def file.
   * @param position Position in file.
   * @return Hover at position.
   */
  public Hover provideHover(final ProductDefFile productDefFile, final Position position) {
    return this.provideHover(
        this.productDefModules, productDefFile, productDefFile.getTopNode(), position);
  }

  /**
   * Provide a hover at the given position.
   *
   * @param moduleDefFile Module.def file.
   * @param position Position in file.
   * @return Hover at position.
   */
  public Hover provideHover(final ModuleDefFile moduleDefFile, final Position position) {
    return this.provideHover(
        this.moduleDefModules, moduleDefFile, moduleDefFile.getTopNode(), position);
  }

  /**
   * Provide a hover at the given position.
   *
   * @param loadListFile LoadList file.
   * @param position Position in file.
   * @return Hover at position.
   */
  public Hover provideHover(final LoadListFile loadListFile, final Position position) {
    return null;
  }

  /**
   * Provide a hover at the given position.
   *
   * @param magikFile Magik file.
   * @param position Position in file.
   * @return Hover at position.
   */
  public Hover provideHover(final MagikTypedFile magikFile, final Position position) {
    return this.provideHover(this.magikModules, magikFile, magikFile.getTopNode(), position);
  }

  private <T extends OpenedFile> Hover provideHover(
      final List<HoverModule<T>> modules,
      final T file,
      final AstNode topNode,
      final Position position) {
    final AstNode hoveredTokenNode =
        AstQuery.nodeAt(topNode, Lsp4jConversion.positionFromLsp4j(position));
    if (hoveredTokenNode == null) {
      return null;
    }

    LOGGER.debug("Hovering on: {}", hoveredTokenNode.getTokenValue());

    // Run the modules in order; the first module claiming the context provides the hover.
    final HoverContext<T> context = new HoverContext<>(file, hoveredTokenNode);
    String content = "";
    for (final HoverModule<T> module : modules) {
      final Optional<String> result = module.tryHover(context);
      if (result.isPresent()) {
        content = result.get();
        break;
      }
    }

    if (content.isEmpty()) {
      return null;
    }

    final MarkupContent contents = new MarkupContent(MarkupKind.MARKDOWN, content);
    final Range range = new Range(hoveredTokenNode);
    final org.eclipse.lsp4j.Range rangeLsp4j = Lsp4jConversion.rangeToLsp4j(range);
    return new Hover(contents, rangeLsp4j);
  }
}
