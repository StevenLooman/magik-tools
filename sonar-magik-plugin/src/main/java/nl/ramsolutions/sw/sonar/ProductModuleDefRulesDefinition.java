package nl.ramsolutions.sw.sonar;

import java.util.List;
import java.util.stream.Collectors;
import nl.ramsolutions.sw.checks.ModuleDefCheckList;
import nl.ramsolutions.sw.checks.ProductDefCheckList;
import nl.ramsolutions.sw.sonar.language.ProductModuleDefLanguage;
import org.sonar.api.SonarRuntime;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonarsource.analyzer.commons.RuleMetadataLoader;

/**
 * ProductDef and ModuleDef rules definition.
 *
 * <p>Combined into one {@link RulesDefinition}, because they share the same file suffixes.
 */
public class ProductModuleDefRulesDefinition implements RulesDefinition {

  public static final String REPOSITORY_KEY = "product_module_def";

  private static final String REPOSITORY_NAME = "SonarAnalyzer";

  private final SonarRuntime runtime;

  public ProductModuleDefRulesDefinition(final SonarRuntime runtime) {
    this.runtime = runtime;
  }

  @Override
  public void define(final Context context) {
    final NewRepository repository =
        context
            .createRepository(
                ProductModuleDefRulesDefinition.REPOSITORY_KEY, ProductModuleDefLanguage.KEY)
            .setName(ProductModuleDefRulesDefinition.REPOSITORY_NAME);

    this.loadProductDefRules(repository);
    this.loadModuleDefRules(repository);

    repository.done();
  }

  private void loadProductDefRules(final NewRepository repository) {
    final RuleMetadataLoader loader =
        new RuleMetadataLoader(ProductDefCheckList.PROFILE_DIR, this.runtime);

    final List<Class<?>> checkClasses =
        ProductDefCheckList.INSTANCE.getChecks().stream()
            .map(clazz -> (Class<?>) clazz)
            .collect(Collectors.toUnmodifiableList());
    loader.addRulesByAnnotatedClass(repository, checkClasses);
  }

  private void loadModuleDefRules(final NewRepository repository) {
    final RuleMetadataLoader loader =
        new RuleMetadataLoader(ModuleDefCheckList.PROFILE_DIR, this.runtime);

    final List<Class<?>> checkClasses =
        ModuleDefCheckList.INSTANCE.getChecks().stream()
            .map(clazz -> (Class<?>) clazz)
            .collect(Collectors.toUnmodifiableList());
    loader.addRulesByAnnotatedClass(repository, checkClasses);
  }
}
