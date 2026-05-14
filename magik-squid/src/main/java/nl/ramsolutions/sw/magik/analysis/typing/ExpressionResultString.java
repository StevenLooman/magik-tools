package nl.ramsolutions.sw.magik.analysis.typing;

import edu.umd.cs.findbugs.annotations.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Container to hold resulting {@link TypeString}s. */
public class ExpressionResultString {

  /** Stream collector. */
  public static final Collector<TypeString, ?, ExpressionResultString> COLLECTOR =
      Collector.of(
          ArrayList<TypeString>::new,
          List::add,
          (list, values) -> {
            list.addAll(values);
            return list;
          },
          ExpressionResultString::new);

  /** Max number of items in a {@link ExpressionResultString}. */
  public static final int MAX_ITEMS = 1024; // 1024 is max for _scatter

  /**
   * Instance of {@link ExpressionResultString} to be used in all cases of undefined expression
   * results.
   */
  public static final ExpressionResultString UNDEFINED =
      new ExpressionResultString(Collections.nCopies(MAX_ITEMS, TypeString.UNDEFINED));

  /** Serialized name of {@link ExpressionResultString.UNDEFINED}. */
  public static final String UNDEFINED_SERIALIZED_NAME = "__UNDEFINED_RESULT__";

  /** Instance of an empty {@link ExpressionResultString}. */
  public static final ExpressionResultString EMPTY = new ExpressionResultString();

  private final List<TypeString> types;

  /** Empty result constructor. */
  public ExpressionResultString() {
    this(Collections.emptyList());
  }

  /** Array/utility constructor. */
  public ExpressionResultString(final TypeString... types) {
    this(List.of(types));
  }

  /**
   * List constructor.
   *
   * @param types Types this {@link ExpressionResult} represents.
   * @throws IllegalArgumentException if {@code types.size() > MAX_ITEMS}.
   */
  public ExpressionResultString(final List<TypeString> types) {
    if (types.size() > MAX_ITEMS) {
      throw new IllegalArgumentException(
          "ExpressionResultString cannot hold more than %d types".formatted(MAX_ITEMS));
    }

    this.types = Collections.unmodifiableList(types);
  }

  /** Combine constructor. */
  public ExpressionResultString(
      final ExpressionResultString result1, final @Nullable ExpressionResultString result2) {
    if (result2 == null) {
      this.types = result1.getTypes();
    } else {
      final int size = Math.max(result1.size(), result2.size());
      final List<TypeString> combinedTypes = new ArrayList<>(size);
      for (int i = 0; i < size; ++i) {
        final TypeString type1 = result1.get(i, TypeString.SW_UNSET);
        final TypeString type2 = result2.get(i, TypeString.SW_UNSET);
        final TypeString combinedType = TypeString.combine(type1, type2);
        combinedTypes.add(combinedType);
      }

      this.types = Collections.unmodifiableList(combinedTypes);
    }
  }

  /**
   * Test if is empty.
   *
   * @return True if empty, false otherwise.
   */
  public boolean isEmpty() {
    return this.types.isEmpty();
  }

  /**
   * Get types.
   *
   * @return Type strings.
   */
  public List<TypeString> getTypes() {
    return Collections.unmodifiableList(this.types);
  }

  /**
   * Get type at index.
   *
   * @param index Index of type.
   * @return Type at index.
   */
  public TypeString get(final int index, final @Nullable TypeString unsetType) {
    if (this.types.isEmpty() || index >= this.types.size()) {
      return unsetType;
    }

    return this.types.get(index);
  }

  public int size() {
    return this.types.size();
  }

  /**
   * Materialize this result into exactly {@code n} per-position types. A trailing variadic (if any)
   * is expanded into its inner type unioned with {@link TypeString#SW_UNSET}, variadic declares
   * 0..N values, so any position may be unfilled. Positions beyond the declared (non-variadic) size
   * are filled with {@link TypeString#SW_UNSET}.
   *
   * <p>Note: {@link ExpressionResultString#UNDEFINED} is the flat {@code nCopies(MAX_ITEMS,
   * UNDEFINED)} form (no variadic tail), so it materializes to bare {@code UNDEFINED} per position.
   * The variadic form {@code _undefined...} (a distinct value) materializes per the uniform rule
   * above to {@code UNDEFINED | sw:unset} per position.
   *
   * @param n Number of positions to produce.
   * @return List of {@code n} types.
   */
  public List<TypeString> materialize(final int n) {
    final int capped = Math.min(n, MAX_ITEMS);
    final List<TypeString> result = new ArrayList<>(capped);
    final int lastIdx = this.types.size() - 1;
    final boolean tailVariadic = !this.types.isEmpty() && this.types.get(lastIdx).isVariadic();
    final int leadingCount = tailVariadic ? lastIdx : this.types.size();
    final TypeString variadicPositionType =
        tailVariadic
            ? TypeString.combine(this.types.get(lastIdx).getVariadicInner(), TypeString.SW_UNSET)
            : null;

    for (int i = 0; i < capped; ++i) {
      if (i < leadingCount) {
        result.add(this.types.get(i));
      } else if (tailVariadic) {
        result.add(variadicPositionType);
      } else {
        result.add(TypeString.SW_UNSET);
      }
    }

    return Collections.unmodifiableList(result);
  }

  /**
   * Substitute {@code from} by {@code to} in a copy of self.
   *
   * @param from From type.
   * @param to To type.
   * @return New {@link ExpressionResultString}.
   */
  public ExpressionResultString substituteType(final TypeString from, final TypeString to) {
    return this.types.stream()
        .map(typeString -> typeString.substituteType(from, to))
        .collect(ExpressionResultString.COLLECTOR);
  }

  /**
   * Get type names of all items of the result.
   *
   * @return Type names of items of the result.
   */
  public String getTypeNames(final String separator) {
    if (this.equals(ExpressionResultString.UNDEFINED)) {
      return ExpressionResultString.UNDEFINED_SERIALIZED_NAME;
    }
    return this.types.stream()
        .map(TypeString::getFullString)
        .collect(Collectors.joining(separator));
  }

  /**
   * Get stream of types contained by this {@link ExpressionResult}.
   *
   * @return Stream of types.
   */
  public Stream<TypeString> stream() {
    return this.types.stream();
  }

  /**
   * Get full string.
   *
   * @return Full string.
   */
  public String getFullString() {
    if (this == UNDEFINED) {
      return UNDEFINED_SERIALIZED_NAME;
    }

    return this.types.stream().map(TypeString::getFullString).collect(Collectors.joining(","));
  }

  @Override
  @SuppressWarnings("checkstyle:NestedIfDepth")
  public String toString() {
    return "%s@%s(%s)"
        .formatted(
            this.getClass().getName(),
            Integer.toHexString(this.hashCode()),
            this.getTypeNames(","));
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.types.toArray());
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

    final ExpressionResultString other = (ExpressionResultString) obj;
    return Objects.equals(this.types, other.types);
  }
}
