package nl.ramsolutions.sw.magik.analysis.typing.types;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import nl.ramsolutions.sw.magik.analysis.typing.ITypeKeeper;

/**
 * MagikType for which Generics are filled.
 */
public class MagikTypeInstance extends AbstractType {

    private final ITypeKeeper typeKeeper;
    private final TypeString typeString;
    private final Set<GenericDefinition> genericDefinitions;

    /**
     * Constructor.
     * @param typeKeeper TypeKeeper.
     * @param typeString Parent type.
     * @param genericDefinitions Definition of generics.
     */
    public MagikTypeInstance(
            final ITypeKeeper typeKeeper,
            final TypeString typeString,
            final Set<GenericDefinition> genericDefinitions) {
        this.typeKeeper = typeKeeper;
        this.typeString = typeString;
        this.genericDefinitions = Set.copyOf(genericDefinitions);
    }

    /**
     * Get {@link GenericDefinition} by its name.
     * @param name Name of {@link GenericDefinition}.
     * @return The {@link GenericDefinition}.
     */
    @CheckForNull
    public GenericDefinition getGenericDefinition(final String name) {
        return this.genericDefinitions.stream()
            .filter(genericDef -> genericDef.getName().equals(name))
            .findAny()
            .orElse(null);
    }

    public MagikType getMagikType() {
        return (MagikType) this.typeKeeper.getType(this.typeString);
    }

    @Override
    public TypeString getTypeString() {
        return this.typeString;
    }

    @Override
    public String getFullName() {
        return this.typeString.getFullString();
    }

    @Override
    public String getName() {
        return this.typeString.getIdentifier();
    }

    @Override
    public List<GenericDeclaration> getGenerics() {
        return Collections.emptyList();
    }

    @Override
    public Collection<Slot> getSlots() {
        final MagikType magikType = this.getMagikType();
        return magikType.getSlots().stream()
            .map(this::substitueGenerics)
            .collect(Collectors.toSet());
    }

    @Override
    public Collection<Method> getMethods() {
        final MagikType magikType = this.getMagikType();
        return magikType.getMethods()
            .stream()
            .map(this::substitueGenerics)
            .collect(Collectors.toSet());
    }

    @Override
    public Collection<Method> getMethods(final String methodName) {
        final MagikType magikType = this.getMagikType();
        return magikType.getMethods(methodName)
            .stream()
            .map(this::substitueGenerics)
            .collect(Collectors.toSet());
    }

    @Override
    public Collection<Method> getLocalMethods() {
        final MagikType magikType = this.getMagikType();
        return magikType.getLocalMethods()
            .stream()
            .map(this::substitueGenerics)
            .collect(Collectors.toSet());
    }

    @Override
    public Collection<Method> getSuperMethods(final String methodName) {
        final MagikType magikType = this.getMagikType();
        return magikType.getSuperMethods(methodName)
            .stream()
            .map(this::substitueGenerics)
            .collect(Collectors.toSet());
    }

    @Override
    public Collection<Method> getSuperMethods(final String methodName, final String superName) {
        final MagikType magikType = this.getMagikType();
        return magikType.getSuperMethods(methodName, superName)
            .stream()
            .map(this::substitueGenerics)
            .collect(Collectors.toSet());
    }

    @Override
    public Collection<AbstractType> getParents() {
        final MagikType magikType = this.getMagikType();
        return magikType.getParents().stream()
            .map(this::substitueGenerics)
            .collect(Collectors.toSet());
    }

    private Method substitueGenerics(final Method method) {
        final Map<TypeString, TypeString> genericTypeMapping = this.getGenericTypeMapping();
        ExpressionResultString callResult = method.getCallResult();
        ExpressionResultString loopbodyResult = method.getLoopbodyResult();
        for (final Map.Entry<TypeString, TypeString> entry : genericTypeMapping.entrySet()) {
            final TypeString from = entry.getKey();
            final TypeString to = entry.getValue();
            callResult = callResult.substituteType(from, to);
            loopbodyResult = loopbodyResult.substituteType(from, to);
        }
        return new Method(
            method.getLocation(),
            method.getModifiers(),
            method.getOwner(),
            method.getName(),
            method.getParameters(),
            method.getAssignmentParameter(),
            method.getDoc(),
            callResult,
            loopbodyResult);
    }

    private Slot substitueGenerics(final Slot slot) {
        final Map<TypeString, TypeString> genericTypes = this.getGenericTypeMapping();
        final TypeString slotType = slot.getType();
        final TypeString newSlotType = genericTypes.getOrDefault(slotType, slotType);
        return new Slot(
            slot.getLocation(),
            slot.getName(),
            newSlotType);
    }

    private AbstractType substitueGenerics(final AbstractType type) {
        final AbstractType rawType = type instanceof final MagikTypeInstance magikTypeInstance
            ? magikTypeInstance.getMagikType()
            : type;
        final TypeString rawTypeString = rawType.getTypeString();
        return new MagikTypeInstance(this.typeKeeper, rawTypeString, this.genericDefinitions);
    }

    private Map<TypeString, TypeString> getGenericTypeMapping() {
        return this.genericDefinitions.stream()
            .collect(Collectors.toMap(
                def -> def.getNameAsTypeString(),
                def -> def.getTypeString()));
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.typeString, this.genericDefinitions);
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null) {
            return false;
        }

        if (this.getClass() != obj.getClass()) {
            return false;
        }

        final MagikTypeInstance other = (MagikTypeInstance) obj;
        return Objects.equals(this.typeString, other.typeString)
               && Objects.equals(this.genericDefinitions, other.genericDefinitions);
    }

    @Override
    public String toString() {
        // Map GenericDeclarations to GenericDefinitions: LinkedHashMap
        final MagikType magikType = this.getMagikType();
        final Map<GenericDeclaration, GenericDefinition> generics = magikType.getGenerics().stream()
            .map(genericDecl -> {
                final String genericDeclName = genericDecl.getName();
                final GenericDefinition genericDef = this.getGenericDefinition(genericDeclName);
                return Map.entry(genericDecl, genericDef);
            })
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                (a, b) -> a,
                LinkedHashMap::new));  // We want order!

        final String genericsStr = generics.entrySet().stream()
            .map(entry -> {
                final GenericDeclaration genericDecl = entry.getKey();
                final GenericDefinition genericDef = entry.getValue();
                if (genericDef == null) {
                    return genericDecl.getTypeString().getFullString();
                }

                return genericDef.getTypeString().getFullString();
            })
            .collect(Collectors.joining(","));
        return String.format(
            "%s@%s(%s<%s>)",
            this.getClass().getName(), Integer.toHexString(this.hashCode()),
            this.getFullName(), genericsStr);
    }

}
