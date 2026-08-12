package nl.ramsolutions.sw.magik.languageserver.definitions;

import com.sonar.sslr.api.AstNode;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import nl.ramsolutions.sw.OpenedFile;
import nl.ramsolutions.sw.loadlist.LoadListFile;
import nl.ramsolutions.sw.loadlist.api.LoadListGrammar;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.Position;
import nl.ramsolutions.sw.magik.analysis.AstQuery;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.moduledef.ModuleDefFile;
import nl.ramsolutions.sw.productdef.ProductDefFile;
import org.eclipse.lsp4j.ServerCapabilities;

/**
 * Definitions provider. Runs a set of {@link DefinitionModule}s and uses the first claimed result.
 */
public class DefinitionsProvider {

  private final List<DefinitionModule<MagikTypedFile>> magikModules;
  private final List<DefinitionModule<ProductDefFile>> productDefModules;
  private final List<DefinitionModule<ModuleDefFile>> moduleDefModules;
  private final List<DefinitionModule<LoadListFile>> loadListModules;

  /** Constructor. */
  public DefinitionsProvider() {
    this.magikModules = this.createMagikModules();
    this.productDefModules = this.createProductDefModules();
    this.moduleDefModules = this.createModuleDefModules();
    this.loadListModules = this.createLoadListModules();
  }

  /**
   * Create the ordered modules for Magik files. Override to add or remove definition modules.
   *
   * @return Ordered modules.
   */
  protected List<DefinitionModule<MagikTypedFile>> createMagikModules() {
    return List.of(
        new MethodInvocationDefinitionModule(),
        new AtomDefinitionModule(),
        new ConditionDefinitionModule(),
        new SlotDefinitionModule());
  }

  /**
   * Create the ordered modules for product.def files. Override to add or remove definition modules.
   *
   * @return Ordered modules.
   */
  protected List<DefinitionModule<ProductDefFile>> createProductDefModules() {
    return List.of(new ProductNameDefinitionModule());
  }

  /**
   * Create the ordered modules for module.def files. Override to add or remove definition modules.
   *
   * @return Ordered modules.
   */
  protected List<DefinitionModule<ModuleDefFile>> createModuleDefModules() {
    return List.of(new ModuleNameDefinitionModule());
  }

  /**
   * Create the ordered modules for load_list.txt files. Override to add or remove definition
   * modules.
   *
   * @return Ordered modules.
   */
  protected List<DefinitionModule<LoadListFile>> createLoadListModules() {
    return List.of(new FileEntryDefinitionModule());
  }

  /**
   * Set server capabilities.
   *
   * @param capabilities Server capabilities.
   */
  public void setCapabilities(final ServerCapabilities capabilities) {
    capabilities.setDefinitionProvider(true);
  }

  /**
   * Provide definitions.
   *
   * @param productDefFile Product.def file.
   * @param position Position.
   * @return Definitions.
   */
  public List<Location> provideDefinitions(
      final ProductDefFile productDefFile, final Position position) {
    final AstNode node = productDefFile.getTopNode();
    final AstNode positionNode = AstQuery.nodeAt(node, position);
    return this.provideDefinitions(this.productDefModules, productDefFile, positionNode);
  }

  /**
   * Provide definitions.
   *
   * @param moduleDefFile Module.def file.
   * @param position Position.
   * @return Definitions.
   */
  public List<Location> provideDefinitions(
      final ModuleDefFile moduleDefFile, final Position position) {
    final AstNode node = moduleDefFile.getTopNode();
    final AstNode positionNode = AstQuery.nodeAt(node, position);
    return this.provideDefinitions(this.moduleDefModules, moduleDefFile, positionNode);
  }

  /**
   * Provide definitions.
   *
   * @param loadListFile LoadList file.
   * @param position Position.
   * @return Definitions.
   */
  public List<Location> provideDefinitions(
      final LoadListFile loadListFile, final Position position) {
    final AstNode node = loadListFile.getTopNode();
    final AstNode positionNode = AstQuery.nodeAt(node, position, LoadListGrammar.FILE_PATH);
    return this.provideDefinitions(this.loadListModules, loadListFile, positionNode);
  }

  /**
   * Provide definitions.
   *
   * @param magikFile Magik file.
   * @param position Position.
   * @return Definitions.
   */
  public List<Location> provideDefinitions(
      final MagikTypedFile magikFile, final Position position) {
    // Should always be on an identifier.
    final AstNode node = magikFile.getTopNode();
    final AstNode positionNode = AstQuery.nodeAt(node, position, MagikGrammar.IDENTIFIER);
    return this.provideDefinitions(this.magikModules, magikFile, positionNode);
  }

  private <T extends OpenedFile> List<Location> provideDefinitions(
      final List<DefinitionModule<T>> modules, final T file, final AstNode positionNode) {
    if (positionNode == null) {
      return Collections.emptyList();
    }

    // Run the modules in order; the first module claiming the context provides the locations.
    final DefinitionContext<T> context = new DefinitionContext<>(file, positionNode);
    for (final DefinitionModule<T> module : modules) {
      final Optional<List<Location>> result = module.tryDefinitions(context);
      if (result.isPresent()) {
        return result.get();
      }
    }

    return Collections.emptyList();
  }
}
