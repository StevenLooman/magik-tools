package nl.ramsolutions.sw.sonar.language;

import java.util.ArrayList;
import java.util.List;
import nl.ramsolutions.sw.sonar.MagikPlugin;
import org.sonar.api.config.Configuration;
import org.sonar.api.resources.AbstractLanguage;

/**
 * Smallworld Magik language.
 */
public class Magik extends AbstractLanguage {

    /**
     * Key for language.
     */
    public static final String KEY = "magik";

    /**
     * Name for language.
     */
    public static final String NAME = "Magik";

    private static final String MAGIK_SUFFIX = "magik";
    private static final String[] DEFAULT_FILE_SUFFIXES = {MAGIK_SUFFIX};

    private final Configuration configuration;

    /**
     * Constructor.
     * @param configuration Configuration.
     */
    public Magik(final Configuration configuration) {
        super(KEY, NAME);
        this.configuration = configuration;
    }

    @Override
    public String[] getFileSuffixes() {
        final String[] stringArray = this.configuration.getStringArray(MagikPlugin.FILE_SUFFIXES_KEY);
        final String[] suffixes = filterEmptyStrings(stringArray);
        return suffixes.length == 0 ? Magik.DEFAULT_FILE_SUFFIXES : suffixes;
    }

    private static String[] filterEmptyStrings(String[] stringArray) {
        final List<String> nonEmptyStrings = new ArrayList<>();
        for (final String string : stringArray) {
            if (!string.trim().isEmpty()) {
                nonEmptyStrings.add(string.trim());
            }
        }
        return nonEmptyStrings.toArray(new String[nonEmptyStrings.size()]);
    }
}
