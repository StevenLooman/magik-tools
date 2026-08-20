package nl.ramsolutions.sw.magik.analysis.definitions;

import java.net.URI;
import java.nio.file.Path;
import java.util.Collection;
import nl.ramsolutions.sw.IDefinition;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import nl.ramsolutions.sw.moduledef.ModuleDefinition;
import nl.ramsolutions.sw.productdef.ProductDefinition;

/** Definition keeper. */
public interface IDefinitionKeeper {

  void add(ProductDefinition definition);

  void add(ModuleDefinition definition);

  void add(MagikFileDefinition definition);

  void add(PackageDefinition definition);

  void add(ExemplarDefinition definition);

  void add(MethodDefinition definition);

  /**
   * Add a {@link SlotDefinition}.
   *
   * @param definition {@link SlotDefinition} to add.
   */
  void add(SlotDefinition definition);

  /**
   * Add an {@link InheritanceDefinition}.
   *
   * @param definition {@link InheritanceDefinition} to add.
   */
  void add(InheritanceDefinition definition);

  void add(GlobalDefinition definition);

  void add(BinaryOperatorDefinition definition);

  void add(ConditionDefinition definition);

  void add(ProcedureDefinition definition);

  void add(IDefinition definition);

  void remove(ProductDefinition definition);

  void remove(ModuleDefinition definition);

  void remove(MagikFileDefinition definition);

  void remove(PackageDefinition definition);

  void remove(ExemplarDefinition definition);

  void remove(MethodDefinition definition);

  /**
   * Remove a {@link SlotDefinition}.
   *
   * @param definition {@link SlotDefinition} to remove.
   */
  void remove(SlotDefinition definition);

  /**
   * Remove an {@link InheritanceDefinition}.
   *
   * @param definition {@link InheritanceDefinition} to remove.
   */
  void remove(InheritanceDefinition definition);

  void remove(GlobalDefinition definition);

  void remove(BinaryOperatorDefinition definition);

  void remove(ConditionDefinition definition);

  void remove(ProcedureDefinition definition);

  void remove(IDefinition definition);

  Collection<ProductDefinition> getProductDefinitions(String name);

  Collection<ProductDefinition> getProductDefinitions();

  Collection<ModuleDefinition> getModuleDefinitions(String name);

  Collection<ModuleDefinition> getModuleDefinitions();

  Collection<MagikFileDefinition> getMagikFileDefinitions(URI uri);

  Collection<MagikFileDefinition> getMagikFileDefinitions();

  Collection<PackageDefinition> getPackageDefinitions(String name);

  Collection<PackageDefinition> getPackageDefinitions();

  Collection<ExemplarDefinition> getExemplarDefinitions(TypeString typeName);

  Collection<ExemplarDefinition> getExemplarDefinitions();

  Collection<MethodDefinition> getMethodDefinitions(TypeString typeName);

  Collection<MethodDefinition> getMethodDefinitions();

  /**
   * Get {@link SlotDefinition}s for the given owner {@link TypeString}.
   *
   * @param typeString Owner of the slots.
   * @return {@link SlotDefinition}s owned by the given type.
   */
  Collection<SlotDefinition> getSlotDefinitions(TypeString typeString);

  /**
   * Get all {@link SlotDefinition}s.
   *
   * @return All {@link SlotDefinition}s.
   */
  Collection<SlotDefinition> getSlotDefinitions();

  /**
   * Get {@link InheritanceDefinition}s for the given child {@link TypeString}.
   *
   * @param typeString Child type.
   * @return {@link InheritanceDefinition}s whose child is the given type.
   */
  Collection<InheritanceDefinition> getInheritanceDefinitions(TypeString typeString);

  /**
   * Get all {@link InheritanceDefinition}s.
   *
   * @return All {@link InheritanceDefinition}s.
   */
  Collection<InheritanceDefinition> getInheritanceDefinitions();

  Collection<GlobalDefinition> getGlobalDefinitions(TypeString typeName);

  Collection<GlobalDefinition> getGlobalDefinitions();

  Collection<BinaryOperatorDefinition> getBinaryOperatorDefinitions(
      String operator, TypeString lhs, TypeString rhs);

  Collection<BinaryOperatorDefinition> getBinaryOperatorDefinitions();

  Collection<ConditionDefinition> getConditionDefinitions(String name);

  Collection<ConditionDefinition> getConditionDefinitions();

  Collection<ProcedureDefinition> getProcedureDefinitions(TypeString typeName);

  Collection<ProcedureDefinition> getProcedureDefinitions();

  /**
   * Get all {@link IDefinitions} from path or lower.
   *
   * <p>Used when a directory is deleted or renamed, since we only get the delete of the directory
   * itself, not the individual files within the directory or sub-directories.
   *
   * @param path Path to search from.
   * @return Definitions.
   */
  Collection<IDefinition> getDefinitionsByPath(Path path);

  /**
   * Atomically replace all definitions for {@code path} with {@code definitions}.
   *
   * <p>A definition that survives the replacement is never momentarily absent to a concurrent
   * reader, unlike removing the old definitions and then adding the new ones. Used when re-indexing
   * an edited file.
   *
   * @param path Path whose definitions to replace.
   * @param definitions New definitions for the path.
   */
  default void replaceDefinitionsForPath(Path path, Collection<IDefinition> definitions) {
    throw new UnsupportedOperationException();
  }

  void clear();
}
