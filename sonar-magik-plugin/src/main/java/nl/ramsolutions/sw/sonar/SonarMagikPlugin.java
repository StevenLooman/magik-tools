package nl.ramsolutions.sw.sonar;

import nl.ramsolutions.sw.sonar.language.LoadListLanguage;
import nl.ramsolutions.sw.sonar.language.MagikLanguage;
import nl.ramsolutions.sw.sonar.language.ProductModuleDefLanguage;
import nl.ramsolutions.sw.sonar.sensors.LoadListSensor;
import nl.ramsolutions.sw.sonar.sensors.MagikSensor;
import nl.ramsolutions.sw.sonar.sensors.ModuleDefSensor;
import nl.ramsolutions.sw.sonar.sensors.ProductDefSensor;
import org.sonar.api.Plugin;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.resources.Qualifiers;

/** SonarQube plugin for Smallworld Magik and related files. */
public class SonarMagikPlugin implements Plugin {

  @Override
  public void define(final Context context) {
    this.defineMagik(context);
    this.defineProductModuleDef(context);
    this.defineLoadList(context);
  }

  private void defineMagik(final Context context) {
    context.addExtension(MagikLanguage.class);
    context.addExtension(
        PropertyDefinition.builder(MagikLanguage.FILE_SUFFIXES_KEY)
            .defaultValue(MagikLanguage.DEFAULT_FILE_SUFFIXES)
            .category(MagikLanguage.MAGIK_CATEGORY)
            .name("File suffixes")
            .multiValues(true)
            .description(
                "List of suffixes for Magik files to analyze. To not filter, leave the list empty.")
            .subCategory("General")
            .onQualifiers(Qualifiers.PROJECT)
            .build());
    context.addExtension(MagikSonarWayProfile.class);
    context.addExtension(MagikRulesDefinition.class);
    context.addExtension(MagikSensor.class);
  }

  private void defineProductModuleDef(final Context context) {
    context.addExtension(ProductModuleDefLanguage.class);
    // Keep this, otherwise SonarQube cannot decide which suffixes to use.
    context.addExtension(
        PropertyDefinition.builder(ProductModuleDefLanguage.FILE_SUFFIXES_KEY)
            .defaultValue(ProductModuleDefLanguage.DEFAULT_FILE_SUFFIXES)
            .category(ProductModuleDefLanguage.PRODUCT_DEF_CATEGORY)
            .name("File suffixes")
            .multiValues(true)
            .description(
                "List of suffixes for product.def and module.def files to analyze. "
                    + "To not filter, leave the list empty.")
            .subCategory("General")
            .onQualifiers(Qualifiers.PROJECT)
            .build());
    context.addExtension(ProductModuleDefRulesDefinition.class);
    context.addExtension(ProductModuleDefSonarWayProfile.class);
    context.addExtension(ProductDefSensor.class);
    context.addExtension(ModuleDefSensor.class);
  }

  private void defineLoadList(final Context context) {
    context.addExtension(LoadListLanguage.class);
    context.addExtension(
        PropertyDefinition.builder(LoadListLanguage.FILE_SUFFIXES_KEY)
            .defaultValue(LoadListLanguage.DEFAULT_FILE_SUFFIXES)
            .category(LoadListLanguage.LOAD_LIST_CATEGORY)
            .name("File suffixes")
            .multiValues(true)
            .description(
                "List of suffixes for load_list.txt and patch_list.txt files to analyze. "
                    + "To not filter, leave the list empty.")
            .subCategory("General")
            .onQualifiers(Qualifiers.PROJECT)
            .build());
    context.addExtension(LoadListSonarWayProfile.class);
    context.addExtension(LoadListRulesDefinition.class);
    context.addExtension(LoadListSensor.class);
  }
}
