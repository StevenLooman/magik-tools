package org.stevenlooman.sw.sonar.language;

import org.apache.commons.lang.StringUtils;
import org.sonar.api.config.Settings;
import org.sonar.api.resources.AbstractLanguage;
import org.stevenlooman.sw.sonar.MagikPlugin;

import java.util.ArrayList;
import java.util.List;

/**
 * Smallworld Magik language.
 */
public class Magik extends AbstractLanguage {

  public static final String KEY = "magik";

  private static final String[] DEFAULT_FILE_SUFFIXES = {"magik"}; // NOSONAR: S1192

  private Settings settings;

  /**
   * Constructor.
   * @param settings Settings.
   */
  public Magik(Settings settings) {
    super(KEY, "Magik");
    this.settings = settings;
  }

  @Override
  public String[] getFileSuffixes() {
    String[] stringArray = settings.getStringArray(MagikPlugin.FILE_SUFFIXES_KEY);
    String[] suffixes = filterEmptyStrings(stringArray);
    return suffixes.length == 0 ? Magik.DEFAULT_FILE_SUFFIXES : suffixes;
  }

  private static String[] filterEmptyStrings(String[] stringArray) {
    List<String> nonEmptyStrings = new ArrayList<>();
    for (String string : stringArray) {
      if (StringUtils.isNotBlank(string.trim())) {
        nonEmptyStrings.add(string.trim());
      }
    }
    return nonEmptyStrings.toArray(new String[nonEmptyStrings.size()]);
  }
}
