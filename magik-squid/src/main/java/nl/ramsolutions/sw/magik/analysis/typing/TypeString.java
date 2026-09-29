package nl.ramsolutions.sw.magik.analysis.typing;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
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
  public static final TypeString INVOKABLE =
      TypeString.ofIdentifier("_invokable", ANONYMOUS_PACKAGE);

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final TypeString PRIVATE = TypeString.ofIdentifier("_private", ANONYMOUS_PACKAGE);

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

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final String SIGNATURE_PARAMETERS = "P";

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final String SIGNATURE_RESULTS = "R";

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final String SIGNATURE_LOOPS = "L";

  private static final String GENERIC_DEFINITION = "_generic_def";
  private static final String GENERIC_REFERENCE = "_generic_ref";
  private static final String PARAMETER = "_parameter";
  private static final String SLOT = "_slot";
  private static final String COMBINED = "_combined";
  private static final String VARIADIC = "_variadic";
  private static final String TUPLE = "_tuple";

  private final @Nullable String string;
  private final String currentPackage;
  private final List<TypeString> combinedTypes;
  private final List<TypeString> generics;
  private final @Nullable TypeString genericType;

  /**
   * Constructor.
   *
   * @param identifier Identifier, e.g., {@code "sw:rope"}.
   * @param currentPackage The current package, e.g., {@code "user"}.
   * @param genericType Generic type, when this is a generic definition.
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
        .filter(gen -> !gen.isGenericDefinition() && !gen.isGenericReference())
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
    final String identifier = this.getIdentifier();
    final String pakkage = this.getPakkage();
    return new TypeString(identifier, pakkage, this.genericType, genericDefinitions);
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
    return new TypeString(identifier, TypeString.PARAMETER, null);
  }

  /**
   * Create a {@link TypeString} of a parameter reference carrying a generic projection suffix (e.g.
   * {@code _parameter(values)<E>}).
   *
   * @param identifier Name of parameter.
   * @param genericReference The generic reference being projected.
   * @return {@link TypeString}.
   */
  public static TypeString ofParameterRef(
      final String identifier, final TypeString genericReference) {
    return new TypeString(identifier, TypeString.PARAMETER, null, genericReference);
  }

  /**
   * Create a {@link TypeString} of a slot reference.
   *
   * @param identifier Name of slot.
   * @return {@link TypeString}.
   */
  public static TypeString ofSlotRef(final String identifier) {
    return new TypeString(identifier, TypeString.SLOT, null);
  }

  /**
   * Create a {@link TypeString} of a slot reference carrying a generic projection suffix (e.g.
   * {@code _slot(my_slot)<E>}).
   *
   * @param identifier Name of slot.
   * @param genericReference The generic reference being projected.
   * @return {@link TypeString}.
   */
  public static TypeString ofSlotRef(final String identifier, final TypeString genericReference) {
    return new TypeString(identifier, TypeString.SLOT, null, genericReference);
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
   * Create a variadic {@link TypeString} wrapping an inner type. Variadic only makes sense as the
   * tail of an {@link ExpressionResultString}.
   *
   * @param inner Inner type. Must not itself be variadic.
   * @return Variadic {@link TypeString}.
   */
  public static TypeString ofVariadic(final TypeString inner) {
    if (inner.isVariadic()) {
      throw new IllegalArgumentException("Variadic cannot wrap another variadic");
    }

    return new TypeString(TypeString.VARIADIC, inner);
  }

  /**
   * Create an {@code _invokable} — the invoke() protocol marker — carrying signature generics.
   *
   * @param signatureGenerics P/R/L generic definitions.
   * @return {@link TypeString}.
   */
  public static TypeString ofInvokable(final TypeString... signatureGenerics) {
    final String identifier = TypeString.INVOKABLE.getIdentifier();
    final String pakkage = TypeString.INVOKABLE.getPakkage();
    return TypeString.ofIdentifier(identifier, pakkage, signatureGenerics);
  }

  /**
   * Create an ordered tuple {@link TypeString} (a signature-generic value, e.g. {@code P=[a, b]}).
   *
   * @param elements Elements, in order. Only the last may be variadic; none may be a tuple.
   * @return Tuple {@link TypeString}.
   */
  public static TypeString ofTuple(final TypeString... elements) {
    for (int i = 0; i < elements.length; ++i) {
      if (elements[i].isTuple()) {
        throw new IllegalArgumentException("Tuple cannot nest a tuple");
      }
      if (elements[i].isVariadic() && i != elements.length - 1) {
        throw new IllegalArgumentException("Variadic element must be last");
      }
    }
    return new TypeString(TypeString.TUPLE, elements);
  }

  public boolean isTuple() {
    return TypeString.TUPLE.equalsIgnoreCase(this.currentPackage);
  }

  /**
   * Get the elements of this tuple, in order.
   *
   * @return Elements.
   * @throws IllegalStateException if this is not a tuple.
   */
  public List<TypeString> getTupleTypes() {
    if (!this.isTuple()) {
      throw new IllegalStateException();
    }
    return Collections.unmodifiableList(this.combinedTypes);
  }

  /**
   * Convert this tuple to an {@link ExpressionResultString}.
   *
   * @return {@link ExpressionResultString} with this tuple's elements.
   */
  public ExpressionResultString toExpressionResultString() {
    final List<TypeString> elements = this.getTupleTypes();
    return new ExpressionResultString(elements);
  }

  /**
   * Create a tuple from an {@link ExpressionResultString}.
   *
   * @param expressionResultString Result string to convert.
   * @return Tuple {@link TypeString}.
   */
  public static TypeString ofExpressionResultString(
      final ExpressionResultString expressionResultString) {
    final TypeString[] elements = expressionResultString.stream().toArray(TypeString[]::new);
    return TypeString.ofTuple(elements);
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
   * @return Full string.
   */
  public String getFullString() {
    if (this.isVariadic()) {
      return this.getVariadicInner().getFullString()
          + TypeStringGrammar.Punctuator.TYPE_VARIADIC.getValue();
    }

    if (this.isCombined()) {
      return this.combinedTypes.stream()
          .map(TypeString::getFullString)
          .sorted()
          .collect(Collectors.joining(TypeStringGrammar.Punctuator.TYPE_COMBINATOR.getValue()));
    }

    if (this.isTuple()) {
      return this.combinedTypes.stream()
          .map(TypeString::getFullString)
          .collect(
              Collectors.joining(
                  TypeStringGrammar.Punctuator.TYPE_SEPARATOR.getValue(),
                  TypeStringGrammar.Punctuator.TYPE_TUPLE_OPEN.getValue(),
                  TypeStringGrammar.Punctuator.TYPE_TUPLE_CLOSE.getValue()));
    }

    if (this.isUndefined()) {
      return this.string;
    }

    if (this.isSelf() || this.isInvokable()) {
      return this.string + this.getGenericDefinitionsFullString();
    }

    if (this.isParameterReference() || this.isSlotReference()) {
      return this.getReferenceFullString();
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

    final String genericDefs = this.getGenericDefinitionsFullString();

    if (this.string.contains(":")) {
      return this.string + genericDefs;
    }

    return this.currentPackage + ":" + this.string + genericDefs;
  }

  private String getReferenceFullString() {
    final String base =
        this.currentPackage
            + TypeStringGrammar.Punctuator.TYPE_ARG_OPEN.getValue()
            + this.string
            + TypeStringGrammar.Punctuator.TYPE_ARG_CLOSE.getValue();
    final TypeString genericRef = this.getReferenceGeneric();
    if (genericRef == null) {
      return base;
    }
    return base
        + TypeStringGrammar.Punctuator.TYPE_GENERIC_OPEN.getValue()
        + genericRef.getIdentifier()
        + TypeStringGrammar.Punctuator.TYPE_GENERIC_CLOSE.getValue();
  }

  private String getGenericDefinitionsFullString() {
    if (this.generics.isEmpty()) {
      return "";
    }

    return this.generics.stream()
        .map(TypeString::getSingleGenericFullString)
        .collect(
            Collectors.joining(
                TypeStringGrammar.Punctuator.TYPE_GENERIC_SEPARATOR.getValue(),
                TypeStringGrammar.Punctuator.TYPE_GENERIC_OPEN.getValue(),
                TypeStringGrammar.Punctuator.TYPE_GENERIC_CLOSE.getValue()));
  }

  private String getSingleGenericFullString() {
    if (this.isGenericReference()) {
      return this.getIdentifier();
    }

    if (this.isGenericDefinition()) {
      return this.string
          + TypeStringGrammar.Punctuator.TYPE_GENERIC_ASSIGN.getValue()
          + this.genericType.getFullString();
    }

    throw new IllegalStateException();
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
    if (this.isVariadic()) {
      return this.getVariadicInner().containsUndefined();
    }

    if (this.isCombined()) {
      return this.combinedTypes.stream().anyMatch(TypeString::containsUndefined);
    }

    return this.isUndefined();
  }

  public boolean isSelf() {
    return !this.isCombined() && TypeString.SELF.getIdentifier().equalsIgnoreCase(this.string);
  }

  public boolean isInvokable() {
    final String invokableIdentifier = TypeString.INVOKABLE.getIdentifier();
    return !this.isCombined()
        && !this.isTuple()
        && invokableIdentifier.equalsIgnoreCase(this.string);
  }

  public boolean isPrivate() {
    return !this.isCombined() && TypeString.PRIVATE.getIdentifier().equalsIgnoreCase(this.string);
  }

  public boolean isSingle() {
    return this.combinedTypes.isEmpty() && !this.isTuple();
  }

  public boolean isCombined() {
    return !this.combinedTypes.isEmpty() && !this.isVariadic() && !this.isTuple();
  }

  public boolean isVariadic() {
    return TypeString.VARIADIC.equalsIgnoreCase(this.currentPackage);
  }

  /**
   * Get the inner type wrapped by this variadic.
   *
   * @return Inner type.
   * @throws IllegalStateException if this is not variadic.
   */
  public TypeString getVariadicInner() {
    if (!this.isVariadic()) {
      throw new IllegalStateException();
    }

    return this.combinedTypes.get(0);
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

  public boolean isSlotReference() {
    return TypeString.SLOT.equalsIgnoreCase(this.currentPackage);
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

    final String identifier = this.getIdentifier();
    final String pakkage = this.getPakkage();
    return new TypeString(identifier, pakkage, this.genericType);
  }

  /**
   * Get types, in order, used for generics.
   *
   * @return Generic definitions.
   */
  public List<TypeString> getGenerics() {
    return Collections.unmodifiableList(this.generics);
  }

  /**
   * Return a copy of this type with {@code type} unioned into the generic named {@code name},
   * adding the generic definition if it is not yet present. Other generics are preserved in order.
   *
   * @param name Generic name (e.g. {@code "E"}).
   * @param type Type to union into that generic.
   * @return New type string with the merged generic.
   */
  public TypeString withMergedGeneric(final String name, final TypeString type) {
    final TypeString reference = TypeString.ofGenericReference(name);
    final List<TypeString> newGenerics = new ArrayList<>();
    boolean found = false;
    for (final TypeString generic : this.generics) {
      if (generic.isGenericDefinition() && generic.getGenericReference().equals(reference)) {
        final TypeString merged = TypeString.combine(generic.getGenericType(), type);
        newGenerics.add(TypeString.ofGenericDefinition(name, merged));
        found = true;
      } else {
        newGenerics.add(generic);
      }
    }
    if (!found) {
      newGenerics.add(TypeString.ofGenericDefinition(name, type));
    }
    return TypeString.ofIdentifier(
        this.getIdentifier(), this.getPakkage(), newGenerics.toArray(TypeString[]::new));
  }

  /**
   * Return a copy of this type with the generic named {@code name} bound to {@code type}, replacing
   * any existing binding for that name (unlike {@link #withMergedGeneric}, which unions). Other
   * generics are preserved in order.
   *
   * @param name Generic name (e.g. {@code "E"}).
   * @param type Type to bind that generic to.
   * @return New type string with the (re)defined generic.
   */
  public TypeString withGenericDefinition(final String name, final TypeString type) {
    final TypeString withoutName = this.withoutGeneric(name);
    return withoutName.withMergedGeneric(name, type);
  }

  /**
   * Return a copy of this type with the generic named {@code name} removed (reset to unbound).
   *
   * @param name Generic name to remove.
   * @return New type string without that generic.
   */
  public TypeString withoutGeneric(final String name) {
    final TypeString reference = TypeString.ofGenericReference(name);
    final TypeString[] newGenerics =
        this.generics.stream()
            .filter(
                generic ->
                    !(generic.isGenericDefinition()
                        && generic.getGenericReference().equals(reference)))
            .toArray(TypeString[]::new);
    return TypeString.ofIdentifier(this.getIdentifier(), this.getPakkage(), newGenerics);
  }

  /**
   * Get the generic for the given generic reference.
   *
   * @param genericReference The generic reference to search for.
   * @return The found generic if found, null otherwise.
   */
  @CheckForNull
  public TypeString getGenericDefinition(final TypeString genericReference) {
    return this.generics.stream()
        .filter(generic -> generic.getGenericReference().equals(genericReference))
        .findAny()
        .orElse(null);
  }

  /**
   * Get the type bound to the given generic reference, or {@code fallbackTypeStr} when this type
   * binds none: it either declares no such generic, or declares it as a bare reference ({@code
   * <E>}), which binds no type.
   *
   * @param genericReference The generic reference to search for.
   * @param fallbackTypeStr Type to return when no type is bound to the reference.
   * @return The bound type, or {@code fallbackTypeStr}.
   */
  @CheckForNull
  public TypeString getGenericValue(
      final TypeString genericReference, final @Nullable TypeString fallbackTypeStr) {
    final TypeString genericDefinition = this.getGenericDefinition(genericReference);
    if (genericDefinition == null) {
      return fallbackTypeStr;
    }

    final TypeString genericTypeStr = genericDefinition.getGenericType();
    return genericTypeStr != null ? genericTypeStr : fallbackTypeStr;
  }

  /**
   * Get the types declared for a signature generic ({@code P}/{@code R}/{@code L}), normalizing the
   * single-value sugar ({@code R=sw:integer} reads as {@code R=[sw:integer]}).
   *
   * @param signatureGenericName Signature generic name.
   * @return Declared types in order; empty for a declared-empty tuple; {@code null} when absent.
   */
  @CheckForNull
  public List<TypeString> getSignatureTypes(final String signatureGenericName) {
    final TypeString reference = TypeString.ofGenericReference(signatureGenericName);
    final TypeString genericDefinition = this.getGenericDefinition(reference);
    if (genericDefinition == null) {
      return null;
    }

    final TypeString value = genericDefinition.getGenericType();
    if (value == null) {
      return null;
    }

    if (value.isTuple()) {
      return value.getTupleTypes();
    }

    return List.of(value);
  }

  /**
   * For a parameter/slot/result reference carrying a projection suffix (e.g. {@code
   * _parameter(x)<E>}), get the generic reference being projected. {@code null} for a bare
   * reference or a non-reference.
   *
   * @return The carried generic reference, or {@code null}.
   */
  @CheckForNull
  public TypeString getReferenceGeneric() {
    if (!this.isReference()) {
      return null;
    }
    return this.generics.stream().filter(TypeString::isGenericReference).findFirst().orElse(null);
  }

  /**
   * Get the reference of the generic.
   *
   * @return Generic reference.
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
    if (this.combinedTypes.isEmpty() || this.isVariadic() || this.isTuple()) {
      return List.of(this);
    }

    return Collections.unmodifiableList(this.combinedTypes);
  }

  /**
   * Collect all suffixed parameter references ({@code _parameter(name)<G>}) reachable within this
   * type string — i.e. parameter references with a non-null {@link #getReferenceGeneric()}. Bare
   * {@code _parameter(name)} references (no suffix) are not included.
   *
   * @return Set of suffixed parameter references found anywhere in this type (across union members,
   *     generic definition values, and variadic inner types).
   */
  public Set<TypeString> collectParameterRefsWithGeneric() {
    final Set<TypeString> result = new HashSet<>();
    this.collectRefsInto(result, TypeString::isParameterRefWithGeneric);
    return result;
  }

  /**
   * Test if this is a signature projection: a parameter reference whose projection suffix names a
   * signature generic ({@code _parameter(name)<P>}, {@code _parameter(name)<R>} or {@code
   * _parameter(name)<L>}).
   *
   * @return True if a signature projection.
   */
  public boolean isSignatureProjection() {
    if (!this.isParameterReference()) {
      return false;
    }

    final TypeString referenceGeneric = this.getReferenceGeneric();
    if (referenceGeneric == null) {
      return false;
    }

    final String genericName = referenceGeneric.getIdentifier();
    return TypeString.SIGNATURE_PARAMETERS.equals(genericName)
        || TypeString.SIGNATURE_RESULTS.equals(genericName)
        || TypeString.SIGNATURE_LOOPS.equals(genericName);
  }

  /**
   * Collect all signature projections reachable within this type string.
   *
   * @return Set of signature projections.
   */
  public Set<TypeString> collectSignatureProjections() {
    final Set<TypeString> result = new HashSet<>();
    this.collectRefsInto(result, TypeString::isSignatureProjection);
    return result;
  }

  /**
   * Collect all reference markers ({@code _parameter(name)} and {@code _slot(name)}, suffixed and
   * bare) reachable within this type string.
   *
   * @return Set of references found anywhere in this type (across union members, generic definition
   *     values, and variadic inner types).
   */
  public Set<TypeString> collectReferences() {
    final Set<TypeString> result = new HashSet<>();
    this.collectRefsInto(result, TypeString::isReference);
    return result;
  }

  private static boolean isParameterRefWithGeneric(final TypeString typeString) {
    if (!typeString.isParameterReference()) {
      return false;
    }

    final TypeString referenceGeneric = typeString.getReferenceGeneric();
    return referenceGeneric != null;
  }

  private boolean isReference() {
    return this.isParameterReference() || this.isSlotReference();
  }

  private void collectRefsInto(final Set<TypeString> result, final Predicate<TypeString> matcher) {
    if (this.isVariadic()) {
      final TypeString inner = this.getVariadicInner();
      inner.collectRefsInto(result, matcher);
      return;
    }
    if (this.isCombined()) {
      for (final TypeString member : this.combinedTypes) {
        member.collectRefsInto(result, matcher);
      }
      return;
    }
    if (this.isTuple()) {
      for (final TypeString element : this.combinedTypes) {
        element.collectRefsInto(result, matcher);
      }
      return;
    }
    if (this.isReference()) {
      // A reference only ever carries a generic reference suffix, never a value to recurse into.
      if (matcher.test(this)) {
        result.add(this);
      }
      return;
    }

    this.collectRefsInGenericsInto(result, matcher);
  }

  private void collectRefsInGenericsInto(
      final Set<TypeString> result, final Predicate<TypeString> matcher) {
    // Recurse into generic definition values (e.g. E=_parameter(other)<E>).
    for (final TypeString generic : this.generics) {
      if (generic.isGenericDefinition()) {
        final TypeString value = generic.getGenericType();
        if (value != null) {
          value.collectRefsInto(result, matcher);
        }
      }
    }
  }

  /**
   * Substitype a {@link TypeString}, or return self.
   *
   * @param from {@link TypeString} to substitute.
   * @param to {@link TypeString} to replace with.
   * @return Replaced {@link TypeString} if {@code from} matches, or this.
   */
  public TypeString substituteType(final TypeString from, final TypeString to) {
    if (from.equals(TypeString.SELF) && this.isSelf() && this.hasGenerics()) {
      return this.rebindSelfGenerics(to);
    }

    if (from.equals(this)) {
      return to;
    }

    if (this.isReference()) {
      return this;
    }

    if (this.isVariadic()) {
      return TypeString.ofVariadic(this.getVariadicInner().substituteType(from, to));
    }

    if (this.isTuple()) {
      final TypeString[] elementsSubstituted =
          this.combinedTypes.stream()
              .map(elementTypeStr -> elementTypeStr.substituteType(from, to))
              .toArray(TypeString[]::new);
      return TypeString.ofTuple(elementsSubstituted);
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
                    if (genTypeStr.isGenericReference()) {
                      final TypeString subbedRef = genTypeStr.substituteType(from, to);
                      return TypeString.ofGenericDefinition(genTypeStr.getIdentifier(), subbedRef);
                    }
                    if (genTypeStr.isGenericDefinition()) {
                      final TypeString value = genTypeStr.getGenericType();
                      final TypeString subbedValue =
                          value == null ? null : value.substituteType(from, to);
                      return TypeString.ofGenericDefinition(
                          genTypeStr.getIdentifier(), subbedValue);
                    }
                    return genTypeStr;
                  })
              .toList()
              .toArray(TypeString[]::new);
      return TypeString.ofIdentifier(this.string, this.currentPackage, genericsSubstitutedArr);
    }

    return this;
  }

  private TypeString rebindSelfGenerics(final TypeString to) {
    if (to.isCombined()) {
      final TypeString[] reboundMembers =
          to.combinedTypes.stream().map(this::rebindSelfGenerics).toArray(TypeString[]::new);
      return TypeString.combine(reboundMembers);
    }

    if (to.isUndefined() || to.isSelf()) {
      return to;
    }

    TypeString rebound = to;
    for (final TypeString generic : this.getGenerics()) {
      if (!generic.isGenericDefinition()) {
        continue;
      }
      final String name = generic.getIdentifier();
      final TypeString value = generic.getGenericType();
      rebound = rebound.withGenericDefinition(name, value);
    }
    return rebound;
  }

  @Override
  public int hashCode() {
    return this.getFullString().hashCode();
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
    return "%s@%s(%s)"
        .formatted(
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

    return TypeString.combine(intersection.toArray(TypeString[]::new));
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

    return TypeString.combine(difference.toArray(TypeString[]::new));
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
            .distinct()
            .collect(Collectors.toUnmodifiableSet());
    if (combinedTypes.size() == 1) {
      return combinedTypes.stream().findFirst().orElseThrow();
    }

    return TypeString.ofCombination(combinedTypes.toArray(TypeString[]::new));
  }
}
