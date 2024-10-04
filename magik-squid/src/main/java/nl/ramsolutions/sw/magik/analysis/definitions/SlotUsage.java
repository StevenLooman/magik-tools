package nl.ramsolutions.sw.magik.analysis.definitions;

import com.sonar.sslr.api.AstNode;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.util.Objects;
import nl.ramsolutions.sw.magik.Location;

/** Slot usage. */
public class SlotUsage {

  private final String slotName;
  private final @Nullable Location location;
  private final @Nullable AstNode node;

  /**
   * Constructor.
   *
   * @param slotName Name of slot.
   * @param location Location of use.
   */
  public SlotUsage(
      final String slotName, final @Nullable Location location, final @Nullable AstNode node) {
    this.slotName = slotName;
    this.location = location;
    this.node = node;
  }

  /**
   * Constructor.
   *
   * @param slotName Name of slot.
   */
  public SlotUsage(final String slotName) {
    this(slotName, null, null);
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
    return new SlotUsage(this.slotName, this.location, null);
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
    return Objects.equals(other.getSlotName(), this.getSlotName());
  }
}
