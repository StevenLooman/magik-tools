package nl.ramsolutions.sw.sonar.language;

import java.util.Arrays;
import org.sonar.api.config.Configuration;
import org.sonar.api.resources.AbstractLanguage;

/** Smallworld Magik language. */
public class Magik extends AbstractLanguage {

  /** Key for language. */
  public static final String KEY = "magik";

  /** Name for language. */
  public static final String NAME = "Magik";

  /** Category for language. */
  public static final String MAGIK_CATEGORY = "magik";

  /** File suffixes key. */
  public static final String FILE_SUFFIXES_KEY = "sonar.magik.file.suffixes";

  /** Default file suffixes. */
  public static final String DEFAULT_FILE_SUFFIXES = ".magik";

  private final Configuration configuration;

  /**
   * Constructor.
   *
   * @param configuration Configuration.
   */
  public Magik(final Configuration configuration) {
    super(Magik.KEY, Magik.NAME);
    this.configuration = configuration;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.sonar.api.resources.AbstractLanguage#getFileSuffixes()
   */
  @Override
  public String[] getFileSuffixes() {
    final String[] stringArray = this.configuration.getStringArray(Magik.FILE_SUFFIXES_KEY);
    final String[] suffixes =
        Arrays.stream(stringArray)
            .filter(s -> s != null)
            .filter(s -> !s.trim().isEmpty())
            .toArray(String[]::new);
    return suffixes.length == 0 ? Magik.DEFAULT_FILE_SUFFIXES.split(",") : suffixes;
  }
}
