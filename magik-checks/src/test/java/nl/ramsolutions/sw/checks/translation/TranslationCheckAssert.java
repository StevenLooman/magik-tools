package nl.ramsolutions.sw.checks.translation;

import java.net.URI;
import nl.ramsolutions.sw.OpenedFile;
import nl.ramsolutions.sw.checks.Check;
import nl.ramsolutions.sw.checks.CheckAssert;
import nl.ramsolutions.sw.checks.TranslationCheck;
import nl.ramsolutions.sw.translation.TranslationFile;

/**
 * Assertions for {@link TranslationCheck} instances.
 *
 * <p>Use via {@link #assertThat(TranslationCheck)}.
 */
public class TranslationCheckAssert extends CheckAssert {

  protected TranslationCheckAssert(final Check actual) {
    super(actual);
  }

  public static TranslationCheckAssert assertThat(final TranslationCheck actual) {
    return new TranslationCheckAssert(actual);
  }

  @Override
  protected OpenedFile createOpenedFile(final String code) {
    return new TranslationFile(TranslationFile.DEFAULT_URI, code);
  }

  @Override
  protected OpenedFile createOpenedFile(final URI uri, final String code) {
    return new TranslationFile(uri, code);
  }
}
