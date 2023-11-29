package nl.ramsolutions.sw.magik.analysis.definitions;

import com.sonar.sslr.api.AstNode;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import nl.ramsolutions.sw.magik.Location;

/**
 * Package definition.
 */
public class PackageDefinition extends Definition {

    private final String name;
    private final List<String> uses;

    /**
     * Constructor.
     * @param moduleName Module where this package is defined.
     * @param node Node of package definition.
     * @param name Name of package.
     * @param uses Uses by package.
     */
    public PackageDefinition(
            final @Nullable Location location,
            final @Nullable String moduleName,
            final @Nullable AstNode node,
            final String name,
            final List<String> uses,
            final String doc) {
        super(location, moduleName, node, doc);
        this.name = name;
        this.uses = List.copyOf(uses);
    }

    @Override
    public String getName() {
        return this.name;
    }

    public List<String> getUses() {
        return Collections.unmodifiableList(this.uses);
    }

    @Override
    public String getPackage() {
        return this.name;
    }

    @Override
    public String toString() {
        return String.format(
            "%s@%s(%s)",
            this.getClass().getName(), Integer.toHexString(this.hashCode()),
            this.getName());
    }

}
