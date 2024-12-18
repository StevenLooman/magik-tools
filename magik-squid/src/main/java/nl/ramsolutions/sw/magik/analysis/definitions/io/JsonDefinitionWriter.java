package nl.ramsolutions.sw.magik.analysis.definitions.io;

import com.google.gson.*;
import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Comparator;
import nl.ramsolutions.sw.magik.analysis.definitions.*;
import nl.ramsolutions.sw.magik.analysis.definitions.io.serializer.*;
import nl.ramsolutions.sw.magik.analysis.typing.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import nl.ramsolutions.sw.moduledef.ModuleDefinition;
import nl.ramsolutions.sw.moduledef.ModuleUsage;
import nl.ramsolutions.sw.productdef.ProductDefinition;
import nl.ramsolutions.sw.productdef.ProductUsage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** JSON-line TypeKeeper writer. */
public final class JsonDefinitionWriter {

  private static final class LowerCaseEnumSerializer<E extends Enum<?>>
      implements JsonSerializer<E> {

    @Override
    public JsonElement serialize(
        final E src, final Type typeOfSrc, final JsonSerializationContext context) {
      final String val = src.toString().toLowerCase();
      return new JsonPrimitive(val);
    }
  }

  private static final class InstantSerializer implements JsonSerializer<Instant> {

    @Override
    public JsonElement serialize(
        final Instant src, final Type typeOfSrc, final JsonSerializationContext context) {
      final long seconds = src.getEpochSecond();
      final long nanos = src.getNano();
      final JsonArray array = new JsonArray();
      array.add(seconds);
      array.add(nanos);
      return array;
    }
  }

  private static final Logger LOGGER = LoggerFactory.getLogger(JsonDefinitionWriter.class);

  private final IDefinitionKeeper definitionKeeper;

  private JsonDefinitionWriter(final IDefinitionKeeper definitionKeeper) {
    this.definitionKeeper = definitionKeeper;
  }

  private void run(final Path path) throws IOException {
    LOGGER.debug("Writing type database to path: {}", path);

    final File file = path.toFile();
    try (FileWriter fileReader = new FileWriter(file, StandardCharsets.ISO_8859_1);
        BufferedWriter bufferedWriter = new BufferedWriter(fileReader)) {
      this.writeProducts(bufferedWriter);
      this.writeModules(bufferedWriter);
      this.writeMagikFiles(bufferedWriter);
      this.writePackages(bufferedWriter);
      this.writeExemplars(bufferedWriter);
      this.writeGlobals(bufferedWriter);
      this.writeMethods(bufferedWriter);
      this.writeProcedures(bufferedWriter);
      this.writeConditions(bufferedWriter);
      this.writeBinaryOperators(bufferedWriter);
    }
  }

  private Gson buildGson() {
    return new GsonBuilder()
        .registerTypeAdapter(ExpressionResultString.class, new ExpressionResultStringSerializer())
        .registerTypeAdapter(Instant.class, new InstantSerializer())
        .registerTypeAdapter(
            ExemplarDefinition.Sort.class, new LowerCaseEnumSerializer<ExemplarDefinition.Sort>())
        .registerTypeAdapter(
            MethodDefinition.Modifier.class,
            new LowerCaseEnumSerializer<MethodDefinition.Modifier>())
        .registerTypeAdapter(
            ProcedureDefinition.Modifier.class,
            new LowerCaseEnumSerializer<ProcedureDefinition.Modifier>())
        .registerTypeAdapter(
            ParameterDefinition.Modifier.class,
            new LowerCaseEnumSerializer<ParameterDefinition.Modifier>())
        .registerTypeAdapter(
            BinaryOperatorDefinition.class, new BinaryOperatorDefinitionSerializer())
        .registerTypeAdapter(ConditionDefinition.class, new ConditionDefinitionSerializer())
        .registerTypeAdapter(ExemplarDefinition.class, new ExemplarDefinitionSerializer())
        .registerTypeAdapter(ExpressionResultString.class, new ExpressionResultStringSerializer())
        .registerTypeAdapter(GlobalDefinition.class, new GlobalDefinitionSerializer())
        .registerTypeAdapter(MethodDefinition.class, new MethodDefinitionSerializer())
        .registerTypeAdapter(ModuleUsage.class, new ModuleUsageSerializer())
        .registerTypeAdapter(PackageDefinition.class, new PackageDefinitionSerializer())
        .registerTypeAdapter(ParameterDefinition.class, new ParameterDefinitionSerializer())
        .registerTypeAdapter(ProcedureDefinition.class, new ProcedureDefinitionSerializer())
        .registerTypeAdapter(ProductDefinition.class, new ProductDefinitionSerializer())
        .registerTypeAdapter(ProductUsage.class, new ProductUsageSerializer())
        .registerTypeAdapter(SlotDefinition.class, new SlotDefinitionSerializer())
        .registerTypeAdapter(TypeString.class, new TypeStringSerializer())
        .registerTypeAdapter(ModuleDefinition.class, new ModuleDefinitionSerializer())
        .registerTypeAdapter(MagikFileDefinition.class, new MagikFileDefinitionSerializer())
        .create();
  }

  private void writeInstruction(final Writer writer, final JsonElement instruction) {
    final String instructionStr = instruction.toString();
    try {
      writer.write(instructionStr);
      writer.write("\n");
    } catch (final IOException exception) {
      LOGGER.error("Caught exception writing instruction", exception);
    }
  }

  private void writeProducts(final Writer writer) {
    final Comparator<ProductDefinition> sorter = Comparator.comparing(ProductDefinition::getName);
    this.definitionKeeper.getProductDefinitions().stream()
        .sorted(sorter)
        .forEach(
            definition -> {
              final Gson gson = this.buildGson();
              final JsonObject instruction = (JsonObject) gson.toJsonTree(definition);
              instruction.addProperty(Instruction.FIELD_NAME, Instruction.PRODUCT.getValue());
              this.writeInstruction(writer, instruction);
            });
  }

  private void writeModules(final Writer writer) {
    final Comparator<ModuleDefinition> sorter = Comparator.comparing(ModuleDefinition::getName);
    this.definitionKeeper.getModuleDefinitions().stream()
        .sorted(sorter)
        .forEach(
            definition -> {
              final Gson gson = this.buildGson();
              final JsonObject instruction = (JsonObject) gson.toJsonTree(definition);
              instruction.addProperty(Instruction.FIELD_NAME, Instruction.MODULE.getValue());
              this.writeInstruction(writer, instruction);
            });
  }

  private void writeMagikFiles(final Writer writer) {
    final Comparator<MagikFileDefinition> sorter =
        Comparator.comparing(MagikFileDefinition::getUri);
    this.definitionKeeper.getMagikFileDefinitions().stream()
        .sorted(sorter)
        .forEach(
            definition -> {
              final Gson gson = this.buildGson();
              final JsonObject instruction = (JsonObject) gson.toJsonTree(definition);
              instruction.addProperty(Instruction.FIELD_NAME, Instruction.MAGIK_FILE.getValue());
              this.writeInstruction(writer, instruction);
            });
  }

  private void writePackages(final Writer writer) {
    final Comparator<PackageDefinition> sorter = Comparator.comparing(PackageDefinition::getName);
    this.definitionKeeper.getPackageDefinitions().stream()
        .sorted(sorter)
        .forEach(
            definition -> {
              final Gson gson = this.buildGson();
              final JsonObject instruction = (JsonObject) gson.toJsonTree(definition);
              instruction.addProperty(Instruction.FIELD_NAME, Instruction.PACKAGE.getValue());
              this.writeInstruction(writer, instruction);
            });
  }

  private void writeExemplars(final Writer writer) {
    final Comparator<ExemplarDefinition> sorter =
        Comparator.comparing(ExemplarDefinition::getTypeString);
    this.definitionKeeper.getExemplarDefinitions().stream()
        .sorted(sorter)
        .forEach(
            definition -> {
              final Gson gson = this.buildGson();
              final JsonObject instruction = (JsonObject) gson.toJsonTree(definition);
              instruction.addProperty(Instruction.FIELD_NAME, Instruction.TYPE.getValue());
              this.writeInstruction(writer, instruction);
            });
  }

  private void writeMethods(final Writer writer) {
    final Comparator<MethodDefinition> typeNameComparer =
        Comparator.comparing(MethodDefinition::getTypeName);
    final Comparator<MethodDefinition> nameComparer =
        Comparator.comparing(MethodDefinition::getName);
    final Comparator<MethodDefinition> sorter = typeNameComparer.thenComparing(nameComparer);
    this.definitionKeeper.getMethodDefinitions().stream()
        .sorted(sorter)
        .forEach(
            definition -> {
              final Gson gson = this.buildGson();
              final JsonObject instruction = (JsonObject) gson.toJsonTree(definition);
              instruction.addProperty(Instruction.FIELD_NAME, Instruction.METHOD.getValue());
              this.writeInstruction(writer, instruction);
            });
  }

  private void writeProcedures(final Writer writer) {
    final Comparator<ProcedureDefinition> sorter =
        Comparator.comparing(ProcedureDefinition::getTypeString);
    this.definitionKeeper.getProcedureDefinitions().stream()
        .sorted(sorter)
        .forEach(
            definition -> {
              final Gson gson = this.buildGson();
              final JsonObject instruction = (JsonObject) gson.toJsonTree(definition);
              instruction.addProperty(Instruction.FIELD_NAME, Instruction.METHOD.getValue());
              this.writeInstruction(writer, instruction);
            });
  }

  private void writeConditions(final BufferedWriter writer) {
    final Comparator<ConditionDefinition> sorter =
        Comparator.comparing(ConditionDefinition::getName);
    this.definitionKeeper.getConditionDefinitions().stream()
        .sorted(sorter)
        .forEach(
            definition -> {
              final Gson gson = this.buildGson();
              final JsonObject instruction = (JsonObject) gson.toJsonTree(definition);
              instruction.addProperty(Instruction.FIELD_NAME, Instruction.CONDITION.getValue());
              this.writeInstruction(writer, instruction);
            });
  }

  private void writeBinaryOperators(final BufferedWriter writer) {
    final Comparator<BinaryOperatorDefinition> lhsComparer =
        Comparator.comparing(BinaryOperatorDefinition::getLhsTypeName);
    final Comparator<BinaryOperatorDefinition> rhsComparer =
        Comparator.comparing(BinaryOperatorDefinition::getRhsTypeName);
    final Comparator<BinaryOperatorDefinition> resultComparer =
        Comparator.comparing(BinaryOperatorDefinition::getResultTypeName);
    final Comparator<BinaryOperatorDefinition> sorter =
        lhsComparer.thenComparing(rhsComparer).thenComparing(resultComparer);
    this.definitionKeeper.getBinaryOperatorDefinitions().stream()
        .sorted(sorter)
        .forEach(
            definition -> {
              final Gson gson = this.buildGson();
              final JsonObject instruction = (JsonObject) gson.toJsonTree(definition);
              instruction.addProperty(
                  Instruction.FIELD_NAME, Instruction.BINARY_OPERATOR.getValue());
              this.writeInstruction(writer, instruction);
            });
  }

  private void writeGlobals(final Writer writer) {
    final Comparator<GlobalDefinition> sorter =
        Comparator.comparing(GlobalDefinition::getTypeString);
    this.definitionKeeper.getGlobalDefinitions().stream()
        .sorted(sorter)
        .forEach(
            definition -> {
              final Gson gson = this.buildGson();
              final JsonObject instruction = (JsonObject) gson.toJsonTree(definition);
              instruction.addProperty(Instruction.FIELD_NAME, Instruction.GLOBAL.getValue());
              this.writeInstruction(writer, instruction);
            });
  }

  /**
   * Write types to a JSON-line file.
   *
   * @param path Path to JSON-line file.
   * @param definitionKeeper {@link IDefinitionKeeper} to dump.
   * @throws IOException -
   */
  public static void write(final Path path, final IDefinitionKeeper definitionKeeper)
      throws IOException {
    final JsonDefinitionWriter reader = new JsonDefinitionWriter(definitionKeeper);
    reader.run(path);
  }
}
