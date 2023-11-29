package nl.ramsolutions.sw.magik.analysis.typing.io;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.analysis.typing.BinaryOperator;
import nl.ramsolutions.sw.magik.analysis.typing.ITypeKeeper;
import nl.ramsolutions.sw.magik.analysis.typing.TypeKeeper;
import nl.ramsolutions.sw.magik.analysis.typing.types.AliasType;
import nl.ramsolutions.sw.magik.analysis.typing.types.Condition;
import nl.ramsolutions.sw.magik.analysis.typing.types.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.types.MagikType;
import nl.ramsolutions.sw.magik.analysis.typing.types.Method;
import nl.ramsolutions.sw.magik.analysis.typing.types.Package;
import nl.ramsolutions.sw.magik.analysis.typing.types.Parameter;
import nl.ramsolutions.sw.magik.analysis.typing.types.TypeString;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for JsonTypeKeeperWriter.
 */
class JsonTypeKeeperWriterTest {

    @Test
    void testWritePackage() throws IOException {
        final ITypeKeeper typeKeeper = new TypeKeeper();
        new Package(typeKeeper, null, null, "test_package");

        final Path path = Files.createTempFile("type_database", ".jsonl");
        JsonTypeKeeperWriter.write(path, typeKeeper);

        assertThat(Files.exists(path)).isTrue();
        assertThat(Files.size(path)).isNotEqualTo(0);
    }

    @Test
    void testWriteType() throws IOException {
        final ITypeKeeper typeKeeper = new TypeKeeper();
        final TypeString aRef = TypeString.ofIdentifier("user:a", "user");
        new MagikType(typeKeeper, null, "test_module", MagikType.Sort.SLOTTED, aRef);

        final Path path = Files.createTempFile("type_database", ".jsonl");
        JsonTypeKeeperWriter.write(path, typeKeeper);

        assertThat(Files.exists(path)).isTrue();
        assertThat(Files.size(path)).isNotEqualTo(0);
    }

    @Test
    void testWriteMethod() throws IOException {
        final ITypeKeeper typeKeeper = new TypeKeeper();
        final TypeString objectRef = TypeString.ofIdentifier("object", "sw");
        final MagikType objectType = (MagikType) typeKeeper.getType(objectRef);
        objectType.addMethod(
            null,
            null,
            EnumSet.of(Method.Modifier.PRIVATE),
            "m1()",
            List.of(
                new Parameter(null, "param1", Parameter.Modifier.OPTIONAL)),
            null,
            "Test method m1().",
            ExpressionResultString.UNDEFINED,
            new ExpressionResultString());

        final Path path = Files.createTempFile("type_database", ".jsonl");
        JsonTypeKeeperWriter.write(path, typeKeeper);

        assertThat(Files.exists(path)).isTrue();
        assertThat(Files.size(path)).isNotEqualTo(0);
    }

    @Test
    void testWriteCondition() throws IOException {
        final ITypeKeeper typeKeeper = new TypeKeeper();
        final Condition errorCondition = new Condition(
            null,
            null,
            "error",
            null,
            List.of("string"),
            null);
        typeKeeper.addCondition(errorCondition);
        final Condition unknownValueCondition = new Condition(
            null,
            null,
            "unknown_value",
            "error",
            List.of(
                "value",
                "permitted_values"),
            "Unknown value");
        typeKeeper.addCondition(unknownValueCondition);

        final Path path = Files.createTempFile("type_database", ".jsonl");
        JsonTypeKeeperWriter.write(path, typeKeeper);

        assertThat(Files.exists(path)).isTrue();
        assertThat(Files.size(path)).isNotEqualTo(0);
    }

    @Test
    void writeProcedure() throws IOException {
        final ITypeKeeper typeKeeper = new TypeKeeper();
        // TODO

        final Path path = Files.createTempFile("type_database", ".jsonl");
        JsonTypeKeeperWriter.write(path, typeKeeper);

        assertThat(Files.exists(path)).isTrue();
        assertThat(Files.size(path)).isNotEqualTo(0);
    }

    @Test
    void testWriteGlobal() throws IOException {
        final ITypeKeeper typeKeeper = new TypeKeeper();
        final TypeString integerRef = TypeString.ofIdentifier("integer", "sw");
        final TypeString aliasedRef = TypeString.ofIdentifier("alias", "user");
        new AliasType(typeKeeper, null, null, aliasedRef, integerRef);

        final Path path = Files.createTempFile("type_database", ".jsonl");
        JsonTypeKeeperWriter.write(path, typeKeeper);

        assertThat(Files.exists(path)).isTrue();
        assertThat(Files.size(path)).isNotEqualTo(0);
    }

    @Test
    void testWriteBinaryOperator() throws IOException {
        final URI uri = URI.create("");
        final Location location = new Location(uri);
        final ITypeKeeper typeKeeper = new TypeKeeper();
        final TypeString char16VectorRef = TypeString.ofIdentifier("sw:char16_vector", "sw");
        final TypeString symbolRef = TypeString.ofIdentifier("sw:char16_vector", "sw");
        final BinaryOperator binaryOperator = new BinaryOperator(
            location,
            "test_module",
            BinaryOperator.Operator.PLUS,
            char16VectorRef,
            symbolRef,
            char16VectorRef,
            null);
        typeKeeper.addBinaryOperator(binaryOperator);

        final Path path = Files.createTempFile("type_database", ".jsonl");
        JsonTypeKeeperWriter.write(path, typeKeeper);

        assertThat(Files.exists(path)).isTrue();
        assertThat(Files.size(path)).isNotEqualTo(0);
    }

}