package nl.ramsolutions.sw.magik.analysis.definitions;

import edu.umd.cs.findbugs.annotations.Nullable;
import java.util.Objects;
import nl.ramsolutions.sw.magik.Location;

/**
 * Condition usage.
 */
public class ConditionUsage {

    private final String conditionName;
    private final Location location;

    /**
     * Constructor.
     * @param conditionName Name of condition.
     * @param location Location of use.
     */
    public ConditionUsage(final String conditionName, final @Nullable Location location) {
        this.conditionName = conditionName;
        this.location = location;
    }

    /**
     * Constructor.
     * @param conditionName Name of condition.
     */
    public ConditionUsage(final String conditionName) {
        this(conditionName, null);
    }

    public String getConditionName() {
        return this.conditionName;
    }

    public Location getLocation() {
        return this.location;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.conditionName);
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

        final ConditionUsage other = (ConditionUsage) obj;
        // Location is not tested!
        return Objects.equals(other.getConditionName(), this.getConditionName());
    }

}
