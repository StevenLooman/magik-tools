package nl.ramsolutions.sw.magik.analysis.typing;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import nl.ramsolutions.sw.magik.api.TypeStringGrammar;

/**
 * Type string, containing package name and identifier. Examples: - {@code "sw:rope"} - {@code
 * "sw:char16_vector|sw:symbol|sw:unset"} - {@code "_undefined"} - {@code "_self|sw:unset"} - {@code
 * "sw:rope<E=sw:integer>"} - {@code "<E>"}
 */
public final class TypeString implements Comparable<TypeString> {

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final String DEFAULT_PACKAGE = "user";

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final String SW_PACKAGE = "sw";

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final String USER_PACKAGE = "user";

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final String ANONYMOUS_PACKAGE = "_anon"; // `_anon` package for anonymous types.

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final TypeString UNDEFINED =
      TypeString.ofIdentifier("_undefined", ANONYMOUS_PACKAGE);

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final TypeString SELF = TypeString.ofIdentifier("_self", ANONYMOUS_PACKAGE);

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final TypeString SW_UNSET = TypeString.ofIdentifier("unset", SW_PACKAGE);

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final TypeString SW_FALSE = TypeString.ofIdentifier("false", SW_PACKAGE);

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final TypeString SW_MAYBE = TypeString.ofIdentifier("maybe", SW_PACKAGE);

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final TypeString SW_CHARACTER = TypeString.ofIdentifier("character", SW_PACKAGE);

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final TypeString SW_BIGNUM = TypeString.ofIdentifier("bignum", SW_PACKAGE);

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final TypeString SW_INTEGER = TypeString.ofIdentifier("integer", SW_PACKAGE);

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final TypeString SW_FLOAT = TypeString.ofIdentifier("float", SW_PACKAGE);

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final TypeString SW_SW_REGEXP = TypeString.ofIdentifier("sw_regexp", SW_PACKAGE);

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final TypeString SW_CHAR16_VECTOR =
      TypeString.ofIdentifier("char16_vector", SW_PACKAGE);

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final TypeString SW_CHAR16_VECTOR_WITH_GENERICS =
      TypeString.ofIdentifier(
          SW_CHAR16_VECTOR.getIdentifier(),
          SW_CHAR16_VECTOR.getPakkage(),
          TypeString.ofGenericDefinition("K", TypeString.SW_INTEGER),
          TypeString.ofGenericDefinition("E", TypeString.SW_CHARACTER));

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final TypeString SW_SYMBOL = TypeString.ofIdentifier("symbol", SW_PACKAGE);

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final TypeString SW_SIMPLE_VECTOR =
      TypeString.ofIdentifier("simple_vector", SW_PACKAGE);

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final TypeString SW_HEAVY_THREAD =
      TypeString.ofIdentifier("heavy_thread", SW_PACKAGE);

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final TypeString SW_LIGHT_THREAD =
      TypeString.ofIdentifier("light_thread", SW_PACKAGE);

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final TypeString SW_GLOBAL_VARIABLE =
      TypeString.ofIdentifier("global_variable", SW_PACKAGE);

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final TypeString SW_PROCEDURE = TypeString.ofIdentifier("procedure", SW_PACKAGE);

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final TypeString SW_OBJECT = TypeString.ofIdentifier("object", SW_PACKAGE);

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final TypeString SW_CONDITION = TypeString.ofIdentifier("condition", SW_PACKAGE);

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final TypeString SW_ENUMERATION_VALUE =
      TypeString.ofIdentifier("enumeration_value", SW_PACKAGE);

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final TypeString SW_INDEXED_FORMAT_MIXIN =
      TypeString.ofIdentifier("indexed_format_mixin", SW_PACKAGE);

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final TypeString SW_SLOTTED_FORMAT_MIXIN =
      TypeString.ofIdentifier("slotted_format_mixin", SW_PACKAGE);

  private static final String GENERIC_DEFINITION = "_generic_def";
  private static final String GENERIC_REFERENCE = "_generic_ref";
  private static final String PARAMETER = "_parameter";
  private static final String COMBINED = "_combined";

  private final @Nullable String string;
  private final String currentPackage;
  private final List<TypeString> combinedTypes;
  private final List<TypeString> generics;
  private final @Nullable TypeString genericType;

  /**
   * Constructor for generics.
   *
   * @param identifier Identifier, e.g., {@code "sw:rope"}.
   * @param currentPackage The current package, e.g., {@code "user"}.
   * @param genericType Generic type.
   * @param generics Generics.
   */
  private TypeString(
      final String identifier,
      final String currentPackage,
      final @Nullable TypeString genericType,
      final TypeString... generics) {
    this.string = identifier.trim();
    this.currentPackage = currentPackage.trim();
    this.combinedTypes = Collections.emptyList();
    this.generics = Arrays.asList(generics);
    this.generics.stream()
        .filter(gen -> !gen.isGenericDefinition())
        .forEach(
            typeStr -> {
              throw new IllegalStateException();
            });
    this.genericType = genericType;
  }

  /**
   * Constructor for combined types.
   *
   * @param currentPackage The current package, e.g., {@code "user"}.
   * @param combinations Combined {@link TypeString}s.
   */
  private TypeString(final String currentPackage, final TypeString... combinations) {
    this.string = null;
    this.currentPackage = currentPackage.trim();
    this.combinedTypes = Arrays.asList(combinations);
    this.generics = Collections.emptyList();
    this.genericType = null;
  }

  /**
   * Get a copy of self, but with (new) generic definitions.
   *
   * @param genericDefinitions Generic definitions.
   * @return Copy of self, with generic definitions.
   */
  public TypeString withGenerics(final TypeString[] genericDefinitions) {
    return TypeString.ofIdentifier(this.getIdentifier(), this.getPakkage(), genericDefinitions);
  }

  /**
   * Create a {@link TypeString} of a generic definition.
   *
   * @param identifier Name of generic.
   * @param genericTypeString Type of the generic.
   * @return {@link TypeString}.
   */
  public static TypeString ofGenericDefinition(
      final String identifier, final TypeString genericTypeString) {
    return new TypeString(identifier, TypeString.GENERIC_DEFINITION, genericTypeString);
  }

  public static TypeString ofGenericReference(final String identifier) {
    return new TypeString(identifier, TypeString.GENERIC_REFERENCE, null);
  }

  /**
   * Create a {@link TypeString} of a parameter reference.
   *
   * @param identifier Name of parameter.
   * @return {@link TypeString}.
   */
  public static TypeString ofParameterRef(final String identifier) {
    return new TypeString(identifier, PARAMETER, null);
  }

  /**
   * Create a {@link TypeString} of an identifier, possibly with generic definitions.
   *
   * @param identifier Identifier of type.
   * @param currentPakkage Current package.
   * @param generics Generics to embed.
   * @return {@link TypeString}.
   */
  public static TypeString ofIdentifier(
      final String identifier, final String currentPakkage, final TypeString... generics) {
    return new TypeString(identifier, currentPakkage, null, generics);
  }

  /**
   * Create a {@link TypeString} of a combination.
   *
   * @param combinations Types to combine.
   * @return {@link TypeString}.
   */
  public static TypeString ofCombination(final TypeString... combinations) {
    Arrays.stream(combinations)
        .forEach(
            typeStr -> {
              if (typeStr.isCombined()) {
                throw new IllegalArgumentException();
              }
            });
    return new TypeString(TypeString.COMBINED, combinations);
  }

  /**
   * Get package of type string, otherwise package it was defined in.
   *
   * @return Package.
   */
  public String getPakkage() {
    if (this.isSingle() && this.string.contains(":")) {
      final String[] parts = this.string.split(":");
      return parts[0];
    }

    return this.currentPackage;
  }

  /**
   * Get the identifier of this TypeString. Will strip package if needed.
   *
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

  /** Get the raw string. */
  public String getString() {
    return this.string;
  }

  /**
   * Get full string.
   *
   * @return
   */
  public String getFullString() {
    if (this.isCombined()) {
      return this.combinedTypes.stream()
          .map(TypeString::getFullString)
          .sorted()
          .collect(Collectors.joining(TypeStringGrammar.Punctuator.TYPE_COMBINATOR.getValue()));
    }

    if (this.isUndefined() || this.isSelf()) {
      return this.string;
    }

    if (this.isGenericDefinition()) {
      return TypeStringGrammar.Punctuator.TYPE_GENERIC_OPEN.getValue()
          + this.string
          + TypeStringGrammar.Punctuator.TYPE_GENERIC_ASSIGN.getValue()
          + this.genericType.getFullString()
          + TypeStringGrammar.Punctuator.TYPE_GENERIC_CLOSE.getValue();
    }

    if (this.isGenericReference()) {
      return TypeStringGrammar.Punctuator.TYPE_GENERIC_OPEN.getValue()
          + this.string
          + TypeStringGrammar.Punctuator.TYPE_GENERIC_CLOSE.getValue();
    }

    final String genericDefs =
        !this.generics.isEmpty()
            ? this.generics.stream()
                .map(
                    typeStr -> {
                      if (typeStr.isGenericReference()) {
                        return typeStr.getIdentifier();
                      } else if (typeStr.isGenericDefinition()) {
                        return typeStr.string
                            + TypeStringGrammar.Punctuator.TYPE_GENERIC_ASSIGN.getValue()
                            + typeStr.genericType.getFullString();
                      }

                      throw new IllegalStateException();
                    })
                .collect(
                    Collectors.joining(
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
   *
   * @return {@code true} if this type is undefined.
   */
  public boolean isUndefined() {
    return !this.isCombined() && TypeString.UNDEFINED.getIdentifier().equalsIgnoreCase(this.string);
  }

  public boolean isAnonymous() {
    return TypeString.ANONYMOUS_PACKAGE.equals(this.currentPackage);
  }

  /**
   * Test if this type contains an undefined type.
   *
   * @return {@code true} if this type contains an undefined type.
   */
  public boolean containsUndefined() {
    if (this.isCombined()) {
      return this.combinedTypes.stream().anyMatch(TypeString::containsUndefined);
    }

    return this.isUndefined();
  }

  public boolean isSelf() {
    return !this.isCombined() && TypeString.SELF.getIdentifier().equalsIgnoreCase(this.string);
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
   *
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
   *
   * @return Generic definitions.
   */
  public List<TypeString> getGenerics() {
    return this.generics;
  }

  /**
   * Get the reference of the generic.
   *
   * @return
   */
  public TypeString getGenericReference() {
    if (!this.isGenericReference() && !this.isGenericDefinition()) {
      throw new IllegalStateException();
    }

    return TypeString.ofGenericReference(this.string);
  }

  /**
   * Get the type of the generic.
   *
   * @return Generic type.
   */
  @CheckForNull
  public TypeString getGenericType() {
    return this.genericType;
  }

  /** Get parts of (combined) string. */
  public List<TypeString> getCombinedTypes() {
    if (this.combinedTypes.isEmpty()) {
      return List.of(this);
    }

    return Collections.unmodifiableList(this.combinedTypes);
  }

  /**
   * Get reference parameter.
   *
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
   *
   * @param from {@link TypeString} to substitute.
   * @param to {@link TypeString} to replace with.
   * @return Replaced {@link TypeString} if {@code from} matches, or this.
   */
  public TypeString substituteType(final TypeString from, final TypeString to) {
    if (from.equals(this)) {
      return to;
    }

    if (this.isCombined()) {
      final TypeString[] combinedSubstitutedArr =
          this.combinedTypes.stream()
              .map(typeString -> typeString.substituteType(from, to))
              .toList()
              .toArray(TypeString[]::new);
      return TypeString.combine(combinedSubstitutedArr);
    }

    if (this.hasGenerics()) {
      final TypeString[] genericsSubstitutedArr =
          this.generics.stream()
              .map(
                  genTypeStr -> {
                    if (!genTypeStr.isGenericReference()) {
                      return genTypeStr;
                    }

                    final String identifier = genTypeStr.getIdentifier();
                    final TypeString subbedTypeStr = genTypeStr.substituteType(from, to);
                    return TypeString.ofGenericDefinition(identifier, subbedTypeStr);
                  })
              .toList()
              .toArray(TypeString[]::new);
      return TypeString.ofIdentifier(this.string, this.currentPackage, genericsSubstitutedArr);
    }

    return this;
  }

  @Override
  public int hashCode() {
    // Hash the bare type, without a package.
    final int index = this.string != null ? this.string.indexOf(':') : -1;
    final String str =
        this.string != null && index != -1 ? this.string.substring(index + 1) : this.string;
    return Objects.hash(this.combinedTypes, str, this.generics, this.genericType);
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
        this.getClass().getName(), Integer.toHexString(this.hashCode()), this.getFullString());
  }

  @Override
  public int compareTo(final TypeString other) {
    return this.getFullString().compareTo(other.getFullString());
  }

  /**
   * Get the intersection of two types.
   *
   * @param type1 Type 1.
   * @param type2 Type 2.
   * @return Intersection of type 1 and type 2.
   */
  @CheckForNull
  public static TypeString intersection(final TypeString type1, final TypeString type2) {
    final Set<TypeString> type1s =
        type1.isCombined() ? Set.copyOf(type1.getCombinedTypes()) : Set.of(type1);
    final Set<TypeString> type2s =
        type2.isCombined() ? Set.copyOf(type2.getCombinedTypes()) : Set.of(type2);
    final Set<TypeString> intersection =
        type1s.stream().filter(type2s::contains).collect(Collectors.toSet());
    if (intersection.isEmpty()) {
      return null;
    }

    return TypeString.ofCombination(intersection.toArray(TypeString[]::new));
  }

  /**
   * Get the difference of two types.
   *
   * @param type1 Type 1.
   * @param type2 Type 2.
   * @return Difference between type 1 and type 2.
   */
  @CheckForNull
  public static TypeString difference(final TypeString type1, final TypeString type2) {
    final Set<TypeString> type1s =
        type1.isCombined() ? Set.copyOf(type1.getCombinedTypes()) : Set.of(type1);
    final Set<TypeString> type2s =
        type2.isCombined() ? Set.copyOf(type2.getCombinedTypes()) : Set.of(type2);
    final Set<TypeString> difference =
        type1s.stream().filter(type -> !type2s.contains(type)).collect(Collectors.toSet());
    if (difference.isEmpty()) {
      return null;
    }

    return TypeString.ofCombination(difference.toArray(TypeString[]::new));
  }

  /**
   * Combine {@link TypeString}s. Any combined {@link TypeString}s will be flattened.
   *
   * @param typeStrs {@link TypeString}s to combine.
   * @return {@link TypeString} representing all types.
   */
  @CheckForNull
  public static TypeString combine(final TypeString... typeStrs) {
    if (typeStrs.length == 0) {
      return null;
    }

    final Set<TypeString> combinedTypes =
        Stream.of(typeStrs)
            .flatMap(
                typeStr -> {
                  if (typeStr.isCombined()) {
                    return typeStr.getCombinedTypes().stream();
                  }

                  return Stream.of(typeStr);
                })
            .collect(Collectors.toUnmodifiableSet());
    if (combinedTypes.size() == 1) {
      return combinedTypes.stream().findFirst().orElseThrow();
    }

    return TypeString.ofCombination(combinedTypes.toArray(TypeString[]::new));
  }
}
