package nl.ramsolutions.sw.sonar;

import nl.ramsolutions.sw.checks.LoadListCheckList;
import nl.ramsolutions.sw.sonar.language.LoadListLanguage;
import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition;
import org.sonarsource.analyzer.commons.BuiltInQualityProfileJsonLoader;

/** Magik Sonar Way profile. */
public final class LoadListSonarWayProfile implements BuiltInQualityProfilesDefinition {

  private static final String PROFILE_NAME = "Sonar way";
  private static final String PROFILE_LOCATION =
      LoadListCheckList.PROFILE_DIR + "/Sonar_way_profile.json";

  @Override
  public void define(final Context context) {
    final NewBuiltInQualityProfile profile =
        context.createBuiltInQualityProfile(PROFILE_NAME, LoadListLanguage.KEY);
    BuiltInQualityProfileJsonLoader.load(
        profile, LoadListCheckList.REPOSITORY_KEY, LoadListSonarWayProfile.PROFILE_LOCATION);
    profile.done();
  }
}
