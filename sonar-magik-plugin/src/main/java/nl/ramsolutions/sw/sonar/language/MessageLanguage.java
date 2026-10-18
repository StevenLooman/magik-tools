package nl.ramsolutions.sw.sonar.language;

import java.util.Arrays;
import java.util.Objects;
import org.sonar.api.config.Configuration;
import org.sonar.api.resources.AbstractLanguage;

/** Smallworld message (.msg) file language. */
public class MessageLanguage extends AbstractLanguage {

  /** Key for language. */
  public static final String KEY = "sw_message";

  /** Name for language. */
  public static final String NAME = "Smallworld Message";

  /** Category for language. */
  public static final String MESSAGE_CATEGORY = "sw_message";

  /** File suffixes key. */
  public static final String FILE_SUFFIXES_KEY = "sonar.sw_message.file.suffixes";

  /** File suffixes. */
  public static final String DEFAULT_FILE_SUFFIXES = ".msg";

  private final Configuration configuration;

  /**
   * Constructor.
   *
   * @param configuration Configuration.
   */
  public MessageLanguage(final Configuration configuration) {
    super(MessageLanguage.KEY, MessageLanguage.NAME);
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
        this.configuration.getStringArray(MessageLanguage.FILE_SUFFIXES_KEY);
    final String[] suffixes =
        Arrays.stream(stringArray)
            .filter(s -> s != null)
            .filter(s -> !s.trim().isEmpty())
            .toArray(String[]::new);
    return suffixes.length == 0 ? MessageLanguage.DEFAULT_FILE_SUFFIXES.split(",") : suffixes;
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

    final MessageLanguage other = (MessageLanguage) obj;
    return Objects.equals(this.configuration, other.configuration);
  }
}
