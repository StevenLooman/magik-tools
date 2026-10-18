package nl.ramsolutions.sw.magik.analysis.indexer;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import nl.ramsolutions.sw.IgnoreHandler;
import nl.ramsolutions.sw.MagikToolsProperties;
import nl.ramsolutions.sw.magik.FileEvent;
import nl.ramsolutions.sw.magik.FileEvent.FileChangeType;
import nl.ramsolutions.sw.magik.analysis.definitions.DefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.MessageHandlerUseDefinition;
import nl.ramsolutions.sw.message.MessageDefinition;
import nl.ramsolutions.sw.message.MessageFileDefinition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Test {@link MessageIndexer}. */
class MessageIndexerTest {

  private Path createMessageFile(final Path tempDir, final String code) throws IOException {
    final Path modulePath = tempDir.resolve("test_module");
    final Path messagesPath = modulePath.resolve("resources").resolve("en_gb").resolve("messages");
    Files.createDirectories(messagesPath);
    Files.writeString(modulePath.resolve("module.def"), "test_module 1\n");
    final Path messagePath = messagesPath.resolve("pipe_editor.msg");
    Files.writeString(messagePath, code);
    return messagePath;
  }

  @Test
  void testFileCreated(final @TempDir Path tempDir) throws IOException {
    final Path messagePath = this.createMessageFile(tempDir, "+editor\n:title Pipes\n");
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper(false);
    final MessageIndexer indexer = new MessageIndexer(definitionKeeper, new IgnoreHandler());

    indexer.handleFileEvent(new FileEvent(messagePath.toUri(), FileChangeType.CREATED));

    final Collection<MessageFileDefinition> definitions =
        definitionKeeper.getMessageFileDefinitions("pipe_editor");
    assertThat(definitions).hasSize(1);
    final MessageFileDefinition definition = definitions.iterator().next();
    assertThat(definition.getLocation().getUri()).isEqualTo(messagePath.toUri());
    assertThat(definition.getTimestamp()).isNotNull();
    assertThat(definition.getModuleName()).isEqualTo("test_module");
    assertThat(definition.getLanguage()).isEqualTo("en_gb");
    assertThat(definition.getMessages())
        .extracting(MessageDefinition::getId)
        .containsExactly("title");
    assertThat(definitionKeeper.getMessageFileDefinitions()).hasSize(1);

    final Collection<MessageHandlerUseDefinition> useDefinitions =
        definitionKeeper.getMessageHandlerUseDefinitions("pipe_editor");
    assertThat(useDefinitions)
        .extracting(MessageHandlerUseDefinition::getUsedHandlerName)
        .containsExactly("editor");
    assertThat(definitionKeeper.getDefinitionsByPath(messagePath)).hasSize(2);
  }

  @Test
  void testDirectoryCreated(final @TempDir Path tempDir) throws IOException {
    this.createMessageFile(tempDir, ":title Pipes\n");
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper(false);
    final MessageIndexer indexer = new MessageIndexer(definitionKeeper, new IgnoreHandler());

    indexer.handleFileEvent(new FileEvent(tempDir.toUri(), FileChangeType.CREATED));

    assertThat(definitionKeeper.getMessageFileDefinitions("pipe_editor")).hasSize(1);
  }

  @Test
  void testFileChanged(final @TempDir Path tempDir) throws IOException {
    final Path messagePath = this.createMessageFile(tempDir, "+editor\n:title Pipes\n");
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper(false);
    final MessageIndexer indexer = new MessageIndexer(definitionKeeper, new IgnoreHandler());
    indexer.handleFileEvent(new FileEvent(messagePath.toUri(), FileChangeType.CREATED));

    Files.writeString(messagePath, "+model\n:title Pipes\n:count #1 pipes\n");
    indexer.handleFileEvent(new FileEvent(messagePath.toUri(), FileChangeType.CHANGED));

    final Collection<MessageFileDefinition> definitions =
        definitionKeeper.getMessageFileDefinitions("pipe_editor");
    assertThat(definitions).hasSize(1);
    assertThat(definitions.iterator().next().getMessages())
        .extracting(MessageDefinition::getId)
        .containsExactly("title", "count");
    assertThat(definitionKeeper.getMessageHandlerUseDefinitions("pipe_editor"))
        .extracting(MessageHandlerUseDefinition::getUsedHandlerName)
        .containsExactly("model");
  }

  @Test
  void testMagikIndexerLeavesUseDefinitionsOfMessageFile(final @TempDir Path tempDir)
      throws IOException {
    final Path messagePath = this.createMessageFile(tempDir, "+editor\n:title Pipes\n");
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper(false);
    final IgnoreHandler ignoreHandler = new IgnoreHandler();
    final MessageIndexer indexer = new MessageIndexer(definitionKeeper, ignoreHandler);
    final MagikIndexer magikIndexer =
        new MagikIndexer(definitionKeeper, MagikToolsProperties.DEFAULT_PROPERTIES, ignoreHandler);

    final FileEvent fileEvent = new FileEvent(messagePath.toUri(), FileChangeType.CHANGED);
    indexer.handleFileEvent(fileEvent);
    magikIndexer.handleFileEvent(fileEvent);

    assertThat(definitionKeeper.getMessageFileDefinitions("pipe_editor")).hasSize(1);
    assertThat(definitionKeeper.getMessageHandlerUseDefinitions("pipe_editor")).hasSize(1);
  }

  @Test
  void testFileDeleted(final @TempDir Path tempDir) throws IOException {
    final Path messagePath = this.createMessageFile(tempDir, "+editor\n:title Pipes\n");
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper(false);
    final MessageIndexer indexer = new MessageIndexer(definitionKeeper, new IgnoreHandler());
    indexer.handleFileEvent(new FileEvent(messagePath.toUri(), FileChangeType.CREATED));

    Files.delete(messagePath);
    indexer.handleFileEvent(new FileEvent(messagePath.toUri(), FileChangeType.DELETED));

    assertThat(definitionKeeper.getMessageFileDefinitions("pipe_editor")).isEmpty();
    assertThat(definitionKeeper.getMessageFileDefinitions()).isEmpty();
    assertThat(definitionKeeper.getMessageHandlerUseDefinitions()).isEmpty();
  }

  @Test
  void testFileContentsDoNotMatchEncoding(final @TempDir Path tempDir) throws IOException {
    final Path messagePath = this.createMessageFile(tempDir, "");
    Files.writeString(
        messagePath,
        "#% text_encoding = utf8\n:title Geometrie\u00ebn\n:count #1\n",
        StandardCharsets.ISO_8859_1);
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper(false);
    final MessageIndexer indexer = new MessageIndexer(definitionKeeper, new IgnoreHandler());

    indexer.handleFileEvent(new FileEvent(messagePath.toUri(), FileChangeType.CREATED));

    final Collection<MessageFileDefinition> definitions =
        definitionKeeper.getMessageFileDefinitions("pipe_editor");
    assertThat(definitions).hasSize(1);
    assertThat(definitions.iterator().next().getMessages())
        .extracting(MessageDefinition::getId)
        .containsExactly("title", "count");
    assertThat(definitions.iterator().next().getTimestamp()).isNotNull();
  }
}
