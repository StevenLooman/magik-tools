package nl.ramsolutions.sw.sonar;

import nl.ramsolutions.sw.checks.TranslationCheckList;
import nl.ramsolutions.sw.sonar.language.TranslationLanguage;
import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition;
import org.sonarsource.analyzer.commons.BuiltInQualityProfileJsonLoader;

/** Translation Sonar Way profile. */
public final class TranslationSonarWayProfile implements BuiltInQualityProfilesDefinition {

  private static final String PROFILE_NAME = "Sonar way";
  private static final String PROFILE_LOCATION =
      TranslationCheckList.PROFILE_DIR + "/Sonar_way_profile.json";

  @Override
  public void define(final Context context) {
    final NewBuiltInQualityProfile profile =
        context.createBuiltInQualityProfile(PROFILE_NAME, TranslationLanguage.KEY);
    BuiltInQualityProfileJsonLoader.load(
        profile, TranslationCheckList.REPOSITORY_KEY, TranslationSonarWayProfile.PROFILE_LOCATION);
    profile.done();
  }
}
