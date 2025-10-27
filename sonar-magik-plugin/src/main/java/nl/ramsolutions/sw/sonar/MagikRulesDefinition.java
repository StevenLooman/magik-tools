package nl.ramsolutions.sw.sonar;

import java.util.List;
import java.util.stream.Collectors;
import nl.ramsolutions.sw.checks.MagikCheckList;
import nl.ramsolutions.sw.sonar.language.MagikLanguage;
import org.sonar.api.SonarRuntime;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonarsource.analyzer.commons.RuleMetadataLoader;

/** Magik rules definition. */
public class MagikRulesDefinition implements RulesDefinition {

  private static final String REPOSITORY_NAME = "SonarAnalyzer";

  private final SonarRuntime runtime;

  public MagikRulesDefinition(final SonarRuntime runtime) {
    this.runtime = runtime;
  }

  @Override
  public void define(final Context context) {
    final NewRepository repository =
        context
            .createRepository(MagikCheckList.REPOSITORY_KEY, MagikLanguage.KEY)
            .setName(MagikRulesDefinition.REPOSITORY_NAME);

    this.loadMagikRules(repository);

    repository.done();
  }

  private void loadMagikRules(final NewRepository repository) {
    final RuleMetadataLoader loader =
        new RuleMetadataLoader(MagikCheckList.PROFILE_DIR, this.runtime);

    final List<Class<?>> checkClasses =
        MagikCheckList.INSTANCE.getChecks().stream()
            .map(clazz -> (Class<?>) clazz)
            .collect(Collectors.toUnmodifiableList());
    loader.addRulesByAnnotatedClass(repository, checkClasses);
  }
}
