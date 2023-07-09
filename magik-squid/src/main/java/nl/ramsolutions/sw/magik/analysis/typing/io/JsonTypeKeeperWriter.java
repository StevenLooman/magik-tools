package nl.ramsolutions.sw.magik.analysis.typing.io;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import nl.ramsolutions.sw.magik.analysis.typing.ITypeKeeper;
import nl.ramsolutions.sw.magik.analysis.typing.TypeKeeper;
import nl.ramsolutions.sw.magik.analysis.typing.types.AbstractType;
import nl.ramsolutions.sw.magik.analysis.typing.types.AliasType;
import nl.ramsolutions.sw.magik.analysis.typing.types.Condition;
import nl.ramsolutions.sw.magik.analysis.typing.types.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.types.MagikType;
import nl.ramsolutions.sw.magik.analysis.typing.types.MagikType.Sort;
import nl.ramsolutions.sw.magik.analysis.typing.types.Method;
import nl.ramsolutions.sw.magik.analysis.typing.types.Package;
import nl.ramsolutions.sw.magik.analysis.typing.types.ProcedureInstance;
import nl.ramsolutions.sw.magik.analysis.typing.types.Slot;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JSON-line TypeKeeper writer.
 */
public final class JsonTypeKeeperWriter {

    private static final Logger LOGGER = LoggerFactory.getLogger(JsonTypeKeeperWriter.class);

    private final ITypeKeeper typeKeeper;

    private JsonTypeKeeperWriter(final ITypeKeeper typeKeeper) {
        this.typeKeeper = typeKeeper;
    }

    private void run(final Path path) throws IOException {
        LOGGER.debug("Writing type database to path: {}", path);

        final File file = path.toFile();
        try (FileWriter fileReader = new FileWriter(file, StandardCharsets.ISO_8859_1);
             BufferedWriter bufferedWriter = new BufferedWriter(fileReader)) {
            this.writePackages(bufferedWriter);
            this.writeTypes1(bufferedWriter);
            this.writeGlobals(bufferedWriter);
            this.writeMethods(bufferedWriter);
            this.writeProcedures(bufferedWriter);
            this.writeConditions(bufferedWriter);
            this.writeBinaryOperators(bufferedWriter);
        }
    }

    private void writeInstruction(final Writer writer, final JSONObject instruction) {
        instruction.write(writer);

        try {
            writer.write("\n");
        } catch (final IOException exception) {
            LOGGER.error("Caught exception writing instruction", exception);
        }
    }

    private void writePackages(final Writer writer) {
        final Comparator<Package> packageNameComparer = Comparator.comparing(Package::getName);
        this.typeKeeper.getPackages().stream()
            .sorted(packageNameComparer)
            .forEach(pakkage -> {
                final JSONObject instruction = new JSONObject();
                instruction.put(InstInstruction.INSTRUCTION.getValue(), InstInstruction.PACKAGE.getValue());
                instruction.put(InstPackage.NAME.getValue(), pakkage.getName());
                final List<String> pakkageUses = pakkage.getUses().stream()
                    .map(Package::getName)
                    .collect(Collectors.toList());
                instruction.put(InstPackage.USES.getValue(), pakkageUses);
                this.writeInstruction(writer, instruction);
            });
    }

    private void writeTypes1(final Writer writer) {
        final Comparator<AbstractType> typeNameComparer = Comparator.comparing(AbstractType::getFullName);
        final Comparator<Slot> slotNameComparer = Comparator.comparing(Slot::getName);
        this.typeKeeper.getTypes().stream()
            .filter(type -> !(type instanceof AliasType))
            .sorted(typeNameComparer)
            .forEach(type -> {
                final List<String> parents = type.getParents().stream()
                    .map(AbstractType::getFullName)
                    .sorted()
                    .collect(Collectors.toList());
                final List<JSONObject> slots = type.getSlots().stream()
                    .sorted(slotNameComparer)
                    .map(slot -> {
                        final JSONObject slotObject = new JSONObject();
                        slotObject.put(InstType.SLOT_NAME.getValue(), slot.getName());
                        slotObject.put(InstType.SLOT_TYPE_NAME.getValue(), slot.getType().getFullString());
                        return slotObject;
                    })
                    .collect(Collectors.toList());

                final List<JSONObject> generics;
                if (type instanceof MagikType) {
                    final MagikType magikType = (MagikType) type;
                    generics = magikType.getGenerics().stream()
                        .map(generic -> {
                            final JSONObject genericObject = new JSONObject();
                            genericObject.put(InstType.GENERIC_NAME.getValue(), generic.getName());
                            genericObject.put(InstType.GENERIC_DOC.getValue(), generic.getDoc());
                            return genericObject;
                        })
                    .collect(Collectors.toList());
                } else {
                    generics = Collections.emptyList();
                }

                final JSONObject instruction = new JSONObject();
                instruction.put(InstInstruction.INSTRUCTION.getValue(), InstInstruction.TYPE.getValue());
                instruction.put(InstType.TYPE_NAME.getValue(), type.getFullName());
                instruction.put(InstType.TYPE_FORMAT.getValue(), this.getTypeFormat(type));
                instruction.put(InstType.PARENTS.getValue(), parents);
                instruction.put(InstType.SLOTS.getValue(), slots);
                instruction.put(InstType.GENERICS.getValue(), generics);
                instruction.put(InstType.DOC.getValue(), type.getDoc());
                this.writeInstruction(writer, instruction);
            });
    }

    private String getTypeFormat(final AbstractType type) {
        if (!(type instanceof MagikType)) {
            throw new IllegalArgumentException("Unknown type: " + type);
        }

        final MagikType magikType = (MagikType) type;
        if (magikType.getSort() == Sort.INTRINSIC) {
            return InstType.INTRINSIC.getValue();
        } else if (magikType.getSort() == Sort.SLOTTED) {
            return InstType.SLOTTED.getValue();
        } else if (magikType.getSort() == Sort.INDEXED) {
            return InstType.INDEXED.getValue();
        } else if (magikType.getSort() == Sort.OBJECT) {
            return InstType.OBJECT.getValue();
        }

        throw new IllegalArgumentException("Unknown type: " + type);
    }

    private void writeGlobals(final Writer writer) {
        this.typeKeeper.getTypes().stream()
            .filter(AliasType.class::isInstance)
            .map(AliasType.class::cast)
            .forEach(type -> {
                final JSONObject instruction = new JSONObject();
                instruction.put(InstInstruction.INSTRUCTION.getValue(), InstInstruction.GLOBAL.getValue());
                instruction.put(InstGlobal.NAME.getValue(), type.getFullName());
                instruction.put(InstGlobal.TYPE_NAME.getValue(), type.getAliasedType().getFullName());
                this.writeInstruction(writer, instruction);
            });
    }

    private void writeMethods(final Writer writer) {
        final Comparator<AbstractType> typeNameComparer = Comparator.comparing(AbstractType::getFullName);
        final Comparator<Method> methodNameComparer = Comparator.comparing(Method::getName);
        this.typeKeeper.getTypes().stream()
            .sorted(typeNameComparer)
            .filter(MagikType.class::isInstance)
            .flatMap(type -> type.getLocalMethods().stream().sorted(methodNameComparer))
            .forEach(method -> {
                final List<JSONObject> parameters = method.getParameters().stream()
                    .map(parameter -> {
                        final JSONObject parameterObject = new JSONObject();
                        parameterObject.put(InstParameter.NAME.getValue(), parameter.getName());
                        parameterObject.put(InstParameter.MODIFIER.getValue(), parameter.getModifier());
                        parameterObject.put(InstParameter.TYPE_NAME.getValue(), parameter.getType().getFullString());
                        return parameterObject;
                    })
                    .collect(Collectors.toList());
                final Path sourceFile = method.getLocation() != null
                    ? method.getLocation().getPath()
                    : null;

                final JSONObject instruction = new JSONObject();
                instruction.put(InstInstruction.INSTRUCTION.getValue(), InstInstruction.METHOD.getValue());
                instruction.put(InstMethod.TYPE_NAME.getValue(), method.getOwner().getFullName());
                instruction.put(InstMethod.METHOD_NAME.getValue(), method.getName());
                instruction.put(InstMethod.MODIFIERS.getValue(), method.getModifiers());
                instruction.put(InstMethod.PARAMETERS.getValue(), parameters);
                if (method.getCallResult() != ExpressionResultString.UNDEFINED) {
                    final ExpressionResultString callResult = method.getCallResult();
                    instruction.put(InstMethod.RETURN_TYPES.getValue(), callResult);
                } else {
                    instruction.put(
                        InstMethod.RETURN_TYPES.getValue(),
                        ExpressionResultString.UNDEFINED_SERIALIZED_NAME);
                }
                if (method.getLoopbodyResult() != ExpressionResultString.UNDEFINED) {
                    final ExpressionResultString loopTypes = method.getLoopbodyResult();
                    instruction.put(InstMethod.LOOP_TYPES.getValue(), loopTypes);
                } else {
                    instruction.put(InstMethod.LOOP_TYPES.getValue(), ExpressionResultString.UNDEFINED_SERIALIZED_NAME);
                }
                instruction.put(InstMethod.SOURCE_FILE.getValue(), sourceFile);
                instruction.put(InstMethod.DOC.getValue(), method.getDoc());
                this.writeInstruction(writer, instruction);
            });
    }

    private void writeProcedures(final Writer writer) {
        final Comparator<AbstractType> typeNameComparer = Comparator.comparing(AbstractType::getFullName);
        this.typeKeeper.getTypes().stream()
            .filter(ProcedureInstance.class::isInstance)
            .sorted(typeNameComparer)
            .forEach(procedure -> {
                final Collection<Method> invokeMethods = procedure.getMethods("invoke()");
                final Method invokeMethod = new ArrayList<>(invokeMethods).get(0);
                final List<JSONObject> parameters = invokeMethod.getParameters().stream()
                    .map(parameter -> {
                        final JSONObject parameterObject = new JSONObject();
                        parameterObject.put(InstParameter.NAME.getValue(), parameter.getName());
                        parameterObject.put(InstParameter.MODIFIER.getValue(), parameter.getModifier());
                        parameterObject.put(InstParameter.TYPE_NAME.getValue(), parameter.getType().getFullString());
                        return parameterObject;
                    })
                    .collect(Collectors.toList());
                final Path sourceFile = invokeMethod.getLocation() != null
                    ? procedure.getLocation().getPath()
                    : null;

                final JSONObject instruction = new JSONObject();
                instruction.put(InstInstruction.INSTRUCTION.getValue(), InstInstruction.METHOD.getValue());
                instruction.put(InstProcedure.PROCEDURE_NAME.getValue(), procedure.getFullName());
                instruction.put(InstProcedure.NAME.getValue(), procedure.getName());
                instruction.put(InstProcedure.DOC.getValue(), procedure.getDoc());
                instruction.put(InstProcedure.SOURCE_FILE.getValue(), procedure.getLocation().getPath());
                instruction.put(InstProcedure.MODIFIERS.getValue(), invokeMethod.getModifiers());
                instruction.put(InstProcedure.PARAMETERS.getValue(), parameters);
                if (invokeMethod.getCallResult() != ExpressionResultString.UNDEFINED) {
                    final ExpressionResultString callResult = invokeMethod.getCallResult();
                    instruction.put(InstProcedure.RETURN_TYPES.getValue(), callResult);
                } else {
                    instruction.put(
                        InstProcedure.RETURN_TYPES.getValue(),
                        ExpressionResultString.UNDEFINED_SERIALIZED_NAME);
                }
                if (invokeMethod.getLoopbodyResult() != ExpressionResultString.UNDEFINED) {
                    final ExpressionResultString loopTypes = invokeMethod.getLoopbodyResult();
                    instruction.put(InstProcedure.LOOP_TYPES.getValue(), loopTypes);
                } else {
                    instruction.put(
                        InstProcedure.LOOP_TYPES.getValue(),
                        ExpressionResultString.UNDEFINED_SERIALIZED_NAME);
                }
                instruction.put(InstProcedure.SOURCE_FILE.getValue(), sourceFile);
                instruction.put(InstProcedure.DOC.getValue(), invokeMethod.getDoc());
                this.writeInstruction(writer, instruction);
            });
    }

    private void writeConditions(final BufferedWriter bufferedWriter) {
        final Comparator<Condition> conditionNameComparer = Comparator.comparing(Condition::getName);
        this.typeKeeper.getConditions().stream()
            .sorted(conditionNameComparer)
            .forEach(condition -> {
                final JSONObject instruction = new JSONObject();
                instruction.put(InstInstruction.INSTRUCTION.getValue(), InstInstruction.CONDITION.getValue());
                instruction.put(InstCondition.NAME.getValue(), condition.getName());
                instruction.put(InstCondition.DOC.getValue(), condition.getDoc());
                instruction.put(InstCondition.SOURCE_FILE.getValue(), condition.getLocation().getPath());
                instruction.put(InstCondition.PARENT.getValue(), condition.getParent());
                instruction.put(InstCondition.DATA_NAME_LIST.getValue(), condition.getDataNameList());
                this.writeInstruction(bufferedWriter, instruction);
            });
    }

    private void writeBinaryOperators(final BufferedWriter bufferedWriter) {
        this.typeKeeper.getBinaryOperators().stream()
            .forEach(binOp -> {
                final JSONObject instruction = new JSONObject();
                instruction.put(InstInstruction.INSTRUCTION.getValue(), InstInstruction.BINARY_OPERATOR.getValue());
                instruction.put(InstBinaryOperator.OPERATOR.getValue(), binOp.getOperator());
                instruction.put(InstBinaryOperator.LHS_TYPE.getValue(), binOp.getLeftType().getFullString());
                instruction.put(InstBinaryOperator.RHS_TYPE.getValue(), binOp.getRightType().getFullString());
                instruction.put(InstBinaryOperator.RETURN_TYPE.getValue(), binOp.getResultType().getFullString());
                this.writeInstruction(bufferedWriter, instruction);
            });
    }

    /**
     * Write types to a JSON-line file.
     * @param path Path to JSON-line file.
     * @param typeKeeper {@link TypeKeeper} to dump.
     * @throws IOException -
     */
    public static void writeTypes(final Path path, final ITypeKeeper typeKeeper) throws IOException {
        final JsonTypeKeeperWriter reader = new JsonTypeKeeperWriter(typeKeeper);
        reader.run(path);
    }

}
