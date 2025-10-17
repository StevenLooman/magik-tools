package nl.ramsolutions.sw.checks.magik;

import java.net.URI;
import nl.ramsolutions.sw.OpenedFile;
import nl.ramsolutions.sw.checks.Check;
import nl.ramsolutions.sw.checks.CheckAssert;
import nl.ramsolutions.sw.checks.MagikCheck;
import nl.ramsolutions.sw.magik.MagikFile;

/**
 * Assertions for {@link MagikCheck} instances.
 *
 * <p>Use via {@link #assertThat(MagikCheck)}.
 */
public class MagikCheckAssert extends CheckAssert {

  protected MagikCheckAssert(final Check actual) {
    super(actual);
  }

  /**
   * Get a new instance for the given {@link Check}.
   *
   * @param actual {@link Check} instance.
   * @return Self.
   */
  public static MagikCheckAssert assertThat(final MagikCheck actual) {
    return new MagikCheckAssert(actual);
  }

  @Override
  protected OpenedFile createOpenedFile(final String code) {
    return new MagikFile(MagikFile.DEFAULT_URI, code);
  }

  @Override
  protected OpenedFile createOpenedFile(final URI uri, final String code) {
    return new MagikFile(uri, code);
  }
}
