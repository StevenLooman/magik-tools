package nl.ramsolutions.sw.definitions;

import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

/**
 * Smallworld product.
 */
public class SwProduct {

    /**
     * Product definition filename.
     */
    public static final String SW_PRODUCT_DEF = "product.def";

    private final String name;
    private final Path path;
    private final Set<SwProduct> children = new HashSet<>();
    private final Set<SwModule> modules = new HashSet<>();

    public SwProduct(final String name, final @Nullable Path path) {
        this.name = name;
        this.path = path;
    }

    /**
     * Get name of product.
     * @return Name of product.
     */
    public String getName() {
        return this.name;
    }

    /**
     * Get path to {{@code product.def}} file.
     * @return Path to {{@code product.def}} file.
     */
    @CheckForNull
    public Path getPath() {
        return this.path;
    }

    /**
     * Get child {{@code SwProduct}}s of this product.
     * @return Child {{@code SwProduct}}s of this product.
     */
    public Set<SwProduct> getChildren() {
        return Collections.unmodifiableSet(this.children);
    }

    /**
     * Add a {{@code SwProduct}} to this product.
     * @param swProduct {{@code SwProduct}} to add.
     */
    public void addChild(final SwProduct swProduct) {
        this.children.add(swProduct);
    }

    /**
     * Get {{@code SwModule}}s in this product.
     * @return Collection of {{@code SwModule}}s in this product.
     */
    public Set<SwModule> getModules() {
        return Collections.unmodifiableSet(modules);
    }

    /**
     * Add a {{@code SwModule}} to this product.
     * @param swModule {{@code SwModule}} to add.
     */
    public void addModule(final SwModule swModule) {
        this.modules.add(swModule);
    }

    @Override
    public String toString() {
        return String.format(
            "%s@%s(%s, %s)",
            this.getClass().getName(), Integer.toHexString(this.hashCode()),
            this.getName(), this.getPath());
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

        final SwProduct otherSwProduct = (SwProduct) obj;
        return Objects.equals(otherSwProduct.getPath(), this.getPath());
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.path);
    }

}
