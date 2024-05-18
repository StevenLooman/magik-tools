package nl.ramsolutions.sw.magik.analysis.indexer;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
  private final Map<Path, Set<PackageDefinition>> indexedPackages = new HashMap<>();
  private final Map<Path, Set<ExemplarDefinition>> indexedTypes = new HashMap<>();
  private final Map<Path, Set<MethodDefinition>> indexedMethods = new HashMap<>();
  private final Map<Path, Set<GlobalDefinition>> indexedGlobals = new HashMap<>();
  private final Map<Path, Set<BinaryOperatorDefinition>> indexedBinaryOperators = new HashMap<>();
  private final Map<Path, Set<ConditionDefinition>> indexedConditions = new HashMap<>();
  private final Map<Path, Set<ProcedureDefinition>> indexedProcedures = new HashMap<>();

  public MagikIndexer(
      final IDefinitionKeeper definitionKeeper,
      final MagikToolsProperties properties,
      final IgnoreHandler ignoreHandler) {
    this.definitionKeeper = definitionKeeper;
    this.properties = properties;
    this.ignoreHandler = ignoreHandler;
  }

  public synchronized void handleFileEvent(final FileEvent fileEvent) throws IOException {
    // Don't index if ignored.
    final URI uri = fileEvent.getUri();
    final Path path = Path.of(uri);
    if (this.ignoreHandler.isIgnored(path)) {
      return;
    }

    final FileChangeType fileChangeType = fileEvent.getFileChangeType();
    final List<Path> indexableFiles =
        fileChangeType == FileChangeType.DELETED
            ? this.getIndexedFiles(path).toList()
            : this.ignoreHandler
                .getIndexableFiles(path)
                .filter(indexablePath -> indexablePath.toString().toLowerCase().endsWith(".magik"))
                .toList();
    switch (fileChangeType) {
      case DELETED:
        indexableFiles.forEach(this::indexPathDeleted);
        break;

      case CREATED:
        indexableFiles.forEach(this::indexPathCreated);
        break;

      case CHANGED:
        indexableFiles.forEach(this::indexPathChanged);
        break;

      default:
        throw new UnsupportedOperationException();
    }
  }

  private Stream<Path> getIndexedFiles(final Path path) {
    // Get all previously indexed files at or below path.
    return Stream.of(
            this.indexedPackages.entrySet().stream().map(Map.Entry::getKey),
            this.indexedTypes.entrySet().stream().map(Map.Entry::getKey),
            this.indexedMethods.entrySet().stream().map(Map.Entry::getKey),
            this.indexedGlobals.entrySet().stream().map(Map.Entry::getKey),
            this.indexedBinaryOperators.entrySet().stream().map(Map.Entry::getKey),
            this.indexedConditions.entrySet().stream().map(Map.Entry::getKey),
            this.indexedProcedures.entrySet().stream().map(Map.Entry::getKey))
        .flatMap(stream -> stream)
        .filter(indexedPath -> indexedPath.startsWith(path));
  }

  /**
   * Index a single magik file when it is created (or first read).
   *
   * @param path Path to magik file.
   */
  @SuppressWarnings("checkstyle:IllegalCatch")
  public void indexPathCreated(final Path path) {
    LOGGER.debug("Scanning created file: {}", path);

    try {
      this.scrubDefinitions(path);
      this.readDefinitions(path);
    } catch (final Exception exception) {
      LOGGER.error("Error indexing created file: " + path, exception);
    }
  }

  /**
   * Index a single magik file when it is changed.
   *
   * @param path Path to magik file.
   */
  @SuppressWarnings("checkstyle:IllegalCatch")
  public void indexPathChanged(final Path path) {
    LOGGER.debug("Scanning changed file: {}", path);

    try {
      this.scrubDefinitions(path);
      this.readDefinitions(path);
    } catch (final Exception exception) {
      LOGGER.error("Error indexing changed file: " + path, exception);
    }
  }

  /**
   * Un-index a single magik file when it is deleted.
   *
   * @param path Path to magik file.
   */
  @SuppressWarnings("checkstyle:IllegalCatch")
  public void indexPathDeleted(final Path path) {
    LOGGER.debug("Scanning deleted file: {}", path);

    try {
      this.scrubDefinitions(path);
    } catch (final Exception exception) {
      LOGGER.error("Error indexing deleted file: " + path, exception);
    }
  }

  private void handleDefinition(final MagikFile magikFile, final MagikDefinition rawDefinition) {
    // Strip off AstNode, we don't want to store this.
    final MagikDefinition definition = rawDefinition.getWithoutNode();

    final Path path = Path.of(magikFile.getUri());
    if (definition instanceof PackageDefinition packageDefinition) {
      this.definitionKeeper.add(packageDefinition);

      final Set<PackageDefinition> defs =
          this.indexedPackages.computeIfAbsent(path, k -> new HashSet<>());
      defs.add(packageDefinition);
    } else if (definition instanceof ExemplarDefinition exemplarDefinition) {
      this.definitionKeeper.add(exemplarDefinition);

      final Set<ExemplarDefinition> defs =
          this.indexedTypes.computeIfAbsent(path, k -> new HashSet<>());
      defs.add(exemplarDefinition);
    } else if (definition instanceof MethodDefinition methodDefinition) {
      this.definitionKeeper.add(methodDefinition);

      final Set<MethodDefinition> defs =
          this.indexedMethods.computeIfAbsent(path, k -> new HashSet<>());
      defs.add(methodDefinition);
    } else if (definition instanceof GlobalDefinition globalDefinition) {
      this.definitionKeeper.add(globalDefinition);

      final Set<GlobalDefinition> defs =
          this.indexedGlobals.computeIfAbsent(path, k -> new HashSet<>());
      defs.add(globalDefinition);
    } else if (definition instanceof BinaryOperatorDefinition binaryOperatorDefinition) {
      this.definitionKeeper.add(binaryOperatorDefinition);

      final Set<BinaryOperatorDefinition> defs =
          this.indexedBinaryOperators.computeIfAbsent(path, k -> new HashSet<>());
      defs.add(binaryOperatorDefinition);
    } else if (definition instanceof ConditionDefinition conditionDefinition) {
      this.definitionKeeper.add(conditionDefinition);

      final Set<ConditionDefinition> defs =
          this.indexedConditions.computeIfAbsent(path, k -> new HashSet<>());
      defs.add(conditionDefinition);
    } else if (definition instanceof ProcedureDefinition procedureDefinition) {
      this.definitionKeeper.add(procedureDefinition);

      final Set<ProcedureDefinition> defs =
          this.indexedProcedures.computeIfAbsent(path, k -> new HashSet<>());
      defs.add(procedureDefinition);
    }
  }

  /**
   * Read definitions from path.
   *
   * @param path Path to magik file.
   */
  private void readDefinitions(final Path path) {
    this.indexedMethods.put(path, new HashSet<>());
    this.indexedGlobals.put(path, new HashSet<>());
    this.indexedBinaryOperators.put(path, new HashSet<>());
    this.indexedPackages.put(path, new HashSet<>());
    this.indexedTypes.put(path, new HashSet<>());
    this.indexedConditions.put(path, new HashSet<>());
    this.indexedProcedures.put(path, new HashSet<>());

    try {
      final long size = Files.size(path);
      if (size > MagikIndexer.MAX_SIZE) {
        LOGGER.warn(
            "Ignoring file: {}, due to size: {}, max size: {}", path, size, MagikIndexer.MAX_SIZE);
        return;
      }

      final MagikFile magikFile = new MagikFile(this.properties, path);
      magikFile
          .getDefinitions()
          .forEach(definition -> this.handleDefinition(magikFile, definition));
    } catch (final IOException exception) {
      LOGGER.error(exception.getMessage(), exception);
    }
  }

  /**
   * Scrub definitions.
   *
   * @param path Path to magik file.
   */
  private void scrubDefinitions(final Path path) {
    this.indexedMethods
        .getOrDefault(path, Collections.emptySet())
        .forEach(this.definitionKeeper::remove);
    this.indexedMethods.remove(path);

    this.indexedGlobals
        .getOrDefault(path, Collections.emptySet())
        .forEach(this.definitionKeeper::remove);
    this.indexedGlobals.remove(path);

    this.indexedBinaryOperators
        .getOrDefault(path, Collections.emptySet())
        .forEach(this.definitionKeeper::remove);
    this.indexedBinaryOperators.remove(path);

    this.indexedTypes.getOrDefault(path, Collections.emptySet()).stream()
        .forEach(this.definitionKeeper::remove);
    this.indexedTypes.remove(path);

    this.indexedPackages.getOrDefault(path, Collections.emptySet()).stream()
        .forEach(this.definitionKeeper::remove);
    this.indexedPackages.remove(path);

    this.indexedConditions
        .getOrDefault(path, Collections.emptySet())
        .forEach(this.definitionKeeper::remove);
    this.indexedConditions.remove(path);

    this.indexedProcedures
        .getOrDefault(path, Collections.emptySet())
        .forEach(this.definitionKeeper::remove);
    this.indexedProcedures.remove(path);
  }
}
