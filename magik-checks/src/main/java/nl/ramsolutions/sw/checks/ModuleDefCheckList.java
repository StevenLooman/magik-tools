package nl.ramsolutions.sw.checks;

import java.util.List;
import java.util.Map;
import nl.ramsolutions.sw.checks.moduledef.ModuleDefMissingDescriptionCheck;
import nl.ramsolutions.sw.checks.moduledef.ModuleDefNameDoesNotMatchDirectoryNameCheck;
import nl.ramsolutions.sw.checks.moduledef.ModuleDefRequiredModuleAlreadyInTestsModulesCheck;
import nl.ramsolutions.sw.checks.moduledef.ModuleDefSyntaxErrorCheck;

/** module.def {@link Check} list. */
public class ModuleDefCheckList extends CheckList<ModuleDefCheck, ModuleDefCodeActionSupplier> {

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final String REPOSITORY_KEY = "module_def";

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final String PROFILE_DIR = "nl/ramsolutions/sw/sonar/l10n/moduledef/rules";

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final ModuleDefCheckList INSTANCE = new ModuleDefCheckList();

  private ModuleDefCheckList() {}

  /**
   * Get the list of {@link ModuleDefCheck}s.
   *
   * @return List of {@link ModuleDefCheck}s.
   */
  @Override
  public List<Class<? extends ModuleDefCheck>> getChecks() {
    return List.of(
        ModuleDefMissingDescriptionCheck.class,
        ModuleDefNameDoesNotMatchDirectoryNameCheck.class,
        ModuleDefRequiredModuleAlreadyInTestsModulesCheck.class,
        ModuleDefSyntaxErrorCheck.class);
  }

  /**
   * Get the {@link ModuleDefCheck}s which have a {@link ModuleDefCodeActionSupplier}.
   *
   * @return Map of {@link ModuleDefCheck}s and their {@link ModuleDefCodeActionSupplier}s.
   */
  @Override
  public Map<Class<? extends ModuleDefCheck>, List<Class<? extends ModuleDefCodeActionSupplier>>>
      getFixers() {
    return Map.of();
  }
}
