package nl.ramsolutions.sw.magik.analysis.definitions.io;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import nl.ramsolutions.sw.magik.analysis.definitions.BinaryOperatorDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.ConditionDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.ExemplarDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.GlobalDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.InheritanceDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.MagikFileDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.PackageDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.ParameterDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.ProcedureDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.SlotDefinition;
import nl.ramsolutions.sw.magik.analysis.typing.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import nl.ramsolutions.sw.moduledef.ModuleDefinition;
import nl.ramsolutions.sw.productdef.ProductDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** JSON-line TypeKeeper writer. */
public final class JsonDefinitionWriter {

  private static final class TypeStringSerializer implements JsonSerializer<TypeString> {

    @Override
    public JsonElement serialize(
        final TypeString src, final Type typeOfSrc, final JsonSerializationContext context) {
      final String fullString = src.getFullString();
      return new JsonPrimitive(fullString);
    }
  }

  private static final class ExpressionResultStringSerializer
      implements JsonSerializer<ExpressionResultString> {

    @Override
    public JsonElement serialize(
        final ExpressionResultString src,
        final Type typeOfSrc,
        final JsonSerializationContext context) {
      if (src.equals(ExpressionResultString.UNDEFINED)) {
        return new JsonPrimitive(ExpressionResultString.UNDEFINED_SERIALIZED_NAME);
      }

      final JsonArray result = new JsonArray();
      src.getTypes().stream().map(TypeString::getFullString).forEach(result::add);
      return result;
    }
  }

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

    // Write to a temporary file first, then move it into place.
    // This guarantees a reader never sees a partially-written file.
    final Path tempPath = path.resolveSibling(path.getFileName().toString() + ".tmp");
    try {
      final File tempFile = tempPath.toFile();
      try (final FileWriter fileWriter = new FileWriter(tempFile, StandardCharsets.UTF_8);
          final BufferedWriter bufferedWriter = new BufferedWriter(fileWriter)) {
        this.writeSchemaVersion(bufferedWriter);
        this.writeProducts(bufferedWriter);
        this.writeModules(bufferedWriter);
        this.writeMagikFiles(bufferedWriter);
        this.writePackages(bufferedWriter);
        this.writeExemplars(bufferedWriter);
        this.writeInheritance(bufferedWriter);
        this.writeSlots(bufferedWriter);
        this.writeGlobals(bufferedWriter);
        this.writeMethods(bufferedWriter);
        this.writeProcedures(bufferedWriter);
        this.writeConditions(bufferedWriter);
        this.writeBinaryOperators(bufferedWriter);
      }

      JsonDefinitionWriter.moveIntoPlace(tempPath, path);
    } catch (final RuntimeException | IOException exception) {
      try {
        Files.deleteIfExists(tempPath);
      } catch (final IOException deleteException) {
        exception.addSuppressed(deleteException);
      }
      throw exception;
    }
  }

  private static void moveIntoPlace(final Path source, final Path target) throws IOException {
    try {
      Files.move(
          source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    } catch (final AtomicMoveNotSupportedException exception) {
      LOGGER.debug("Atomic move not supported, falling back to non-atomic replace", exception);
      Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
    }
  }

  private Gson buildGson() {
    return new GsonBuilder()
        .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
        .registerTypeAdapter(TypeString.class, new TypeStringSerializer())
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
        .create();
  }

  private void writeSchemaVersion(final Writer writer) {
    final JsonObject versionObject = new JsonObject();
    versionObject.addProperty(
        Instruction.INSTRUCTION.getValue(), Instruction.SCHEMA_VERSION.getValue());
    versionObject.addProperty("value", JsonDefinitionReader.SCHEMA_VERSION);
    this.writeInstruction(writer, versionObject.toString());
  }

  private void writeInstruction(final Writer writer, final String instruction) {
    try {
      writer.write(instruction);
      writer.write("\n");
    } catch (final IOException exception) {
      LOGGER.error("Caught exception writing instruction", exception);
    }
  }

  private <T> String serializeDefinition(
      final Gson gson, final T definition, final Instruction instruction) {
    final JsonObject instructionObject = (JsonObject) gson.toJsonTree(definition);
    instructionObject.addProperty(Instruction.INSTRUCTION.getValue(), instruction.getValue());
    return instructionObject.toString();
  }

  private <T> void writeDefinitions(
      final Writer writer,
      final Collection<T> definitions,
      final Comparator<T> sorter,
      final Instruction instruction) {
    final Gson gson = this.buildGson();
    // If the sorter does not provide a total order, tie-break using the serialized form.
    final Comparator<T> totalSorter =
        sorter.thenComparing(definition -> this.serializeDefinition(gson, definition, instruction));
    definitions.stream()
        .sorted(totalSorter)
        .forEach(
            definition ->
                this.writeInstruction(
                    writer, this.serializeDefinition(gson, definition, instruction)));
  }

  private void writeProducts(final Writer writer) {
    this.writeDefinitions(
        writer,
        this.definitionKeeper.getProductDefinitions(),
        Comparator.comparing(ProductDefinition::getName),
        Instruction.PRODUCT);
  }

  private void writeModules(final Writer writer) {
    this.writeDefinitions(
        writer,
        this.definitionKeeper.getModuleDefinitions(),
        Comparator.comparing(ModuleDefinition::getName),
        Instruction.MODULE);
  }

  private void writeMagikFiles(final Writer writer) {
    this.writeDefinitions(
        writer,
        this.definitionKeeper.getMagikFileDefinitions(),
        Comparator.comparing(MagikFileDefinition::getUri),
        Instruction.MAGIK_FILE);
  }

  private void writePackages(final Writer writer) {
    this.writeDefinitions(
        writer,
        this.definitionKeeper.getPackageDefinitions(),
        Comparator.comparing(PackageDefinition::getName),
        Instruction.PACKAGE);
  }

  private void writeExemplars(final Writer writer) {
    this.writeDefinitions(
        writer,
        this.definitionKeeper.getExemplarDefinitions(),
        Comparator.comparing(ExemplarDefinition::getTypeString),
        Instruction.TYPE);
  }

  private void writeInheritance(final Writer writer) {
    final Comparator<InheritanceDefinition> childComparer =
        Comparator.comparing(InheritanceDefinition::getChildTypeName);
    final Comparator<InheritanceDefinition> parentComparer =
        Comparator.comparing(InheritanceDefinition::getParentTypeName);
    this.writeDefinitions(
        writer,
        this.definitionKeeper.getInheritanceDefinitions(),
        childComparer.thenComparing(parentComparer),
        Instruction.INHERITANCE);
  }

  private void writeSlots(final Writer writer) {
    final Comparator<SlotDefinition> ownerComparer =
        Comparator.comparing(SlotDefinition::getOwnerTypeName);
    final Comparator<SlotDefinition> nameComparer = Comparator.comparing(SlotDefinition::getName);
    this.writeDefinitions(
        writer,
        this.definitionKeeper.getSlotDefinitions(),
        ownerComparer.thenComparing(nameComparer),
        Instruction.SLOT);
  }

  private void writeMethods(final Writer writer) {
    final Comparator<MethodDefinition> typeNameComparer =
        Comparator.comparing(MethodDefinition::getTypeName);
    final Comparator<MethodDefinition> nameComparer =
        Comparator.comparing(MethodDefinition::getName);
    this.writeDefinitions(
        writer,
        this.definitionKeeper.getMethodDefinitions(),
        typeNameComparer.thenComparing(nameComparer),
        Instruction.METHOD);
  }

  private void writeProcedures(final Writer writer) {
    this.writeDefinitions(
        writer,
        this.definitionKeeper.getProcedureDefinitions(),
        Comparator.comparing(ProcedureDefinition::getTypeString),
        Instruction.PROCEDURE);
  }

  private void writeConditions(final Writer writer) {
    this.writeDefinitions(
        writer,
        this.definitionKeeper.getConditionDefinitions(),
        Comparator.comparing(ConditionDefinition::getName),
        Instruction.CONDITION);
  }

  private void writeBinaryOperators(final Writer writer) {
    final Comparator<BinaryOperatorDefinition> lhsComparer =
        Comparator.comparing(BinaryOperatorDefinition::getLhsTypeName);
    final Comparator<BinaryOperatorDefinition> rhsComparer =
        Comparator.comparing(BinaryOperatorDefinition::getRhsTypeName);
    final Comparator<BinaryOperatorDefinition> resultComparer =
        Comparator.comparing(BinaryOperatorDefinition::getResultTypeName);
    this.writeDefinitions(
        writer,
        this.definitionKeeper.getBinaryOperatorDefinitions(),
        lhsComparer.thenComparing(rhsComparer).thenComparing(resultComparer),
        Instruction.BINARY_OPERATOR);
  }

  private void writeGlobals(final Writer writer) {
    this.writeDefinitions(
        writer,
        this.definitionKeeper.getGlobalDefinitions(),
        Comparator.comparing(GlobalDefinition::getTypeString),
        Instruction.GLOBAL);
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
