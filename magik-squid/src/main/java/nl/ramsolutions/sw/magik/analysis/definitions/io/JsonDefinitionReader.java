package nl.ramsolutions.sw.magik.analysis.definitions.io;

import com.google.gson.*;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import nl.ramsolutions.sw.magik.PathMapping;
import nl.ramsolutions.sw.magik.analysis.definitions.*;
import nl.ramsolutions.sw.magik.analysis.definitions.io.deserializer.*;
import nl.ramsolutions.sw.magik.analysis.typing.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import nl.ramsolutions.sw.moduledef.ModuleDefinition;
import nl.ramsolutions.sw.moduledef.ModuleUsage;
import nl.ramsolutions.sw.productdef.ProductDefinition;
import nl.ramsolutions.sw.productdef.ProductUsage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** JSON-line TypeKeeper reader. */
public final class JsonDefinitionReader {

  private static final Logger LOGGER = LoggerFactory.getLogger(JsonDefinitionReader.class);
  private static final Logger LOGGER_DURATION =
      LoggerFactory.getLogger(JsonDefinitionReader.class.getName() + "Duration");

  public static final String TYPE_DB_DEFAULT_ALIAS = "$default";
  private static final String TYPE_DB_DEFAULT_PATH =
      "../../type_dbs"; // relative to smallworldGis path
  public static final Integer TYPE_DB_VERSION = 2;
  public static final String TYPE_DB_EXT = ".v" + TYPE_DB_VERSION + ".jsonl";

  private final IDefinitionKeeper definitionKeeper;
  private final List<PathMapping> mappings;
  private final Gson gson;
  private final ExecutorService threadPool;

  private JsonDefinitionReader(
      final IDefinitionKeeper definitionKeeper, final @Nullable List<PathMapping> mappings) {
    this.definitionKeeper = definitionKeeper;
    this.mappings = mappings;

    final int processors = Runtime.getRuntime().availableProcessors();
    final int threadsToRun = Math.max(processors / 2, 6);
    this.threadPool = Executors.newFixedThreadPool(threadsToRun);

    this.gson = this.createGson();
  }

  private Gson createGson() {
    final GsonBuilder builder = new GsonBuilder();
    return builder
        .registerTypeAdapter(TypeString.class, new TypeStringDeserializer(mappings))
        .registerTypeAdapter(
            ExpressionResultString.class, new ExpressionResultStringDeserializer(mappings))
        .registerTypeAdapter(
            ExemplarDefinition.Sort.class,
            new LowerCaseEnumDeserializer<>(mappings, ExemplarDefinition.Sort.class))
        .registerTypeAdapter(
            MethodDefinition.Modifier.class,
            new LowerCaseEnumDeserializer<>(mappings, MethodDefinition.Modifier.class))
        .registerTypeAdapter(
            ProcedureDefinition.Modifier.class,
            new LowerCaseEnumDeserializer<>(mappings, ProcedureDefinition.Modifier.class))
        .registerTypeAdapter(
            ParameterDefinition.Modifier.class,
            new LowerCaseEnumDeserializer<>(mappings, ParameterDefinition.Modifier.class))
        .registerTypeAdapter(SlotDefinition.class, new SlotDefinitionDeserializer(mappings))
        .registerTypeAdapter(
            ParameterDefinition.class, new ParameterDefinitionDeserializer(mappings))
        .registerTypeAdapter(ProductDefinition.class, new ProductDefinitionDeserializer(mappings))
        .registerTypeAdapter(ModuleDefinition.class, new ModuleDefinitionDeserializer(mappings))
        .registerTypeAdapter(PackageDefinition.class, new PackageDefinitionDeserializer(mappings))
        .registerTypeAdapter(ExemplarDefinition.class, new ExemplarDefinitionDeserializer(mappings))
        .registerTypeAdapter(MethodDefinition.class, new MethodDefinitionDeserializer(mappings))
        .registerTypeAdapter(
            ConditionDefinition.class, new ConditionDefinitionDeserializer(mappings))
        .registerTypeAdapter(
            BinaryOperatorDefinition.class, new BinaryOperatorDefinitionDeserializer(mappings))
        .registerTypeAdapter(
            ProcedureDefinition.class, new ProcedureDefinitionDeserializer(mappings))
        .registerTypeAdapter(GlobalDefinition.class, new GlobalDefinitionDeserializer(mappings))
        .registerTypeAdapter(ProductUsage.class, new ProductUsageDeserializer(mappings))
        .registerTypeAdapter(ModuleUsage.class, new ModuleUsageDeserializer(mappings))
        .registerTypeAdapter(
            MagikFileDefinition.class, new MagikFileDefinitionDeserializer(mappings))
        .create();
  }

  /**
   * Read types from a JSON-line file.
   *
   * @param path Path to JSON-line file.
   * @param definitionKeeper {@link IDefinitionKeeper} to fill.
   */
  public static void readTypes(final Path path, final IDefinitionKeeper definitionKeeper) {
    readTypes(path, definitionKeeper, Collections.emptyList());
  }

  /**
   * Read types from a JSON-line file.
   *
   * @param path Path to JSON-line file.
   * @param definitionKeeper {@link IDefinitionKeeper} to fill.
   * @param mappings the path mappings to use for locations
   */
  public static void readTypes(
      final Path path,
      final IDefinitionKeeper definitionKeeper,
      final @Nullable List<PathMapping> mappings) {
    BaseDeserializer.clearParsedFiles();

    final JsonDefinitionReader reader = new JsonDefinitionReader(definitionKeeper, mappings);
    final long start = System.nanoTime();
    reader.run(path);
    LOGGER_DURATION.trace(
        "Duration: {} readTypes, type db: {}", (System.nanoTime() - start) / 1000000000.0, path);
  }

  /**
   * parses a path for a type db and will replace {@value
   * JsonDefinitionReader#TYPE_DB_DEFAULT_ALIAS} with the default type db path
   *
   * @param gisPath the gisPath if the default alias gets replaced
   * @param pathStr the path that should get parsed
   * @return the parsed Path or null if the file can't be found
   */
  @Nullable
  public static Path parseTypeDBPath(@Nullable String gisPath, String pathStr) {
    Path path = Path.of(pathStr);
    if (pathStr.equals(TYPE_DB_DEFAULT_ALIAS) && gisPath != null) {
      path = generateDefaultTypeDBPath(gisPath);
    }

    if (!Files.exists(path)) {
      return null;
    }

    return path;
  }

  /**
   * Generate the default type db path
   *
   * @param gisPath the Smallworld GIS path to use as the basis
   * @return the path
   */
  public static Path generateDefaultTypeDBPath(String gisPath) {
    return Paths.get(gisPath, TYPE_DB_DEFAULT_PATH);
  }

  public void run(final Path path) {
    LOGGER.info("Reading type database from path: {}", path);

    List<CompletableFuture<Boolean>> completableFutures = new ArrayList<>();

    final File file = path.toFile();
    int lineNo = 1;
    try (FileReader fileReader = new FileReader(file, StandardCharsets.ISO_8859_1);
        BufferedReader bufferedReader = new BufferedReader(fileReader)) {
      String line = bufferedReader.readLine();
      while (line != null) {
        completableFutures.add(this.processLineSafe(lineNo, line, path));

        ++lineNo;
        line = bufferedReader.readLine();
      }
    } catch (final IOException exception) {
      LOGGER.error("IO Error reading line no: {}", lineNo, exception);
      throw new IllegalStateException(exception);
    }

    CompletableFuture<Void> allFutures =
        CompletableFuture.allOf(completableFutures.toArray(new CompletableFuture[0]));
    try {
      allFutures.get();
      LOGGER.info("Finished reading type database from: {}", path);
    } catch (InterruptedException | ExecutionException e) {
      LOGGER.error("Error while reading type database from: {}", path, e);
    }
  }

  @SuppressWarnings("checkstyle:IllegalCatch")
  private CompletableFuture<Boolean> processLineSafe(
      final int lineNo, final String line, final Path path) {
    CompletableFuture<Boolean> completableFuture = new CompletableFuture<>();

    threadPool.submit(
        () -> {
          try {
            if (lineNo % 10000 == 0 && LOGGER.isDebugEnabled()) {
              LOGGER.debug("On line {} of {}", lineNo, path);
            }
            this.processLine(line);
            completableFuture.complete(true);
          } catch (final Exception exception) {
            LOGGER.error("Error parsing line {}, line data: {}", lineNo, line);
            LOGGER.error(exception.getMessage(), exception);
            completableFuture.complete(false);
          }
        });

    return completableFuture;
  }

  private void processLine(String line) {
    if (line.trim().startsWith("//")) {
      // Ignore comments.
      return;
    }

    final JsonElement jsonTree = JsonParser.parseString(line);
    final JsonObject obj = jsonTree.getAsJsonObject();
    final JsonElement instructionObj = obj.get(Instruction.FIELD_NAME);
    final Instruction instruction = Instruction.fromValue(instructionObj.getAsInt());

    switch (instruction) {
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
        throw new IllegalStateException(
            "Unexpected instruction: " + instruction + "\nline: " + line);
    }
  }

  private void handleProduct(final JsonObject obj) {
    ProductDefinition definition = gson.fromJson(obj, ProductDefinition.class);
    this.definitionKeeper.add(definition);
  }

  private void handleModule(final JsonObject obj) {
    ModuleDefinition definition = gson.fromJson(obj, ModuleDefinition.class);
    this.definitionKeeper.add(definition);
  }

  private void handleMagikFile(final JsonObject obj) {
    final MagikFileDefinition definition = gson.fromJson(obj, MagikFileDefinition.class);
    this.definitionKeeper.add(definition);
  }

  private void handlePackage(final JsonObject obj) {
    PackageDefinition definition = gson.fromJson(obj, PackageDefinition.class);
    this.definitionKeeper.add(definition);
  }

  private void handleType(final JsonObject obj) {
    ExemplarDefinition definition = gson.fromJson(obj, ExemplarDefinition.class);

    // We are allowed to overwrite definitions which have no location, as these will most likely
    // be the default definitions from DefaultDefinitionsAdder.
    final TypeString typeString = definition.getTypeString();
    this.definitionKeeper.getExemplarDefinitions(typeString).stream()
        .filter(def -> def.getLocation() == null)
        .forEach(this.definitionKeeper::remove);

    this.definitionKeeper.add(definition);
  }

  private void handleMethod(final JsonObject obj) {
    MethodDefinition definition = gson.fromJson(obj, MethodDefinition.class);
    this.definitionKeeper.add(definition);
  }

  private void handleCondition(final JsonObject obj) {
    ConditionDefinition definition = gson.fromJson(obj, ConditionDefinition.class);
    this.definitionKeeper.add(definition);
  }

  private void handleBinaryOperator(final JsonObject obj) {
    BinaryOperatorDefinition definition = gson.fromJson(obj, BinaryOperatorDefinition.class);
    this.definitionKeeper.add(definition);
  }

  private void handleProcedure(final JsonObject obj) {
    ProcedureDefinition definition = gson.fromJson(obj, ProcedureDefinition.class);
    this.definitionKeeper.add(definition);
  }

  private void handleGlobal(final JsonObject obj) {
    GlobalDefinition definition = gson.fromJson(obj, GlobalDefinition.class);
    this.definitionKeeper.add(definition);
  }
}
