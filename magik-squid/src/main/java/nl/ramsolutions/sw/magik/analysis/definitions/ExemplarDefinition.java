package nl.ramsolutions.sw.magik.analysis.definitions;

import com.sonar.sslr.api.AstNode;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;

/** Exemplar definition. */
public class ExemplarDefinition extends MagikDefinition implements ITypeStringDefinition {

  /** Exemplar sort. */
  @SuppressWarnings("checkstyle:JavadocVariable")
  public enum Sort {
    UNDEFINED,
    SLOTTED,
    INDEXED,
    INTRINSIC,
    OBJECT;
  }

  private final Sort sort;
  private final TypeString typeName;
  private final List<SlotDefinition> slots;
  private final List<TypeString> parents;
  private final Set<String> topics;

  /**
   * Constructor.
   *
   * @param moduleName Name of module where this is defined.
   * @param node Node for definition.
   * @param sort Type of exemplar.
   * @param typeName Name of slotted exemplar.
   * @param slots Slots of slotted exemplar.
   * @param parents Parents of slotted exemplar.
   * @param topics Topics.
   */
  @SuppressWarnings({"checkstyle:ParameterNumber", "java:S107"})
  public ExemplarDefinition(
      final @Nullable Location location,
      final @Nullable Instant timestamp,
      final @Nullable String moduleName,
      final @Nullable String doc,
      final @Nullable AstNode node,
      final Sort sort,
      final TypeString typeName,
      final List<SlotDefinition> slots,
      final List<TypeString> parents,
      final Set<String> topics) {
    super(location, timestamp, moduleName, doc, node);

    if (!typeName.isSingle()) {
      throw new IllegalStateException();
    }

    this.sort = sort;
    this.typeName = typeName;
    this.slots = List.copyOf(slots);
    this.parents = List.copyOf(parents);
    this.topics = Set.copyOf(topics);
  }

  public List<SlotDefinition> getSlots() {
    return Collections.unmodifiableList(this.slots);
  }

  /**
   * Get slot by name.
   *
   * @param name Name of slot.
   * @return Slot.
   */
  @CheckForNull
  public SlotDefinition getSlot(final String name) {
    return this.slots.stream().filter(slot -> slot.getName().equals(name)).findAny().orElse(null);
  }

  public List<TypeString> getParents() {
    return Collections.unmodifiableList(this.parents);
  }

  public Set<String> getTopics() {
    return Collections.unmodifiableSet(this.topics);
  }

  @Override
  public TypeString getTypeString() {
    return this.typeName;
  }

  public Sort getSort() {
    return this.sort;
  }

  @Override
  public String getName() {
    return this.typeName.getFullString();
  }

  @Override
  public String getPackage() {
    return this.typeName.getPakkage();
  }

  @Override
  public ExemplarDefinition getWithoutNode() {
    return new ExemplarDefinition(
        this.getLocation(),
        this.getTimestamp(),
        this.getModuleName(),
        this.getDoc(),
        null,
        this.sort,
        this.typeName,
        this.slots.stream().map(SlotDefinition::getWithoutNode).toList(),
        this.parents,
        Collections.emptySet());
  }

  @Override
  public String toString() {
    return String.format(
        "%s@%s(%s)",
        this.getClass().getName(),
        Integer.toHexString(this.hashCode()),
        this.getTypeString().getFullString());
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        this.getLocation(),
        this.getTimestamp(),
        this.getModuleName(),
        this.getDoc(),
        this.sort,
        this.typeName,
        this.slots,
        this.parents);
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

    final ExemplarDefinition other = (ExemplarDefinition) obj;
    return Objects.equals(this.getLocation(), other.getLocation())
        && Objects.equals(this.getModuleName(), other.getModuleName())
        && Objects.equals(this.getDoc(), other.getDoc())
        && Objects.equals(this.sort, other.sort)
        && Objects.equals(this.typeName, other.typeName)
        && Objects.equals(this.slots, other.slots)
        && Objects.equals(this.parents, other.parents);
  }
}
