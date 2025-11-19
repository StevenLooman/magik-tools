package nl.ramsolutions.sw.sonar.language;

import java.util.Arrays;
import java.util.Objects;
import org.sonar.api.config.Configuration;
import org.sonar.api.resources.AbstractLanguage;

/** Smallworld load_list.txt and patch_list.txt languages. */
public class LoadListLanguage extends AbstractLanguage {

  /** Key for language. */
  public static final String KEY = "load_list";

  /** Name for language. */
  public static final String NAME = "Load List";

  /** Category for language. */
  public static final String LOAD_LIST_CATEGORY = "load_list";

  /** File suffixes key. */
  public static final String FILE_SUFFIXES_KEY = "sonar.load_list.file.suffixes";

  /** File suffixes. */
  public static final String DEFAULT_FILE_SUFFIXES = ".txt";

  private final Configuration configuration;

  /**
   * Constructor.
   *
   * @param configuration Configuration.
   */
  public LoadListLanguage(final Configuration configuration) {
    super(LoadListLanguage.KEY, LoadListLanguage.NAME);
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
        this.configuration.getStringArray(LoadListLanguage.FILE_SUFFIXES_KEY);
    final String[] suffixes =
        Arrays.stream(stringArray)
            .filter(s -> s != null)
            .filter(s -> !s.trim().isEmpty())
            .toArray(String[]::new);
    return suffixes.length == 0 ? LoadListLanguage.DEFAULT_FILE_SUFFIXES.split(",") : suffixes;
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

    final LoadListLanguage other = (LoadListLanguage) obj;
    return Objects.equals(this.configuration, other.configuration);
  }
}
