package nl.ramsolutions.sw.magik.languageserver.indexer;

import java.io.IOException;
import java.nio.file.Path;
import java.util.stream.Stream;
import nl.ramsolutions.sw.magik.MagikFile;
import nl.ramsolutions.sw.magik.analysis.definitions.Definition;
import nl.ramsolutions.sw.magik.analysis.definitions.EnumerationDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.IndexedExemplarDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.MixinDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.PackageDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.SlottedExemplarDefinition;
import nl.ramsolutions.sw.magik.analysis.typing.ITypeKeeper;
import nl.ramsolutions.sw.magik.analysis.typing.types.GlobalReference;
import nl.ramsolutions.sw.magik.analysis.typing.types.IndexedType;
import nl.ramsolutions.sw.magik.analysis.typing.types.IntrinsicType;
import nl.ramsolutions.sw.magik.analysis.typing.types.MagikType;
import nl.ramsolutions.sw.magik.analysis.typing.types.Package;
import nl.ramsolutions.sw.magik.analysis.typing.types.SlottedType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Pre-indexer step to create initial packages and exemplars/mixins.
 *
 * <p>
 * The {{MagikIndexer}} does the real indexing, this pre-indexing is required to not be bound
 * to the load order of the session, but still get the correct inheritance hierarchy.
 * </p>
 *
 * <p>
 * Does a two-step part to correctly build the hierachy:
 * 1. Gather all types (exemplars, mixins)
 * 2. Builds the complete type hierarchy
 * </p>
 */
public class MagikPreIndexer {

    private static final Logger LOGGER = LoggerFactory.getLogger(MagikPreIndexer.class);

    private static final String DEF_MIXIN = "def_mixin";
    private static final String DEF_INDEXED_EXEMPLAR = "def_indexed_exemplar";
    private static final String DEF_SLOTTED_EXEMPLAR = "def_slotted_exemplar";
    private static final String DEF_ENUMERATION = "def_enumeration";
    private static final String DEF_PACKAGE = "def_package";

    private final ITypeKeeper typeKeeper;

    public MagikPreIndexer(ITypeKeeper typeKeeper) {
        this.typeKeeper = typeKeeper;
    }

    /**
     * Index all magik file(s).
     * @param paths Paths to index.
     * @throws IOException -
     */
    public void indexPaths(Stream<Path> paths) throws IOException {
        paths.forEach(this::indexPath);
    }

    /**
     * Index a single magik file.
     * @param path Path to magik file.
     */
    public void indexPath(Path path) {
        LOGGER.debug("Scanning file: {}", path);
        try {
            final MagikFile magikFile = new MagikFile(path);

            // Ensure its worth parsing this file.
            final String code = magikFile.getSource();
            if (!this.sourceContainsDefinition(code)) {
                return;
            }

            magikFile.getDefinitions()
                .forEach(this::handleDefinition);
        } catch (IOException exception) {
            LOGGER.error(exception.getMessage(), exception);
        }
    }

    private boolean sourceContainsDefinition(final String code) {
        final String codeLowered = code.toLowerCase();
        return codeLowered.contains(DEF_PACKAGE)
            || codeLowered.contains(DEF_ENUMERATION)
            || codeLowered.contains(DEF_SLOTTED_EXEMPLAR)
            || codeLowered.contains(DEF_INDEXED_EXEMPLAR)
            || codeLowered.contains(DEF_MIXIN);
    }

    private void handleDefinition(final Definition definition) {
        if (definition instanceof PackageDefinition) {
            final PackageDefinition packageDefinition = (PackageDefinition) definition;
            this.handleDefinition(packageDefinition);
        } else if (definition instanceof IndexedExemplarDefinition) {
            final IndexedExemplarDefinition indexedExemplarDefinition = (IndexedExemplarDefinition) definition;
            this.handleDefinition(indexedExemplarDefinition);
        } else if (definition instanceof EnumerationDefinition) {
            final EnumerationDefinition enumerationDefinition = (EnumerationDefinition) definition;
            this.handleDefinition(enumerationDefinition);
        } else if (definition instanceof SlottedExemplarDefinition) {
            final SlottedExemplarDefinition slottedExemplarDefinition = (SlottedExemplarDefinition) definition;
            this.handleDefinition(slottedExemplarDefinition);
        } else if (definition instanceof MixinDefinition) {
            final MixinDefinition mixinDefinition = (MixinDefinition) definition;
            this.handleDefinition(mixinDefinition);
        }
    }

    private void handleDefinition(final PackageDefinition definition) {
        final String pakkageName = definition.getName();
        final Package pakkage;
        if (this.typeKeeper.hasPackage(pakkageName)) {
            pakkage = this.typeKeeper.getPackage(pakkageName);
        } else {
            pakkage = new Package(pakkageName);
            this.typeKeeper.addPackage(pakkage);
        }

        definition.getUses().stream()
            .forEach(use -> {
                final Package usePakkage;
                if (!this.typeKeeper.hasPackage(use)) {
                    usePakkage = new Package(use);
                    this.typeKeeper.addPackage(usePakkage);
                } else {
                    usePakkage = this.typeKeeper.getPackage(use);
                }

                pakkage.addUse(usePakkage);
            });

        LOGGER.debug("Indexed package: {}", pakkage);
    }

    private void handleDefinition(final IndexedExemplarDefinition definition) {
        final GlobalReference globalRef = definition.getGlobalReference();
        this.ensurePackage(globalRef);

        final MagikType magikType = new IndexedType(globalRef);
        this.typeKeeper.addType(magikType);
        LOGGER.debug("Indexed indexed exemplar: {}", magikType);
    }

    private void handleDefinition(final EnumerationDefinition definition) {
        final GlobalReference globalRef = definition.getGlobalReference();
        this.ensurePackage(globalRef);

        final MagikType magikType = new SlottedType(globalRef);
        this.typeKeeper.addType(magikType);
        LOGGER.debug("Indexed enumeration: {}", magikType);
    }

    private void handleDefinition(final SlottedExemplarDefinition definition) {
        final GlobalReference globalRef = definition.getGlobalReference();
        this.ensurePackage(globalRef);

        final MagikType magikType = new SlottedType(globalRef);
        this.typeKeeper.addType(magikType);
        LOGGER.debug("Indexed slotted exemplar: {}", magikType);
    }

    private void handleDefinition(final MixinDefinition definition) {
        final GlobalReference globalRef = definition.getGlobalReference();
        this.ensurePackage(globalRef);

        final MagikType magikType = new IntrinsicType(globalRef);
        this.typeKeeper.addType(magikType);
        LOGGER.debug("Indexed mixin: {}", magikType);
    }

    private Package ensurePackage(final GlobalReference globalReference) {
        final String pakkageName = globalReference.getPakkage();
        if (!this.typeKeeper.hasPackage(pakkageName)) {
            final Package pakkage = new Package(pakkageName);
            this.typeKeeper.addPackage(pakkage);
            LOGGER.debug("Indexed package: {}", pakkage);
        }

        return this.typeKeeper.getPackage(pakkageName);
    }

}
