package nl.ramsolutions.sw.magik.analysis.typing.types;

import edu.umd.cs.findbugs.annotations.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Container to hold resulting {@link AbstractType}s for an EXPRESSION node.
 */
public class ExpressionResult {

    /**
     * Stream collector.
     */
    public static final Collector<AbstractType, ?, ExpressionResult> COLLECTOR = Collector.of(
        ArrayList<AbstractType>::new,
        List::add,
        (list, values) -> {
            list.addAll(values);
            return list;
        },
        ExpressionResult::new);

    /**
     * Instance of {@link ExpressionResult} to be used in all cases of undefined expression results.
     */
    public static final ExpressionResult UNDEFINED = new ExpressionResult(
        Collections.nCopies(1024, UndefinedType.INSTANCE));    // 1024 is max for _scatter

    private static final int MAX_ITEMS = 1024;

    private final List<AbstractType> types;

    /**
     * Empty result constructor.
     */
    public ExpressionResult() {
        this(Collections.emptyList());
    }

    /**
     * Array/utility constructor.
     */
    public ExpressionResult(final AbstractType... types) {
        this(List.of(types));
    }

    /**
     * List constructor.
     * @param types Types this {@link ExpressionResult} represents.
     */
    public ExpressionResult(final List<AbstractType> types) {
        this.types = Collections.unmodifiableList(types);
    }

    /**
     * Combine constructor.
     * @param result1 First {@link ExpressionResult}.
     * @param result2 Second {@link ExpressionResult}.
     * @param unsetType Unset type, used for filler.
     */
    public ExpressionResult(
            final ExpressionResult result1,
            final @Nullable ExpressionResult result2,
            final AbstractType unsetType) {
        if (result2 == null) {
            this.types = result1.getTypes();
        } else {
            final int size = Math.max(result1.size(), result2.size());
            final List<AbstractType> combinedTypes = new ArrayList<>(size);
            for (int i = 0; i < size; ++i) {
                final AbstractType type1 = result1.get(i, unsetType);
                final AbstractType type2 = result2.get(i, unsetType);
                final AbstractType combinedType = CombinedType.combine(type1, type2);
                combinedTypes.add(combinedType);
            }

            this.types = Collections.unmodifiableList(combinedTypes);
        }
    }

    /**
     * Test if is empty.
     * @return True if empty, false otherwise.
     */
    public boolean isEmpty() {
        return this.types.isEmpty();
    }

    /**
     * Test if this contains any {@link UndefinedType}.
     * @return True if this contains any {@link UndefinedType}, false otherwise.
     */
    public boolean containsUndefined() {
        if (this == ExpressionResult.UNDEFINED) {
            return true;
        }

        return this.stream()
            .anyMatch(type -> type == UndefinedType.INSTANCE);
    }

    /**
     * Get types.
     * @return
     */
    public List<AbstractType> getTypes() {
        return Collections.unmodifiableList(this.types);
    }

    /**
     * Get type at index.
     * @param index Index of type.
     * @return Type at index.
     */
    public AbstractType get(final int index, final @Nullable AbstractType unsetType) {
        if (this.types.isEmpty()
            || index >= this.types.size()) {
            return unsetType;
        }

        return this.types.get(index);
    }

    public int size() {
        return this.types.size();
    }

    /**
     * Substitue {@code from} by {@code to} in a copy of self.
     * @param from To substitute.
     * @param to To substitute with.
     * @return New {@link ExpressionResult}.
     */
    public ExpressionResult substituteType(final AbstractType from, final AbstractType to) {
        return this.types.stream()
            .map(type -> type.substituteType(from, to))
            .collect(ExpressionResult.COLLECTOR);
    }

    /**
     * Get type names of all items of the result.
     * @return Type names of items of the result.
     */
    public String getTypeNames(final String separator) {
        if (this == ExpressionResult.UNDEFINED) {
            return "UNDEFINED...";
        }

        // Determine first index of trailing homogenous sequence.
        int firstRepeatingIndex = MAX_ITEMS;
        AbstractType lastType = null;
        if (this.types.size() == MAX_ITEMS) {
            lastType = this.get(firstRepeatingIndex - 1, null);
            for (int i = this.types.size() - 1; i > -1; --i) {
                AbstractType type = this.types.get(i);
                if (type.equals(lastType)) {
                    firstRepeatingIndex = i;
                } else {
                    break;
                }
            }
        }

        final StringBuilder builder = new StringBuilder();
        final String typesStr = this.types.stream()
            .limit(firstRepeatingIndex)
            .map(AbstractType::getFullName)
            .collect(Collectors.joining(separator));
        builder.append(typesStr);

        // If a trailing sequence was found, append one with three dots.
        if (lastType != null) {
            if (!typesStr.isEmpty()) {
                builder.append(separator);
            }

            builder
                .append(lastType.getFullName())
                .append("...");
        }
        return builder.toString();
    }

    /**
     * Get stream of types contained by this {@link ExpressionResult}.
     * @return Stream of types.
     */
    public Stream<AbstractType> stream() {
        return this.types.stream();
    }

    @Override
    public String toString() {
        return String.format(
            "%s@%s(%s)",
            this.getClass().getName(), Integer.toHexString(this.hashCode()),
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

        final ExpressionResult other = (ExpressionResult) obj;
        return Objects.equals(this.types, other.types);
    }

}
