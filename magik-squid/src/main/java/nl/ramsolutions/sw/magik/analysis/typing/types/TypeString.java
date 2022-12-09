package nl.ramsolutions.sw.magik.analysis.typing.types;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import nl.ramsolutions.sw.magik.api.MagikKeyword;
import nl.ramsolutions.sw.magik.api.NewDocGrammar;

/**
 * Type string, containing package name and identifier.
 * Examples:
 * - {@code "sw:rope"}
 * - {@code "sw:char16_vector|sw:symbol|sw:unset"}
 * - {@code "_undefined"}
 * - {@code "_self|sw:unset"}
 */
public final class TypeString {

    @SuppressWarnings("checkstyle:JavadocVariable")
    public static final TypeString UNDEFINED = new TypeString(UndefinedType.SERIALIZED_NAME);
    @SuppressWarnings("checkstyle:JavadocVariable")
    public static final TypeString SELF = new TypeString(SelfType.SERIALIZED_NAME);

    @SuppressWarnings("checkstyle:JavadocVariable")
    public static final String TYPE_COMBINATOR = NewDocGrammar.Punctuator.TYPE_COMBINATOR.getValue();

    /**
     * Stream collector.
     */
    public static final Collector<TypeString, ?, TypeString> COLLECTOR = Collector.of(
        ArrayList<TypeString>::new,
        (list, value) -> list.add(value),
        (list, values) -> {
            list.addAll(values);
            return list;
        },
        list -> {
            final String str = list.stream()
                .map(TypeString::getString)
                .sorted()
                .collect(Collectors.joining(TYPE_COMBINATOR));
            return new TypeString(str);
        });

    private static final String TYPE_COMBINATOR_RE = Pattern.quote(TYPE_COMBINATOR);
    private static final String DEFAULT_PACKAGE = "user";

    private final String string;
    private final String currentPackage;

    /**
     * Constructor.
     * @param string Type string, e.g., {@code "sw:rope"}.
     */
    public TypeString(final String string) {
        this.string = string.trim();
        this.currentPackage = DEFAULT_PACKAGE;
    }

    /**
     * Constructor.
     * @param identifier Identifier, e.g., {@code "sw:rope"}.
     * @param currentPackage The current package, e.g., {@code "user"}.
     */
    public TypeString(final String identifier, final String currentPackage) {
        this.string = identifier.trim();
        this.currentPackage = currentPackage.trim();
    }

    /**
     * Static constructor, for readability.
     * @param string String, e.g., {@code "sw:rope"}, or {@code "sw:symbol|sw:unset"}.
     * @return New type string.
     */
    public static TypeString of(final String string) {
        return new TypeString(string);
    }

    /**
     * Static constructor, for readability.
     * @param string Identifier, e.g., {@code "rope"}.
     * @param currentPackage Package name, e.g., {@code "sw"}.
     * @return Type strring..
     */
    public static TypeString of(final String string, final String currentPackage) {
        return new TypeString(string, currentPackage);
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
        if (this.string.contains(":")) {
            return this.string;
        }

        return this.currentPackage + ":" + this.string;
    }

    public boolean isUndefined() {
        return this.getString().equalsIgnoreCase(UndefinedType.SERIALIZED_NAME);
    }

    public boolean isSelf() {
        return this.getString().equals(SelfType.SERIALIZED_NAME)
               || this.getString().equalsIgnoreCase(MagikKeyword.CLONE.getValue());
    }

    public boolean isSingle() {
        return !this.getString().contains(TYPE_COMBINATOR);
    }

    /**
     * Get parts of (combined) string.
     */
    public List<TypeString> parts() {
        final String[] parts = this.string.split(TYPE_COMBINATOR_RE);
        return Stream.of(parts)
            .map(String::trim)
            .map(part -> TypeString.of(part, this.currentPackage))
            .collect(Collectors.toList());
    }

    public boolean isParameterReference() {
        return this.string.toLowerCase().startsWith("_parameter");
    }

    /**
     * Get reference parameter.
     * @return Referenced parameter.
     */
    public String referencedParameter() {
        if (!this.isParameterReference()) {
            throw new IllegalStateException();
        }

        final int indexOpen = this.string.indexOf("(");
        final int indexClose = this.string.indexOf(")");
        if (indexOpen == -1
            || indexClose == -1) {
            // Don't crash, just
            return null;
        }

        return this.string.substring(indexOpen + 1, indexClose).trim();
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.currentPackage, this.string);
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
            "%s@%s(%s:%s)",
            this.getClass().getName(), Integer.toHexString(this.hashCode()),
            this.getPakkage(), this.getString());
    }

}
