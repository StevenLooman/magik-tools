package nl.ramsolutions.sw.sonar;

import java.util.List;
import java.util.stream.Collectors;
import nl.ramsolutions.sw.checks.MagikCheckList;
import nl.ramsolutions.sw.sonar.language.Magik;
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
            .createRepository(MagikCheckList.REPOSITORY_KEY, Magik.KEY)
            .setName(MagikRulesDefinition.REPOSITORY_NAME);

    final RuleMetadataLoader loader =
        new RuleMetadataLoader(
            MagikCheckList.PROFILE_DIR, MagikCheckList.PROFILE_LOCATION, this.runtime);
    loader.addRulesByAnnotatedClass(repository, MagikRulesDefinition.getCheckClasses());

    repository.done();
  }

  private static List<Class<?>> getCheckClasses() {
    return MagikCheckList.getChecks().stream()
        .map(clazz -> (Class<?>) clazz)
        .collect(Collectors.toUnmodifiableList()); // NOSONAR: Keep VSCode/Java plugin sane.
  }
}
