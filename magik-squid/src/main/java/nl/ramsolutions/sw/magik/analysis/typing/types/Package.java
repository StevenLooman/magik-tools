package nl.ramsolutions.sw.magik.analysis.typing.types;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.analysis.typing.ITypeKeeper;

/**
 * Smallworld Package.
 */
public class Package {

    private final String moduleName;
    private final String name;
    private final Set<String> uses = ConcurrentHashMap.newKeySet();
    private final Map<String, AbstractType> types = new ConcurrentHashMap<>();
    private final ITypeKeeper typeKeeper;
    private Location location;
    private String doc;

    /**
     * Constructor.
     * @param name Name of package.
     */
    public Package(
            final ITypeKeeper typeKeeper,
            final @Nullable Location location,
            final @Nullable String moduleName,
            final String name) {
        this.typeKeeper = typeKeeper;
        this.location = location;
        this.moduleName = moduleName;
        this.name = name;

        // Add self to TypeKeeper.
        this.typeKeeper.addPackage(this);
    }

    @CheckForNull
    public String getModuleName() {
        return this.moduleName;
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
     * Add a use of another package.
     * @param pakkageName Other package.
     */
    public void addUse(final String pakkageName) {
        this.uses.add(pakkageName);
    }

    /**
     * Clear uses.
     */
    public void clearUses() {
        this.uses.clear();
    }

    /**
     * Get used packages.
     * @return Used packages.
     */
    public Set<String> getUses() {
        return Collections.unmodifiableSet(this.uses);
    }

    /**
     * Get all used packages.
     * @return All used packages.
     */
    public Set<Package> getUsesPackages() {
        return this.uses.stream()
            .map(this.typeKeeper::getPackage)
            .filter(Objects::nonNull)
            .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * See if {@code key} is defined in this package or a used package.
     * @param key Key to check.
     * @return true if defined in this or any used package, false otherwise.
     */
    public boolean containsKey(final String key) {
        return this.get(key) != null;
    }

    /**
     * See if {@code key} is defined in this package.
     * @param key Key to check.
     * @return true if defined in this, false otherwise.
     */
    public boolean containsKeyLocal(final String key) {
        return this.types.containsKey(key);
    }

    /**
     * Test if type is contained by this package.
     * @param type Type to check.
     * @return True if contained, false otherwise.
     */
    public boolean containsTypeLocal(final AbstractType type) {
        final String identifier = type.getTypeString().getIdentifier();
        return this.types.containsKey(identifier)
            && this.types.get(identifier).equals(type);
    }

    /**
     * Get a {@link AbstractType} defined in this package.
     * This also includes used packages.
     * @param key Key to get.
     * @return {@link AbstractType} if found, otherwise null.
     */
    @CheckForNull
    public AbstractType get(final String key) {
        if (!this.types.containsKey(key)) {
            for (final Package usedPackage : this.getUsesPackages()) {
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
     * Put all types.
     * @param newTypes Types to add.
     */
    public void putAll(final Map<String, AbstractType> newTypes) {
        this.types.putAll(newTypes);
    }

    /**
     * Get all types in this package.
     * @return All types in this pacakge.
     */
    public Map<String, AbstractType> getTypes() {
        return Collections.unmodifiableMap(this.types);
    }

    /**
     * Remove a type from this package.
     * @param type Type to remove.
     */
    public void remove(final AbstractType type) {
        final String identifier = type.getTypeString().getIdentifier();
        this.types.remove(identifier);
    }

    /**
     * Set method documentation.
     * @param comment Method doc.
     */
    public void setDoc(final String comment) {
        this.doc = comment;
    }

    /**
     * Get method documentation.
     * @return Method doc.
     */
    @CheckForNull
    public String getDoc() {
        return this.doc;
    }

    @Override
    public String toString() {
        final String usesParts = this.getUsesPackages().stream()
                .map(Package::getName)
                .collect(Collectors.joining(","));
        return String.format(
                "%s@%s(%s, uses: %s)",
                this.getClass().getName(), Integer.toHexString(this.hashCode()),
                this.getName(),
                usesParts);
    }

}
