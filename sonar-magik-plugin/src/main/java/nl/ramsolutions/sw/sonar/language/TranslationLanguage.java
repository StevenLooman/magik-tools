package nl.ramsolutions.sw.sonar.language;

import java.util.Arrays;
import java.util.Objects;
import org.sonar.api.config.Configuration;
import org.sonar.api.resources.AbstractLanguage;

/** Smallworld translation (.trn) file language. */
public class TranslationLanguage extends AbstractLanguage {

  /** Key for language. */
  public static final String KEY = "sw_translation";

  /** Name for language. */
  public static final String NAME = "Smallworld Translation";

  /** Category for language. */
  public static final String TRANSLATION_CATEGORY = "sw_translation";

  /** File suffixes key. */
  public static final String FILE_SUFFIXES_KEY = "sonar.sw_translation.file.suffixes";

  /** File suffixes. */
  public static final String DEFAULT_FILE_SUFFIXES = ".trn";

  private final Configuration configuration;

  /**
   * Constructor.
   *
   * @param configuration Configuration.
   */
  public TranslationLanguage(final Configuration configuration) {
    super(TranslationLanguage.KEY, TranslationLanguage.NAME);
    this.configuration = configuration;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.sonar.api.resources.AbstractLanguage#getFileSuffixes()
   */
  @Override
  public String[] getFileSuffixes() {
    final String[] stringArray =
        this.configuration.getStringArray(TranslationLanguage.FILE_SUFFIXES_KEY);
    final String[] suffixes =
        Arrays.stream(stringArray)
            .filter(s -> s != null)
            .filter(s -> !s.trim().isEmpty())
            .toArray(String[]::new);
    return suffixes.length == 0 ? TranslationLanguage.DEFAULT_FILE_SUFFIXES.split(",") : suffixes;
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

    final TranslationLanguage other = (TranslationLanguage) obj;
    return Objects.equals(this.configuration, other.configuration);
  }
}
