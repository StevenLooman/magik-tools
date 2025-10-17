package nl.ramsolutions.sw.sonar;

import nl.ramsolutions.sw.sonar.language.ProductModuleDefLanguage;
import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition;
import org.sonarsource.analyzer.commons.BuiltInQualityProfileJsonLoader;

/**
 * ProductDef and ModuleDef Sonar Way profile.
 *
 * <p>Combined into one {@link BuiltInQualityProfilesDefinition}, because they share the same file
 * suffixes.
 */
public final class ProductModuleDefSonarWayProfile implements BuiltInQualityProfilesDefinition {

  private static final String REPOSITORY_KEY = "product_module_def";
  private static final String PROFILE_NAME = "Sonar way";
  private static final String PROFILE_DIR =
      "nl/ramsolutions/sw/sonar/l10n/product_module_def/rules";
  private static final String PROFILE_LOCATION =
      ProductModuleDefSonarWayProfile.PROFILE_DIR + "/Sonar_way_profile.json";

  @Override
  public void define(final Context context) {
    final NewBuiltInQualityProfile profile =
        context.createBuiltInQualityProfile(
            ProductModuleDefSonarWayProfile.PROFILE_NAME, ProductModuleDefLanguage.KEY);
    BuiltInQualityProfileJsonLoader.load(
        profile,
        ProductModuleDefSonarWayProfile.REPOSITORY_KEY,
        ProductModuleDefSonarWayProfile.PROFILE_LOCATION);
    profile.done();
  }
}
