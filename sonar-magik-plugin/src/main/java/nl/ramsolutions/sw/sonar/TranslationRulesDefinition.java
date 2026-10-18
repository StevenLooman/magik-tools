package nl.ramsolutions.sw.sonar;

import java.util.List;
import java.util.stream.Collectors;
import nl.ramsolutions.sw.checks.TranslationCheckList;
import nl.ramsolutions.sw.sonar.language.TranslationLanguage;
import org.sonar.api.SonarRuntime;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonarsource.analyzer.commons.RuleMetadataLoader;

/** Translation rules definition. */
public class TranslationRulesDefinition implements RulesDefinition {

  public static final String REPOSITORY_KEY = "sw_translation";

  private static final String REPOSITORY_NAME = "SonarAnalyzer";

  private final SonarRuntime runtime;

  public TranslationRulesDefinition(final SonarRuntime runtime) {
    this.runtime = runtime;
  }

  @Override
  public void define(final Context context) {
    final NewRepository repository =
        context
            .createRepository(TranslationRulesDefinition.REPOSITORY_KEY, TranslationLanguage.KEY)
            .setName(TranslationRulesDefinition.REPOSITORY_NAME);

    this.loadTranslationRules(repository);

    repository.done();
  }

  private void loadTranslationRules(final NewRepository repository) {
    final RuleMetadataLoader loader =
        new RuleMetadataLoader(TranslationCheckList.PROFILE_DIR, this.runtime);

    final List<Class<?>> checkClasses =
        TranslationCheckList.INSTANCE.getChecks().stream()
            .map(clazz -> (Class<?>) clazz)
            .collect(Collectors.toUnmodifiableList());
    loader.addRulesByAnnotatedClass(repository, checkClasses);
  }
}
