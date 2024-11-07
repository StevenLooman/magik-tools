package nl.ramsolutions.sw.magik;

import com.github.marschall.memoryfilesystem.MemoryFileSystemBuilder;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import nl.ramsolutions.sw.IgnoreHandler;
import nl.ramsolutions.sw.MagikToolsProperties;
import nl.ramsolutions.sw.magik.analysis.definitions.DefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.indexer.MagikIndexer;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/** In memory Smallworld project. */
public class SmallworldProjectExtension implements BeforeEachCallback, AfterEachCallback {

  private static final MagikToolsProperties PROPERTIES =
      new MagikToolsProperties(
          Map.of(
              "magik.typing.indexGlobalUsages", "true",
              "magik.typing.indexMethodUsages", "true",
              "magik.typing.indexSlotUsages", "true",
              "magik.typing.indexConditionUsages", "true"));

  private FileSystem fileSystem;
  private IDefinitionKeeper definitionKeeper;

  public FileSystem getFileSystem() {
    return this.fileSystem;
  }

  public IDefinitionKeeper getDefinitionKeeper() {
    return this.definitionKeeper;
  }

  public Path pathOf(final String pathStr) {
    return this.fileSystem.getPath(pathStr);
  }

  public MagikTypedFile addMagikFile(final Path path, final String code) throws IOException {
    Objects.requireNonNull(this.fileSystem);
    Objects.requireNonNull(this.definitionKeeper);

    Files.writeString(path, code);
    final URI uri = path.toUri();
    final MagikTypedFile magikFile =
        new MagikTypedFile(SmallworldProjectExtension.PROPERTIES, uri, code, this.definitionKeeper);
    final IgnoreHandler ignoreHandler = new IgnoreHandler();
    final MagikIndexer magikIndexer = new MagikIndexer(definitionKeeper, PROPERTIES, ignoreHandler);
    final FileEvent fileEvent = new FileEvent(uri, FileEvent.FileChangeType.CREATED);
    magikIndexer.handleFileEvent(fileEvent);
    return magikFile;
  }

  @Override
  public void beforeEach(final ExtensionContext context) throws Exception {
    this.fileSystem = MemoryFileSystemBuilder.newEmpty().build("test");
    this.definitionKeeper = new DefinitionKeeper();
  }

  @Override
  public void afterEach(final ExtensionContext context) throws Exception {
    if (this.fileSystem != null) {
      this.fileSystem.close();
    }

    this.definitionKeeper = null;
  }
}
