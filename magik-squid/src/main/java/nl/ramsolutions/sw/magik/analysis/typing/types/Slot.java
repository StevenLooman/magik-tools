package nl.ramsolutions.sw.magik.analysis.typing.types;

import java.util.Objects;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import nl.ramsolutions.sw.magik.Location;

/**
 * Slot.
 */
public class Slot {

    private final Location location;
    private final String name;
    private final TypeString type;

    /**
     * Constructor.
     * @param location Location where this slot is defined.
     * @param name Name of this slot.
     */
    public Slot(final @Nullable Location location, final String name, final TypeString type) {
        this.location = location;
        this.name = name;
        this.type = type;
    }

    /**
     * Get the location of the slot.
     * @return Location of the slot.
     */
    @CheckForNull
    public Location getLocation() {
        return this.location;
    }

    /**
     * Get the name of the slot.
     * @return Name of the slot.
     */
    public String getName() {
        return this.name;
    }

    /**
     * Get the type of the slot.
     */
    public TypeString getType() {
        return this.type;
    }

    @Override
    public String toString() {
        return String.format(
            "%s@%s(%s)",
            this.getClass().getName(), Integer.toHexString(this.hashCode()),
            this.getName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.location, this.name, this.type);
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

        final Slot other = (Slot) obj;
        return Objects.equals(this.location, other.location)
            && Objects.equals(this.name, other.name)
            && Objects.equals(this.type, other.type);
    }

}
