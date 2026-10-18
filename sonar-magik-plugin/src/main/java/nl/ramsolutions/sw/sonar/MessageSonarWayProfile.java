package nl.ramsolutions.sw.sonar;

import nl.ramsolutions.sw.checks.MessageCheckList;
import nl.ramsolutions.sw.sonar.language.MessageLanguage;
import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition;
import org.sonarsource.analyzer.commons.BuiltInQualityProfileJsonLoader;

/** Message Sonar Way profile. */
public final class MessageSonarWayProfile implements BuiltInQualityProfilesDefinition {

  private static final String PROFILE_NAME = "Sonar way";
  private static final String PROFILE_LOCATION =
      MessageCheckList.PROFILE_DIR + "/Sonar_way_profile.json";

  @Override
  public void define(final Context context) {
    final NewBuiltInQualityProfile profile =
        context.createBuiltInQualityProfile(PROFILE_NAME, MessageLanguage.KEY);
    BuiltInQualityProfileJsonLoader.load(
        profile, MessageCheckList.REPOSITORY_KEY, MessageSonarWayProfile.PROFILE_LOCATION);
    profile.done();
  }
}
