package nl.ramsolutions.sw.checks;

import java.util.List;
import java.util.Map;
import nl.ramsolutions.sw.checks.message.MessageFileMustStartWithTextEncodingCheck;
import nl.ramsolutions.sw.checks.message.MessageInvalidEscapeCheck;
import nl.ramsolutions.sw.checks.message.MessageSyntaxErrorCheck;

/** Message (.msg) file {@link Check} list. */
public class MessageCheckList extends CheckList<MessageCheck, MessageCodeActionSupplier> {

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final String REPOSITORY_KEY = "sw_message";

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final String PROFILE_DIR = "nl/ramsolutions/sw/sonar/l10n/message/rules";

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final MessageCheckList INSTANCE = new MessageCheckList();

  private MessageCheckList() {}

  /**
   * Get the list of {@link MessageCheck}s.
   *
   * @return List of {@link MessageCheck}s.
   */
  @Override
  public List<Class<? extends MessageCheck>> getChecks() {
    return List.of(
        MessageFileMustStartWithTextEncodingCheck.class,
        MessageInvalidEscapeCheck.class,
        MessageSyntaxErrorCheck.class);
  }

  /**
   * Get the {@link MessageCheck}s which have a {@link MessageCodeActionSupplier}.
   *
   * @return Map of {@link MessageCheck}s and their {@link MessageCodeActionSupplier}s.
   */
  @Override
  public Map<Class<? extends MessageCheck>, List<Class<? extends MessageCodeActionSupplier>>>
      getFixers() {
    return Map.of();
  }
}
