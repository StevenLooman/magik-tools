package nl.ramsolutions.sw.message;

import com.sonar.sslr.api.AstNode;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import nl.ramsolutions.sw.MagikToolsProperties;
import nl.ramsolutions.sw.OpenedFile;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.Range;
import nl.ramsolutions.sw.magik.analysis.definitions.MessageHandlerUseDefinition;
import nl.ramsolutions.sw.message.parser.MessageParser;

/** Message (.msg) file. */
public class MessageFile extends OpenedFile {

  public static final String LANGUAGE_ID = "sw-message";
  public static final URI DEFAULT_URI = URI.create("memory://source.msg");
  public static final Location DEFAULT_LOCATION = new Location(DEFAULT_URI, Range.DEFAULT_RANGE);

  private AstNode astNode;

  /**
   * Constructor.
   *
   * @param uri URI.
   * @param source Source.
   */
  public MessageFile(final URI uri, final String source) {
    super(uri, source);
  }

  /**
   * Constructor.
   *
   * @param properties Properties.
   * @param uri URI.
   * @param source Source.
   */
  public MessageFile(final MagikToolsProperties properties, final URI uri, final String source) {
    super(properties, uri, source);
  }

  /**
   * Constructor.
   *
   * @param path Path.
   * @throws IOException -
   */
  public MessageFile(final Path path) throws IOException {
    super(path);
  }

  /**
   * Get the message file definition for this file, with its messages.
   *
   * @return {@link MessageFileDefinition} of this file.
   */
  public MessageFileDefinition getMessageFileDefinition() {
    final MessageFileDefinitionParser parser = new MessageFileDefinitionParser();
    return parser.parseDefinition(this);
  }

  /**
   * Get the message handlers which are used by the message handler of this file, as named by this
   * file.
   *
   * @return {@link MessageHandlerUseDefinition}s of this file.
   */
  public List<MessageHandlerUseDefinition> getMessageHandlerUseDefinitions() {
    final MessageFileDefinitionParser parser = new MessageFileDefinitionParser();
    return parser.parseUseDefinitions(this);
  }

  /**
   * Parse the text for this file and return the top level {@link AstNode}.
   *
   * @return Top level {@link AstNode}.
   */
  public synchronized AstNode getTopNode() {
    if (this.astNode == null) {
      final MessageParser parser = new MessageParser();
      final String source = this.getSource();
      final URI uri = this.getUri();
      this.astNode = parser.parse(source, uri);
    }

    return this.astNode;
  }

  @Override
  public String getLanguageId() {
    return LANGUAGE_ID;
  }

  @Override
  public String toString() {
    return "%s@%s(%s)"
        .formatted(this.getClass().getName(), Integer.toHexString(this.hashCode()), this.getUri());
  }
}
