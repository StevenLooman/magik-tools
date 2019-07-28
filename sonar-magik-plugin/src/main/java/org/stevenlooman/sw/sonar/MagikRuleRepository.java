package org.stevenlooman.sw.sonar;

import org.sonar.api.server.rule.RulesDefinition;
import org.sonarsource.analyzer.commons.RuleMetadataLoader;
import org.stevenlooman.sw.magik.CheckList;
import org.stevenlooman.sw.magik.MagikCheck;
import org.stevenlooman.sw.sonar.language.Magik;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class MagikRuleRepository implements RulesDefinition {

  static final String REPOSITORY_NAME = "SonarAnalyzer";
  static final String RESOURCE_FOLDER = "org/stevenlooman/sw/sonar/l10n/magik/rules";

  private List<String> templatedRules() throws IllegalAccessException, InstantiationException {
    List<String> templatedRules = new ArrayList<>();
    for (Class<?> klass: getCheckClasses()) {
      MagikCheck instance = (MagikCheck) klass.newInstance();
      if (instance.isTemplatedCheck()) {
        continue;
      }
      
      org.sonar.check.Rule rule = klass.getAnnotation(org.sonar.check.Rule.class);
      if (rule == null) {
        continue;
      }

      templatedRules.add(rule.key());
    }
    return templatedRules;
  }

  @Override
  public void define(Context context) {
    NewRepository repository = context
        .createRepository(CheckList.REPOSITORY_KEY, Magik.KEY)
        .setName(REPOSITORY_NAME);

    RuleMetadataLoader loader = new RuleMetadataLoader(RESOURCE_FOLDER, CheckList.PROFILE_LOCATION);
    loader.addRulesByAnnotatedClass(repository, getCheckClasses());

    try {
      List<String> templatedRules = templatedRules();
      repository.rules().stream()
          .filter(rule -> templatedRules.contains(rule.key()))
          .forEach(rule -> rule.setTemplate(true));
    } catch (IllegalAccessException | InstantiationException ex) {

    }

    repository.done();
  }

  private static List<Class> getCheckClasses() {
    return StreamSupport.stream(CheckList.getChecks().spliterator(), false)
        .collect(Collectors.toList());
  }

}
