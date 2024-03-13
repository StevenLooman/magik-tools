package nl.ramsolutions.sw.sonar;

import nl.ramsolutions.sw.sonar.language.Magik;
import nl.ramsolutions.sw.sonar.sensors.MagikSensor;
import org.sonar.api.Plugin;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.resources.Qualifiers;

/**
 * Magik plugin.
 */
public class MagikPlugin implements Plugin {

    @Override
    public void define(final Context context) {
        context.addExtension(Magik.class);
        context.addExtension(
            PropertyDefinition.builder(Magik.FILE_SUFFIXES_KEY)
                .defaultValue(Magik.DEFAULT_FILE_SUFFIXES)
                .category(Magik.MAGIK_CATEGORY)
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

}
