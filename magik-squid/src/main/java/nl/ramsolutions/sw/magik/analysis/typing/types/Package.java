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
     * Get all types in this package.
     * @return All types in this pacakge.
     */
    public Map<String, AbstractType> getTypes() {
        return this.typeKeeper.getTypes().stream()
            .filter(type -> type.getTypeString().getPakkage().equals(this.name))
            .collect(Collectors.toMap(
                type -> type.getName(),
                type -> type));
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
