package nl.ramsolutions.sw.checks.message;

import java.net.URI;
import nl.ramsolutions.sw.OpenedFile;
import nl.ramsolutions.sw.checks.Check;
import nl.ramsolutions.sw.checks.CheckAssert;
import nl.ramsolutions.sw.checks.MessageCheck;
import nl.ramsolutions.sw.message.MessageFile;

/**
 * Assertions for {@link MessageCheck} instances.
 *
 * <p>Use via {@link #assertThat(MessageCheck)}.
 */
public class MessageCheckAssert extends CheckAssert {

  protected MessageCheckAssert(final Check actual) {
    super(actual);
  }

  public static MessageCheckAssert assertThat(final MessageCheck actual) {
    return new MessageCheckAssert(actual);
  }

  @Override
  protected OpenedFile createOpenedFile(final String code) {
    return new MessageFile(MessageFile.DEFAULT_URI, code);
  }

  @Override
  protected OpenedFile createOpenedFile(final URI uri, final String code) {
    return new MessageFile(uri, code);
  }
}
