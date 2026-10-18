package nl.ramsolutions.sw.sonar;

import java.util.List;
import java.util.stream.Collectors;
import nl.ramsolutions.sw.checks.MessageCheckList;
import nl.ramsolutions.sw.sonar.language.MessageLanguage;
import org.sonar.api.SonarRuntime;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonarsource.analyzer.commons.RuleMetadataLoader;

/** Message rules definition. */
public class MessageRulesDefinition implements RulesDefinition {

  public static final String REPOSITORY_KEY = "sw_message";

  private static final String REPOSITORY_NAME = "SonarAnalyzer";

  private final SonarRuntime runtime;

  public MessageRulesDefinition(final SonarRuntime runtime) {
    this.runtime = runtime;
  }

  @Override
  public void define(final Context context) {
    final NewRepository repository =
        context
            .createRepository(MessageRulesDefinition.REPOSITORY_KEY, MessageLanguage.KEY)
            .setName(MessageRulesDefinition.REPOSITORY_NAME);

    this.loadMessageRules(repository);

    repository.done();
  }

  private void loadMessageRules(final NewRepository repository) {
    final RuleMetadataLoader loader =
        new RuleMetadataLoader(MessageCheckList.PROFILE_DIR, this.runtime);

    final List<Class<?>> checkClasses =
        MessageCheckList.INSTANCE.getChecks().stream()
            .map(clazz -> (Class<?>) clazz)
            .collect(Collectors.toUnmodifiableList());
    loader.addRulesByAnnotatedClass(repository, checkClasses);
  }
}
