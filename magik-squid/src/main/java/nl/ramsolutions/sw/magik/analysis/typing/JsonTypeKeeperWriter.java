package nl.ramsolutions.sw.magik.analysis.typing;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import nl.ramsolutions.sw.magik.analysis.typing.types.AbstractType;
import nl.ramsolutions.sw.magik.analysis.typing.types.AliasType;
import nl.ramsolutions.sw.magik.analysis.typing.types.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.types.MagikType;
import nl.ramsolutions.sw.magik.analysis.typing.types.MagikType.Sort;
import nl.ramsolutions.sw.magik.analysis.typing.types.Method;
import nl.ramsolutions.sw.magik.analysis.typing.types.Package;
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
                instruction.put("instruction", "package");
                instruction.put("name", pakkage.getName());
                final List<String> pakkageUses = pakkage.getUses().stream()
                    .map(usedPakkage -> usedPakkage.getName())
                    .collect(Collectors.toList());
                instruction.put("uses", pakkageUses);
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
                        slotObject.put("name", slot.getName());
                        slotObject.put("type_name", slot.getType().getFullString());
                        return slotObject;
                    })
                    .collect(Collectors.toList());

                final List<JSONObject> generics;
                if (type instanceof MagikType) {
                    final MagikType magikType = (MagikType) type;
                    generics = magikType.getGenerics().stream()
                        .map(generic -> {
                            final JSONObject genericObject = new JSONObject();
                            genericObject.put("name", generic.getName());
                            genericObject.put("doc", generic.getDoc());
                            return genericObject;
                        })
                    .collect(Collectors.toList());
                } else {
                    generics = Collections.emptyList();
                }

                final JSONObject instruction = new JSONObject();
                instruction.put("instruction", "type");
                instruction.put("type_name", type.getFullName());
                instruction.put("type_format", this.getTypeFormat(type));
                instruction.put("parents", parents);
                instruction.put("slots", slots);
                instruction.put("generics", generics);
                instruction.put("doc", type.getDoc());
                this.writeInstruction(writer, instruction);
            });
    }

    private String getTypeFormat(final AbstractType type) {
        if (!(type instanceof MagikType)) {
            throw new IllegalArgumentException("Unknown type: " + type);
        }

        final MagikType magikType = (MagikType) type;
        if (magikType.getSort() == Sort.INTRINSIC) {
            return "intrinsic";
        } else if (magikType.getSort() == Sort.SLOTTED) {
            return "slotted";
        } else if (magikType.getSort() == Sort.INDEXED) {
            return "indexed";
        } else if (magikType.getSort() == Sort.OBJECT) {
            return "object";
        }

        throw new IllegalArgumentException("Unknown type: " + type);
    }

    private void writeGlobals(final Writer writer) {
        this.typeKeeper.getTypes().stream()
            .filter(AliasType.class::isInstance)
            .map(AliasType.class::cast)
            .forEach(type -> {
                final JSONObject instruction = new JSONObject();
                instruction.put("instruction", "global");
                instruction.put("name", type.getFullName());
                instruction.put("type_name", type.getAliasedType().getFullName());
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
                        parameterObject.put("name", parameter.getName());
                        parameterObject.put("modifier", parameter.getModifier());
                        parameterObject.put("type_name", parameter.getType().getFullName());
                        return parameterObject;
                    })
                    .collect(Collectors.toList());
                final Path sourceFile = method.getLocation() != null
                    ? method.getLocation().getPath()
                    : null;

                final JSONObject instruction = new JSONObject();
                instruction.put("instruction", "method");
                instruction.put("type_name", method.getOwner().getFullName());
                instruction.put("method_name", method.getName());
                instruction.put("modifiers", method.getModifiers());
                instruction.put("parameters", parameters);
                if (method.getCallResult() != ExpressionResultString.UNDEFINED) {
                    final ExpressionResultString callResult = method.getCallResult();
                    instruction.put("return_types", callResult);
                } else {
                    instruction.put("return_types", ExpressionResultString.UNDEFINED_SERIALIZED_NAME);
                }
                if (method.getLoopbodyResult() != ExpressionResultString.UNDEFINED) {
                    final ExpressionResultString loopTypes = method.getLoopbodyResult();
                    instruction.put("loop_types", loopTypes);
                } else {
                    instruction.put("return_types", ExpressionResultString.UNDEFINED_SERIALIZED_NAME);
                }
                instruction.put("source_file", sourceFile);
                instruction.put("doc", method.getDoc());
                this.writeInstruction(writer, instruction);
            });
    }

    private void writeProcedures(final Writer writer) {
        // {
        // "instruction":"procedure",
        // "name":"sw:sys!ds_record_cf",
        // "procedure_name":"sys!ds_record_cf",
        // "modifiers":[],
        // "parameters":[],
        // "return_types":"__UNDEFINED_RESULT__",
        // "loop_types":[],
        // "source_file":null,
        // "doc":""
        // }
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
