package nl.ramsolutions.sw.magik.languageserver.references;

import com.sonar.sslr.api.AstNode;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import nl.ramsolutions.sw.OpenedFile;
import nl.ramsolutions.sw.loadlist.LoadListFile;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.Position;
import nl.ramsolutions.sw.magik.analysis.AstQuery;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.moduledef.ModuleDefFile;
import nl.ramsolutions.sw.productdef.ProductDefFile;
import org.eclipse.lsp4j.ServerCapabilities;

/**
 * References provider. Runs a set of {@link ReferencesModule}s and uses the first claimed result.
 */
public class ReferencesProvider {

  private final List<ReferencesModule<MagikTypedFile>> magikModules;
  private final List<ReferencesModule<ProductDefFile>> productDefModules;
  private final List<ReferencesModule<ModuleDefFile>> moduleDefModules;

  /** Constructor. */
  public ReferencesProvider() {
    this.magikModules = this.createMagikModules();
    this.productDefModules = this.createProductDefModules();
    this.moduleDefModules = this.createModuleDefModules();
  }

  /**
   * Create the ordered modules for Magik files. Override to add or remove references modules.
   *
   * @return Ordered modules.
   */
  protected List<ReferencesModule<MagikTypedFile>> createMagikModules() {
    return List.of(
        new MethodNameReferencesModule(),
        new ExemplarNameReferencesModule(),
        new AtomReferencesModule(),
        new ConditionNameReferencesModule(),
        new SlotReferencesModule());
  }

  /**
   * Create the ordered modules for product.def files. Override to add or remove references modules.
   *
   * @return Ordered modules.
   */
  protected List<ReferencesModule<ProductDefFile>> createProductDefModules() {
    return List.of(new ProductNameReferencesModule());
  }

  /**
   * Create the ordered modules for module.def files. Override to add or remove references modules.
   *
   * @return Ordered modules.
   */
  protected List<ReferencesModule<ModuleDefFile>> createModuleDefModules() {
    return List.of(new ModuleNameReferencesModule());
  }

  /**
   * Set server capabilities.
   *
   * @param capabilities Server capabilities.
   */
  public void setCapabilities(final ServerCapabilities capabilities) {
    capabilities.setReferencesProvider(true);
  }

  /**
   * Provide references.
   *
   * @param productDefFile Product.def file.
   * @param position Position in file.
   * @return Locations for references.
   */
  public List<Location> provideReferences(
      final ProductDefFile productDefFile, final Position position) {
    final AstNode node = productDefFile.getTopNode();
    final AstNode positionNode = AstQuery.nodeAt(node, position);
    return this.provideReferences(this.productDefModules, productDefFile, positionNode);
  }

  /**
   * Provide references.
   *
   * @param moduleDefFile Module.def file.
   * @param position Position in file.
   * @return Locations for references.
   */
  public List<Location> provideReferences(
      final ModuleDefFile moduleDefFile, final Position position) {
    final AstNode node = moduleDefFile.getTopNode();
    final AstNode positionNode = AstQuery.nodeAt(node, position);
    return this.provideReferences(this.moduleDefModules, moduleDefFile, positionNode);
  }

  /**
   * Provide references.
   *
   * @param loadListFile LoadList file.
   * @param position Position in file.
   * @return Locations for references.
   */
  public List<Location> provideReferences(
      final LoadListFile loadListFile, final Position position) {
    return Collections.emptyList();
  }

  /**
   * Provide references.
   *
   * @param magikFile Magik file.
   * @param position Position in file.
   * @return Locations for references.
   */
  public List<Location> provideReferences(final MagikTypedFile magikFile, final Position position) {
    // Should always be on an identifier.
    final AstNode node = magikFile.getTopNode();
    final AstNode positionNode = AstQuery.nodeAt(node, position, MagikGrammar.IDENTIFIER);
    return this.provideReferences(this.magikModules, magikFile, positionNode);
  }

  private <T extends OpenedFile> List<Location> provideReferences(
      final List<ReferencesModule<T>> modules, final T file, final AstNode positionNode) {
    if (positionNode == null) {
      return Collections.emptyList();
    }

    // Run the modules in order; the first module claiming the context provides the locations.
    final ReferencesContext<T> context = new ReferencesContext<>(file, positionNode);
    for (final ReferencesModule<T> module : modules) {
      final Optional<List<Location>> result = module.tryReferences(context);
      if (result.isPresent()) {
        return result.get();
      }
    }

    return Collections.emptyList();
  }
}
