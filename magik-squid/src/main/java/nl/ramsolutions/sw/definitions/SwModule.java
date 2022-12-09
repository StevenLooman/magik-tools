package nl.ramsolutions.sw.definitions;

import java.nio.file.Path;
import java.util.Objects;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

/**
 * Smallworld module.
 */
public class SwModule {

    /**
     * Module definition filename.
     */
    public static final String SW_MODULE_DEF = "module.def";

    private final String name;
    private final Path path;

    /**
     * Constructor.
     * @param name Name of module.
     * @param path Path to {@code module.def} file.
     */
    public SwModule(final String name, final @Nullable Path path) {
        this.name = name;
        this.path = path;
    }

    public String getName() {
        return this.name;
    }

    @CheckForNull
    public Path getPath() {
        return this.path;
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

        final SwModule otherSwModule = (SwModule) obj;
        return Objects.equals(otherSwModule.getPath(), this.getPath());
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.path);
    }

}
