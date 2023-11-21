package nl.ramsolutions.sw.definitions;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import nl.ramsolutions.sw.magik.Location;

/**
 * Smallworld product.
 */
public class SwProductDefinition {

    private final @Nullable Location location;
    private final String name;
    private final @Nullable String version;
    private final @Nullable String versionComment;
    private final Set<SwProductDefinition> children = new HashSet<>();
    private final Set<SwModuleDefinition> modules = new HashSet<>();

    /**
     * Constructor.
     * @param location
     * @param name
     * @param version
     * @param versionComment
     */
    public SwProductDefinition(
            final @Nullable Location location,
            final String name,
            final @Nullable String version,
            final @Nullable String versionComment) {
        this.name = name;
        this.location = location;
        this.version = version;
        this.versionComment = versionComment;
    }

    /**
     * Get name of product.
     * @return Name of product.
     */
    public String getName() {
        return this.name;
    }

    /**
     * Get path to {@code product.def} file.
     * @return Path to {@code product.def} file.
     */
    @CheckForNull
    public Location getLocation() {
        return this.location;
    }

    @CheckForNull
    public String getVersion() {
        return this.version;
    }

    @CheckForNull
    public String getVersionComment() {
        return this.versionComment;
    }

    /**
     * Get child {@link SwProductDefinition}s of this product.
     * @return Child {@link SwProductDefinition}s of this product.
     */
    public Set<SwProductDefinition> getChildren() {
        return Collections.unmodifiableSet(this.children);
    }

    /**
     * Add a {@link SwProductDefinition} to this product.
     * @param swProduct {@link SwProductDefinition} to add.
     */
    public void addChild(final SwProductDefinition swProduct) {
        this.children.add(swProduct);
    }

    /**
     * Get {@link SwModuleDefinition}s in this product.
     * @return Collection of {@link SwModuleDefinition}s in this product.
     */
    public Set<SwModuleDefinition> getModules() {
        return Collections.unmodifiableSet(modules);
    }

    /**
     * Add a {@link SwModuleDefinition} to this product.
     * @param swModule {@link SwModuleDefinition} to add.
     */
    public void addModule(final SwModuleDefinition swModule) {
        this.modules.add(swModule);
    }

    @Override
    public String toString() {
        return String.format(
            "%s@%s(%s, %s, %s)",
            this.getClass().getName(), Integer.toHexString(this.hashCode()),
            this.getName(), this.getVersion(), this.getVersionComment());
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

        final SwProductDefinition otherSwProduct = (SwProductDefinition) obj;
        return Objects.equals(otherSwProduct.getLocation(), this.getLocation())
            && Objects.equals(otherSwProduct.getName(), this.getName())
            && Objects.equals(otherSwProduct.getVersion(), this.getVersion())
            && Objects.equals(otherSwProduct.getVersionComment(), this.getVersionComment());
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.location, this.name, this.version, this.versionComment);
    }

}
