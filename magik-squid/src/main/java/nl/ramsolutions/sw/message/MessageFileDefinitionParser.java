package nl.ramsolutions.sw.message;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.Token;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.analysis.definitions.MessageHandlerUseDefinition;
import nl.ramsolutions.sw.message.api.MessageGrammar;
import nl.ramsolutions.sw.moduledef.ModuleDefFile;

/**
 * Parser to get the {@link MessageFileDefinition} and the {@link MessageHandlerUseDefinition}s of a
 * {@link MessageFile}.
 */
public class MessageFileDefinitionParser {

  private static final String MESSAGES_DIRECTORY = "messages";
  private static final int LANGUAGE_PATH_PARTS_COUNT = 3;

  /**
   * Parse the definition of a message file.
   *
   * @param messageFile Message file to parse.
   * @return Definition of the message file.
   */
  public MessageFileDefinition parseDefinition(final MessageFile messageFile) {
    final URI uri = messageFile.getUri();
    final Location location = new Location(uri);
    final Instant timestamp = messageFile.getTimestamp();
    final String moduleName = MessageFileDefinitionParser.getModuleName(uri);
    final String handlerName = MessageFileDefinitionParser.getHandlerName(uri);
    final String language = MessageFileDefinitionParser.getLanguage(uri);

    final AstNode topNode = messageFile.getTopNode();
    final String[] sourceLines = messageFile.getSourceLines();
    final List<MessageDefinition> messages =
        topNode.getChildren(MessageGrammar.MESSAGE).stream()
            .map(messageNode -> this.parseMessage(uri, sourceLines, messageNode))
            .toList();

    return new MessageFileDefinition(
        location, timestamp, moduleName, handlerName, language, messages);
  }

  /**
   * Parse the message handlers which are used by the message handler, as named by the message file.
   *
   * @param messageFile Message file to parse.
   * @return A definition for each used message handler.
   */
  public List<MessageHandlerUseDefinition> parseUseDefinitions(final MessageFile messageFile) {
    final URI uri = messageFile.getUri();
    final Instant timestamp = messageFile.getTimestamp();
    final String moduleName = MessageFileDefinitionParser.getModuleName(uri);
    final String handlerName = MessageFileDefinitionParser.getHandlerName(uri);

    final AstNode topNode = messageFile.getTopNode();
    return topNode.getDescendants(MessageGrammar.USE_NAME).stream()
        .map(
            useNameNode ->
                new MessageHandlerUseDefinition(
                    new Location(uri, useNameNode),
                    timestamp,
                    moduleName,
                    null,
                    handlerName,
                    useNameNode.getTokenValue()))
        .toList();
  }

  private MessageDefinition parseMessage(
      final URI uri, final String[] sourceLines, final AstNode messageNode) {
    final AstNode idNode = messageNode.getFirstChild(MessageGrammar.MESSAGE_ID);
    final Location location = new Location(uri, idNode);
    final String id = MessageSymbol.nameOf(idNode.getTokenValue());
    final AstNode languageNode = messageNode.getFirstChild(MessageGrammar.LANGUAGE);
    final String language =
        languageNode != null ? MessageSymbol.nameOf(languageNode.getTokenValue()) : null;
    final boolean removal = messageNode.getFirstChild(MessageGrammar.REMOVE) != null;

    final StringBuilder textBuilder = new StringBuilder();
    int line = -1;
    for (final AstNode textLineNode : messageNode.getDescendants(MessageGrammar.TEXT_LINE)) {
      final Token firstToken = textLineNode.getToken();
      final Token lastToken = textLineNode.getLastToken();
      final int tokenLine = firstToken.getLine();
      for (; line != -1 && line < tokenLine; ++line) {
        textBuilder.append('\n');
      }
      line = tokenLine;

      final String sourceLine = sourceLines[tokenLine - 1];
      final int endColumn = lastToken.getColumn() + lastToken.getValue().length();
      textBuilder.append(sourceLine, firstToken.getColumn(), endColumn);
    }

    final TreeSet<Integer> parameters = new TreeSet<>();
    for (final AstNode parameterNode : messageNode.getDescendants(MessageGrammar.PARAMETER)) {
      final String digits = parameterNode.getTokenValue().replaceAll("\\D", "");
      parameters.add(MessageDefinition.parameterNumberOf(digits));
    }

    return new MessageDefinition(
        location, id, language, removal, textBuilder.toString(), new ArrayList<>(parameters));
  }

  /**
   * Get the name of the message handler for a message file: the name of the file, up to the first
   * dot.
   *
   * @param uri URI of the message file.
   * @return Name of the message handler.
   */
  public static String getHandlerName(final URI uri) {
    final String uriPath = uri.getPath();
    final String path = uriPath != null && !uriPath.isEmpty() ? uriPath : uri.toString();
    final String filename = path.substring(path.lastIndexOf('/') + 1);
    final int dotIndex = filename.indexOf('.');
    return dotIndex != -1 ? filename.substring(0, dotIndex) : filename;
  }

  /**
   * Get the language of a message file, i.e., {@code en_gb} for a file in the directory {@code
   * resources/en_gb/messages}.
   *
   * @param uri URI of the message file.
   * @return Language, or null if the file is not in a messages directory.
   */
  @CheckForNull
  public static String getLanguage(final URI uri) {
    final String path = uri.getPath();
    if (path == null) {
      return null;
    }

    final String[] parts = path.split("/");
    final int length = parts.length;
    if (length < LANGUAGE_PATH_PARTS_COUNT
        || !parts[length - 2].equalsIgnoreCase(MESSAGES_DIRECTORY)) {
      return null;
    }

    return parts[length - LANGUAGE_PATH_PARTS_COUNT].toLowerCase();
  }

  @CheckForNull
  private static String getModuleName(final URI uri) {
    final ModuleDefFile moduleDefFile = ModuleDefFile.getModuleDefFileForUri(uri, null);
    return moduleDefFile != null ? moduleDefFile.getModuleDefinition().getName() : null;
  }
}
