package nl.ramsolutions.sw.magik.analysis.definitions.io;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.analysis.definitions.BinaryOperatorDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.ConditionDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.DefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.ExemplarDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.GlobalDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.PackageDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.ParameterDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.ProcedureDefinition;
import nl.ramsolutions.sw.magik.analysis.typing.types.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.types.TypeString;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for JsonDefinitionWriter.
 */
class JsonDefinitionWriterTest {

    private Path tempPath;

    @BeforeEach
    void createTempFile() throws IOException {
        this.tempPath = Files.createTempFile("type_database", ".jsonl");
    }

    @AfterEach
    void unlinkTempFile() throws IOException {
        if (Files.exists(tempPath)) {
            Files.delete(this.tempPath);
        }
    }

    @Test
    void testWritePackage() throws IOException {
        final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
        definitionKeeper.add(
            new PackageDefinition(
            null,
            null,
            null,
            null,
            "test_package",
            List.of("sw")));

        JsonDefinitionWriter.write(this.tempPath, definitionKeeper);

        assertThat(Files.exists(this.tempPath)).isTrue();
        assertThat(Files.size(this.tempPath)).isNotEqualTo(0);
    }

    @Test
    void testWriteType() throws IOException {
        final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
        final TypeString aRef = TypeString.ofIdentifier("user:a", "user");
        definitionKeeper.add(
            new ExemplarDefinition(
                null,
                "test_module",
                null,
                null,
                ExemplarDefinition.Sort.SLOTTED,
                aRef,
                List.of(),
                List.of()));

        JsonDefinitionWriter.write(this.tempPath, definitionKeeper);

        assertThat(Files.exists(this.tempPath)).isTrue();
        assertThat(Files.size(this.tempPath)).isNotEqualTo(0);
    }

    @Test
    void testWriteMethod() throws IOException {
        final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
        final TypeString objectRef = TypeString.ofIdentifier("object", "sw");
        definitionKeeper.add(
            new MethodDefinition(
                null,
                null,
                "Test method m1().",
                null,
                objectRef,
                "m1()",
                Set.of(MethodDefinition.Modifier.PRIVATE),
                List.of(
                    new ParameterDefinition(
                        null,
                        null,
                        null,
                        null,
                        "param1",
                        ParameterDefinition.Modifier.OPTIONAL,
                        TypeString.UNDEFINED)),
                null,
                ExpressionResultString.UNDEFINED,
                ExpressionResultString.EMPTY));

        JsonDefinitionWriter.write(this.tempPath, definitionKeeper);

        assertThat(Files.exists(this.tempPath)).isTrue();
        assertThat(Files.size(this.tempPath)).isNotEqualTo(0);
    }

    @Test
    void testWriteCondition() throws IOException {
        final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
        definitionKeeper.add(
            new ConditionDefinition(
                null,
                null,
                null,
                null,
                "error",
                null,
                List.of("string")));
        definitionKeeper.add(
            new ConditionDefinition(
                null,
                null,
                "Unknown value",
                null,
                "unknown_value",
                "error",
                List.of(
                    "value",
                    "permitted_values")));

        JsonDefinitionWriter.write(this.tempPath, definitionKeeper);

        assertThat(Files.exists(this.tempPath)).isTrue();
        assertThat(Files.size(this.tempPath)).isNotEqualTo(0);
    }

    @Test
    void writeProcedure() throws IOException {
        final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
        definitionKeeper.add(
            new ProcedureDefinition(
                null,
                null,
                "Test procedure",
                null,
                Set.of(),
                TypeString.ofIdentifier("prc", "user"),
                "test_proc",
                List.of(),
                ExpressionResultString.UNDEFINED,
                ExpressionResultString.EMPTY));

        JsonDefinitionWriter.write(this.tempPath, definitionKeeper);

        assertThat(Files.exists(this.tempPath)).isTrue();
        assertThat(Files.size(this.tempPath)).isNotEqualTo(0);
    }

    @Test
    void testWriteGlobal() throws IOException {
        final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
        final TypeString integerRef = TypeString.ofIdentifier("integer", "sw");
        final TypeString aliasedRef = TypeString.ofIdentifier("alias", "user");
        definitionKeeper.add(
            new GlobalDefinition(
                null,
                null,
                null,
                null,
                aliasedRef,
                integerRef));

        JsonDefinitionWriter.write(this.tempPath, definitionKeeper);

        assertThat(Files.exists(this.tempPath)).isTrue();
        assertThat(Files.size(this.tempPath)).isNotEqualTo(0);
    }

    @Test
    void testWriteBinaryOperator() throws IOException {
        final Location location = Location.validLocation(null);
        final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
        final TypeString char16VectorRef = TypeString.ofIdentifier("sw:char16_vector", "sw");
        final TypeString symbolRef = TypeString.ofIdentifier("sw:char16_vector", "sw");
        definitionKeeper.add(
            new BinaryOperatorDefinition(
                location,
                "test_module",
                null,
                null,
                "+",
                char16VectorRef,
                symbolRef,
                char16VectorRef));

        JsonDefinitionWriter.write(this.tempPath, definitionKeeper);

        assertThat(Files.exists(this.tempPath)).isTrue();
        assertThat(Files.size(this.tempPath)).isNotEqualTo(0);
    }

}
