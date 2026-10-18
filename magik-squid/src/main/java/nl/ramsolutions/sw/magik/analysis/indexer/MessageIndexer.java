package nl.ramsolutions.sw.magik.analysis.indexer;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import nl.ramsolutions.sw.IDefinition;
import nl.ramsolutions.sw.IgnoreHandler;
import nl.ramsolutions.sw.SourceFileScanner;
import nl.ramsolutions.sw.magik.FileEvent;
import nl.ramsolutions.sw.magik.FileEvent.FileChangeType;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.MessageHandlerUseDefinition;
import nl.ramsolutions.sw.message.MessageFile;
import nl.ramsolutions.sw.message.MessageFileDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Message file indexer. */
public class MessageIndexer {

  private static final Logger LOGGER = LoggerFactory.getLogger(MessageIndexer.class);

  private final IDefinitionKeeper definitionKeeper;
  private final IgnoreHandler ignoreHandler;

  public MessageIndexer(
      final IDefinitionKeeper definitionKeeper, final IgnoreHandler ignoreHandler) {
    this.definitionKeeper = definitionKeeper;
    this.ignoreHandler = ignoreHandler;
  }

  /**
   * Handle file event.
   *
   * @param fileEvent {@link FileEvent} to handle.
   * @throws IOException -
   */
  public synchronized void handleFileEvent(final FileEvent fileEvent) throws IOException {
    LOGGER.debug("Handling file event: {}", fileEvent);

    final FileChangeType fileChangeType = fileEvent.getFileChangeType();
    final Path path = fileEvent.getPath();
    if (fileChangeType == FileChangeType.CHANGED || fileChangeType == FileChangeType.DELETED) {
      // Only drop what this indexer owns: every watched-file event reaches all indexers, and only
      // the owning one puts its definitions back.
      final Collection<IDefinition> definitions = this.definitionKeeper.getDefinitionsByPath(path);
      final List<IDefinition> ownDefinitions =
          definitions.stream().filter(MessageIndexer::isMessageFileOwned).toList();
      ownDefinitions.forEach(this.definitionKeeper::remove);
    }

    if (fileChangeType == FileChangeType.CREATED || fileChangeType == FileChangeType.CHANGED) {
      final SourceFileScanner scanner =
          new SourceFileScanner(this.ignoreHandler, SourceFileScanner.MESSAGE_FILE_FILTER);
      scanner.getFiles(path).forEach(this::indexFile);
    }

    LOGGER.debug("Handled file event: {}", fileEvent);
  }

  /**
   * Test if the definition comes from a message file: the definition of the message file itself, or
   * a message handler which is used, as named by the message file.
   *
   * @param definition Definition to test.
   * @return True if the definition comes from a message file.
   */
  public static boolean isMessageFileOwned(final IDefinition definition) {
    if (definition instanceof MessageFileDefinition) {
      return true;
    }

    final Location location = definition.getLocation();
    return definition instanceof MessageHandlerUseDefinition
        && location != null
        && location
            .getUri()
            .toString()
            .toLowerCase()
            .endsWith(SourceFileScanner.SW_MESSAGE_EXTENSION);
  }

  /**
   * Index a single message file when it is created (or first read).
   *
   * @param path Path to message file.
   */
  @SuppressWarnings("checkstyle:IllegalCatch")
  private void indexFile(final Path path) {
    LOGGER.debug("Indexing created/updated file: {}", path);

    try {
      final MessageFile messageFile = new MessageFile(path);
      final MessageFileDefinition definition = messageFile.getMessageFileDefinition();
      this.definitionKeeper.add(definition);
      messageFile.getMessageHandlerUseDefinitions().forEach(this.definitionKeeper::add);
    } catch (final Exception exception) {
      LOGGER.error("Error indexing created file: " + path, exception);
    }
  }
}
