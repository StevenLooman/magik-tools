package nl.ramsolutions.sw.sonar;

import nl.ramsolutions.sw.checks.ProductDefCheckList;
import nl.ramsolutions.sw.sonar.language.ProductDef;
import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition;
import org.sonarsource.analyzer.commons.BuiltInQualityProfileJsonLoader;

/** ProductDef Sonar Way profile. */
public final class ProductDefSonarWayProfile implements BuiltInQualityProfilesDefinition {

  static final String PROFILE_NAME = "Sonar way";
  static final String PROFILE_LOCATION =
      ProductDefCheckList.PROFILE_DIR + "/Sonar_way_profile.json";

  @Override
  public void define(final Context context) {
    final NewBuiltInQualityProfile profile =
        context.createBuiltInQualityProfile(PROFILE_NAME, ProductDef.KEY);
    BuiltInQualityProfileJsonLoader.load(
        profile, ProductDefCheckList.REPOSITORY_KEY, ProductDefSonarWayProfile.PROFILE_LOCATION);
    profile.done();
  }
}
