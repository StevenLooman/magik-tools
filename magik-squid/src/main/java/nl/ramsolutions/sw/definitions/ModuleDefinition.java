package nl.ramsolutions.sw.definitions;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import nl.ramsolutions.sw.magik.Location;

/**
 * Smallworld module.
 */
public class ModuleDefinition {

    private final @Nullable Location location;
    private final String name;
    private final String baseVersion;
    private final @Nullable String currentVersion;
    private final List<String> requireds;

    /**
     * Constructor.
     * @param name Name of module.
     * @param location {@link Location} of the module definition.
     */
    public ModuleDefinition(
            final @Nullable Location location,
            final String name,
            final String baseVersion,
            final @Nullable String currentVersion,
            final List<String> requireds) {
        this.location = location;
        this.name = name;
        this.baseVersion = baseVersion;
        this.currentVersion = currentVersion;
        this.requireds = List.copyOf(requireds);
    }

    public String getName() {
        return this.name;
    }

    @CheckForNull
    public Location getLocation() {
        return this.location;
    }

    public String getBaseVersion() {
        return this.baseVersion;
    }

    public String getCurrentVersion() {
        return this.currentVersion;
    }

    public List<String> getRequireds() {
        return Collections.unmodifiableList(this.requireds);
    }

    @Override
    public String toString() {
        return String.format(
            "%s@%s(%s, %s, %s)",
            this.getClass().getName(), Integer.toHexString(this.hashCode()),
            this.getName(), this.getBaseVersion(), this.getCurrentVersion());
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

        final ModuleDefinition otherSwModule = (ModuleDefinition) obj;
        return Objects.equals(otherSwModule.getLocation(), this.getLocation())
            && Objects.equals(otherSwModule.getName(), this.getName())
            && Objects.equals(otherSwModule.getBaseVersion(), this.getBaseVersion())
            && Objects.equals(otherSwModule.getCurrentVersion(), this.getCurrentVersion());
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.location, this.name, this.baseVersion, this.currentVersion);
    }

}
