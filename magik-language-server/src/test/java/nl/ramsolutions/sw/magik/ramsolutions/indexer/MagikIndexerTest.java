package nl.ramsolutions.sw.magik.ramsolutions.indexer;

import java.nio.file.Path;
import java.util.Collection;
import nl.ramsolutions.sw.magik.analysis.typing.ITypeKeeper;
import nl.ramsolutions.sw.magik.analysis.typing.TypeKeeper;
import nl.ramsolutions.sw.magik.analysis.typing.types.AbstractType;
import nl.ramsolutions.sw.magik.analysis.typing.types.GlobalReference;
import nl.ramsolutions.sw.magik.analysis.typing.types.Method;
import nl.ramsolutions.sw.magik.analysis.typing.types.UndefinedType;
import nl.ramsolutions.sw.magik.languageserver.indexer.MagikIndexer;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test ReferencesProvider.
 */
class MagikIndexerTest {

    /**
     * VSCode runs from module directory, mvn runs from project directory.
     *
     * @return Proper {{Path}} to file.
     */
    protected Path getPath(final Path relativePath) {
        final Path path = Path.of(".").toAbsolutePath().getParent();
        if (path.endsWith("magik-language-server")) {
            return Path.of("..").resolve(relativePath);
        }
        return Path.of(".").resolve(relativePath);
    }

    @Test
    void testFileCreated() {
        final Path path = Path.of("magik-language-server/src/test/resources/test_magik_indexer.magik");
        final Path fixedPath = this.getPath(path).toAbsolutePath();
        final ITypeKeeper typeKeeper = new TypeKeeper();
        final MagikIndexer magikIndexer = new MagikIndexer(typeKeeper);
        magikIndexer.indexPathCreated(fixedPath);

        // Test type.
        final GlobalReference globalRef = GlobalReference.of("user:test_exemplar");
        final AbstractType type = typeKeeper.getType(globalRef);
        assertThat(type).isNotEqualTo(UndefinedType.INSTANCE);

        // Test methods.
        final Collection<Method> newMethods = type.getLocalMethods("new()");
        assertThat(newMethods).hasSize(1);
        final Collection<Method> initMethods = type.getLocalMethods("init()");
        assertThat(initMethods).hasSize(1);
    }

    @Test
    void testFileChanged() {
        // Read first.
        final Path path = Path.of("magik-language-server/src/test/resources/test_magik_indexer.magik");
        final Path fixedPath = this.getPath(path).toAbsolutePath();
        final ITypeKeeper typeKeeper = new TypeKeeper();
        final MagikIndexer magikIndexer = new MagikIndexer(typeKeeper);
        magikIndexer.indexPathCreated(fixedPath);

        // Pretend update.
        magikIndexer.indexPathChanged(fixedPath);

        // Test type.
        final GlobalReference globalRef = GlobalReference.of("user:test_exemplar");
        final AbstractType type = typeKeeper.getType(globalRef);
        assertThat(type).isNotEqualTo(UndefinedType.INSTANCE);

        // Test methods.
        final Collection<Method> newMethods = type.getLocalMethods("new()");
        assertThat(newMethods).hasSize(1);
        final Collection<Method> initMethods = type.getLocalMethods("init()");
        assertThat(initMethods).hasSize(1);
    }

    @Test
    void testFileDeleted() {
        // Read first.
        final Path path = Path.of("magik-language-server/src/test/resources/test_magik_indexer.magik");
        final Path fixedPath = this.getPath(path).toAbsolutePath();
        final ITypeKeeper typeKeeper = new TypeKeeper();
        final MagikIndexer magikIndexer = new MagikIndexer(typeKeeper);
        magikIndexer.indexPathCreated(fixedPath);

        // Test type.
        final GlobalReference globalRef = GlobalReference.of("user:test_exemplar");
        final AbstractType type = typeKeeper.getType(globalRef);
        assertThat(type).isNotEqualTo(UndefinedType.INSTANCE);

        // Pretend update.
        magikIndexer.indexPathDeleted(fixedPath);

        // Test type.
        final AbstractType typeRemoved = typeKeeper.getType(globalRef);
        assertThat(typeRemoved).isEqualTo(UndefinedType.INSTANCE);
    }

}
