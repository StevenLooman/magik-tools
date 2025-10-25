package nl.ramsolutions.sw.checks;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import nl.ramsolutions.sw.checks.moduledef.ModuleDefMissingDescriptionCheck;
import nl.ramsolutions.sw.checks.moduledef.ModuleDefNameDoesNotMatchDirectoryNameCheck;
import nl.ramsolutions.sw.checks.moduledef.ModuleDefRequiredModuleAlreadyInTestsModulesCheck;
import nl.ramsolutions.sw.checks.moduledef.ModuleDefSyntaxErrorCheck;

/** module.def {@link Check} list. */
public class ModuleDefCheckList {

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final String REPOSITORY_KEY = "module_def";

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final String PROFILE_DIR = "nl/ramsolutions/sw/sonar/l10n/moduledef/rules";

  private ModuleDefCheckList() {}

  /**
   * Get the list of {@link ModuleDefCheck}s.
   *
   * @return List of {@link ModuleDefCheck}s.
   */
  public static List<Class<? extends ModuleDefCheck>> getChecks() {
    return List.of(
        ModuleDefMissingDescriptionCheck.class,
        ModuleDefNameDoesNotMatchDirectoryNameCheck.class,
        ModuleDefRequiredModuleAlreadyInTestsModulesCheck.class,
        ModuleDefSyntaxErrorCheck.class);
  }

  /**
   * Get the list of {@link ModuleDefCheck}s, casted to {@link Check}s.
   *
   * @return List of {@link Check}s.
   */
  public static List<Class<? extends Check>> getBaseChecks() {
    return ModuleDefCheckList.getChecks().stream()
        .map(clazz -> (Class<? extends Check>) clazz)
        .collect(Collectors.toList());
  }

  /**
   * Get the {@link ModuleDefCheck}s which have a {@link CheckFixer}.
   *
   * @return Map of {@link ModuleDefCheck}s and their {@link CheckFixer}s.
   */
  public static Map<Class<? extends ModuleDefCheck>, List<Class<? extends CheckFixer>>>
      getFixers() {
    return Map.of();
  }
}
