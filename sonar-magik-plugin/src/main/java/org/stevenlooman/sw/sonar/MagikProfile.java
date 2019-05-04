package org.stevenlooman.sw.sonar;

import static org.stevenlooman.sw.sonar.MagikRuleRepository.RESOURCE_FOLDER;

import org.sonar.api.SonarRuntime;
import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition;
import org.sonarsource.analyzer.commons.BuiltInQualityProfileJsonLoader;
import org.stevenlooman.sw.magik.CheckList;
import org.stevenlooman.sw.sonar.language.Magik;

public final class MagikProfile implements BuiltInQualityProfilesDefinition {

  static final String PROFILE_NAME = "Sonar way";
  static final String PROFILE_LOCATION = RESOURCE_FOLDER + "/Sonar_way_profile.json";
  private final SonarRuntime sonarRuntime;

  public MagikProfile(SonarRuntime sonarRuntime) {
    this.sonarRuntime = sonarRuntime;
  }

  @Override
  public void define(Context context) {
    NewBuiltInQualityProfile profile = context.createBuiltInQualityProfile(PROFILE_NAME, Magik.KEY);
    BuiltInQualityProfileJsonLoader.load(
        profile, CheckList.REPOSITORY_KEY, PROFILE_LOCATION, RESOURCE_FOLDER, sonarRuntime);
    profile.done();
  }
}
