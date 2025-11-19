package nl.ramsolutions.sw.sonar;

import java.util.List;
import java.util.stream.Collectors;
import nl.ramsolutions.sw.checks.LoadListCheckList;
import nl.ramsolutions.sw.sonar.language.LoadListLanguage;
import org.sonar.api.SonarRuntime;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonarsource.analyzer.commons.RuleMetadataLoader;

/** LoadList rules definition. */
public class LoadListRulesDefinition implements RulesDefinition {

  public static final String REPOSITORY_KEY = "load_list";

  private static final String REPOSITORY_NAME = "SonarAnalyzer";

  private final SonarRuntime runtime;

  public LoadListRulesDefinition(final SonarRuntime runtime) {
    this.runtime = runtime;
  }

  @Override
  public void define(final Context context) {
    final NewRepository repository =
        context
            .createRepository(LoadListRulesDefinition.REPOSITORY_KEY, LoadListLanguage.KEY)
            .setName(LoadListRulesDefinition.REPOSITORY_NAME);

    this.loadLoadListRules(repository);

    repository.done();
  }

  private void loadLoadListRules(final NewRepository repository) {
    final RuleMetadataLoader loader =
        new RuleMetadataLoader(LoadListCheckList.PROFILE_DIR, this.runtime);

    final List<Class<?>> checkClasses =
        LoadListCheckList.INSTANCE.getChecks().stream()
            .map(clazz -> (Class<?>) clazz)
            .collect(Collectors.toUnmodifiableList());
    loader.addRulesByAnnotatedClass(repository, checkClasses);
  }
}
