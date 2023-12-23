package nl.ramsolutions.sw.magik.analysis.definitions;

import java.util.Objects;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import nl.ramsolutions.sw.magik.Location;

/**
 * Slot usage.
 */
@Immutable
public class SlotUsage {

    private final String slotName;
    private final Location location;

    /**
     * Constructor.
     * @param slotName Name of slot.
     * @param location Location of use.
     */
    public SlotUsage(final String slotName, final @Nullable Location location) {
        this.slotName = slotName;
        this.location = location;
    }

    /**
     * Constructor.
     * @param slotName Name of slot.
     */
    public SlotUsage(final String slotName) {
        this(slotName, null);
    }

    public String getSlotName() {
        return this.slotName;
    }

    public Location getLocation() {
        return this.location;
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
