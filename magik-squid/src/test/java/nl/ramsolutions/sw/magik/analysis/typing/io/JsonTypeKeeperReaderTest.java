package nl.ramsolutions.sw.magik.analysis.typing.io;

import java.io.IOException;
import java.nio.file.Path;
import nl.ramsolutions.sw.magik.analysis.typing.ITypeKeeper;
import nl.ramsolutions.sw.magik.analysis.typing.TypeKeeper;
import nl.ramsolutions.sw.magik.analysis.typing.types.AbstractType;
import nl.ramsolutions.sw.magik.analysis.typing.types.Slot;
import nl.ramsolutions.sw.magik.analysis.typing.types.TypeString;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for JsonTypeKeeperReader.
 */
class JsonTypeKeeperReaderTest {

    @Test
    void testReadType() throws IOException {
        final Path path = Path.of("src/test/resources/tests/type_database.jsonl");
        final ITypeKeeper typeKeeper = new TypeKeeper();
        JsonTypeKeeperReader.readTypes(path, typeKeeper);

        final TypeString aRef = TypeString.ofIdentifier("a", "user");
        final AbstractType aType = typeKeeper.getType(aRef);
        assertThat(aType).isNotNull();

        final Slot slot1 = aType.getSlot("slot1");
        final TypeString integerRef = TypeString.ofIdentifier("integer", "sw");
        final Slot slot1Expected = new Slot(null, "slot1", integerRef);
        assertThat(slot1).isEqualTo(slot1Expected);

        final Slot slot2 = aType.getSlot("slot2");
        final TypeString floatRef = TypeString.ofIdentifier("float", "sw");
        final Slot slot2Expected = new Slot(null, "slot2", floatRef);
        assertThat(slot2).isEqualTo(slot2Expected);

        assertThat(aType.getDoc()).isEqualTo("Test exemplar a");
        assertThat(aType.getModuleName()).isEqualTo("test_module");
    }

}
