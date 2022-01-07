package nl.ramsolutions.sw.sonar;

import nl.ramsolutions.sw.sonar.language.Magik;
import nl.ramsolutions.sw.sonar.sensors.MagikSquidSensor;
import org.sonar.api.Plugin;

/**
 * Magik plugin.
 */
public class MagikPlugin implements Plugin {

    /**
     * File suffixes key.
     */
    public static final String FILE_SUFFIXES_KEY = "sonar.magik.file.suffixes";

    @Override
    public void define(final Context context) {
        context.addExtension(Magik.class);
        context.addExtension(MagikProfile.class);
        context.addExtension(MagikRuleRepository.class);
        context.addExtension(MagikSquidSensor.class);
    }

}
