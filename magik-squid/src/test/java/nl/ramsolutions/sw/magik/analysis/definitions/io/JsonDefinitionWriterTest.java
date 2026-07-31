package nl.ramsolutions.sw.magik.analysis.definitions.io;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
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

  private static final String INSTRUCTION = "instruction";
  private static final Instant TIMESTAMP = Instant.ofEpochSecond(1234567890L, 123456789);
  private static final Location LOCATION = new Location(URI.create("file:///file.magik"));

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

  /** Read the written definitions carrying the given instruction. */
  private List<JsonObject> readWritten(final String instruction) throws IOException {
    return Files.readAllLines(this.tempPath, StandardCharsets.UTF_8).stream()
        .map(line -> JsonParser.parseString(line).getAsJsonObject())
        .filter(object -> object.get(INSTRUCTION).getAsString().equals(instruction))
        .toList();
  }

  /** Read the one and only written definition carrying the given instruction. */
  private JsonObject readWrittenSingle(final String instruction) throws IOException {
    final List<JsonObject> definitions = this.readWritten(instruction);
    assertThat(definitions).hasSize(1);
    return definitions.get(0);
  }

  /** Read the written definition carrying the given instruction and `name`. */
  private JsonObject readWrittenNamed(final String instruction, final String name)
      throws IOException {
    return this.readWritten(instruction).stream()
        .filter(object -> object.get("name").getAsString().equals(name))
        .findAny()
        .orElseThrow();
  }

  private String getString(final JsonObject object, final String field) {
    return object.get(field).getAsString();
  }

  private List<String> getStrings(final JsonObject object, final String field) {
    return object.get(field).getAsJsonArray().asList().stream()
        .map(element -> element.getAsString())
        .toList();
  }

  @Test
  void testWriteProduct() throws IOException {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    definitionKeeper.add(
        new ProductDefinition(
            new Location(URI.create("file:///product.def")),
            TIMESTAMP,
            "test_product",
            "parent_product",
            "1",
            "p1",
            "Test product",
            "Test product for testing",
            List.of(new ProductUsage("used_product", null))));

    JsonDefinitionWriter.write(this.tempPath, definitionKeeper);

    final JsonObject product = this.readWrittenSingle("product");
    assertThat(this.getString(product, "name")).isEqualTo("test_product");
    assertThat(this.getString(product, "parent")).isEqualTo("parent_product");
    assertThat(this.getString(product, "version")).isEqualTo("1");
    assertThat(this.getString(product, "version_comment")).isEqualTo("p1");
    assertThat(this.getString(product, "title")).isEqualTo("Test product");
    assertThat(this.getString(product, "description")).isEqualTo("Test product for testing");
    assertThat(this.getString(product.getAsJsonObject("location"), "uri"))
        .isEqualTo("file:///product.def");
    assertThat(product.getAsJsonArray("usages").asList())
        .extracting(element -> this.getString(element.getAsJsonObject(), "name"))
        .containsExactly("used_product");
  }

  @Test
  void testWriteModule() throws IOException {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    definitionKeeper.add(
        new ModuleDefinition(
            new Location(URI.create("file:///module.def")),
            TIMESTAMP,
            "test_module",
            "test_product",
            "1",
            "2",
            "Test module",
            List.of(new ModuleUsage("used_module", null)),
            List.of(new ModuleUsage("test_used_module", null))));

    JsonDefinitionWriter.write(this.tempPath, definitionKeeper);

    final JsonObject module = this.readWrittenSingle("module");
    assertThat(this.getString(module, "name")).isEqualTo("test_module");
    assertThat(this.getString(module, "product")).isEqualTo("test_product");
    assertThat(this.getString(module, "base_version")).isEqualTo("1");
    assertThat(this.getString(module, "current_version")).isEqualTo("2");
    assertThat(this.getString(module, "description")).isEqualTo("Test module");
    assertThat(module.getAsJsonArray("required_modules").asList())
        .extracting(element -> this.getString(element.getAsJsonObject(), "name"))
        .containsExactly("used_module");
    assertThat(module.getAsJsonArray("test_modules").asList())
        .extracting(element -> this.getString(element.getAsJsonObject(), "name"))
        .containsExactly("test_used_module");
  }

  @Test
  void testWriteMagikFile() throws IOException {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    definitionKeeper.add(new MagikFileDefinition(LOCATION, TIMESTAMP));

    JsonDefinitionWriter.write(this.tempPath, definitionKeeper);

    final JsonObject magikFile = this.readWrittenSingle("magik_file");
    assertThat(this.getString(magikFile.getAsJsonObject("location"), "uri"))
        .isEqualTo("file:///file.magik");
    assertThat(magikFile.getAsJsonArray("timestamp").toString())
        .isEqualTo("[1234567890,123456789]");
  }

  @Test
  void testWritePackage() throws IOException {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    definitionKeeper.add(
        new PackageDefinition(
            LOCATION, TIMESTAMP, null, null, null, "test_package", List.of("sw")));

    JsonDefinitionWriter.write(this.tempPath, definitionKeeper);

    final JsonObject packageDef = this.readWrittenNamed("package", "test_package");
    assertThat(this.getStrings(packageDef, "uses")).containsExactly("sw");
    assertThat(this.getString(packageDef.getAsJsonObject("location"), "uri"))
        .isEqualTo("file:///file.magik");
  }

  @Test
  void testWriteType() throws IOException {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper(false);
    final TypeString aRef = TypeString.ofIdentifier("a", "user");
    definitionKeeper.add(
        new ExemplarDefinition(
            LOCATION,
            TIMESTAMP,
            "test_module",
            "Test exemplar a.",
            null,
            ExemplarDefinition.Sort.SLOTTED,
            aRef,
            null));

    JsonDefinitionWriter.write(this.tempPath, definitionKeeper);

    final JsonObject exemplar = this.readWrittenSingle("type");
    assertThat(this.getString(exemplar, "type_name")).isEqualTo("user:a");
    assertThat(this.getString(exemplar, "sort")).isEqualTo("slotted");
    assertThat(this.getString(exemplar, "module_name")).isEqualTo("test_module");
    assertThat(this.getString(exemplar, "doc")).isEqualTo("Test exemplar a.");
  }

  @Test
  void testWriteAndReadMixinType() throws IOException {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final TypeString mixinRef = TypeString.ofIdentifier("user:my_mixin", "user");
    definitionKeeper.add(
        new ExemplarDefinition(
            LOCATION,
            TIMESTAMP,
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

    assertThat(Files.readString(this.tempPath))
        .contains(
            "{\"owner_type_name\":\"user:a\",\"name\":\"slot1\",\"type_name\":\"sw:integer\","
                + "\"instruction\":\"slot\"}");

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
            LOCATION,
            TIMESTAMP,
            "test_module",
            "Test method m1().",
            null,
            TypeString.SW_OBJECT,
            "m1()",
            Set.of(MethodDefinition.Modifier.PRIVATE),
            List.of(
                new ParameterDefinition(
                    LOCATION,
                    TIMESTAMP,
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

    final JsonObject method = this.readWrittenSingle("method");
    assertThat(this.getString(method, "type_name")).isEqualTo("sw:object");
    assertThat(this.getString(method, "method_name")).isEqualTo("m1()");
    assertThat(this.getString(method, "module_name")).isEqualTo("test_module");
    assertThat(this.getString(method, "doc")).isEqualTo("Test method m1().");
    assertThat(this.getStrings(method, "modifiers")).containsExactly("private");
    assertThat(this.getString(method, "return_types")).isEqualTo("__UNDEFINED_RESULT__");
    assertThat(this.getStrings(method, "loop_types")).isEmpty();

    final JsonObject parameter = method.getAsJsonArray("parameters").get(0).getAsJsonObject();
    assertThat(this.getString(parameter, "name")).isEqualTo("param1");
    assertThat(this.getString(parameter, "modifier")).isEqualTo("optional");
    assertThat(this.getString(parameter, "type_name")).isEqualTo("_undefined");
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
            LOCATION,
            TIMESTAMP,
            null,
            "Unknown value",
            null,
            "unknown_value",
            "error",
            List.of("value", "permitted_values"),
            new Pragma(null, Set.of("basic"), Set.of(), Set.of())));

    JsonDefinitionWriter.write(this.tempPath, definitionKeeper);

    assertThat(this.readWritten("condition"))
        .extracting(object -> this.getString(object, "name"))
        .containsExactly("error", "unknown_value");

    final JsonObject condition = this.readWrittenNamed("condition", "unknown_value");
    assertThat(this.getString(condition, "parent")).isEqualTo("error");
    assertThat(this.getString(condition, "doc")).isEqualTo("Unknown value");
    assertThat(this.getStrings(condition, "data_names"))
        .containsExactly("value", "permitted_values");
    assertThat(this.getStrings(condition.getAsJsonObject("pragma"), "classify_levels"))
        .containsExactly("basic");
  }

  @Test
  void testWriteProcedure() throws IOException {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    definitionKeeper.add(
        new ProcedureDefinition(
            LOCATION,
            TIMESTAMP,
            "test_module",
            "Test procedure",
            null,
            Set.of(ProcedureDefinition.Modifier.ITER),
            TypeString.ofIdentifier("prc", "user"),
            "test_proc",
            List.of(
                new ParameterDefinition(
                    null,
                    null,
                    null,
                    null,
                    null,
                    "param1",
                    ParameterDefinition.Modifier.NONE,
                    TypeString.SW_INTEGER)),
            null,
            ExpressionResultString.UNDEFINED,
            ExpressionResultString.EMPTY));

    JsonDefinitionWriter.write(this.tempPath, definitionKeeper);

    final JsonObject procedure = this.readWrittenSingle("procedure");
    assertThat(this.getString(procedure, "type_name")).isEqualTo("user:prc");
    assertThat(this.getString(procedure, "procedure_name")).isEqualTo("test_proc");
    assertThat(this.getString(procedure, "module_name")).isEqualTo("test_module");
    assertThat(this.getString(procedure, "doc")).isEqualTo("Test procedure");
    assertThat(this.getStrings(procedure, "modifiers")).containsExactly("iter");
    assertThat(this.getString(procedure, "return_types")).isEqualTo("__UNDEFINED_RESULT__");

    final JsonObject parameter = procedure.getAsJsonArray("parameters").get(0).getAsJsonObject();
    assertThat(this.getString(parameter, "name")).isEqualTo("param1");
    assertThat(this.getString(parameter, "type_name")).isEqualTo("sw:integer");

    assertThat(this.readWritten("method")).isEmpty();
  }

  @Test
  void testWriteGlobal() throws IOException {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final TypeString aliasedRef = TypeString.ofIdentifier("alias", "user");
    definitionKeeper.add(
        new GlobalDefinition(
            null, null, null, null, null, aliasedRef, TypeString.SW_INTEGER, null));

    JsonDefinitionWriter.write(this.tempPath, definitionKeeper);

    assertThat(Files.readString(this.tempPath))
        .contains(
            "{\"type_name\":\"user:alias\",\"aliased_type_name\":\"sw:integer\","
                + "\"used_methods\":[],\"used_globals\":[],\"used_binary_operators\":[],"
                + "\"instruction\":\"global\"}");
  }

  @Test
  void testWriteBinaryOperator() throws IOException {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    definitionKeeper.add(
        new BinaryOperatorDefinition(
            LOCATION,
            TIMESTAMP,
            "test_module",
            null,
            null,
            "+",
            TypeString.SW_CHAR16_VECTOR,
            TypeString.SW_SYMBOL,
            TypeString.SW_CHAR16_VECTOR));

    JsonDefinitionWriter.write(this.tempPath, definitionKeeper);

    final JsonObject binaryOperator = this.readWrittenSingle("binary_operator");
    assertThat(this.getString(binaryOperator, "operator")).isEqualTo("+");
    assertThat(this.getString(binaryOperator, "lhs_type_name")).isEqualTo("sw:char16_vector");
    assertThat(this.getString(binaryOperator, "rhs_type_name")).isEqualTo("sw:symbol");
    assertThat(this.getString(binaryOperator, "result_type_name")).isEqualTo("sw:char16_vector");
    assertThat(this.getString(binaryOperator, "module_name")).isEqualTo("test_module");
  }

  private IDefinitionKeeper createKeeperWithAllDefinitionKinds() {
    final TypeString typeA = TypeString.ofIdentifier("a", "user");
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper(false);
    definitionKeeper.add(
        new ProductDefinition(
            new Location(URI.create("file:///product.def")),
            TIMESTAMP,
            "test_product",
            null,
            "1",
            null,
            null,
            null,
            List.of()));
    definitionKeeper.add(
        new ModuleDefinition(
            new Location(URI.create("file:///module.def")),
            TIMESTAMP,
            "test_module",
            null,
            "1",
            null,
            null,
            List.of(),
            List.of()));
    definitionKeeper.add(new MagikFileDefinition(LOCATION, TIMESTAMP));
    definitionKeeper.add(
        new PackageDefinition(
            LOCATION, TIMESTAMP, null, null, null, "test_package", List.of("sw")));
    definitionKeeper.add(
        new ExemplarDefinition(
            LOCATION, TIMESTAMP, null, null, null, ExemplarDefinition.Sort.SLOTTED, typeA, null));
    definitionKeeper.add(
        new InheritanceDefinition(null, null, null, null, null, typeA, TypeString.SW_OBJECT));
    definitionKeeper.add(
        new SlotDefinition(null, null, null, null, null, typeA, "slot1", TypeString.SW_INTEGER));
    definitionKeeper.add(
        new GlobalDefinition(
            null,
            null,
            null,
            null,
            null,
            TypeString.ofIdentifier("alias", "user"),
            TypeString.SW_INTEGER,
            null));
    definitionKeeper.add(
        new MethodDefinition(
            LOCATION,
            TIMESTAMP,
            null,
            null,
            null,
            typeA,
            "m1()",
            Set.of(),
            List.of(),
            null,
            null,
            ExpressionResultString.UNDEFINED,
            ExpressionResultString.EMPTY));
    definitionKeeper.add(
        new ProcedureDefinition(
            LOCATION,
            TIMESTAMP,
            null,
            null,
            null,
            Set.of(),
            TypeString.ofIdentifier("prc", "user"),
            "test_proc",
            List.of(),
            null,
            ExpressionResultString.UNDEFINED,
            ExpressionResultString.EMPTY));
    definitionKeeper.add(
        new ConditionDefinition(
            null, null, null, null, null, "test_error", null, List.of("string"), null));
    definitionKeeper.add(
        new BinaryOperatorDefinition(
            LOCATION,
            TIMESTAMP,
            null,
            null,
            null,
            "+",
            TypeString.SW_CHAR16_VECTOR,
            TypeString.SW_SYMBOL,
            TypeString.SW_CHAR16_VECTOR));
    return definitionKeeper;
  }

  @Test
  void testWriteUsesOwnInstructionPerDefinitionKind() throws IOException {
    final IDefinitionKeeper definitionKeeper = this.createKeeperWithAllDefinitionKinds();

    JsonDefinitionWriter.write(this.tempPath, definitionKeeper);

    final Map<String, Long> countPerInstruction =
        Files.readAllLines(this.tempPath, StandardCharsets.UTF_8).stream()
            .map(line -> JsonParser.parseString(line).getAsJsonObject())
            .collect(
                Collectors.groupingBy(
                    object -> this.getString(object, INSTRUCTION), Collectors.counting()));

    // The keeper always holds the `_anon` package, next to the added `test_package`.
    assertThat(countPerInstruction)
        .containsExactlyInAnyOrderEntriesOf(
            Map.ofEntries(
                Map.entry("product", 1L),
                Map.entry("module", 1L),
                Map.entry("magik_file", 1L),
                Map.entry("package", 2L),
                Map.entry("type", 1L),
                Map.entry("inheritance", 1L),
                Map.entry("slot", 1L),
                Map.entry("global", 1L),
                Map.entry("method", 1L),
                Map.entry("procedure", 1L),
                Map.entry("condition", 1L),
                Map.entry("binary_operator", 1L)));
  }

  @Test
  void testWriteAndReadAllDefinitionKinds() throws IOException {
    final TypeString typeA = TypeString.ofIdentifier("a", "user");
    final IDefinitionKeeper definitionKeeper = this.createKeeperWithAllDefinitionKinds();

    JsonDefinitionWriter.write(this.tempPath, definitionKeeper);

    final IDefinitionKeeper readKeeper = new DefinitionKeeper(false);
    JsonDefinitionReader.readTypes(this.tempPath, readKeeper);

    assertThat(readKeeper.getProductDefinitions())
        .extracting(ProductDefinition::getName)
        .containsExactly("test_product");
    assertThat(readKeeper.getModuleDefinitions())
        .extracting(ModuleDefinition::getName)
        .containsExactly("test_module");
    assertThat(readKeeper.getMagikFileDefinitions())
        .extracting(MagikFileDefinition::getUri)
        .containsExactly(URI.create("file:///file.magik"));
    assertThat(readKeeper.getPackageDefinitions())
        .extracting(PackageDefinition::getName)
        .contains("test_package");
    assertThat(readKeeper.getExemplarDefinitions())
        .extracting(ExemplarDefinition::getTypeString)
        .containsExactly(typeA);
    assertThat(readKeeper.getInheritanceDefinitions())
        .extracting(InheritanceDefinition::getParentTypeName)
        .containsExactly(TypeString.SW_OBJECT);
    assertThat(readKeeper.getSlotDefinitions())
        .extracting(SlotDefinition::getName)
        .containsExactly("slot1");
    assertThat(readKeeper.getGlobalDefinitions())
        .extracting(GlobalDefinition::getAliasedTypeName)
        .containsExactly(TypeString.SW_INTEGER);
    assertThat(readKeeper.getMethodDefinitions())
        .extracting(MethodDefinition::getName)
        .containsExactly("user:a.m1()");
    assertThat(readKeeper.getProcedureDefinitions())
        .extracting(ProcedureDefinition::getProcedureName)
        .containsExactly("test_proc");
    assertThat(readKeeper.getConditionDefinitions())
        .extracting(ConditionDefinition::getName)
        .containsExactly("test_error");
    assertThat(readKeeper.getBinaryOperatorDefinitions())
        .extracting(BinaryOperatorDefinition::getOperator)
        .containsExactly("+");
  }

  @Test
  void testWriteAndReadVariadicMethodResult() throws IOException {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final TypeString bRef = TypeString.ofIdentifier("user:b", "user");
    final ExpressionResultString variadicResult =
        new ExpressionResultString(TypeString.ofVariadic(TypeString.SW_INTEGER));
    final MethodDefinition original =
        new MethodDefinition(
            LOCATION,
            TIMESTAMP,
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
            LOCATION,
            TIMESTAMP,
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
            LOCATION,
            TIMESTAMP,
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
            LOCATION, TIMESTAMP, null, doc, null, ExemplarDefinition.Sort.SLOTTED, typeRef, null));

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
            TIMESTAMP,
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
}
