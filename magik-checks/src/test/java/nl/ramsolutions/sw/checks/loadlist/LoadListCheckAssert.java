package nl.ramsolutions.sw.checks.loadlist;

import java.net.URI;
import nl.ramsolutions.sw.OpenedFile;
import nl.ramsolutions.sw.checks.Check;
import nl.ramsolutions.sw.checks.CheckAssert;
import nl.ramsolutions.sw.checks.LoadListCheck;
import nl.ramsolutions.sw.loadlist.LoadListFile;

/**
 * Assertions for {@link LoadListCheck} instances.
 *
 * <p>Use via {@link #assertThat(LoadListCheck)}.
 */
public class LoadListCheckAssert extends CheckAssert {

  protected LoadListCheckAssert(final Check actual) {
    super(actual);
  }

  /**
   * Get a new instance for the given {@link Check}.
   *
   * @param actual {@link Check} instance.
   * @return Self.
   */
  public static LoadListCheckAssert assertThat(final LoadListCheck actual) {
    return new LoadListCheckAssert(actual);
  }

  @Override
  protected OpenedFile createOpenedFile(final String code) {
    return new LoadListFile(LoadListFile.DEFAULT_URI, code);
  }

  @Override
  protected OpenedFile createOpenedFile(final URI uri, final String code) {
    return new LoadListFile(uri, code);
  }
}
