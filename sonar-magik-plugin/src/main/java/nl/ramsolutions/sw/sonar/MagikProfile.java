package nl.ramsolutions.sw.sonar;

import nl.ramsolutions.sw.magik.checks.CheckList;
import nl.ramsolutions.sw.sonar.language.Magik;
import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition;
import org.sonarsource.analyzer.commons.BuiltInQualityProfileJsonLoader;

/** Magik profile. */
public final class MagikProfile implements BuiltInQualityProfilesDefinition {

  static final String PROFILE_NAME = "Sonar way";
  static final String PROFILE_LOCATION = CheckList.PROFILE_DIR + "/Sonar_way_profile.json";

  @Override
  public void define(final Context context) {
    final NewBuiltInQualityProfile profile =
        context.createBuiltInQualityProfile(PROFILE_NAME, Magik.KEY);
    BuiltInQualityProfileJsonLoader.load(profile, CheckList.REPOSITORY_KEY, PROFILE_LOCATION);
    profile.done();
  }
}
