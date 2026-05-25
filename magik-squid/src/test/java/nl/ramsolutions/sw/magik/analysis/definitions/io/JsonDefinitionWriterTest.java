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
import nl.ramsolutions.sw.magik.analysis.definitions.Pragma;
import nl.ramsolutions.sw.magik.analysis.definitions.ProcedureDefinition;
import nl.ramsolutions.sw.magik.analysis.typing.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import nl.ramsolutions.sw.moduledef.ModuleDefinition;
import nl.ramsolutions.sw.moduledef.ModuleUsage;
import nl.ramsolutions.sw.productdef.ProductDefinition;
import nl.ramsolutions.sw.productdef.ProductUsage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Tests for {@link JsonDefinitionWriter}. */
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
            List.of(new ModuleUsage("used_module", null)),
            List.of(new ModuleUsage("test_used_module", null))));

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
            null));

    JsonDefinitionWriter.write(this.tempPath, definitionKeeper);

    assertThat(Files.exists(this.tempPath)).isTrue();
    assertThat(Files.size(this.tempPath)).isNotZero();
  }

  @Test
  void testWriteAndReadMixinType() throws IOException {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final TypeString mixinRef = TypeString.ofIdentifier("user:my_mixin", "user");
    definitionKeeper.add(
        new ExemplarDefinition(
            new Location(URI.create("file:///file.magik")),
            Instant.now(),
            "test_module",
            null,
            null,
            ExemplarDefinition.Sort.MIXIN,
            mixinRef,
            Collections.emptyList(),
            Collections.emptyList(),
            null));

    JsonDefinitionWriter.write(this.tempPath, definitionKeeper);
    assertThat(Files.readString(this.tempPath)).contains("\"mixin\"");

    final IDefinitionKeeper readBack = new DefinitionKeeper();
    JsonDefinitionReader.readTypes(this.tempPath, readBack);
    final ExemplarDefinition roundTripped =
        readBack.getExemplarDefinitions(mixinRef).stream().findAny().orElseThrow();
    assertThat(roundTripped.getSort()).isEqualTo(ExemplarDefinition.Sort.MIXIN);
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
            null,
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
        new ConditionDefinition(
            null, null, null, null, null, "error", null, List.of("string"), null));
    definitionKeeper.add(
        new ConditionDefinition(
            new Location(URI.create("file:///file.magik")),
            Instant.now(),
            null,
            "Unknown value",
            null,
            "unknown_value",
            "error",
            List.of("value", "permitted_values"),
            new Pragma(null, Set.of("basic"), Set.of(), Set.of())));

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
            null,
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
        new GlobalDefinition(
            null, null, null, null, null, aliasedRef, TypeString.SW_INTEGER, null));

    JsonDefinitionWriter.write(this.tempPath, definitionKeeper);

    assertThat(Files.exists(this.tempPath)).isTrue();
    assertThat(Files.size(this.tempPath)).isNotZero();
  }

  @Test
  void testWriteAndReadVariadicMethodResult() throws IOException {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final TypeString bRef = TypeString.ofIdentifier("user:b", "user");
    final ExpressionResultString variadicResult =
        new ExpressionResultString(TypeString.ofVariadic(TypeString.SW_INTEGER));
    final MethodDefinition original =
        new MethodDefinition(
            new Location(URI.create("file:///file.magik")),
            Instant.now(),
            null,
            "Variadic",
            null,
            bRef,
            "m_variadic()",
            Collections.emptySet(),
            Collections.emptyList(),
            null,
            null,
            variadicResult,
            ExpressionResultString.EMPTY);
    definitionKeeper.add(original);

    JsonDefinitionWriter.write(this.tempPath, definitionKeeper);

    final IDefinitionKeeper readBack = new DefinitionKeeper();
    JsonDefinitionReader.readTypes(this.tempPath, readBack);

    final MethodDefinition roundTripped =
        readBack.getMethodDefinitions(bRef).stream().findAny().orElseThrow();
    assertThat(roundTripped.getReturnTypes()).isEqualTo(variadicResult);
    assertThat(roundTripped.getReturnTypes().getTypes().get(0).isVariadic()).isTrue();
    assertThat(roundTripped.getReturnTypes().getTypes().get(0).getVariadicInner())
        .isEqualTo(TypeString.SW_INTEGER);
  }

  @Test
  void testWriteAndReadLeadingPlusVariadicMethodResult() throws IOException {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final TypeString bRef = TypeString.ofIdentifier("user:b", "user");
    final ExpressionResultString leadingPlusVariadic =
        new ExpressionResultString(
            TypeString.SW_SYMBOL,
            TypeString.SW_CHAR16_VECTOR,
            TypeString.ofVariadic(TypeString.SW_INTEGER));
    final MethodDefinition original =
        new MethodDefinition(
            new Location(URI.create("file:///file.magik")),
            Instant.now(),
            null,
            "LeadingPlusVariadic",
            null,
            bRef,
            "m_leading_variadic()",
            Collections.emptySet(),
            Collections.emptyList(),
            null,
            null,
            leadingPlusVariadic,
            ExpressionResultString.EMPTY);
    definitionKeeper.add(original);

    JsonDefinitionWriter.write(this.tempPath, definitionKeeper);

    final IDefinitionKeeper readBack = new DefinitionKeeper();
    JsonDefinitionReader.readTypes(this.tempPath, readBack);

    final MethodDefinition roundTripped =
        readBack.getMethodDefinitions(bRef).stream().findAny().orElseThrow();
    assertThat(roundTripped.getReturnTypes()).isEqualTo(leadingPlusVariadic);
    assertThat(roundTripped.getReturnTypes().getTypes().get(0)).isEqualTo(TypeString.SW_SYMBOL);
    assertThat(roundTripped.getReturnTypes().getTypes().get(1))
        .isEqualTo(TypeString.SW_CHAR16_VECTOR);
    assertThat(roundTripped.getReturnTypes().getTypes().get(2).isVariadic()).isTrue();
    assertThat(roundTripped.getReturnTypes().getTypes().get(2).getVariadicInner())
        .isEqualTo(TypeString.SW_INTEGER);
  }

  @Test
  void testWriteAndReadVariadicCombinedInnerMethodResult() throws IOException {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final TypeString bRef = TypeString.ofIdentifier("user:b", "user");
    final TypeString integerOrUnset =
        TypeString.combine(TypeString.SW_INTEGER, TypeString.SW_UNSET);
    final ExpressionResultString combinedVariadic =
        new ExpressionResultString(TypeString.ofVariadic(integerOrUnset));
    final MethodDefinition original =
        new MethodDefinition(
            new Location(URI.create("file:///file.magik")),
            Instant.now(),
            null,
            "VariadicCombined",
            null,
            bRef,
            "m_variadic_combined()",
            Collections.emptySet(),
            Collections.emptyList(),
            null,
            null,
            combinedVariadic,
            ExpressionResultString.EMPTY);
    definitionKeeper.add(original);

    JsonDefinitionWriter.write(this.tempPath, definitionKeeper);

    final IDefinitionKeeper readBack = new DefinitionKeeper();
    JsonDefinitionReader.readTypes(this.tempPath, readBack);

    final MethodDefinition roundTripped =
        readBack.getMethodDefinitions(bRef).stream().findAny().orElseThrow();
    assertThat(roundTripped.getReturnTypes()).isEqualTo(combinedVariadic);
    assertThat(roundTripped.getReturnTypes().getTypes().get(0).isVariadic()).isTrue();
    assertThat(roundTripped.getReturnTypes().getTypes().get(0).getVariadicInner())
        .isEqualTo(integerOrUnset);
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
