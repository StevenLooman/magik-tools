package nl.ramsolutions.sw.magik.analysis.typing.types;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import nl.ramsolutions.sw.magik.analysis.Location;

/**
 * Smallworld Package.
 */
public class Package {

    private final String name;
    private final Set<Package> uses = new HashSet<>();
    private final Map<String, AbstractType> types = new HashMap<>();
    private Location location;

    /**
     * Constructor.
     * @param name Name of package.
     */
    public Package(final String name) {
        this.name = name;

        this.setLocation(null);
    }

    /**
     * Get the name of the package.
     * @return Name of package.
     */
    public String getName() {
        return this.name;
    }

    /**
     * Get the location where the package is defined.
     * @return Location of package definition.
     */
    @CheckForNull
    public Location getLocation() {
        return this.location;
    }

    /**
     * Set the location where the package is defined.
     * @param location Location of package definition.
     */
    public void setLocation(final @Nullable Location location) {
        this.location = location;
    }

    /**
     * Add a use of another package.
     * @param usePackage Other package.
     */
    public void addUse(final Package usePackage) {
        this.uses.add(usePackage);
    }

    /**
     * Get all used packages.
     * @return All used packages.
     */
    public Set<Package> uses() {
        return Collections.unmodifiableSet(this.uses);
    }

    /**
     * See if {{key}} is defined in this package.
     * This also includes used packages.
     * @param key Key to check.
     * @return true if defined in this package, false otherwise.
     */
    public boolean containsKey(final String key) {
        if (!this.types.containsKey(key)) {
            for (final Package usedPackage : this.uses) {
                if (usedPackage.containsKey(key)) {
                    return true;
                }
            }

            return false;
        }

        return true;
    }

    /**
     * Get a {{AbstractType}} defined in this package.
     * This also includes used packages.
     * @param key Key to get.
     * @return {{AbstractType}} if found, otherwise null.
     */
    @CheckForNull
    public AbstractType get(final String key) {
        if (!this.types.containsKey(key)) {
            for (final Package usedPackage : this.uses) {
                final AbstractType type = usedPackage.get(key);
                if (type != null) {
                    return type;
                }
            }
        }

        return this.types.get(key);
    }

    /**
     * Put a type associated with key.
     * @param key Key of type.
     * @param type Type.
     */
    public void put(final String key, final AbstractType type) {
        this.types.put(key, type);
    }

    /**
     * Get all types in this package.
     * @return All types in this pacakge.
     */
    public Map<String, AbstractType> getTypes() {
        return Collections.unmodifiableMap(this.types);
    }

    /**
     * Test if type is contained by this package.
     * @param type Type to check.
     * @return True if contained, false otherwise.
     */
    public boolean containsTypeLocal(final AbstractType type) {
        final String typeName = type.getName();
        return this.types.containsKey(typeName)
            && this.types.get(typeName).equals(type);
    }

    /**
     * Remove an identifier from this package.
     * @param identifier Identifier to remove.
     */
    public void remove(final String identifier) {
        this.types.remove(identifier);
    }

    /**
     * Remove a type from this package.
     * @param type Type to remove.
     */
    public void remove(final AbstractType type) {
        final String identifier = type.getName();
        this.remove(identifier);
    }

    @Override
    public String toString() {
        final String usesParts = this.uses().stream()
                .map(Package::getName)
                .collect(Collectors.joining(","));
        return String.format(
                "%s@%s(%s, uses: %s)",
                this.getClass().getName(), Integer.toHexString(this.hashCode()),
                this.getName(),
                usesParts);
    }

}
