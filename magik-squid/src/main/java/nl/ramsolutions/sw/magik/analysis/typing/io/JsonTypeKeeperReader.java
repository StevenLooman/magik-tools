package nl.ramsolutions.sw.magik.analysis.typing.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.InvalidParameterException;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import nl.ramsolutions.sw.magik.analysis.Location;
import nl.ramsolutions.sw.magik.analysis.typing.BinaryOperator;
import nl.ramsolutions.sw.magik.analysis.typing.ITypeKeeper;
import nl.ramsolutions.sw.magik.analysis.typing.TypeKeeper;
import nl.ramsolutions.sw.magik.analysis.typing.TypeReader;
import nl.ramsolutions.sw.magik.analysis.typing.types.AbstractType;
import nl.ramsolutions.sw.magik.analysis.typing.types.AliasType;
import nl.ramsolutions.sw.magik.analysis.typing.types.Condition;
import nl.ramsolutions.sw.magik.analysis.typing.types.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.types.GenericDeclaration;
import nl.ramsolutions.sw.magik.analysis.typing.types.MagikType;
import nl.ramsolutions.sw.magik.analysis.typing.types.MagikType.Sort;
import nl.ramsolutions.sw.magik.analysis.typing.types.Method;
import nl.ramsolutions.sw.magik.analysis.typing.types.Package;
import nl.ramsolutions.sw.magik.analysis.typing.types.Parameter;
import nl.ramsolutions.sw.magik.analysis.typing.types.ProcedureInstance;
import nl.ramsolutions.sw.magik.analysis.typing.types.TypeString;
import nl.ramsolutions.sw.magik.analysis.typing.types.UndefinedType;
import nl.ramsolutions.sw.magik.parser.TypeStringParser;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JSON-line TypeKeeper reader.
 */
public final class JsonTypeKeeperReader {

    private static final Logger LOGGER = LoggerFactory.getLogger(JsonTypeKeeperReader.class);
    private static final TypeString SW_PROCEDURE_REF = TypeString.ofIdentifier("procedure", "sw");

    private static final String SW_PAKKAGE = "sw";

    private final ITypeKeeper typeKeeper;
    private final TypeReader typeParser;

    private JsonTypeKeeperReader(final ITypeKeeper typeKeeper) {
        this.typeKeeper = typeKeeper;
        this.typeParser = new TypeReader(this.typeKeeper);
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
        } catch (final JSONException exception) {
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
        if (line.startsWith("//")) {
            // Ignore comments.
            return;
        }

        final JSONTokener tokener = new JSONTokener(line);
        final JSONObject obj = new JSONObject(tokener);
        final String instructionStr = obj.getString(InstInstruction.INSTRUCTION.getValue());
        final InstInstruction instruction = InstInstruction.fromValue(instructionStr);
        switch (instruction) {
            case PACKAGE:
                this.handlePackage(obj);
                break;

            case TYPE:
                this.handleType(obj);
                break;

            case GLOBAL:
                this.handleGlobal(obj);
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

            default:
                break;
        }
    }

    private void handlePackage(final JSONObject instruction) {
        final String pakkageName = instruction.getString(InstPackage.NAME.getValue());
        final Package pakkage = this.ensurePackage(pakkageName);

        instruction.getJSONArray(InstPackage.USES.getValue()).forEach(useObj -> {
            final String use = (String) useObj;
            this.ensurePackage(use);
            pakkage.addUse(use);
        });
    }

    private void handleType(final JSONObject instruction) {
        final String typeFormat = instruction.getString(InstType.TYPE_FORMAT.getValue());
        final String name = instruction.getString(InstType.TYPE_NAME.getValue());
        final TypeString typeString = TypeStringParser.parseTypeString(name);
        final MagikType type;
        if (this.typeKeeper.getType(typeString) != UndefinedType.INSTANCE) {
            type = (MagikType) this.typeKeeper.getType(typeString);
        } else if (InstType.INTRINSIC.getValue().equals(typeFormat)) {
            type = new MagikType(this.typeKeeper, Sort.INTRINSIC, typeString);
        } else if (InstType.SLOTTED.getValue().equals(typeFormat)) {
            type = new MagikType(this.typeKeeper, Sort.SLOTTED, typeString);
        } else if (InstType.INDEXED.getValue().equals(typeFormat)) {
            type = new MagikType(this.typeKeeper, Sort.INDEXED, typeString);
        } else if (InstType.OBJECT.getValue().equals(typeFormat)) {
            type = new MagikType(this.typeKeeper, Sort.OBJECT, typeString);
        } else {
            throw new InvalidParameterException("Unknown type: " + typeFormat);
        }

        if (instruction.has(InstType.DOC.getValue())) {
            final String doc = instruction.getString(InstType.DOC.getValue());
            type.setDoc(doc);
        }

        instruction.getJSONArray(InstType.SLOTS.getValue()).forEach(slotObj -> {
            final JSONObject slot = (JSONObject) slotObj;
            final String slotName = slot.getString(InstType.SLOT_NAME.getValue());
            final String slotTypeName = slot.getString(InstType.SLOT_TYPE_NAME.getValue());
            final TypeString slotTypeString = TypeStringParser.parseTypeString(slotTypeName);
            type.addSlot(null, slotName, slotTypeString);
        });

        if (instruction.has(InstType.GENERICS.getValue())) {
            instruction.getJSONArray(InstType.GENERICS.getValue()).forEach(genericObj -> {
                final JSONObject generic = (JSONObject) genericObj;
                final String genericName = generic.getString(InstType.GENERIC_NAME.getValue());
                final String genericDoc = generic.getString(InstType.GENERIC_DOC.getValue());
                final GenericDeclaration genericDeclaration = type.addGeneric(null, genericName);
                genericDeclaration.setDoc(genericDoc);
            });
        }

        final JSONArray parents = instruction.getJSONArray(InstType.PARENTS.getValue());
        parents.forEach(parentObj -> {
            final String parentStr = (String) parentObj;
            final TypeString parentRef = TypeString.ofIdentifier(parentStr, SW_PAKKAGE);
            type.addParent(parentRef);
        });
    }

    private void handleGlobal(final JSONObject instruction) {
        final String name = instruction.getString(InstGlobal.NAME.getValue());
        final TypeString typeString = TypeString.ofIdentifier(name, TypeString.DEFAULT_PACKAGE);
        final AbstractType type = this.typeKeeper.getType(typeString);
        if (type != UndefinedType.INSTANCE) {
            // Prevent adding duplicates and/or errors.
            LOGGER.debug("Skipping already known global: {}", typeString);
            return;
        }

        final String typeName = instruction.getString(InstGlobal.TYPE_NAME.getValue());
        final TypeString aliasedRef = TypeStringParser.parseTypeString(typeName);

        final AliasType global = new AliasType(this.typeKeeper, typeString, aliasedRef);
        this.typeKeeper.addType(global);
    }

    private void handleMethod(final JSONObject instruction) {
        final String typeName = instruction.getString(InstMethod.TYPE_NAME.getValue());
        final TypeString typeRef = TypeString.ofIdentifier(typeName, TypeString.DEFAULT_PACKAGE);
        final AbstractType abstractType = this.typeKeeper.getType(typeRef);
        if (abstractType == UndefinedType.INSTANCE) {
            throw new IllegalStateException("Unknown type: " + typeName);
        }
        final MagikType type = (MagikType) abstractType;

        // What if method already exists? Might be improved by taking the line into account as well.
        final String methodName = instruction.getString(InstMethod.METHOD_NAME.getValue());
        final Location location = instruction.get(InstMethod.SOURCE_FILE.getValue()) != JSONObject.NULL
            ? new Location(Path.of(instruction.getString(InstMethod.SOURCE_FILE.getValue())).toUri())
            : null;
        final boolean isAlreadyKnown = type.getLocalMethods(methodName).stream()
            .anyMatch(method ->
                method.getLocation() == null && location == null
                || method.getLocation() != null && method.getLocation().equals(location));
        if (isAlreadyKnown) {
            LOGGER.debug("Skipping already known method: {}.{}", typeName, methodName);
            return;
        }

        final JSONArray modifiersArray = instruction.getJSONArray(InstMethod.MODIFIERS.getValue());
        final EnumSet<Method.Modifier> modifiers = StreamSupport.stream(modifiersArray.spliterator(), false)
            .map(String.class::cast)
            .map(String::toUpperCase)
            .map(Method.Modifier::valueOf)
            .collect(Collectors.toCollection(() -> EnumSet.noneOf(Method.Modifier.class)));
        final List<Parameter> parameters =
            this.parseParameters(instruction.getJSONArray(InstMethod.PARAMETERS.getValue()));
        final Parameter assignmentParameter;
        if (methodName.contains("<<")) {
            int lastIdx = parameters.size() - 1;
            assignmentParameter = parameters.get(lastIdx);
            parameters.remove(lastIdx);
        } else {
            assignmentParameter = null;
        }
        final ExpressionResultString result =
            this.parseExpressionResultString(instruction.get(InstMethod.RETURN_TYPES.getValue()));
        final ExpressionResultString loopResult =
            this.parseExpressionResultString(instruction.get(InstMethod.LOOP_TYPES.getValue()));
        final String methodDoc = instruction.get(InstMethod.DOC.getValue()) != JSONObject.NULL
            ? instruction.getString(InstMethod.DOC.getValue())
            : null;
        type.addMethod(location, modifiers, methodName, parameters, assignmentParameter, methodDoc, result, loopResult);
    }

    private void handleCondition(final JSONObject instruction) {
        final String name = instruction.getString(InstCondition.NAME.getValue());
        final String doc = instruction.getString(InstCondition.DOC.getValue());
        final String parent = instruction.getString(InstCondition.PARENT.getValue());
        final Location location = instruction.get(InstCondition.SOURCE_FILE.getValue()) != JSONObject.NULL
            ? new Location(Path.of(instruction.getString(InstCondition.SOURCE_FILE.getValue())).toUri())
            : null;
        final JSONArray dataNameListArray = instruction.getJSONArray(InstCondition.DATA_NAME_LIST.getValue());
        final List<String> dataNameList = StreamSupport.stream(dataNameListArray.spliterator(), false)
            .map(String.class::cast)
            .collect(Collectors.toList());
        final Condition condition = new Condition(location, name, parent, dataNameList, doc);
        this.typeKeeper.addCondition(condition);
    }

    private void handleBinaryOperator(final JSONObject instruction) {
        final BinaryOperator.Operator operator =
            BinaryOperator.Operator.valueFor(instruction.getString(InstBinaryOperator.OPERATOR.getValue()));
        final String lhsTypeStr = instruction.getString(InstBinaryOperator.LHS_TYPE.getValue());
        final TypeString lhsTypeRef = TypeStringParser.parseTypeString(lhsTypeStr);
        final String rhsTypeStr = instruction.getString(InstBinaryOperator.RHS_TYPE.getValue());
        final TypeString rhsTypeRef = TypeStringParser.parseTypeString(rhsTypeStr);
        final String returnTypeStr = instruction.getString(InstBinaryOperator.RETURN_TYPE.getValue());
        final TypeString resultTypeRef = TypeStringParser.parseTypeString(returnTypeStr);
        final BinaryOperator binaryOperator = new BinaryOperator(operator, lhsTypeRef, rhsTypeRef, resultTypeRef);
        this.typeKeeper.addBinaryOperator(binaryOperator);
    }

    private List<Parameter> parseParameters(final JSONArray parametersArray) {
        return StreamSupport.stream(parametersArray.spliterator(), false)
            .map(parameterThing -> {
                final JSONObject parameterObj = (JSONObject) parameterThing;
                final String name = parameterObj.getString(InstParameter.NAME.getValue());
                final Parameter.Modifier modifier =
                    parameterObj.get(InstParameter.MODIFIER.getValue()) != JSONObject.NULL
                    ? Parameter.Modifier.valueOf(
                        parameterObj.getString(InstParameter.MODIFIER.getValue()).toUpperCase())
                    : Parameter.Modifier.NONE;
                final String typeName = parameterObj.getString(InstParameter.TYPE_NAME.getValue());
                final TypeString typeString = TypeStringParser.parseTypeString(typeName);
                final AbstractType type = this.typeParser.parseTypeString(typeString);
                return new Parameter(name, modifier, type);
            })
            .collect(Collectors.toList());
    }

    private ExpressionResultString parseExpressionResultString(final Object obj) {
        if (obj == JSONObject.NULL) {
            return ExpressionResultString.UNDEFINED;
        }

        if (obj instanceof String) {
            final String expressionResultString = (String) obj;
            return TypeStringParser.parseExpressionResultString(expressionResultString, TypeString.DEFAULT_PACKAGE);
        }

        if (obj instanceof JSONArray) {
            final JSONArray array = (JSONArray) obj;
            return StreamSupport.stream(array.spliterator(), false)
                .map(String.class::cast)
                .map(TypeStringParser::parseTypeString)
                .collect(ExpressionResultString.COLLECTOR);
        }

        throw new InvalidParameterException("Don't know what to do with: " + obj);
    }

    private void handleProcedure(final JSONObject instruction) {
        final JSONArray modifiersArray = instruction.getJSONArray(InstProcedure.MODIFIERS.getValue());
        final EnumSet<Method.Modifier> modifiers = StreamSupport.stream(modifiersArray.spliterator(), false)
            .map(String.class::cast)
            .map(String::toUpperCase)
            .map(Method.Modifier::valueOf)
            .collect(Collectors.toCollection(() -> EnumSet.noneOf(Method.Modifier.class)));
        final Location location = instruction.get(InstProcedure.SOURCE_FILE.getValue()) != JSONObject.NULL
            ? new Location(Path.of(instruction.getString(InstProcedure.SOURCE_FILE.getValue())).toUri())
            : null;
        final List<Parameter> parameters =
            this.parseParameters(instruction.getJSONArray(InstProcedure.PARAMETERS.getValue()));
        final ExpressionResultString resultStr =
            this.parseExpressionResultString(instruction.get(InstProcedure.RETURN_TYPES.getValue()));
        final ExpressionResultString loopResultStr =
            this.parseExpressionResultString(instruction.get(InstProcedure.LOOP_TYPES.getValue()));

        final String procedureName = instruction.getString(InstProcedure.PROCEDURE_NAME.getValue());
        final MagikType procedureType = (MagikType) this.typeKeeper.getType(SW_PROCEDURE_REF);
        final String methodDoc = instruction.get(InstProcedure.DOC.getValue()) != JSONObject.NULL
            ? instruction.getString(InstProcedure.DOC.getValue())
            : null;
        final ProcedureInstance instance = new ProcedureInstance(
            procedureType,
            location,
            procedureName,
            modifiers,
            parameters,
            methodDoc,
            resultStr,
            loopResultStr);

        // Create alias to instance.
        final String name = instruction.getString(InstProcedure.NAME.getValue());
        final TypeString typeString = TypeString.ofIdentifier(name, TypeString.DEFAULT_PACKAGE);
        final AbstractType type = this.typeKeeper.getType(typeString);
        if (type != UndefinedType.INSTANCE) {
            // Prevent adding duplicates and/or errors.
            LOGGER.debug("Skipping already known procedure: {}", typeString);
            return;
        }

        new AliasType(this.typeKeeper, typeString, instance);
    }

    private Package ensurePackage(final String pakkageName) {
        if (!this.typeKeeper.hasPackage(pakkageName)) {
            new Package(this.typeKeeper, pakkageName);
        }
        return this.typeKeeper.getPackage(pakkageName);
    }

    /**
     * Read types from a JSON-line file.
     * @param path Path to JSON-line file.
     * @param typeKeeper {@link TypeKeeper} to fill.
     * @throws IOException -
     */
    public static void readTypes(final Path path, final ITypeKeeper typeKeeper) throws IOException {
        final JsonTypeKeeperReader reader = new JsonTypeKeeperReader(typeKeeper);
        reader.run(path);
    }

}
