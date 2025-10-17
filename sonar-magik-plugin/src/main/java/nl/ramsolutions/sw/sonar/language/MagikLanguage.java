package nl.ramsolutions.sw.sonar.language;

import java.util.Arrays;
import java.util.Objects;
import org.sonar.api.config.Configuration;
import org.sonar.api.resources.AbstractLanguage;

/** Smallworld Magik language. */
public class MagikLanguage extends AbstractLanguage {

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
  public MagikLanguage(final Configuration configuration) {
    super(MagikLanguage.KEY, MagikLanguage.NAME);
    this.configuration = configuration;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.sonar.api.resources.AbstractLanguage#getFileSuffixes()
   */
  @Override
  public String[] getFileSuffixes() {
    final String[] stringArray = this.configuration.getStringArray(MagikLanguage.FILE_SUFFIXES_KEY);
    final String[] suffixes =
        Arrays.stream(stringArray)
            .filter(s -> s != null)
            .filter(s -> !s.trim().isEmpty())
            .toArray(String[]::new);
    return suffixes.length == 0 ? MagikLanguage.DEFAULT_FILE_SUFFIXES.split(",") : suffixes;
  }

  @Override
  public int hashCode() {
    return super.hashCode() + Objects.hash(this.configuration);
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }

    if (!super.equals(obj)) {
      return false;
    }

    if (this.getClass() != obj.getClass()) {
      return false;
    }

    final MagikLanguage other = (MagikLanguage) obj;
    return Objects.equals(this.configuration, other.configuration);
  }
}
