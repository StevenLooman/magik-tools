package nl.ramsolutions.sw.message;

import com.sonar.sslr.api.AstNode;
import nl.ramsolutions.sw.message.analysis.MessageAstWalker;

/** Message file visitor. */
public abstract class MessageVisitor extends MessageAstWalker {

  private MessageFile messageFile;

  public MessageFile getMessageFile() {
    return this.messageFile;
  }

  /**
   * Scan the file.
   *
   * @param scannedMessageFile Context to use.
   */
  public void scanFile(final MessageFile scannedMessageFile) {
    this.messageFile = scannedMessageFile;

    final AstNode topNode = this.messageFile.getTopNode();
    this.walkAst(topNode);
  }
}
