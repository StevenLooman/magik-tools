package nl.ramsolutions.sw.magik.analysis.definitions.io;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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
import java.nio.file.Path;
import java.util.Comparator;
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
import nl.ramsolutions.sw.magik.analysis.typing.TypeKeeperDefinitionExtractor;
import nl.ramsolutions.sw.magik.analysis.typing.types.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.types.TypeString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JSON-line TypeKeeper writer.
 */
public final class JsonDefinitionWriter {

    private static class TypeStringSerializer implements JsonSerializer<TypeString> {

        @Override
        public JsonElement serialize(
                final TypeString src,
                final Type typeOfSrc,
                final JsonSerializationContext context) {
            final String fullString = src.getFullString();
            return new JsonPrimitive(fullString);
        }

    }

    private static class ExpressionResultStringSerializer implements JsonSerializer<ExpressionResultString> {

        @Override
        public JsonElement serialize(
                final ExpressionResultString src,
                final Type typeOfSrc,
                final JsonSerializationContext context) {
            throw new UnsupportedOperationException("Unimplemented method 'serialize'");
        }

    }

    private static class LowerCaseEnumSerializer<E extends Enum<?>> implements JsonSerializer<E> {

        @Override
        public JsonElement serialize(
                final E src,
                final Type typeOfSrc,
                final JsonSerializationContext context) {
            final String val = src.toString().toLowerCase();
            return new JsonPrimitive(val);
        }

    }

    private static final Logger LOGGER = LoggerFactory.getLogger(JsonDefinitionWriter.class);

    private final TypeKeeperDefinitionExtractor typeKeeperExtractor;

    private JsonDefinitionWriter(final ITypeKeeper typeKeeper) {
        this.typeKeeperExtractor = new TypeKeeperDefinitionExtractor(typeKeeper);
    }

    private void run(final Path path) throws IOException {
        LOGGER.debug("Writing type database to path: {}", path);

        final File file = path.toFile();
        try (FileWriter fileReader = new FileWriter(file, StandardCharsets.ISO_8859_1);
             BufferedWriter bufferedWriter = new BufferedWriter(fileReader)) {
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
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .registerTypeAdapter(TypeString.class, new TypeStringSerializer())
            .registerTypeAdapter(
                ExemplarDefinition.Sort.class, new LowerCaseEnumSerializer<ExemplarDefinition.Sort>())
            .registerTypeAdapter(
                MethodDefinition.Modifier.class, new LowerCaseEnumSerializer<MethodDefinition.Modifier>())
            .registerTypeAdapter(
                ProcedureDefinition.Modifier.class, new LowerCaseEnumSerializer<ProcedureDefinition.Modifier>())
            .registerTypeAdapter(
                ParameterDefinition.Modifier.class, new LowerCaseEnumSerializer<ParameterDefinition.Modifier>())
            .registerTypeAdapter(ExpressionResultString.class, new ExpressionResultStringSerializer())
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

    private void writePackages(final Writer writer) {
        final Comparator<PackageDefinition> sorter = Comparator.comparing(PackageDefinition::getName);
        this.typeKeeperExtractor.getPackageDefinitions().stream()
            .sorted(sorter)
            .forEach(definition -> {
                final Gson gson = this.buildGson();
                final JsonObject instruction = (JsonObject) gson.toJsonTree(definition);
                instruction.addProperty(Instruction.INSTRUCTION.getValue(), Instruction.PACKAGE.getValue());
                this.writeInstruction(writer, instruction);
            });
    }

    private void writeExemplars(final Writer writer) {
        final Comparator<ExemplarDefinition> sorter = Comparator.comparing(ExemplarDefinition::getTypeString);
        this.typeKeeperExtractor.getExemplarDefinitions().stream()
            .sorted(sorter)
            .forEach(definition -> {
                final Gson gson = this.buildGson();
                final JsonObject instruction = (JsonObject) gson.toJsonTree(definition);
                instruction.addProperty(Instruction.INSTRUCTION.getValue(), Instruction.TYPE.getValue());
                this.writeInstruction(writer, instruction);
            });
    }

    private void writeMethods(final Writer writer) {
        final Comparator<MethodDefinition> typeNameComparer = Comparator.comparing(MethodDefinition::getTypeName);
        final Comparator<MethodDefinition> nameComparer = Comparator.comparing(MethodDefinition::getName);
        final Comparator<MethodDefinition> sorter = typeNameComparer.thenComparing(nameComparer);
        this.typeKeeperExtractor.getMethodDefinitions().stream()
            .sorted(sorter)
            .forEach(definition -> {
                final Gson gson = this.buildGson();
                final JsonObject instruction = (JsonObject) gson.toJsonTree(definition);
                instruction.addProperty(Instruction.INSTRUCTION.getValue(), Instruction.METHOD.getValue());
                this.writeInstruction(writer, instruction);
            });
    }

    private void writeProcedures(final Writer writer) {
        final Comparator<ProcedureDefinition> sorter = Comparator.comparing(ProcedureDefinition::getTypeName);
        this.typeKeeperExtractor.getProcedureDefinitions().stream()
            .sorted(sorter)
            .forEach(definition -> {
                final Gson gson = this.buildGson();
                final JsonObject instruction = (JsonObject) gson.toJsonTree(definition);
                instruction.addProperty(Instruction.INSTRUCTION.getValue(), Instruction.METHOD.getValue());
                this.writeInstruction(writer, instruction);
            });
    }

    private void writeConditions(final BufferedWriter writer) {
        final Comparator<ConditionDefinition> sorter = Comparator.comparing(ConditionDefinition::getName);
        this.typeKeeperExtractor.getConditionDefinitions().stream()
            .sorted(sorter)
            .forEach(definition -> {
                final Gson gson = this.buildGson();
                final JsonObject instruction = (JsonObject) gson.toJsonTree(definition);
                instruction.addProperty(Instruction.INSTRUCTION.getValue(), Instruction.CONDITION.getValue());
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
        final Comparator<BinaryOperatorDefinition> sorter = lhsComparer
            .thenComparing(rhsComparer)
            .thenComparing(resultComparer);
        this.typeKeeperExtractor.getBinaryOperatorDefinitions().stream()
            .sorted(sorter)
            .forEach(definition -> {
                final Gson gson = this.buildGson();
                final JsonObject instruction = (JsonObject) gson.toJsonTree(definition);
                instruction.addProperty(
                    Instruction.BINARY_OPERATOR.getValue(), Instruction.BINARY_OPERATOR.getValue());
                this.writeInstruction(writer, instruction);
            });
    }

    private void writeGlobals(final Writer writer) {
        final Comparator<GlobalDefinition> sorter = Comparator.comparing(GlobalDefinition::getTypeString);
        this.typeKeeperExtractor.getGlobalDefinitions().stream()
            .sorted(sorter)
            .forEach(definition -> {
                final Gson gson = this.buildGson();
                final JsonObject instruction = (JsonObject) gson.toJsonTree(definition);
                instruction.addProperty(Instruction.INSTRUCTION.getValue(), Instruction.GLOBAL.getValue());
                this.writeInstruction(writer, instruction);
            });
    }

    /**
     * Write types to a JSON-line file.
     * @param path Path to JSON-line file.
     * @param typeKeeper {@link TypeKeeper} to dump.
     * @throws IOException -
     */
    public static void write(final Path path, final ITypeKeeper typeKeeper) throws IOException {
        final JsonDefinitionWriter reader = new JsonDefinitionWriter(typeKeeper);
        reader.run(path);
    }

}
