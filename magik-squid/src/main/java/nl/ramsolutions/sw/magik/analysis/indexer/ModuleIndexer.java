package nl.ramsolutions.sw.magik.analysis.indexer;

import com.sonar.sslr.api.RecognitionException;
import java.io.IOException;
import java.nio.file.Path;
import nl.ramsolutions.sw.IDefinition;
import nl.ramsolutions.sw.IgnoreHandler;
import nl.ramsolutions.sw.magik.FileEvent;
import nl.ramsolutions.sw.magik.FileEvent.FileChangeType;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.moduledef.ModuleDefFile;
import nl.ramsolutions.sw.moduledef.ModuleDefFileScanner;
import nl.ramsolutions.sw.moduledef.ModuleDefinition;
import nl.ramsolutions.sw.productdef.ProductDefFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Module definition indexer. */
public class ModuleIndexer {

  private static final Logger LOGGER = LoggerFactory.getLogger(ModuleIndexer.class);

  private final IDefinitionKeeper definitionKeeper;
  private final IgnoreHandler ignoreHandler;

  public ModuleIndexer(
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
      this.definitionKeeper.getDefinitionsByPath(path).forEach(this.definitionKeeper::remove);
    }

    if (fileChangeType == FileChangeType.CREATED || fileChangeType == FileChangeType.CHANGED) {
      final ModuleDefFileScanner moduleDefFileScanner =
          new ModuleDefFileScanner(this.ignoreHandler);
      moduleDefFileScanner.getModuleDefFiles(path).stream().forEach(this::indexFile);
    }

    LOGGER.debug("Handled file event: {}", fileEvent);
  }

  /**
   * Index a single magik file when it is created (or first read).
   *
   * @param path Path to magik file.
   */
  @SuppressWarnings("checkstyle:IllegalCatch")
  private void indexFile(final Path path) {
    LOGGER.debug("Scanning created file: {}", path);

    try {
      this.readDefinitions(path);
    } catch (final Exception exception) {
      LOGGER.error("Error indexing created file: " + path, exception);
    }
  }

  private void readDefinitions(final Path path) throws IOException {
    final Path productDefPath = ModuleDefFileScanner.getProductDefFileForPath(path);
    final ProductDefFile productDefFile;
    if (productDefPath != null) {
      productDefFile = new ProductDefFile(productDefPath, this.definitionKeeper, null);
    } else {
      productDefFile = null;
    }

    try {
      final ModuleDefFile moduleDefFile =
          new ModuleDefFile(path, this.definitionKeeper, productDefFile);
      final ModuleDefinition definition = moduleDefFile.getModuleDefinition();
      final IDefinition bareDefinition = definition.getBareDefinition();
      this.definitionKeeper.add(bareDefinition);
    } catch (final RecognitionException exception) {
      LOGGER.warn("Error parsing defintion at: " + path, exception);
    }
  }
}
