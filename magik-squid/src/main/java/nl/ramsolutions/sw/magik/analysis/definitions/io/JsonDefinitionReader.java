package nl.ramsolutions.sw.magik.analysis.definitions.io;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.InstanceCreator;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
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
import nl.ramsolutions.sw.magik.analysis.definitions.Provenance;
import nl.ramsolutions.sw.magik.analysis.definitions.SlotDefinition;
import nl.ramsolutions.sw.magik.analysis.typing.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import nl.ramsolutions.sw.magik.parser.TypeStringParser;
import nl.ramsolutions.sw.moduledef.ModuleDefinition;
import nl.ramsolutions.sw.productdef.ProductDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** JSON-line TypeKeeper reader. */
public final class JsonDefinitionReader {

  private static final class TypeStringDeserializer implements JsonDeserializer<TypeString> {

    @Override
    public TypeString deserialize(
        final JsonElement json, final Type typeOfT, final JsonDeserializationContext context)
        throws JsonParseException {
      final String identifier = json.getAsString();
      return TypeStringParser.parseTypeString(identifier);
    }
  }

  private static final class ExpressionResultStringDeserializer
      implements JsonDeserializer<ExpressionResultString> {

    @Override
    public ExpressionResultString deserialize(
        final JsonElement json, final Type typeOfT, final JsonDeserializationContext context)
        throws JsonParseException {
      if (json.isJsonPrimitive()
          && json.getAsString().equals(ExpressionResultString.UNDEFINED_SERIALIZED_NAME)) {
        return ExpressionResultString.UNDEFINED;
      } else if (json.isJsonArray()) {
        final List<JsonElement> elements = json.getAsJsonArray().asList();
        final List<TypeString> types = new java.util.ArrayList<>(elements.size());
        for (int i = 0; i < elements.size(); ++i) {
          final String raw = elements.get(i).getAsString();
          final boolean variadic = i == elements.size() - 1 && raw.endsWith("...");
          final String inner = variadic ? raw.substring(0, raw.length() - 3) : raw;
          TypeString typeString = TypeStringParser.parseTypeString(inner);
          if (variadic) {
            typeString = TypeString.ofVariadic(typeString);
          }
          types.add(typeString);
        }
        return new ExpressionResultString(types);
      }

      throw new IllegalStateException();
    }
  }

  private static final class LowerCaseEnumDeserializer<E extends Enum<?>>
      implements JsonDeserializer<E> {

    @SuppressWarnings("unchecked")
    @Override
    public E deserialize(
        final JsonElement json, final Type typeOfT, final JsonDeserializationContext context)
        throws JsonParseException {
      final String value = json.getAsString().toUpperCase();
      if (typeOfT instanceof Class<?> clazz) {
        if (clazz.isEnum()) {
          return Arrays.stream(clazz.getEnumConstants())
              .filter(enumValue -> enumValue.toString().equals(value))
              .map(enumValue -> (E) enumValue)
              .findFirst()
              .orElseThrow();
        }
      }

      throw new IllegalStateException("Value '" + value + "' is not a known Enum value");
    }
  }

  private static final class InstantDeserializer implements JsonDeserializer<Instant> {

    @Override
    public Instant deserialize(
        final JsonElement json, final Type typeOfT, final JsonDeserializationContext context)
        throws JsonParseException {
      final JsonArray array = json.getAsJsonArray();
      if (array.size() == 2) {
        final long seconds = array.get(0).getAsLong();
        final long nanos = array.get(1).getAsLong();
        return Instant.ofEpochSecond(seconds, nanos);
      }

      throw new IllegalStateException();
    }
  }

  private static final class ProductDefinitionCreator
      implements InstanceCreator<ProductDefinition> {

    @Override
    public ProductDefinition createInstance(final Type type) {
      return new ProductDefinition(
          null, null, null, null, null, null, null, null, Collections.emptyList());
    }
  }

  private static final class ModuleDefinitionCreator implements InstanceCreator<ModuleDefinition> {

    @Override
    public ModuleDefinition createInstance(final Type type) {
      return new ModuleDefinition(
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          Collections.emptyList(),
          Collections.emptyList());
    }
  }

  private static final class MethodDefinitionCreator implements InstanceCreator<MethodDefinition> {

    @Override
    public MethodDefinition createInstance(final Type type) {
      return new MethodDefinition(
          null,
          null,
          null,
          null,
          null,
          TypeString.UNDEFINED,
          "dummy_method",
          Collections.emptySet(),
          Collections.emptyList(),
          null,
          null,
          ExpressionResultString.UNDEFINED,
          ExpressionResultString.UNDEFINED);
    }
  }

  private static final class ExemplarDefinitionCreator
      implements InstanceCreator<ExemplarDefinition> {

    @Override
    public ExemplarDefinition createInstance(final Type type) {
      return new ExemplarDefinition(
          null,
          null,
          null,
          null,
          null,
          ExemplarDefinition.Sort.UNDEFINED,
          TypeString.UNDEFINED,
          null);
    }
  }

  private static final class MagikFileDefinitionCreator
      implements InstanceCreator<MagikFileDefinition> {

    @Override
    public MagikFileDefinition createInstance(final Type type) {
      // Defaults provenance to REASONED, even if not set in the source JSON.
      return new MagikFileDefinition(null, null);
    }
  }

  private static final class InheritanceDefinitionCreator
      implements InstanceCreator<InheritanceDefinition> {

    @Override
    public InheritanceDefinition createInstance(final Type type) {
      // Defaults provenance to REASONED, even if not set in the source JSON.
      return new InheritanceDefinition(
          null, null, null, null, null, TypeString.UNDEFINED, TypeString.UNDEFINED);
    }
  }

  private static final class SlotDefinitionCreator implements InstanceCreator<SlotDefinition> {

    @Override
    public SlotDefinition createInstance(final Type type) {
      // Defaults provenance to REASONED, even if not set in the source JSON.
      return new SlotDefinition(
          null, null, null, null, null, TypeString.UNDEFINED, "dummy_slot", TypeString.UNDEFINED);
    }
  }

  private static final class BinaryOperatorDefinitionCreator
      implements InstanceCreator<BinaryOperatorDefinition> {

    @Override
    public BinaryOperatorDefinition createInstance(final Type type) {
      // Defaults provenance to REASONED, even if not set in the source JSON.
      return new BinaryOperatorDefinition(
          null,
          null,
          null,
          null,
          null,
          "dummy_operator",
          TypeString.UNDEFINED,
          TypeString.UNDEFINED,
          TypeString.UNDEFINED);
    }
  }

  private static final class ProcedureDefinitionCreator
      implements InstanceCreator<ProcedureDefinition> {

    @Override
    public ProcedureDefinition createInstance(final Type type) {
      return new ProcedureDefinition(
          null,
          null,
          null,
          null,
          null,
          Collections.emptySet(),
          TypeString.UNDEFINED,
          null,
          Collections.emptyList(),
          null,
          ExpressionResultString.UNDEFINED,
          ExpressionResultString.UNDEFINED);
    }
  }

  private static final class GlobalDefinitionCreator implements InstanceCreator<GlobalDefinition> {

    @Override
    public GlobalDefinition createInstance(final Type type) {
      return new GlobalDefinition(
          null,
          null,
          null,
          null,
          null,
          TypeString.UNDEFINED,
          TypeString.UNDEFINED,
          null,
          Collections.emptyList());
    }
  }

  private static final class PackageDefinitionCreator
      implements InstanceCreator<PackageDefinition> {

    @Override
    public PackageDefinition createInstance(final Type type) {
      return new PackageDefinition(null, null, null, null, null, "dummy_package", List.of());
    }
  }

  private static final class ConditionDefinitionCreator
      implements InstanceCreator<ConditionDefinition> {

    @Override
    public ConditionDefinition createInstance(final Type type) {
      return new ConditionDefinition(
          null, null, null, null, null, "dummy_condition", null, List.of(), null);
    }
  }

  private static final Logger LOGGER = LoggerFactory.getLogger(JsonDefinitionReader.class);

  static final int SCHEMA_VERSION = 1;

  private final IDefinitionKeeper definitionKeeper;
  private Provenance defaultProvenance = Provenance.DUMPED;

  private JsonDefinitionReader(final IDefinitionKeeper definitionKeeper) {
    this.definitionKeeper = definitionKeeper;
  }

  private Provenance provenanceOf(final String stored) {
    return stored != null ? Provenance.valueOf(stored) : this.defaultProvenance;
  }

  private String provenanceRawOf(final JsonObject instruction) {
    return instruction.has("provenance") ? instruction.get("provenance").getAsString() : null;
  }

  private void run(final Path path) throws IOException {
    LOGGER.debug("Reading type database from path: {}", path);

    final File file = path.toFile();
    int lineNo = 1;
    try (FileReader fileReader = new FileReader(file, StandardCharsets.UTF_8);
        BufferedReader bufferedReader = new BufferedReader(fileReader)) {
      String line = bufferedReader.readLine();
      while (line != null) {
        this.processLineSafe(lineNo, line);

        ++lineNo;
        line = bufferedReader.readLine();
      }
    } catch (final JsonParseException exception) {
      LOGGER.error("JSON Error reading line no: {}", lineNo);
      throw new IllegalStateException(exception);
    }
  }

  @SuppressWarnings("checkstyle:IllegalCatch")
  private void processLineSafe(final int lineNo, final String line)
      throws IncompatibleCacheException {
    try {
      this.processLine(line);
    } catch (final IncompatibleCacheException exception) {
      // A rejected cache must abort the whole read; do not swallow it as a per-line parse error.
      throw exception;
    } catch (final RuntimeException exception) {
      LOGGER.error("Error parsing line {}, line data: {}", lineNo, line);
      LOGGER.error(exception.getMessage(), exception);
    }
  }

  private void processLine(final String line) throws IncompatibleCacheException {
    if (line.trim().startsWith("//")) {
      // Ignore comments.
      return;
    }

    final JsonObject obj = JsonParser.parseString(line).getAsJsonObject();
    final String instructionStr = obj.get(Instruction.INSTRUCTION.getValue()).getAsString();
    final Instruction instruction = Instruction.fromValue(instructionStr);
    switch (instruction) {
      case SCHEMA_VERSION:
        this.verifySchemaVersion(obj);
        break;

      case PRODUCT:
        this.handleProduct(obj);
        break;

      case MODULE:
        this.handleModule(obj);
        break;

      case MAGIK_FILE:
        this.handleMagikFile(obj);
        break;

      case PACKAGE:
        this.handlePackage(obj);
        break;

      case TYPE:
        this.handleType(obj);
        break;

      case INHERITANCE:
        this.handleInheritance(obj);
        break;

      case SLOT:
        this.handleSlot(obj);
        break;

      case METHOD:
        this.handleMethod(obj);
        break;

      case PROCEDURE:
        this.handleProcedure(obj);
        break;

      case CONDITION:
        this.handleCondition(obj);
        break;

      case BINARY_OPERATOR:
        this.handleBinaryOperator(obj);
        break;

      case GLOBAL:
        this.handleGlobal(obj);
        break;

      default:
        break;
    }
  }

  private void verifySchemaVersion(final JsonObject instruction) throws IncompatibleCacheException {
    final int found = instruction.get("value").getAsInt();
    if (found != JsonDefinitionReader.SCHEMA_VERSION) {
      throw new IncompatibleCacheException(
          "types cache has schema version "
              + found
              + ", expected "
              + JsonDefinitionReader.SCHEMA_VERSION);
    }
  }

  private Gson buildGson() {
    return new GsonBuilder()
        .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
        .registerTypeAdapter(TypeString.class, new TypeStringDeserializer())
        .registerTypeAdapter(ExpressionResultString.class, new ExpressionResultStringDeserializer())
        .registerTypeAdapter(Instant.class, new InstantDeserializer())
        .registerTypeAdapter(
            ExemplarDefinition.Sort.class, new LowerCaseEnumDeserializer<ExemplarDefinition.Sort>())
        .registerTypeAdapter(
            MethodDefinition.Modifier.class,
            new LowerCaseEnumDeserializer<MethodDefinition.Modifier>())
        .registerTypeAdapter(
            ProcedureDefinition.Modifier.class,
            new LowerCaseEnumDeserializer<ProcedureDefinition.Modifier>())
        .registerTypeAdapter(
            ParameterDefinition.Modifier.class,
            new LowerCaseEnumDeserializer<ParameterDefinition.Modifier>())
        .registerTypeAdapter(ProductDefinition.class, new ProductDefinitionCreator())
        .registerTypeAdapter(ModuleDefinition.class, new ModuleDefinitionCreator())
        .registerTypeAdapter(MethodDefinition.class, new MethodDefinitionCreator())
        .registerTypeAdapter(ProcedureDefinition.class, new ProcedureDefinitionCreator())
        .registerTypeAdapter(ExemplarDefinition.class, new ExemplarDefinitionCreator())
        .registerTypeAdapter(GlobalDefinition.class, new GlobalDefinitionCreator())
        .registerTypeAdapter(PackageDefinition.class, new PackageDefinitionCreator())
        .registerTypeAdapter(ConditionDefinition.class, new ConditionDefinitionCreator())
        .registerTypeAdapter(MagikFileDefinition.class, new MagikFileDefinitionCreator())
        .registerTypeAdapter(InheritanceDefinition.class, new InheritanceDefinitionCreator())
        .registerTypeAdapter(SlotDefinition.class, new SlotDefinitionCreator())
        .registerTypeAdapter(BinaryOperatorDefinition.class, new BinaryOperatorDefinitionCreator())
        .create();
  }

  private void handleProduct(final JsonObject instruction) {
    final Gson gson = this.buildGson();
    final ProductDefinition definition = gson.fromJson(instruction, ProductDefinition.class);
    this.definitionKeeper.add(definition);
  }

  private void handleModule(final JsonObject instruction) {
    final Gson gson = this.buildGson();
    final ModuleDefinition definition = gson.fromJson(instruction, ModuleDefinition.class);
    this.definitionKeeper.add(definition);
  }

  private void handleMagikFile(final JsonObject instruction) {
    final Gson gson = this.buildGson();
    final MagikFileDefinition definition = gson.fromJson(instruction, MagikFileDefinition.class);
    final String provenanceRaw = this.provenanceRawOf(instruction);
    final Provenance provenance = this.provenanceOf(provenanceRaw);
    final MagikFileDefinition provenancedDefinition = definition.withProvenance(provenance);
    this.definitionKeeper.add(provenancedDefinition);
  }

  private void handlePackage(final JsonObject instruction) {
    final Gson gson = this.buildGson();
    final PackageDefinition definition = gson.fromJson(instruction, PackageDefinition.class);
    final String provenanceRaw = this.provenanceRawOf(instruction);
    final Provenance provenance = this.provenanceOf(provenanceRaw);
    final PackageDefinition provenancedDefinition = definition.withProvenance(provenance);
    this.definitionKeeper.add(provenancedDefinition);
  }

  private void handleType(final JsonObject instruction) {
    final Gson gson = this.buildGson();
    final ExemplarDefinition definition = gson.fromJson(instruction, ExemplarDefinition.class);

    // We are allowed to overwrite definitions which have no location, as these will most likely
    // be the default definitions from DefaultDefinitionsAdder.
    final TypeString typeString = definition.getTypeString();
    this.definitionKeeper.getExemplarDefinitions(typeString).stream()
        .filter(def -> def.getLocation() == null)
        .forEach(this.definitionKeeper::remove);
    final String provenanceRaw = this.provenanceRawOf(instruction);
    final Provenance provenance = this.provenanceOf(provenanceRaw);
    final ExemplarDefinition provenancedDefinition = definition.withProvenance(provenance);
    this.definitionKeeper.add(provenancedDefinition);
  }

  private void handleInheritance(final JsonObject instruction) {
    final Gson gson = this.buildGson();
    final InheritanceDefinition definition =
        gson.fromJson(instruction, InheritanceDefinition.class);
    final String provenanceRaw = this.provenanceRawOf(instruction);
    final Provenance provenance = this.provenanceOf(provenanceRaw);
    final InheritanceDefinition provenancedDefinition = definition.withProvenance(provenance);
    this.definitionKeeper.add(provenancedDefinition);
  }

  private void handleSlot(final JsonObject instruction) {
    final Gson gson = this.buildGson();
    final SlotDefinition definition = gson.fromJson(instruction, SlotDefinition.class);
    final String provenanceRaw = this.provenanceRawOf(instruction);
    final Provenance provenance = this.provenanceOf(provenanceRaw);
    final SlotDefinition provenancedDefinition = definition.withProvenance(provenance);
    this.definitionKeeper.add(provenancedDefinition);
  }

  private void handleMethod(final JsonObject instruction) {
    final Gson gson = this.buildGson();
    final MethodDefinition definition = gson.fromJson(instruction, MethodDefinition.class);
    final String provenanceRaw = this.provenanceRawOf(instruction);
    final Provenance provenance = this.provenanceOf(provenanceRaw);
    final MethodDefinition provenancedDefinition = definition.withProvenance(provenance);
    this.definitionKeeper.add(provenancedDefinition);
  }

  private void handleCondition(final JsonObject instruction) {
    final Gson gson = this.buildGson();
    final ConditionDefinition definition = gson.fromJson(instruction, ConditionDefinition.class);
    final String provenanceRaw = this.provenanceRawOf(instruction);
    final Provenance provenance = this.provenanceOf(provenanceRaw);
    final ConditionDefinition provenancedDefinition = definition.withProvenance(provenance);
    this.definitionKeeper.add(provenancedDefinition);
  }

  private void handleBinaryOperator(final JsonObject instruction) {
    final Gson gson = this.buildGson();
    final BinaryOperatorDefinition definition =
        gson.fromJson(instruction, BinaryOperatorDefinition.class);
    final String provenanceRaw = this.provenanceRawOf(instruction);
    final Provenance provenance = this.provenanceOf(provenanceRaw);
    final BinaryOperatorDefinition provenancedDefinition = definition.withProvenance(provenance);
    this.definitionKeeper.add(provenancedDefinition);
  }

  private void handleProcedure(final JsonObject instruction) {
    final Gson gson = this.buildGson();
    final ProcedureDefinition definition = gson.fromJson(instruction, ProcedureDefinition.class);
    final String provenanceRaw = this.provenanceRawOf(instruction);
    final Provenance provenance = this.provenanceOf(provenanceRaw);
    final ProcedureDefinition provenancedDefinition = definition.withProvenance(provenance);
    this.definitionKeeper.add(provenancedDefinition);
  }

  private void handleGlobal(final JsonObject instruction) {
    final Gson gson = this.buildGson();
    final GlobalDefinition definition = gson.fromJson(instruction, GlobalDefinition.class);
    final String provenanceRaw = this.provenanceRawOf(instruction);
    final Provenance provenance = this.provenanceOf(provenanceRaw);
    final GlobalDefinition provenancedDefinition = definition.withProvenance(provenance);
    this.definitionKeeper.add(provenancedDefinition);
  }

  /**
   * Read types from a JSON-line file, defaulting provenance-less rows to {@link Provenance#DUMPED}.
   *
   * @param path Path to JSON-line file.
   * @param definitionKeeper {@link IDefinitionKeeper} to fill.
   * @throws IOException -
   */
  public static void readTypes(final Path path, final IDefinitionKeeper definitionKeeper)
      throws IOException {
    readTypes(path, definitionKeeper, Provenance.DUMPED);
  }

  /**
   * Read types from a JSON-line file, defaulting the provenance of instructions with no {@code
   * provenance} property to {@code defaultProvenance}.
   *
   * @param path Path to JSON-line file.
   * @param definitionKeeper {@link IDefinitionKeeper} to fill.
   * @param defaultProvenance Provenance to use for instructions with no {@code provenance}
   *     property.
   * @throws IOException -
   */
  public static void readTypes(
      final Path path, final IDefinitionKeeper definitionKeeper, final Provenance defaultProvenance)
      throws IOException {
    final JsonDefinitionReader reader = new JsonDefinitionReader(definitionKeeper);
    reader.defaultProvenance = defaultProvenance;
    reader.run(path);
  }
}
