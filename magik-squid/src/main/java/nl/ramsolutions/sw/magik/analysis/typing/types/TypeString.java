package nl.ramsolutions.sw.magik.analysis.typing.types;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.annotation.concurrent.Immutable;
import nl.ramsolutions.sw.magik.api.TypeStringGrammar;

/**
 * Type string, containing package name and identifier.
 * Examples:
 * - {@code "sw:rope"}
 * - {@code "sw:char16_vector|sw:symbol|sw:unset"}
 * - {@code "_undefined"}
 * - {@code "_self|sw:unset"}
 * - {@code "sw:rope<E=sw:integer>"}
 * - {@code "<E>"}
 */
@Immutable
public final class TypeString implements Comparable<TypeString> {

    @SuppressWarnings("checkstyle:JavadocVariable")
    public static final String DEFAULT_PACKAGE = "user";
    @SuppressWarnings("checkstyle:JavadocVariable")
    public static final String SW_PACKAGE = "sw";

    @SuppressWarnings("checkstyle:JavadocVariable")
    public static final TypeString UNDEFINED = new TypeString(UndefinedType.SERIALIZED_NAME, DEFAULT_PACKAGE);
    @SuppressWarnings("checkstyle:JavadocVariable")
    public static final TypeString SELF = new TypeString(SelfType.SERIALIZED_NAME, DEFAULT_PACKAGE);
    @SuppressWarnings("checkstyle:JavadocVariable")
    public static final TypeString SW_UNSET = TypeString.ofIdentifier("unset", SW_PACKAGE);
    @SuppressWarnings("checkstyle:JavadocVariable")
    public static final TypeString SW_FALSE = TypeString.ofIdentifier("false", "sw");
    @SuppressWarnings("checkstyle:JavadocVariable")
    public static final TypeString SW_MAYBE = TypeString.ofIdentifier("maybe", "sw");
    @SuppressWarnings("checkstyle:JavadocVariable")
    public static final TypeString SW_CHARACTER = TypeString.ofIdentifier("character", "sw");
    @SuppressWarnings("checkstyle:JavadocVariable")
    public static final TypeString SW_BIGNUM = TypeString.ofIdentifier("bignum", "sw");
    @SuppressWarnings("checkstyle:JavadocVariable")
    public static final TypeString SW_INTEGER = TypeString.ofIdentifier("integer", "sw");
    @SuppressWarnings("checkstyle:JavadocVariable")
    public static final TypeString SW_FLOAT = TypeString.ofIdentifier("float", "sw");
    @SuppressWarnings("checkstyle:JavadocVariable")
    public static final TypeString SW_SW_REGEXP = TypeString.ofIdentifier("sw_regexp", "sw");
    @SuppressWarnings("checkstyle:JavadocVariable")
    public static final TypeString SW_CHAR16_VECTOR = TypeString.ofIdentifier("char16_vector", "sw");
    @SuppressWarnings("checkstyle:JavadocVariable")
    public static final TypeString SW_CHAR16_VECTOR_WITH_GENERICS =
        TypeString.ofIdentifier(SW_CHAR16_VECTOR.getIdentifier(), SW_CHAR16_VECTOR.getPakkage(),
        TypeString.ofGenericDefinition("K", TypeString.SW_INTEGER),
        TypeString.ofGenericDefinition("E", TypeString.SW_CHARACTER));
    @SuppressWarnings("checkstyle:JavadocVariable")
    public static final TypeString SW_SYMBOL = TypeString.ofIdentifier("symbol", "sw");
    @SuppressWarnings("checkstyle:JavadocVariable")
    public static final TypeString SW_SIMPLE_VECTOR = TypeString.ofIdentifier("simple_vector", "sw");
    @SuppressWarnings("checkstyle:JavadocVariable")
    public static final TypeString SW_HEAVY_THREAD = TypeString.ofIdentifier("heavy_thread", "sw");
    @SuppressWarnings("checkstyle:JavadocVariable")
    public static final TypeString SW_LIGHT_THREAD = TypeString.ofIdentifier("light_thread", "sw");
    @SuppressWarnings("checkstyle:JavadocVariable")
    public static final TypeString SW_GLOBAL_VARIABLE = TypeString.ofIdentifier("global_variable", "sw");
    @SuppressWarnings("checkstyle:JavadocVariable")
    public static final TypeString SW_PROCEDURE = TypeString.ofIdentifier("procedure", "sw");
    @SuppressWarnings("checkstyle:JavadocVariable")
    public static final TypeString SW_OBJECT = TypeString.ofIdentifier("object", "sw");
    @SuppressWarnings("checkstyle:JavadocVariable")
    public static final TypeString SW_CONDITION = TypeString.ofIdentifier("condition", "sw");
    @SuppressWarnings("checkstyle:JavadocVariable")
    public static final TypeString SW_ENUMERATION_VALUE = TypeString.ofIdentifier("enumeration_value", "sw");
    @SuppressWarnings("checkstyle:JavadocVariable")
    public static final TypeString SW_INDEXED_FORMAT_MIXIN = TypeString.ofIdentifier("indexed_format_mixin", "sw");
    @SuppressWarnings("checkstyle:JavadocVariable")
    public static final TypeString SW_SLOTTED_FORMAT_MIXIN = TypeString.ofIdentifier("slotted_format_mixin", "sw");

    private static final String GENERIC_DEFINITION = "_generic_def";
    private static final String GENERIC_REFERENCE = "_generic_ref";
    private static final String PARAMETER = "_parameter";

    private final String string;
    private final String currentPackage;
    private final List<TypeString> combinedTypes;
    private final List<TypeString> generics;

    /**
     * Constructor.
     * @param identifier Identifier, e.g., {@code "sw:rope"}.
     * @param currentPackage The current package, e.g., {@code "user"}.
     */
    private TypeString(final String identifier, final String currentPackage) {
        this.string = identifier.trim();
        this.currentPackage = currentPackage.trim();
        this.combinedTypes = Collections.emptyList();
        this.generics = Collections.emptyList();
    }

    /**
     * Constructor for generics.
     * @param identifier Identifier, e.g., {@code "sw:rope"}.
     * @param currentPackage The current package, e.g., {@code "user"}.
     */
    private TypeString(final String identifier, final String currentPackage, final TypeString... generics) {
        this.string = identifier.trim();
        this.currentPackage = currentPackage.trim();
        this.combinedTypes = Collections.emptyList();
        this.generics = Arrays.asList(generics);
    }

    /**
     * Constructor for combined types.
     * @param currentPackage The current package, e.g., {@code "user"}.
     * @param combinations Combined {@link TypeString}s.
     */
    private TypeString(final String currentPackage, final TypeString... combinations) {
        this.string = null;
        this.currentPackage = currentPackage.trim();
        this.combinedTypes = Arrays.asList(combinations);
        this.generics = Collections.emptyList();
    }

    /**
     * Create a {@link TypeString} of a generic definition.
     * @param identifier Name of generic.
     * @param definition Type of the generic, singular.
     * @return {@link TypeString}.
     */
    public static TypeString ofGenericDefinition(final String identifier, final TypeString... definition) {
        if (definition.length != 1) {
            throw new IllegalStateException();
        }

        return new TypeString(identifier, TypeString.GENERIC_DEFINITION, definition);
    }

    public static TypeString ofGenericReference(final String identifier) {
        return new TypeString(identifier, TypeString.GENERIC_REFERENCE);
    }

    /**
     * Create a {@link TypeString} of a parameter reference.
     * @param identifier Name of parameter.
     * @return {@link TypeString}.
     */
    public static TypeString ofParameterRef(final String identifier) {
        return new TypeString(identifier, PARAMETER);
    }

    /**
     * Create a {@link TypeString} of an identifier, possibly with generic definitions.
     * @param identifier Identifier of type.
     * @param currentPakkage Current package.
     * @param generics Generics to embed.
     * @return {@link TypeString}.
     */
    public static TypeString ofIdentifier(
            final String identifier,
            final String currentPakkage,
            final TypeString... generics) {
        return new TypeString(identifier, currentPakkage, generics);
    }

    /**
     * Create a {@link TypeString} of a combination.
     * @param currentPakkage Current package.
     * @param combinations Types to combine.
     * @return {@link TypeString}.
     */
    public static TypeString ofCombination(final String currentPakkage, final TypeString... combinations) {
        return new TypeString(currentPakkage, combinations);
    }

    /**
     * Get package of type string, otherwise package it was defined in.
     * @return Package.
     */
    public String getPakkage() {
        if (this.isSingle()
            && this.string.contains(":")) {
            final String[] parts = this.string.split(":");
            return parts[0];
        }

        return this.currentPackage;
    }

    /**
     * Get the identifier of this TypeString.
     * Will strip package if needed.
     * @return Identifier.
     */
    public String getIdentifier() {
        if (!this.isSingle()) {
            throw new IllegalStateException();
        }

        if (this.string.contains(":")) {
            final String[] parts = this.string.split(":");
            return parts[1];
        }

        return this.string;
    }

    /**
     * Get the raw string.
     */
    public String getString() {
        return this.string;
    }

    /**
     * Get full string.
     * @return
     */
    public String getFullString() {
        if (this.isCombined()) {
            return this.combinedTypes.stream()
                .map(TypeString::getFullString)
                .sorted()
                .collect(Collectors.joining(TypeStringGrammar.Punctuator.TYPE_COMBINATOR.getValue()));
        }

        if (this.isUndefined()
            || this.isSelf()) {
            return this.string;
        }

        if (this.isGenericDefinition()) {
            return TypeStringGrammar.Punctuator.TYPE_GENERIC_OPEN.getValue()
                + this.string + TypeStringGrammar.Punctuator.TYPE_GENERIC_ASSIGN.getValue()
                + this.generics.get(0).getFullString()
                + TypeStringGrammar.Punctuator.TYPE_GENERIC_CLOSE.getValue();
        }

        if (this.isGenericReference()) {
            return TypeStringGrammar.Punctuator.TYPE_GENERIC_OPEN.getValue()
                + this.string
                + TypeStringGrammar.Punctuator.TYPE_GENERIC_CLOSE.getValue();
        }

        final String genericDefs = !this.generics.isEmpty()
            ? this.generics.stream()
                .map(typeStr -> {
                    if (typeStr.isGenericReference()) {
                        return typeStr.getIdentifier();
                    } else if (typeStr.isGenericDefinition()) {
                        return typeStr.string
                            + TypeStringGrammar.Punctuator.TYPE_GENERIC_ASSIGN.getValue()
                            + typeStr.generics.get(0).getFullString();
                    }

                    throw new IllegalStateException();
                })
                .collect(Collectors.joining(
                    TypeStringGrammar.Punctuator.TYPE_GENERIC_SEPARATOR.getValue(),
                    TypeStringGrammar.Punctuator.TYPE_GENERIC_OPEN.getValue(),
                    TypeStringGrammar.Punctuator.TYPE_GENERIC_CLOSE.getValue()))
            : "";

        if (this.string.contains(":")) {
            return this.string + genericDefs;
        }

        return this.currentPackage + ":" + this.string + genericDefs;
    }

    /**
     * Test if this type is undefined.
     * @return {@code true} if this type is undefined.
     */
    public boolean isUndefined() {
        return !this.isCombined()
            && this.getString().equalsIgnoreCase(UndefinedType.SERIALIZED_NAME);
    }

    /**
     * Test if this type contains an undefined type.
     * @return {@code true} if this type contains an undefined type.
     */
    public boolean containsUndefined() {
        if (this.isCombined()) {
            return this.combinedTypes.stream()
                .anyMatch(TypeString::containsUndefined);
        }

        return this.isUndefined();
    }

    public boolean isSelf() {
        return !this.isCombined()
            && this.getString().equalsIgnoreCase(SelfType.SERIALIZED_NAME);
    }

    public boolean isSingle() {
        return this.combinedTypes.isEmpty();
    }

    public boolean isCombined() {
        return !this.combinedTypes.isEmpty();
    }

    public boolean isGenericDefinition() {
        return TypeString.GENERIC_DEFINITION.equalsIgnoreCase(this.currentPackage);
    }

    public boolean isGenericReference() {
        return TypeString.GENERIC_REFERENCE.equalsIgnoreCase(this.currentPackage);
    }

    public boolean hasGenerics() {
        return !this.generics.isEmpty();
    }

    public boolean isParameterReference() {
        return TypeString.PARAMETER.equalsIgnoreCase(this.currentPackage);
    }

    /**
     * Get type without generic.
     * @return Bare type without any generics.
     */
    public TypeString getWithoutGenerics() {
        if (!this.hasGenerics()) {
            return this;
        }

        return TypeString.ofIdentifier(this.getIdentifier(), this.getPakkage());
    }

    /**
     * Get types, in order, used for generics.
     * @return Types of generics.
     */
    public List<TypeString> getGenerics() {
        return this.generics;
    }

    /**
     * Get parts of (combined) string.
     */
    public List<TypeString> getCombinedTypes() {
        return Collections.unmodifiableList(this.combinedTypes);
    }

    /**
     * Get reference parameter.
     * @return Referenced parameter.
     */
    public String getReferencedParameter() {
        if (!this.isParameterReference()) {
            throw new IllegalStateException();
        }

        return this.string;
    }

    /**
     * Substitype a {@link TypeString}, or return self.
     * @param from {@link TypeString} to substitute.
     * @param to {@link TypeString} to replace with.
     * @return Replaced {@link TypeString} if {@code from} matches, or this.
     */
    public TypeString substituteType(final TypeString from, final TypeString to) {
        if (from.equals(this)) {
            return to;
        }

        if (this.isCombined()) {
            final TypeString[] combinedSubstitutedArr = this.combinedTypes.stream()
                .map(typeString -> typeString.substituteType(from, to))
                .collect(Collectors.toList())
                .toArray(TypeString[]::new);
            return TypeString.ofCombination(this.currentPackage, combinedSubstitutedArr);
        }

        if (this.hasGenerics()) {
            final TypeString[] genericsSubstitutedArr = this.generics.stream()
                .map(genTypeStr -> {
                    if (!genTypeStr.isGenericReference()) {
                        return genTypeStr;
                    }

                    final String identifier = genTypeStr.getIdentifier();
                    final TypeString subbedTypeStr = genTypeStr.substituteType(from, to);
                    return TypeString.ofGenericDefinition(identifier, subbedTypeStr);
                })
                .collect(Collectors.toList())
                .toArray(TypeString[]::new);
            return TypeString.ofIdentifier(this.string, this.currentPackage, genericsSubstitutedArr);
        }

        return this;
    }

    @Override
    public int hashCode() {
        if (this.isCombined()) {
            return Objects.hash(this.combinedTypes);
        }

        // Hash the bare type, without a package.
        final int index = this.string.indexOf(":");
        final String str = index == -1
            ? this.string
            : this.string.substring(index + 1);
        return Objects.hash(str);
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

        final TypeString other = (TypeString) obj;
        return Objects.equals(this.getFullString(), other.getFullString());
    }

    @Override
    public String toString() {
        return String.format(
            "%s@%s(%s)",
            this.getClass().getName(), Integer.toHexString(this.hashCode()),
            this.getFullString());
    }

    @Override
    public int compareTo(final TypeString other) {
        return this.getFullString().compareTo(other.getFullString());
    }

}
