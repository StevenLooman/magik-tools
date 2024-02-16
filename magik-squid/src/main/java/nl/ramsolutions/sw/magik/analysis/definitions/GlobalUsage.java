package nl.ramsolutions.sw.magik.analysis.definitions;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.util.Objects;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.analysis.typing.types.TypeString;

/**
 * Global usage.
 */
public class GlobalUsage {

    private final TypeString typeName;
    private final Location location;

    /**
     * Constructor.
     * @param typeName Type name.
     * @param location Location of use.
     */
    public GlobalUsage(final TypeString typeName, final @Nullable Location location) {
        this.typeName = typeName;
        this.location = location;
    }

    public TypeString getTypeName() {
        return this.typeName;
    }

    @CheckForNull
    public Location getLocation() {
        return this.location;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.typeName);
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

        final GlobalUsage other = (GlobalUsage) obj;
        // Location is not tested!
        return Objects.equals(other.getTypeName(), this.getTypeName());
    }

}
