package nl.ramsolutions.sw.magik.analysis.definitions.io;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import nl.ramsolutions.sw.definitions.ModuleDefinition;
import nl.ramsolutions.sw.definitions.ModuleUsage;
import nl.ramsolutions.sw.definitions.ProductDefinition;
import nl.ramsolutions.sw.definitions.ProductUsage;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.analysis.definitions.BinaryOperatorDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.ConditionDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.DefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.ExemplarDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.GlobalDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.MagikFileDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.PackageDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.ParameterDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.ProcedureDefinition;
import nl.ramsolutions.sw.magik.analysis.typing.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Tests for JsonDefinitionWriter. */
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
  void testWriteProduct() throws IOException {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    definitionKeeper.add(
        new ProductDefinition(
            new Location(URI.create("file:///product.def")),
            Instant.now(),
            "test_product",
            null,
            "1",
            "p1",
            "Test product",
            "Test product for testing",
            List.of(new ProductUsage("used_product", null))));

    JsonDefinitionWriter.write(this.tempPath, definitionKeeper);

    assertThat(Files.exists(this.tempPath)).isTrue();
    assertThat(Files.size(this.tempPath)).isNotZero();
  }

  @Test
  void testWriteModule() throws IOException {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    definitionKeeper.add(
        new ModuleDefinition(
            new Location(URI.create("file:///module.def")),
            Instant.now(),
            "test_module",
            null,
            "1",
            null,
            "Test module",
            List.of(new ModuleUsage("used_module", null))));

    JsonDefinitionWriter.write(this.tempPath, definitionKeeper);

    assertThat(Files.exists(this.tempPath)).isTrue();
    assertThat(Files.size(this.tempPath)).isNotZero();
  }

  @Test
  void testWriteMagikFile() throws IOException {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    definitionKeeper.add(
        new MagikFileDefinition(new Location(URI.create("file:///file.magik")), Instant.now()));

    JsonDefinitionWriter.write(this.tempPath, definitionKeeper);

    assertThat(Files.exists(this.tempPath)).isTrue();
    assertThat(Files.size(this.tempPath)).isNotZero();
  }

  @Test
  void testWritePackage() throws IOException {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    definitionKeeper.add(
        new PackageDefinition(
            new Location(URI.create("file:///file.magik")),
            Instant.now(),
            null,
            null,
            null,
            "test_package",
            List.of("sw")));

    JsonDefinitionWriter.write(this.tempPath, definitionKeeper);

    assertThat(Files.exists(this.tempPath)).isTrue();
    assertThat(Files.size(this.tempPath)).isNotZero();
  }

  @Test
  void testWriteType() throws IOException {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final TypeString aRef = TypeString.ofIdentifier("user:a", "user");
    definitionKeeper.add(
        new ExemplarDefinition(
            new Location(URI.create("file:///file.magik")),
            Instant.now(),
            "test_module",
            null,
            null,
            ExemplarDefinition.Sort.SLOTTED,
            aRef,
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptySet()));

    JsonDefinitionWriter.write(this.tempPath, definitionKeeper);

    assertThat(Files.exists(this.tempPath)).isTrue();
    assertThat(Files.size(this.tempPath)).isNotZero();
  }

  @Test
  void testWriteMethod() throws IOException {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    definitionKeeper.add(
        new MethodDefinition(
            new Location(URI.create("file:///file.magik")),
            Instant.now(),
            null,
            "Test method m1().",
            null,
            TypeString.SW_OBJECT,
            "m1()",
            Set.of(MethodDefinition.Modifier.PRIVATE),
            List.of(
                new ParameterDefinition(
                    new Location(URI.create("file:///file.magik")),
                    Instant.now(),
                    null,
                    null,
                    null,
                    "param1",
                    ParameterDefinition.Modifier.OPTIONAL,
                    TypeString.UNDEFINED)),
            null,
            Collections.emptySet(),
            ExpressionResultString.UNDEFINED,
            ExpressionResultString.EMPTY));

    JsonDefinitionWriter.write(this.tempPath, definitionKeeper);

    assertThat(Files.exists(this.tempPath)).isTrue();
    assertThat(Files.size(this.tempPath)).isNotZero();
  }

  @Test
  void testWriteCondition() throws IOException {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    definitionKeeper.add(
        new ConditionDefinition(null, null, null, null, null, "error", null, List.of("string")));
    definitionKeeper.add(
        new ConditionDefinition(
            new Location(URI.create("file:///file.magik")),
            Instant.now(),
            null,
            "Unknown value",
            null,
            "unknown_value",
            "error",
            List.of("value", "permitted_values")));

    JsonDefinitionWriter.write(this.tempPath, definitionKeeper);

    assertThat(Files.exists(this.tempPath)).isTrue();
    assertThat(Files.size(this.tempPath)).isNotZero();
  }

  @Test
  void writeProcedure() throws IOException {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    definitionKeeper.add(
        new ProcedureDefinition(
            new Location(URI.create("file:///file.magik")),
            Instant.now(),
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
    assertThat(Files.size(this.tempPath)).isNotZero();
  }

  @Test
  void testWriteGlobal() throws IOException {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final TypeString aliasedRef = TypeString.ofIdentifier("alias", "user");
    definitionKeeper.add(
        new GlobalDefinition(null, null, null, null, null, aliasedRef, TypeString.SW_INTEGER));

    JsonDefinitionWriter.write(this.tempPath, definitionKeeper);

    assertThat(Files.exists(this.tempPath)).isTrue();
    assertThat(Files.size(this.tempPath)).isNotZero();
  }

  @Test
  void testWriteBinaryOperator() throws IOException {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    definitionKeeper.add(
        new BinaryOperatorDefinition(
            new Location(URI.create("file:///file.magik")),
            Instant.now(),
            "test_module",
            null,
            null,
            "+",
            TypeString.SW_CHAR16_VECTOR,
            TypeString.SW_SYMBOL,
            TypeString.SW_CHAR16_VECTOR));

    JsonDefinitionWriter.write(this.tempPath, definitionKeeper);

    assertThat(Files.exists(this.tempPath)).isTrue();
    assertThat(Files.size(this.tempPath)).isNotZero();
  }
}
