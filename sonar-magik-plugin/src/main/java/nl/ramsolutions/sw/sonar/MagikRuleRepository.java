package nl.ramsolutions.sw.sonar;

import java.util.List;
import java.util.stream.Collectors;
import nl.ramsolutions.sw.magik.checks.CheckList;
import nl.ramsolutions.sw.sonar.language.Magik;
import org.sonar.api.SonarRuntime;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonarsource.analyzer.commons.RuleMetadataLoader;

/**
 * Magik rule repository.
 */
public class MagikRuleRepository implements RulesDefinition {

    private static final String REPOSITORY_NAME = "SonarAnalyzer";

    private final SonarRuntime sonarRuntime;

    public MagikRuleRepository(final SonarRuntime sonarRuntime) {
        this.sonarRuntime = sonarRuntime;
    }

    @Override
    public void define(final Context context) {
        final NewRepository repository = context
            .createRepository(CheckList.REPOSITORY_KEY, Magik.KEY)
            .setName(MagikRuleRepository.REPOSITORY_NAME);

        final RuleMetadataLoader loader =
            new RuleMetadataLoader(CheckList.PROFILE_DIR, CheckList.PROFILE_LOCATION, this.sonarRuntime);
        loader.addRulesByAnnotatedClass(repository, MagikRuleRepository.getCheckClasses());

        repository.done();
    }

    private static List<Class<?>> getCheckClasses() {
        return CheckList.getChecks().stream()
            .map(clazz -> (Class<?>) clazz)
            .collect(Collectors.toList());
    }

}
