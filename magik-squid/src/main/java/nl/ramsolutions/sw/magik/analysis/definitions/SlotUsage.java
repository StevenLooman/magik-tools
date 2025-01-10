package nl.ramsolutions.sw.magik.analysis.definitions;

import com.sonar.sslr.api.AstNode;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.util.Objects;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;

/** Slot usage. */
public class SlotUsage {

  private final TypeString typeName;
  private final String slotName;
  private final @Nullable Location location;
  private final @Nullable AstNode node;

  /**
   * Constructor.
   *
   * @param typeName Type name.
   * @param slotName Name of slot.
   * @param location Location of use.
   * @param node Node of use.
   */
  public SlotUsage(
      final TypeString typeName,
      final String slotName,
      final @Nullable Location location,
      final @Nullable AstNode node) {
    this.typeName = typeName;
    this.slotName = slotName;
    this.location = location;
    this.node = node;
  }

  /**
   * Constructor.
   *
   * @param typeName Type name.
   * @param slotName Name of slot.
   */
  public SlotUsage(final TypeString typeName, final String slotName) {
    this(typeName, slotName, null, null);
  }

  public TypeString getTypeName() {
    return this.typeName;
  }

  public String getSlotName() {
    return this.slotName;
  }

  @CheckForNull
  public Location getLocation() {
    return this.location;
  }

  @CheckForNull
  public AstNode getNode() {
    return this.node;
  }

  public SlotUsage getWithoutNode() {
    return new SlotUsage(this.typeName, this.slotName, this.location, null);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.slotName);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (obj == null) {
      return false;
    }

    if (this.getClass() != obj.getClass()) {
      return false;
    }

    final SlotUsage other = (SlotUsage) obj;
    // Location is not tested!
    return Objects.equals(other.getTypeName(), this.getTypeName())
        && Objects.equals(other.getSlotName(), this.getSlotName());
  }
}
