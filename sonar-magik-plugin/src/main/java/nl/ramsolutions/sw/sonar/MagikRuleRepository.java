package nl.ramsolutions.sw.sonar;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import nl.ramsolutions.sw.magik.checks.CheckList;
import nl.ramsolutions.sw.sonar.language.Magik;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonarsource.analyzer.commons.RuleMetadataLoader;

/**
 * Magik rule repository.
 */
public class MagikRuleRepository implements RulesDefinition {

    private static final String REPOSITORY_NAME = "SonarAnalyzer";

    private List<String> templatedRules() {
        return CheckList.getTemplatedChecks().stream()
            .map(checkClass -> checkClass.getAnnotation(org.sonar.check.Rule.class))
            .filter(Objects::nonNull)
            .map(org.sonar.check.Rule::key)
            .collect(Collectors.toList());
    }

    @Override
    public void define(final Context context) {
        final NewRepository repository = context
            .createRepository(CheckList.REPOSITORY_KEY, Magik.KEY)
            .setName(MagikRuleRepository.REPOSITORY_NAME);

        final RuleMetadataLoader loader = new RuleMetadataLoader(CheckList.PROFILE_DIR, CheckList.PROFILE_LOCATION);
        loader.addRulesByAnnotatedClass(repository, MagikRuleRepository.getCheckClasses());

        final List<String> templatedRules = templatedRules();
        repository.rules().stream()
            .filter(rule -> templatedRules.contains(rule.key()))
            .forEach(rule -> rule.setTemplate(true));

        repository.done();
    }

    private static List<Class<?>> getCheckClasses() {
        return CheckList.getChecks();
    }

}
