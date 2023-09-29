package nl.ramsolutions.sw.magik.analysis.typing.types;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import nl.ramsolutions.sw.magik.api.MagikKeyword;
import nl.ramsolutions.sw.magik.api.TypeStringGrammar;

/**
 * Type string, containing package name and identifier.
 * Examples:
 * - {@code "sw:rope"}
 * - {@code "sw:char16_vector|sw:symbol|sw:unset"}
 * - {@code "_undefined"}
 * - {@code "_self|sw:unset"}
 * - {@code "sw:rope<sw:integer>"}
 */
public final class TypeString {

    @SuppressWarnings("checkstyle:JavadocVariable")
    public static final String DEFAULT_PACKAGE = "user";
    public static final String SW_PACKAGE = "sw";

    @SuppressWarnings("checkstyle:JavadocVariable")
    public static final TypeString UNDEFINED = new TypeString(UndefinedType.SERIALIZED_NAME, DEFAULT_PACKAGE);
    @SuppressWarnings("checkstyle:JavadocVariable")
    public static final TypeString SELF = new TypeString(SelfType.SERIALIZED_NAME, DEFAULT_PACKAGE);
    @SuppressWarnings("checkstyle:JavadocVariable")
    public static final TypeString SW_UNSET = TypeString.ofIdentifier(MagikKeyword.UNSET.getValue(), SW_PACKAGE);

    private static final String GENERIC = "_generic";
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
     * Create a {@link TypeString} of a generic.
     * @param identifier Name of generic.
     * @return {@link TypeString}.
     */
    public static TypeString ofGeneric(final String identifier) {
        return new TypeString(identifier, TypeString.GENERIC);
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
    public static TypeString ofCombination(
            final String currentPakkage,
            final TypeString... combinations) {
        return new TypeString(currentPakkage, combinations);
    }

    /**
     * Get package of type string, otherwise package it was defined in.
     * @return Package.
     */
    public String getPakkage() {
        if (!this.isSingle()) {
            throw new IllegalStateException();
        }

        if (this.string.contains(":")) {
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

        final String genericDefs = !this.generics.isEmpty()
            ? this.generics.stream()
                .map(TypeString::getFullString)
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

    public boolean isGeneric() {
        return TypeStringGrammar.Keyword.TYPE_STRING_GENERIC.getValue().equalsIgnoreCase(this.currentPackage);
    }

    public boolean isGenericParametered() {
        return !this.generics.isEmpty();
    }

    /**
     * Get type without generic.
     * @return Bare type without any generics.
     */
    public TypeString getWithoutGenerics() {
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

    public boolean isParameterReference() {
        return TypeStringGrammar.Keyword.TYPE_STRING_PARAMETER.getValue().equalsIgnoreCase(this.currentPackage);
    }

    /**
     * Get reference parameter.
     * @return Referenced parameter.
     */
    public String referencedParameter() {
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
            "%s@%s(%s, %s)",
            this.getClass().getName(), Integer.toHexString(this.hashCode()),
            this.currentPackage, this.string);
    }

}
