package org.stevenlooman.sw.sonar;

import org.sonar.api.profiles.ProfileDefinition;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.utils.ValidationMessages;
import org.sonarsource.analyzer.commons.ProfileDefinitionReader;
import org.stevenlooman.sw.magik.CheckList;
import org.stevenlooman.sw.sonar.language.Magik;

public final class MagikProfile extends ProfileDefinition {

  private final RuleFinder ruleFinder;

  public MagikProfile(RuleFinder ruleFinder) {
    this.ruleFinder = ruleFinder;
  }

  @Override
  public RulesProfile createProfile(ValidationMessages messages) {
    RulesProfile profile = RulesProfile.create(CheckList.SONAR_WAY_PROFILE, Magik.KEY);
    ProfileDefinitionReader definitionReader = new ProfileDefinitionReader(ruleFinder);
    definitionReader.activateRules(profile, CheckList.REPOSITORY_KEY, CheckList.PROFILE_LOCATION);
    return profile;
  }
}
