package nl.ramsolutions.sw.sonar;

import java.util.List;
import java.util.stream.Collectors;
import nl.ramsolutions.sw.checks.ProductDefCheckList;
import nl.ramsolutions.sw.sonar.language.ProductDef;
import org.sonar.api.SonarRuntime;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonarsource.analyzer.commons.RuleMetadataLoader;

/** ProductDef rules definition. */
public class ProductDefRulesDefinition implements RulesDefinition {

  private static final String REPOSITORY_NAME = "SonarAnalyzer";

  private final SonarRuntime runtime;

  public ProductDefRulesDefinition(final SonarRuntime runtime) {
    this.runtime = runtime;
  }

  @Override
  public void define(final Context context) {
    final NewRepository repository =
        context
            .createRepository(ProductDefCheckList.REPOSITORY_KEY, ProductDef.KEY)
            .setName(ProductDefRulesDefinition.REPOSITORY_NAME);

    this.loadProductDefRules(repository);

    repository.done();
  }

  private void loadProductDefRules(final NewRepository repository) {
    final RuleMetadataLoader loader =
        new RuleMetadataLoader(ProductDefCheckList.PROFILE_DIR, this.runtime);

    final List<Class<?>> checkClasses =
        ProductDefCheckList.getChecks().stream()
            .map(clazz -> (Class<?>) clazz)
            .collect(Collectors.toUnmodifiableList());
    loader.addRulesByAnnotatedClass(repository, checkClasses);
  }
}
