package nl.ramsolutions.sw.magik.analysis.definitions;

import com.sonar.sslr.api.AstNode;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import nl.ramsolutions.sw.magik.Location;

/**
 * Base class for definitions.
 */
public abstract class Definition {

    private final Location location;
    private final String moduleName;
    private final AstNode node;
    private final String doc;

    /**
     * Constructor.
     * @param location Location.
     * @param moduleName Name of the module this definition resides in.
     * @param node Node.
     */
    protected Definition(
            final @Nullable Location location,
            final @Nullable String moduleName,
            final @Nullable AstNode node,
            final @Nullable String doc) {
        this.location = location;
        this.moduleName = moduleName;
        this.node = node;
        this.doc = doc;
    }

    /**
     * Get the location of the definition.
     * @return Location of definition.
     */
    @CheckForNull
    public Location getLocation() {
        return this.location;
    }

    /**
     * Get the name of the module this definition resides in.
     * @return Module name.
     */
    @CheckForNull
    public String getModuleName() {
        return this.moduleName;
    }

    /**
     * Get parsed node.
     * @return
     */
    @CheckForNull
    public AstNode getNode() {
        return this.node;
    }

    /**
     * Get doc.
     * @return
     */
    @CheckForNull
    public String getDoc() {
        return this.doc;
    }

    /**
     * Get name of definition.
     * @return Name of definition.
     */
    public abstract String getName();

    /**
     * Get name of package this definition lives in.
     * @return Package name.
     */
    @CheckForNull
    public abstract String getPackage();

}
