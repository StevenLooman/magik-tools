package nl.ramsolutions.sw.magik.analysis.typing;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.InvalidParameterException;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import nl.ramsolutions.sw.magik.analysis.Location;
import nl.ramsolutions.sw.magik.analysis.typing.types.AbstractType;
import nl.ramsolutions.sw.magik.analysis.typing.types.AliasType;
import nl.ramsolutions.sw.magik.analysis.typing.types.ExpressionResult;
import nl.ramsolutions.sw.magik.analysis.typing.types.GlobalReference;
import nl.ramsolutions.sw.magik.analysis.typing.types.IndexedType;
import nl.ramsolutions.sw.magik.analysis.typing.types.IntrinsicType;
import nl.ramsolutions.sw.magik.analysis.typing.types.MagikType;
import nl.ramsolutions.sw.magik.analysis.typing.types.Method;
import nl.ramsolutions.sw.magik.analysis.typing.types.Package;
import nl.ramsolutions.sw.magik.analysis.typing.types.Parameter;
import nl.ramsolutions.sw.magik.analysis.typing.types.ProcedureInstance;
import nl.ramsolutions.sw.magik.analysis.typing.types.Slot;
import nl.ramsolutions.sw.magik.analysis.typing.types.SlottedType;
import nl.ramsolutions.sw.magik.analysis.typing.types.UndefinedType;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JSON-line TypeKeeper reader.
 */
public final class JsonTypeKeeperReader {

    private static final Logger LOGGER = LoggerFactory.getLogger(JsonTypeKeeperReader.class);

    private static final String SW_PAKKAGE = "sw";

    private final Path path;
    private final ITypeKeeper typeKeeper;
    private final TypeParser typeParser;
    private final Map<MagikType, JSONArray> typeParents = new HashMap<>();
    private final Map<Slot, String> slotTypeNames = new HashMap<>();

    private JsonTypeKeeperReader(final Path path, final ITypeKeeper typeKeeper) {
        this.path = path;
        this.typeKeeper = typeKeeper;
        this.typeParser = new TypeParser(this.typeKeeper);
    }

    private void run() throws IOException {
        LOGGER.debug("Reading type database from path: {}", this.path);

        final File file = this.path.toFile();
        try (FileReader fileReader = new FileReader(file, StandardCharsets.ISO_8859_1);
             BufferedReader bufferedReader = new BufferedReader(fileReader)) {
            String line = bufferedReader.readLine();
            while (line != null) {
                this.processLine(line);

                line = bufferedReader.readLine();
            }
        }

        this.postProcess();
    }

    private void processLine(final String line) {
        if (line.startsWith("#")) {
            // Ignore comments.
            return;
        }

        final JSONTokener tokener = new JSONTokener(line);
        final JSONObject obj = new JSONObject(tokener);
        final String instruction = obj.getString("instruction");
        switch (instruction) {
            case "package":
                this.handlePackage(obj);
                break;

            case "type":
                this.handleType(obj);
                break;

            case "global":
                this.handleGlobal(obj);
                break;

            case "method":
                this.handleMethod(obj);
                break;

            case "procedure":
                this.handleProcedure(obj);
                break;

            default:
                break;
        }
    }

    private void handlePackage(final JSONObject instruction) {
        final String name = instruction.getString("name");
        if (!this.typeKeeper.hasPackage(name)) {
            final Package pakkage = new Package(name);
            this.typeKeeper.addPackage(pakkage);
        }

        final Package pakkage = this.typeKeeper.getPackage(name);
        instruction.getJSONArray("uses").forEach(useObj -> {
            final String use = (String) useObj;
            final Package usePackage = this.typeKeeper.getPackage(use);
            pakkage.addUse(usePackage);
        });
    }

    private void handleType(final JSONObject instruction) {
        final String typeFormat = instruction.getString("type_format");
        final String name = instruction.getString("type_name");
        final GlobalReference globalRef = GlobalReference.of(name);
        final MagikType type;
        if (this.typeKeeper.getType(globalRef) != UndefinedType.INSTANCE) {
            type = (MagikType) this.typeKeeper.getType(globalRef);
        } else if ("intrinsic".equals(typeFormat)) {
            type = new IntrinsicType(globalRef);
        } else if ("slotted".equals(typeFormat)) {
            type = new SlottedType(globalRef);
        } else if ("indexed".equals(typeFormat)) {
            type = new IndexedType(globalRef);
        } else {
            throw new InvalidParameterException("Unknown type: " + typeFormat);
        }

        if (instruction.has("doc")) {
            final String doc = instruction.getString("doc");
            type.setDoc(doc);
        }

        instruction.getJSONArray("slots").forEach(slotObj -> {
            final JSONObject slot = (JSONObject) slotObj;
            final String slotName = slot.getString("name");
            final Slot typeSlot = type.addSlot(null, slotName);
            final String slotTypeName = slot.getString("type_name");
            this.slotTypeNames.put(typeSlot, slotTypeName);
        });

        // Save inheritance for later...
        final JSONArray parents = instruction.getJSONArray("parents");
        this.typeParents.put(type, parents);

        this.typeKeeper.addType(type);
    }

    private void postProcess() {
        // Parents.
        this.typeParents.entrySet().forEach(entry -> {
            final MagikType type = entry.getKey();
            final JSONArray parentsArray = entry.getValue();
            parentsArray.forEach(parentObj -> {
                final String parentStr = (String) parentObj;
                final MagikType parentType = (MagikType) this.typeParser.parseTypeString(parentStr, SW_PAKKAGE);
                type.addParent(parentType);
            });
        });

        // Slot types.
        this.slotTypeNames.entrySet().forEach(entry -> {
            final Slot slot = entry.getKey();
            final String typeName = entry.getValue();
            final AbstractType type = this.typeParser.parseTypeString(typeName, SW_PAKKAGE);
            slot.setType(type);
        });
    }

    private void handleGlobal(final JSONObject instruction) {
        final String name = instruction.getString("name");
        final GlobalReference globalRef = GlobalReference.of(name);

        final String typeName = instruction.getString("type_name");
        final ExpressionResult parsedResult = this.typeParser.parseExpressionResultString(typeName, SW_PAKKAGE);
        final AbstractType type = parsedResult.get(0, UndefinedType.INSTANCE);

        final AliasType global = new AliasType(globalRef, type);
        this.typeKeeper.addType(global);
    }

    private void handleMethod(final JSONObject instruction) {
        final String typeName = instruction.getString("type_name");
        final ExpressionResult parsedResult = this.typeParser.parseExpressionResultString(typeName, SW_PAKKAGE);
        final AbstractType abstractType = (AbstractType) parsedResult.get(0, UndefinedType.INSTANCE);
        if (abstractType == UndefinedType.INSTANCE) {
            throw new InvalidParameterException("Unknown type: " + typeName);
        }
        final MagikType type = (MagikType) abstractType;

        final JSONArray modifiersArray = (JSONArray) instruction.getJSONArray("modifiers");
        final EnumSet<Method.Modifier> modifiers = StreamSupport.stream(modifiersArray.spliterator(), false)
            .map(String.class::cast)
            .map(String::toUpperCase)
            .map(Method.Modifier::valueOf)
            .collect(Collectors.toCollection(() -> EnumSet.noneOf(Method.Modifier.class)));
        final Location location = instruction.get("source_file") != JSONObject.NULL
            ? new Location(Path.of(instruction.getString("source_file")).toUri())
            : null;
        final String methodName = instruction.getString("method_name");
        final List<Parameter> parameters = this.parseParameters(instruction.getJSONArray("parameters"));
        final Parameter assignmentParameter;
        if (methodName.contains("<<")) {
            int lastIdx = parameters.size() - 1;
            assignmentParameter = parameters.get(lastIdx);
            parameters.remove(lastIdx);
        } else {
            assignmentParameter = null;
        }
        final ExpressionResult result = this.parseExpressionResult(instruction.get("return_types"));
        final ExpressionResult loopResult = this.parseExpressionResult(instruction.get("loop_types"));
        final Method method =
            type.addMethod(modifiers, location, methodName, parameters, assignmentParameter, result, loopResult);

        if (instruction.get("doc") != JSONObject.NULL) {
            final String doc = instruction.getString("doc");
            method.setDoc(doc);
        }
    }

    private List<Parameter> parseParameters(final JSONArray parametersArray) {
        return StreamSupport.stream(parametersArray.spliterator(), false)
            .map(parameterThing -> {
                final JSONObject parameterObj = (JSONObject) parameterThing;
                final String name = parameterObj.getString("name");
                final Parameter.Modifier modifier = parameterObj.get("modifier") != JSONObject.NULL
                    ? Parameter.Modifier.valueOf(parameterObj.getString("modifier").toUpperCase())
                    : Parameter.Modifier.NONE;
                final String typeNameStr = parameterObj.getString("type_name");
                final AbstractType type = this.typeParser.parseTypeString(typeNameStr, SW_PAKKAGE);
                return new Parameter(name, modifier, type);
            })
            .collect(Collectors.toList());
    }

    private ExpressionResult parseExpressionResult(final Object obj) {
        if (obj == JSONObject.NULL) {
            return ExpressionResult.UNDEFINED;
        }

        if (obj instanceof String) {
            final String expressionResultStr = (String) obj;
            return this.typeParser.parseExpressionResultString(expressionResultStr, SW_PAKKAGE);
        }

        if (obj instanceof JSONArray) {
            final JSONArray array = (JSONArray) obj;
            return StreamSupport.stream(array.spliterator(), false)
                .map(String.class::cast)
                .map(typeStr -> this.typeParser.parseTypeString(typeStr, SW_PAKKAGE))
                .collect(ExpressionResult.COLLECTOR);
        }

        throw new InvalidParameterException("Don't know what to do with: " + obj);
    }

    private void handleProcedure(final JSONObject instruction) {
        final JSONArray modifiersArray = (JSONArray) instruction.getJSONArray("modifiers");
        final EnumSet<Method.Modifier> modifiers = StreamSupport.stream(modifiersArray.spliterator(), false)
            .map(String.class::cast)
            .map(String::toUpperCase)
            .map(Method.Modifier::valueOf)
            .collect(Collectors.toCollection(() -> EnumSet.noneOf(Method.Modifier.class)));
        final Location location = instruction.get("source_file") != JSONObject.NULL
            ? new Location(Path.of(instruction.getString("source_file")).toUri())
            : null;
        final List<Parameter> parameters = this.parseParameters(instruction.getJSONArray("parameters"));
        final ExpressionResult result = this.parseExpressionResult(instruction.get("return_types"));
        final ExpressionResult loopResult = this.parseExpressionResult(instruction.get("loop_types"));

        final String name = instruction.getString("name");
        final GlobalReference globalRef = GlobalReference.of(name);
        final String procedureName = instruction.getString("procedure_name");
        final ProcedureInstance instance = new ProcedureInstance(globalRef, procedureName);
        final Method method = instance.addMethod(
            modifiers, location, "invoke()", parameters, null, result, loopResult);
        this.typeKeeper.addType(instance);

        if (instruction.get("doc") != JSONObject.NULL) {
            final String doc = instruction.getString("doc");
            method.setDoc(doc);
        }
    }

    /**
     * Read types from a JSON-line file.
     * @param path Path to JSON-line file.
     * @param typeKeeper {{TypeKeeper}} to fill.
     * @throws IOException -
     */
    public static void readTypes(final Path path, final ITypeKeeper typeKeeper) throws IOException {
        final JsonTypeKeeperReader reader = new JsonTypeKeeperReader(path, typeKeeper);
        reader.run();
    }

}
