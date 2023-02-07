package nl.ramsolutions.sw.magik.analysis.typing.indexer;

import java.nio.file.Path;
import java.util.Collection;
import nl.ramsolutions.sw.magik.analysis.typing.ITypeKeeper;
import nl.ramsolutions.sw.magik.analysis.typing.TypeKeeper;
import nl.ramsolutions.sw.magik.analysis.typing.types.AbstractType;
import nl.ramsolutions.sw.magik.analysis.typing.types.Method;
import nl.ramsolutions.sw.magik.analysis.typing.types.Slot;
import nl.ramsolutions.sw.magik.analysis.typing.types.TypeString;
import nl.ramsolutions.sw.magik.analysis.typing.types.UndefinedType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test MagikIndexer.
 */
class MagikIndexerTest {

    /**
     * VSCode runs from module directory, mvn runs from project directory.
     *
     * @return Proper {@link Path} to file.
     */
    protected Path getPath(final Path relativePath) {
        final Path path = Path.of(".").toAbsolutePath().getParent();
        if (path.endsWith("magik-squid")) {
            return Path.of("..").resolve(relativePath);
        }

        return Path.of(".").resolve(relativePath);
    }

    @Test
    void testFileCreated() {
        final Path path = Path.of("magik-squid/src/test/resources/test_magik_indexer.magik");
        final Path fixedPath = this.getPath(path).toAbsolutePath();
        final ITypeKeeper typeKeeper = new TypeKeeper();
        final MagikIndexer magikIndexer = new MagikIndexer(typeKeeper);
        magikIndexer.indexPathCreated(fixedPath);

        // Test type.
        final TypeString typeString = TypeString.ofIdentifier("test_exemplar", "user");
        final AbstractType type = typeKeeper.getType(typeString);
        assertThat(type).isNotEqualTo(UndefinedType.INSTANCE);

        // Test methods.
        final Collection<Method> newMethods = type.getLocalMethods("new()");
        assertThat(newMethods).hasSize(1);
        final Collection<Method> initMethods = type.getLocalMethods("init()");
        assertThat(initMethods).hasSize(1);

        // Test slots.
        final Collection<Slot> slots = type.getSlots();
        assertThat(slots)
            .hasSize(1);
        final Slot slot = slots.iterator().next();
        assertThat(slot)
            .extracting("name")
                .isEqualTo("slot_a");
    }

    @Test
    void testFileChanged() {
        // Read first.
        final Path path = Path.of("magik-squid/src/test/resources/test_magik_indexer.magik");
        final Path fixedPath = this.getPath(path).toAbsolutePath();
        final ITypeKeeper typeKeeper = new TypeKeeper();
        final MagikIndexer magikIndexer = new MagikIndexer(typeKeeper);
        magikIndexer.indexPathCreated(fixedPath);

        // Pretend update.
        magikIndexer.indexPathChanged(fixedPath);

        // Test type.
        final TypeString typeString = TypeString.ofIdentifier("test_exemplar", "user");
        final AbstractType type = typeKeeper.getType(typeString);
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
        final Path path = Path.of("magik-squid/src/test/resources/test_magik_indexer.magik");
        final Path fixedPath = this.getPath(path).toAbsolutePath();
        final ITypeKeeper typeKeeper = new TypeKeeper();
        final MagikIndexer magikIndexer = new MagikIndexer(typeKeeper);
        magikIndexer.indexPathCreated(fixedPath);

        // Test type.
        final TypeString typeString = TypeString.ofIdentifier("test_exemplar", "user");
        final AbstractType type = typeKeeper.getType(typeString);
        assertThat(type).isNotEqualTo(UndefinedType.INSTANCE);

        // Pretend delete.
        magikIndexer.indexPathDeleted(fixedPath);

        // Test type.
        final AbstractType typeRemoved = typeKeeper.getType(typeString);
        assertThat(typeRemoved).isEqualTo(UndefinedType.INSTANCE);
    }

    @Test
    void testTypeDoc() {
        final Path path = Path.of("magik-squid/src/test/resources/test_magik_indexer_with_type_doc.magik");
        final Path fixedPath = this.getPath(path).toAbsolutePath();
        final ITypeKeeper typeKeeper = new TypeKeeper();
        final MagikIndexer magikIndexer = new MagikIndexer(typeKeeper);
        magikIndexer.indexPathCreated(fixedPath);

        // Test type.
        final TypeString typeString = TypeString.ofIdentifier("test_exemplar", "user");
        final AbstractType type = typeKeeper.getType(typeString);
        assertThat(type).isNotEqualTo(UndefinedType.INSTANCE);

        // Test methods.
        final Collection<Method> newMethods = type.getLocalMethods("new()");
        assertThat(newMethods).hasSize(1);
        final Collection<Method> initMethods = type.getLocalMethods("init()");
        assertThat(initMethods).hasSize(1);

        // Test slots.
        final Collection<Slot> slots = type.getSlots();
        assertThat(slots)
            .hasSize(2);
        final Slot slotA = slots.stream()
            .filter(slot -> slot.getName().equals("slot_a"))
            .findAny()
            .orElseThrow();
        assertThat(slotA)
            .extracting("type")
                .isEqualTo(TypeString.ofIdentifier("rope", "sw"));

        final Slot slotB = slots.stream()
            .filter(slot -> slot.getName().equals("slot_b"))
            .findAny()
            .orElseThrow();
        assertThat(slotB)
            .extracting("type")
                .isEqualTo(
                    TypeString.ofIdentifier("property_list", "sw",
                        TypeString.ofIdentifier("symbol", "sw"),
                        TypeString.ofIdentifier("integer", "sw")));
    }

}
