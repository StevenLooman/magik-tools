package nl.ramsolutions.sw.magik.analysis.typing.indexer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import nl.ramsolutions.sw.magik.MagikFile;
import nl.ramsolutions.sw.magik.analysis.definitions.BinaryOperatorDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.ConditionDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.Definition;
import nl.ramsolutions.sw.magik.analysis.definitions.ExemplarDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.GlobalDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.PackageDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Magik file indexer.
 */
public class MagikIndexer {

    private static final Logger LOGGER = LoggerFactory.getLogger(MagikIndexer.class);
    private static final long MAX_SIZE = 1024L * 1024L * 10L;  // 10 MB

    private final IDefinitionKeeper definitionKeeper;
    private final Map<Path, Set<PackageDefinition>> indexedPackages = new HashMap<>();
    private final Map<Path, Set<ExemplarDefinition>> indexedTypes = new HashMap<>();
    private final Map<Path, Set<MethodDefinition>> indexedMethods = new HashMap<>();
    private final Map<Path, Set<GlobalDefinition>> indexedGlobals = new HashMap<>();
    private final Map<Path, Set<BinaryOperatorDefinition>> indexedBinaryOperators = new HashMap<>();
    private final Map<Path, Set<ConditionDefinition>> indexedConditions = new HashMap<>();

    public MagikIndexer(final IDefinitionKeeper definitionKeeper) {
        this.definitionKeeper = definitionKeeper;
    }

    /**
     * Index all magik file(s).
     * @param paths Paths to index.
     * @throws IOException -
     */
    public void indexPaths(final Stream<Path> paths) throws IOException {
        paths
            .filter(path -> path.toString().toLowerCase().endsWith(".magik"))
            .forEach(this::indexPathCreated);
    }

    /**
     * Index a single magik file when it is created (or first read).
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

    private void handleDefinition(final MagikFile magikFile, final Definition rawDefinition) {
        // Strip off AstNode, we don't want to store this.
        final Definition definition = rawDefinition.getWithoutNode();

        final Path path = Path.of(magikFile.getUri());
        if (definition instanceof PackageDefinition) {
            final PackageDefinition packageDefinition = (PackageDefinition) definition;
            this.definitionKeeper.add(packageDefinition);

            final Set<PackageDefinition> defs = this.indexedPackages.computeIfAbsent(path, k -> new HashSet<>());
            defs.add(packageDefinition);
        } else if (definition instanceof ExemplarDefinition) {
            final ExemplarDefinition exemplarDefinition = (ExemplarDefinition) definition;
            this.definitionKeeper.add(exemplarDefinition);

            final Set<ExemplarDefinition> defs = this.indexedTypes.computeIfAbsent(path, k -> new HashSet<>());
            defs.add(exemplarDefinition);
        } else if (definition instanceof MethodDefinition) {
            final MethodDefinition methodDefinition = (MethodDefinition) definition;
            this.definitionKeeper.add(methodDefinition);

            final Set<MethodDefinition> defs = this.indexedMethods.computeIfAbsent(path, k -> new HashSet<>());
            defs.add(methodDefinition);
        } else if (definition instanceof GlobalDefinition) {
            final GlobalDefinition globalDefinition = (GlobalDefinition) definition;
            this.definitionKeeper.add(globalDefinition);

            final Set<GlobalDefinition> defs = this.indexedGlobals.computeIfAbsent(path, k -> new HashSet<>());
            defs.add(globalDefinition);
        } else if (definition instanceof BinaryOperatorDefinition) {
            final BinaryOperatorDefinition binaryOperatorDefinition = (BinaryOperatorDefinition) definition;
            this.definitionKeeper.add(binaryOperatorDefinition);

            final Set<BinaryOperatorDefinition> defs =
                this.indexedBinaryOperators.computeIfAbsent(path, k -> new HashSet<>());
            defs.add(binaryOperatorDefinition);
        } else if (definition instanceof ConditionDefinition) {
            final ConditionDefinition conditionDefinition = (ConditionDefinition) definition;
            this.definitionKeeper.add(conditionDefinition);

            final Set<ConditionDefinition> defs = this.indexedConditions.computeIfAbsent(path, k -> new HashSet<>());
            defs.add(conditionDefinition);
        }
    }

    /**
     * Read definitions from path.
     * @param path Path to magik file.
     */
    private void readDefinitions(final Path path) {
        this.indexedMethods.put(path, new HashSet<>());
        this.indexedGlobals.put(path, new HashSet<>());
        this.indexedBinaryOperators.put(path, new HashSet<>());
        this.indexedPackages.put(path, new HashSet<>());
        this.indexedTypes.put(path, new HashSet<>());
        this.indexedConditions.put(path, new HashSet<>());

        try {
            final long size = Files.size(path);
            if (size > MagikIndexer.MAX_SIZE) {
                LOGGER.warn("Ignoring file: {}, due to size: {}, max size: {}", path, size, MagikIndexer.MAX_SIZE);
            }

            final MagikFile magikFile = new MagikFile(path);
            magikFile.getDefinitions()
                .forEach(definition -> this.handleDefinition(magikFile, definition));
        } catch (final IOException exception) {
            LOGGER.error(exception.getMessage(), exception);
        }
    }

    /**
     * Scrub definitions.
     * @param path Path to magik file.
     */
    private void scrubDefinitions(final Path path) {
        this.indexedMethods.getOrDefault(path, Collections.emptySet())
            .forEach(this.definitionKeeper::remove);
        this.indexedMethods.remove(path);

        this.indexedGlobals.getOrDefault(path, Collections.emptySet())
            .forEach(this.definitionKeeper::remove);
        this.indexedGlobals.remove(path);

        this.indexedBinaryOperators.getOrDefault(path, Collections.emptySet())
            .forEach(this.definitionKeeper::remove);
        this.indexedBinaryOperators.remove(path);

        this.indexedTypes.getOrDefault(path, Collections.emptySet()).stream()
            .forEach(this.definitionKeeper::remove);
        this.indexedTypes.remove(path);

        this.indexedPackages.getOrDefault(path, Collections.emptySet()).stream()
            .forEach(this.definitionKeeper::remove);
        this.indexedPackages.remove(path);

        this.indexedConditions.getOrDefault(path, Collections.emptySet())
            .forEach(this.definitionKeeper::remove);
        this.indexedConditions.remove(path);
    }

}
