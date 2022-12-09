package nl.ramsolutions.sw.magik.analysis.typing.types;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import nl.ramsolutions.sw.magik.analysis.Location;

/**
 * Slot.
 */
public class Slot {

    private final Location location;
    private final MagikType owner;
    private final String name;
    private AbstractType type = UndefinedType.INSTANCE;

    /**
     * Constructor.
     * @param location Location where this slot is defined.
     * @param owner Owner of the slot.
     * @param name Name of this slot.
     */
    public Slot(final @Nullable Location location, final MagikType owner, final String name) {
        this.location = location;
        this.owner = owner;
        this.name = name;
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
     * Get the owner of the slot.
     * @return Owner of the slot.
     */
    public MagikType getOwner() {
        return this.owner;
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
    public AbstractType getType() {
        return this.type;
    }

    /**
     * Set the type of the slot.
     * @param type New type of this slot.
     */
    public void setType(AbstractType type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return String.format(
                "%s@%s(%s.%s)",
                this.getClass().getName(), Integer.toHexString(this.hashCode()),
                this.getOwner().getFullName(), this.getName());
    }

}
