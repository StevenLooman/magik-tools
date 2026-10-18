package nl.ramsolutions.sw.checks;

import java.util.List;
import nl.ramsolutions.sw.OpenedFile;
import nl.ramsolutions.sw.magik.CodeAction;
import nl.ramsolutions.sw.magik.Range;
import nl.ramsolutions.sw.message.MessageFile;

/** {@link CodeAction} supplier for message (.msg) files. */
public abstract class MessageCodeActionSupplier extends CodeActionSupplier {

  /**
   * Provide {@link CodeAction}s for {@link MessageFile}s.
   *
   * @param messageFile {@link MessageFile} to provide fixes for.
   * @param range {@link Range} to provide fixes for.
   * @return List of {@link CodeAction}s to be applied.
   */
  public abstract List<CodeAction> provideMessageCodeActions(
      final MessageFile messageFile, final Range range);

  @Override
  public List<CodeAction> provideCodeActions(final OpenedFile file, final Range range) {
    if (!(file instanceof MessageFile messageFile)) {
      throw new IllegalArgumentException(
          "OpenedFile is not a MessageFile: " + file.getClass().getName());
    }

    return this.provideMessageCodeActions(messageFile, range);
  }
}
