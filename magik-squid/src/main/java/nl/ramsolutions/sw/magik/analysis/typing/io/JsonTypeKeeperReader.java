package nl.ramsolutions.sw.magik.analysis.typing.io;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import nl.ramsolutions.sw.magik.analysis.definitions.BinaryOperatorDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.ConditionDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.ExemplarDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.GlobalDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.PackageDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.ParameterDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.ProcedureDefinition;
import nl.ramsolutions.sw.magik.analysis.typing.ITypeKeeper;
import nl.ramsolutions.sw.magik.analysis.typing.TypeKeeper;
import nl.ramsolutions.sw.magik.analysis.typing.TypeKeeperDefinitionInserter;
import nl.ramsolutions.sw.magik.analysis.typing.types.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.types.TypeString;
import nl.ramsolutions.sw.magik.parser.TypeStringParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JSON-line TypeKeeper reader.
 */
public final class JsonTypeKeeperReader {

    private static class TypeStringDeserializer implements JsonDeserializer<TypeString> {

        @Override
        public TypeString deserialize(
                final JsonElement json,
                final Type typeOfT,
                final JsonDeserializationContext context)
                throws JsonParseException {
            final String identifier = json.getAsString();
            return TypeStringParser.parseTypeString(identifier);
        }

    }

    private static class ExpressionResultStringDeserializer implements JsonDeserializer<ExpressionResultString> {

        @Override
        public ExpressionResultString deserialize(
                final JsonElement json,
                final Type typeOfT,
                final JsonDeserializationContext context)
                throws JsonParseException {
            if (json.isJsonPrimitive() && json.getAsString().equals(ExpressionResultString.UNDEFINED_SERIALIZED_NAME)) {
                return ExpressionResultString.UNDEFINED;
            } else if (json.isJsonArray()) {
                final List<TypeString> types = json.getAsJsonArray().asList().stream()
                    .map(jsonElement -> jsonElement.getAsString())
                    .map(identifier -> TypeStringParser.parseTypeString(identifier))
                    .collect(Collectors.toList());
                return new ExpressionResultString(types);
            }

            throw new IllegalStateException();
        }

    }

    private static class LowerCaseEnumDeserializer<E extends Enum<?>> implements JsonDeserializer<E> {

        @SuppressWarnings("unchecked")
        @Override
        public E deserialize(
                final JsonElement json,
                final Type typeOfT,
                final JsonDeserializationContext context)
                throws JsonParseException {
            final String value = json.getAsString().toUpperCase();
            if (typeOfT instanceof Class) {
                final Class<?> clazz = (Class<?>) typeOfT;
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

    private static final Logger LOGGER = LoggerFactory.getLogger(JsonTypeKeeperReader.class);

    private final TypeKeeperDefinitionInserter typeKeeperInserter;

    private JsonTypeKeeperReader(final ITypeKeeper typeKeeper) {
        this.typeKeeperInserter = new TypeKeeperDefinitionInserter(typeKeeper);
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    private void run(final Path path) throws IOException {
        LOGGER.debug("Reading type database from path: {}", path);

        final File file = path.toFile();
        int lineNo = 1;
        try (FileReader fileReader = new FileReader(file, StandardCharsets.ISO_8859_1);
             BufferedReader bufferedReader = new BufferedReader(fileReader)) {
            String line = bufferedReader.readLine();
            while (line != null) {
                this.processLineSafe(lineNo, line);

                ++lineNo;
                line = bufferedReader.readLine();
            }
        } catch (final JsonParseException exception) {
            // This will never be hit, the catch block above handles it.
            LOGGER.error("JSON Error reading line no: {}", lineNo);
            throw new IllegalStateException(exception);
        }
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    private void processLineSafe(int lineNo, String line) {
        try {
            this.processLine(line);
        } catch (final RuntimeException exception) {
            LOGGER.error("Error parsing line {}, line data: {}", lineNo, line);
            LOGGER.error(exception.getMessage(), exception);
        }
    }

    private void processLine(final String line) {
        if (line.trim().startsWith("//")) {
            // Ignore comments.
            return;
        }

        final JsonObject obj = JsonParser.parseString(line).getAsJsonObject();
        final String instructionStr = obj.get(Instruction.INSTRUCTION.getValue()).getAsString();
        final Instruction instruction = Instruction.fromValue(instructionStr);
        switch (instruction) {
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
                break;
        }
    }

    private Gson buildGson() {
        return new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .registerTypeAdapter(TypeString.class, new TypeStringDeserializer())
            .registerTypeAdapter(
                ExemplarDefinition.Sort.class, new LowerCaseEnumDeserializer<ExemplarDefinition.Sort>())
            .registerTypeAdapter(
                MethodDefinition.Modifier.class, new LowerCaseEnumDeserializer<MethodDefinition.Modifier>())
            .registerTypeAdapter(
                ProcedureDefinition.Modifier.class, new LowerCaseEnumDeserializer<ProcedureDefinition.Modifier>())
            .registerTypeAdapter(
                ParameterDefinition.Modifier.class, new LowerCaseEnumDeserializer<ParameterDefinition.Modifier>())
            .registerTypeAdapter(ExpressionResultString.class, new ExpressionResultStringDeserializer())
            .create();
    }

    private void handlePackage(final JsonObject instruction) {
        final Gson gson = this.buildGson();
        final PackageDefinition definition = gson.fromJson(instruction, PackageDefinition.class);
        this.typeKeeperInserter.feed(definition);
    }

    private void handleType(final JsonObject instruction) {
        final Gson gson = this.buildGson();
        final ExemplarDefinition definition = gson.fromJson(instruction, ExemplarDefinition.class);
        this.typeKeeperInserter.feed(definition);
    }

    private void handleMethod(final JsonObject instruction) {
        final Gson gson = this.buildGson();
        final MethodDefinition definition = gson.fromJson(instruction, MethodDefinition.class);
        this.typeKeeperInserter.feed(definition);
    }

    private void handleCondition(final JsonObject instruction) {
        final Gson gson = this.buildGson();
        final ConditionDefinition definition = gson.fromJson(instruction, ConditionDefinition.class);
        this.typeKeeperInserter.feed(definition);
    }

    private void handleBinaryOperator(final JsonObject instruction) {
        final Gson gson = this.buildGson();
        final BinaryOperatorDefinition definition = gson.fromJson(instruction, BinaryOperatorDefinition.class);
        this.typeKeeperInserter.feed(definition);
    }

    private void handleProcedure(final JsonObject instruction) {
        final Gson gson = this.buildGson();
        final ProcedureDefinition definition = gson.fromJson(instruction, ProcedureDefinition.class);
        this.typeKeeperInserter.feed(definition);
    }

    private void handleGlobal(final JsonObject instruction) {
        final Gson gson = this.buildGson();
        final GlobalDefinition definition = gson.fromJson(instruction, GlobalDefinition.class);
        this.typeKeeperInserter.feed(definition);
    }

    /**
     * Read types from a JSON-line file.
     * @param path Path to JSON-line file.
     * @param typeKeeper {@link TypeKeeper} to fill.
     * @throws IOException -
     */
    public static void read(final Path path, final ITypeKeeper typeKeeper) throws IOException {
        final JsonTypeKeeperReader reader = new JsonTypeKeeperReader(typeKeeper);
        reader.run(path);
    }

}
