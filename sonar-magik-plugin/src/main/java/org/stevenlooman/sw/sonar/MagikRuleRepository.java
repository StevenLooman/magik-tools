package org.stevenlooman.sw.sonar;

import com.google.common.collect.Sets;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonarsource.analyzer.commons.RuleMetadataLoader;
import org.stevenlooman.sw.magik.CheckList;
import org.stevenlooman.sw.sonar.language.Magik;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class MagikRuleRepository implements RulesDefinition {

  private static final String REPOSITORY_NAME = "SonarAnalyzer";
  private static final String RESOURCE_FOLDER = "org/stevenlooman/sw/sonar/l10n/magik/rules";

  private static final Set<String> TEMPLATE_RULE_KEYS = Sets.newHashSet(
      "XPath",
      "MethodComplexity",
      "LineLength",
      "CommentRegularExpression"
      );

  @Override
  public void define(Context context) {
    NewRepository repository = context
        .createRepository(CheckList.REPOSITORY_KEY, Magik.KEY)
        .setName(REPOSITORY_NAME);

    getRuleMetadataLoader().addRulesByAnnotatedClass(repository, getCheckClasses());

    repository.rules().stream()
        .filter(rule -> TEMPLATE_RULE_KEYS.contains(rule.key()))
        .forEach(rule -> rule.setTemplate(true));

    repository.done();
  }

  private static RuleMetadataLoader getRuleMetadataLoader() {
    return new RuleMetadataLoader(RESOURCE_FOLDER, CheckList.PROFILE_LOCATION);
  }

  private static List<Class> getCheckClasses() {
    return StreamSupport.stream(CheckList.getChecks().spliterator(), false)
        .collect(Collectors.toList());
  }

}
