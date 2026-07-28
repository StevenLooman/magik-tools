package nl.ramsolutions.sw.magik.analysis.definitions.io;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Collection;
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
import nl.ramsolutions.sw.magik.analysis.definitions.InheritanceDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.MagikFileDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.PackageDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.ParameterDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.Pragma;
import nl.ramsolutions.sw.magik.analysis.definitions.ProcedureDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.SlotDefinition;
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
  void testWriteAndReadSlot() throws IOException {
    final TypeString typeA = TypeString.ofIdentifier("a", "user");
    final IDefinitionKeeper writeKeeper = new DefinitionKeeper();
    writeKeeper.add(
        new ExemplarDefinition(
            null, null, null, null, null, ExemplarDefinition.Sort.SLOTTED, typeA, null));
    writeKeeper.add(
        new SlotDefinition(null, null, null, null, null, typeA, "slot1", TypeString.SW_INTEGER));

    JsonDefinitionWriter.write(this.tempPath, writeKeeper);

    final IDefinitionKeeper readKeeper = new DefinitionKeeper();
    JsonDefinitionReader.readTypes(this.tempPath, readKeeper);

    final Collection<SlotDefinition> slotDefs = readKeeper.getSlotDefinitions(typeA);
    assertThat(slotDefs)
        .extracting(SlotDefinition::getName, SlotDefinition::getTypeName)
        .containsExactly(tuple("slot1", TypeString.SW_INTEGER));
  }

  @Test
  void testWriteAndReadInheritance() throws IOException {
    final TypeString typeA = TypeString.ofIdentifier("a", "user");
    final IDefinitionKeeper writeKeeper = new DefinitionKeeper();
    writeKeeper.add(
        new ExemplarDefinition(
            null, null, null, null, null, ExemplarDefinition.Sort.SLOTTED, typeA, null));
    writeKeeper.add(
        new InheritanceDefinition(null, null, null, null, null, typeA, TypeString.SW_OBJECT));

    JsonDefinitionWriter.write(this.tempPath, writeKeeper);

    assertThat(Files.readString(this.tempPath))
        .contains(
            "{\"child_type_name\":\"user:a\",\"parent_type_name\":\"sw:object\","
                + "\"instruction\":\"inheritance\"}");

    final IDefinitionKeeper readKeeper = new DefinitionKeeper();
    JsonDefinitionReader.readTypes(this.tempPath, readKeeper);

    assertThat(readKeeper.getInheritanceDefinitions(typeA))
        .extracting(InheritanceDefinition::getParentTypeName)
        .containsExactly(TypeString.SW_OBJECT);
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
  void testWriteMethodSetsInCanonicalOrder() throws IOException {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    definitionKeeper.add(
        new MethodDefinition(
            null,
            null,
            null,
            null,
            null,
            TypeString.SW_OBJECT,
            "m1()",
            Set.of(
                MethodDefinition.Modifier.ITER,
                MethodDefinition.Modifier.PRIVATE,
                MethodDefinition.Modifier.ABSTRACT),
            List.of(),
            null,
            new Pragma(
                null,
                Set.of("advanced", "basic"),
                Set.of("zebra", "yak", "walrus", "vole", "unicorn", "tapir"),
                Set.of("internal", "external", "redefinable")),
            ExpressionResultString.UNDEFINED,
            ExpressionResultString.EMPTY));

    JsonDefinitionWriter.write(this.tempPath, definitionKeeper);

    assertThat(Files.readString(this.tempPath))
        .contains("\"modifiers\":[\"private\",\"abstract\",\"iter\"]")
        .contains("\"classify_levels\":[\"advanced\",\"basic\"]")
        .contains("\"topics\":[\"tapir\",\"unicorn\",\"vole\",\"walrus\",\"yak\",\"zebra\"]")
        .contains("\"usages\":[\"external\",\"internal\",\"redefinable\"]");
  }

  @Test
  void testWriteProcedureSetsInCanonicalOrder() throws IOException {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    definitionKeeper.add(
        new ProcedureDefinition(
            null,
            null,
            null,
            null,
            null,
            Set.of(ProcedureDefinition.Modifier.ITER),
            TypeString.ofIdentifier("prc", "user"),
            "test_proc",
            List.of(),
            new Pragma(null, List.of("basic", "external", "quux", "qux", "quuz", "corge")),
            ExpressionResultString.UNDEFINED,
            ExpressionResultString.EMPTY));

    JsonDefinitionWriter.write(this.tempPath, definitionKeeper);

    assertThat(Files.readString(this.tempPath))
        .contains("\"modifiers\":[\"iter\"]")
        .contains("\"classify_levels\":[\"basic\"]")
        .contains("\"topics\":[\"corge\",\"quux\",\"quuz\",\"qux\"]")
        .contains("\"usages\":[\"external\"]");
  }

  private MethodDefinition createTiedMethod(final String uri) {
    return new MethodDefinition(
        new Location(URI.create(uri)),
        null,
        null,
        null,
        null,
        TypeString.ofIdentifier("dup_exemplar", "user"),
        "m_same()",
        Set.of(MethodDefinition.Modifier.PRIVATE),
        List.of(),
        null,
        null,
        ExpressionResultString.UNDEFINED,
        ExpressionResultString.EMPTY);
  }

  private ExemplarDefinition createTiedExemplar(final String uri) {
    return new ExemplarDefinition(
        new Location(URI.create(uri)),
        null,
        null,
        null,
        null,
        ExemplarDefinition.Sort.SLOTTED,
        TypeString.ofIdentifier("dup_exemplar", "user"),
        null);
  }

  private String writeToNewFile(final IDefinitionKeeper definitionKeeper) throws IOException {
    final Path path = Files.createTempFile("type_database_order", ".jsonl");
    try {
      JsonDefinitionWriter.write(path, definitionKeeper);
      return Files.readString(path);
    } finally {
      Files.deleteIfExists(path);
    }
  }

  @Test
  void testWriteTiedMethodsIndependentOfEncounterOrder() throws IOException {
    final MethodDefinition first = this.createTiedMethod("file:///a.magik");
    final MethodDefinition second = this.createTiedMethod("file:///b.magik");

    final String forwards =
        this.writeToNewFile(
            new DefinitionKeeper() {
              @Override
              public Collection<MethodDefinition> getMethodDefinitions() {
                return List.of(first, second);
              }
            });
    final String backwards =
        this.writeToNewFile(
            new DefinitionKeeper() {
              @Override
              public Collection<MethodDefinition> getMethodDefinitions() {
                return List.of(second, first);
              }
            });

    assertThat(forwards).isEqualTo(backwards);
  }

  @Test
  void testWriteTiedExemplarsIndependentOfEncounterOrder() throws IOException {
    final ExemplarDefinition first = this.createTiedExemplar("file:///a.magik");
    final ExemplarDefinition second = this.createTiedExemplar("file:///b.magik");

    final String forwards =
        this.writeToNewFile(
            new DefinitionKeeper() {
              @Override
              public Collection<ExemplarDefinition> getExemplarDefinitions() {
                return List.of(first, second);
              }
            });
    final String backwards =
        this.writeToNewFile(
            new DefinitionKeeper() {
              @Override
              public Collection<ExemplarDefinition> getExemplarDefinitions() {
                return List.of(second, first);
              }
            });

    assertThat(forwards).isEqualTo(backwards);
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
  void testWriteAndReadNonLatin1Doc() throws IOException {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final TypeString typeRef = TypeString.ofIdentifier("user:a", "user");
    final String doc = "Price in € — greeting 你好";
    definitionKeeper.add(
        new ExemplarDefinition(
            new Location(URI.create("file:///file.magik")),
            Instant.now(),
            null,
            doc,
            null,
            ExemplarDefinition.Sort.SLOTTED,
            typeRef,
            null));

    JsonDefinitionWriter.write(this.tempPath, definitionKeeper);

    final IDefinitionKeeper readBack = new DefinitionKeeper(false);
    JsonDefinitionReader.readTypes(this.tempPath, readBack);
    final ExemplarDefinition roundTripped =
        readBack.getExemplarDefinitions(typeRef).stream().findAny().orElseThrow();
    assertThat(roundTripped.getDoc()).isEqualTo(doc);
  }

  @Test
  void testWriteFailureLeavesExistingFilePreserved() throws IOException {
    final String existingContent = "existing complete database\n";
    Files.writeString(this.tempPath, existingContent);

    final DefinitionKeeper failingKeeper =
        new DefinitionKeeper() {
          @Override
          public Collection<MethodDefinition> getMethodDefinitions() {
            throw new IllegalStateException("Simulated failure mid-write");
          }
        };
    failingKeeper.add(
        new ProductDefinition(
            new Location(URI.create("file:///product.def")),
            Instant.now(),
            "test_product",
            null,
            "1",
            "p1",
            "Test product",
            "Test product for testing",
            List.of()));

    assertThatThrownBy(() -> JsonDefinitionWriter.write(this.tempPath, failingKeeper))
        .isInstanceOf(IllegalStateException.class);

    assertThat(Files.readString(this.tempPath)).isEqualTo(existingContent);
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
