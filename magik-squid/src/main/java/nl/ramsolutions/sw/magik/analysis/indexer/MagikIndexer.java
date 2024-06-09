package nl.ramsolutions.sw.magik.analysis.indexer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import nl.ramsolutions.sw.IgnoreHandler;
import nl.ramsolutions.sw.MagikToolsProperties;
import nl.ramsolutions.sw.magik.FileEvent;
import nl.ramsolutions.sw.magik.FileEvent.FileChangeType;
import nl.ramsolutions.sw.magik.MagikFile;
import nl.ramsolutions.sw.magik.analysis.definitions.BinaryOperatorDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.ConditionDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.ExemplarDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.GlobalDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.MagikDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.PackageDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.ProcedureDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Magik file indexer. */
public class MagikIndexer {

  private static final Logger LOGGER = LoggerFactory.getLogger(MagikIndexer.class);
  private static final long MAX_SIZE = 1024L * 1024L * 10L; // 10 MB

  private final IDefinitionKeeper definitionKeeper;
  private final MagikToolsProperties properties;
  private final IgnoreHandler ignoreHandler;

  public MagikIndexer(
      final IDefinitionKeeper definitionKeeper,
      final MagikToolsProperties properties,
      final IgnoreHandler ignoreHandler) {
    this.definitionKeeper = definitionKeeper;
    this.properties = properties;
    this.ignoreHandler = ignoreHandler;
  }

  public synchronized void handleFileEvent(final FileEvent fileEvent) throws IOException {
    LOGGER.debug("Handling file event: {}", fileEvent);

    // Don't index if ignored.
    final Path path = fileEvent.getPath();
    if (this.ignoreHandler.isIgnored(path)) {
      LOGGER.debug("Handled file event: {} (ignored)", fileEvent);
      return;
    }

    final FileChangeType fileChangeType = fileEvent.getFileChangeType();
    if (fileChangeType == FileChangeType.CHANGED || fileChangeType == FileChangeType.DELETED) {
      this.getIndexedDefinitions(path).forEach(this::removeDefinition);
    }

    if (fileChangeType == FileChangeType.CREATED || fileChangeType == FileChangeType.CHANGED) {
      final List<Path> indexableFiles =
          this.ignoreHandler
              .getIndexableFiles(path)
              .filter(indexablePath -> indexablePath.toString().toLowerCase().endsWith(".magik"))
              .toList();
      indexableFiles.forEach(this::indexFile);
    }

    LOGGER.debug("Handled file event: {}", fileEvent);
  }

  /**
   * Get all indexed definitions from path or lower.
   *
   * <p>Used when a directory is deleted or renamed, since we only get the delete of the directory
   * itself, not the individual files within the directory or sub-directories.
   *
   * @param path Path to search from.
   * @return Indexed definitions.
   */
  private Collection<IDefinition> getIndexedDefinitions(final Path path) {
    return Stream.of(
            this.definitionKeeper.getPackageDefinitions(),
            this.definitionKeeper.getExemplarDefinitions(),
            this.definitionKeeper.getMethodDefinitions(),
            this.definitionKeeper.getGlobalDefinitions(),
            this.definitionKeeper.getBinaryOperatorDefinitions(),
            this.definitionKeeper.getConditionDefinitions(),
            this.definitionKeeper.getProcedureDefinitions())
        .flatMap(collection -> collection.stream())
        .filter(def -> def.getLocation() != null && def.getLocation().getPath().startsWith(path))
        .collect(Collectors.toSet());
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

  private void addDefinition(final MagikDefinition rawDefinition) {
    // Strip off AstNode, we don't want to store this.
    final MagikDefinition definition = rawDefinition.getWithoutNode();

    if (definition instanceof PackageDefinition packageDefinition) {
      this.definitionKeeper.add(packageDefinition);
    } else if (definition instanceof ExemplarDefinition exemplarDefinition) {
      this.definitionKeeper.add(exemplarDefinition);
    } else if (definition instanceof MethodDefinition methodDefinition) {
      this.definitionKeeper.add(methodDefinition);
    } else if (definition instanceof GlobalDefinition globalDefinition) {
      this.definitionKeeper.add(globalDefinition);
    } else if (definition instanceof BinaryOperatorDefinition binaryOperatorDefinition) {
      this.definitionKeeper.add(binaryOperatorDefinition);
    } else if (definition instanceof ConditionDefinition conditionDefinition) {
      this.definitionKeeper.add(conditionDefinition);
    } else if (definition instanceof ProcedureDefinition procedureDefinition) {
      this.definitionKeeper.add(procedureDefinition);
    }
  }

  private void removeDefinition(final IDefinition definition) {
    if (definition instanceof PackageDefinition packageDefinition) {
      this.definitionKeeper.remove(packageDefinition);
    } else if (definition instanceof ExemplarDefinition exemplarDefinition) {
      this.definitionKeeper.remove(exemplarDefinition);
    } else if (definition instanceof MethodDefinition methodDefinition) {
      this.definitionKeeper.remove(methodDefinition);
    } else if (definition instanceof GlobalDefinition globalDefinition) {
      this.definitionKeeper.remove(globalDefinition);
    } else if (definition instanceof BinaryOperatorDefinition binaryOperatorDefinition) {
      this.definitionKeeper.remove(binaryOperatorDefinition);
    } else if (definition instanceof ConditionDefinition conditionDefinition) {
      this.definitionKeeper.remove(conditionDefinition);
    } else if (definition instanceof ProcedureDefinition procedureDefinition) {
      this.definitionKeeper.remove(procedureDefinition);
    }
  }

  /**
   * Read definitions from path.
   *
   * @param path Path to magik file.
   */
  private void readDefinitions(final Path path) {
    try {
      final long size = Files.size(path);
      if (size > MagikIndexer.MAX_SIZE) {
        LOGGER.warn(
            "Ignoring file: {}, due to size: {}, max size: {}", path, size, MagikIndexer.MAX_SIZE);
        return;
      }

      final MagikFile magikFile = new MagikFile(this.properties, path);
      magikFile.getDefinitions().forEach(this::addDefinition);
    } catch (final IOException exception) {
      LOGGER.error(exception.getMessage(), exception);
    }
  }
}
