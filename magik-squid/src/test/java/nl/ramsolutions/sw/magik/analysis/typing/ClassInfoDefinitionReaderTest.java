package nl.ramsolutions.sw.magik.analysis.typing;

import java.io.IOException;
import java.nio.file.Path;
import nl.ramsolutions.sw.magik.analysis.typing.types.AbstractType;
import nl.ramsolutions.sw.magik.analysis.typing.types.Condition;
import nl.ramsolutions.sw.magik.analysis.typing.types.Method;
import nl.ramsolutions.sw.magik.analysis.typing.types.TypeString;
import nl.ramsolutions.sw.magik.analysis.typing.types.UndefinedType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for ClassInfoDefinitionReader.
 */
class ClassInfoDefinitionReaderTest {

    @Test
    void testRead() throws IOException {
        final Path path = Path.of("src/test/resources/magik_tools.class_definition_reader_test.1.jar");
        final TypeKeeper typeKeeper = new TypeKeeper();
        ClassInfoDefinitionReader.readTypes(path, typeKeeper);

        // Globals.
        final TypeString doAnotherThingRef = TypeString.ofIdentifier("!do_another_thing!", "sw");
        final AbstractType doAnotherThingType = typeKeeper.getType(doAnotherThingRef);
        assertThat(doAnotherThingType).isNotEqualTo(UndefinedType.INSTANCE);
        final TypeString reportRef = TypeString.ofIdentifier("!report!", "sw");
        final AbstractType reportType = typeKeeper.getType(reportRef);
        assertThat(reportType).isNotEqualTo(UndefinedType.INSTANCE);

        // Conditions.
        final Condition condition1 = typeKeeper.getCondition("example_condition_1");
        assertThat(condition1).isNotNull();
        final Condition condition2 = typeKeeper.getCondition("example_condition_2");
        assertThat(condition2).isNotNull();

        // Mixins.
        final TypeString exampleMixinRef = TypeString.ofIdentifier("example_mixin", "sw");
        final AbstractType exampleMixinType = typeKeeper.getType(exampleMixinRef);
        assertThat(exampleMixinType).isNotEqualTo(UndefinedType.INSTANCE);

        // Classes.
        final TypeString exampleClassRef = TypeString.ofIdentifier("example_class", "sw");
        final AbstractType exampleClassType = typeKeeper.getType(exampleClassRef);
        assertThat(exampleClassType).isNotEqualTo(UndefinedType.INSTANCE);

        // Methods.
        final Method doSomethingMethod = exampleMixinType.getLocalMethods("do_something()").stream()
            .findAny()
            .orElseThrow();
        assertThat(doSomethingMethod).isNotNull();
        final Method doSomethingElseMethod = exampleMixinType.getLocalMethods("do_something_else()").stream()
            .findAny()
            .orElseThrow();
        assertThat(doSomethingElseMethod).isNotNull();

    }

}
