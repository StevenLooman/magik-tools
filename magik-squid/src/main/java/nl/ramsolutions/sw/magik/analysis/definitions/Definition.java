package nl.ramsolutions.sw.magik.analysis.definitions;

import com.sonar.sslr.api.AstNode;
import javax.annotation.Nullable;
import nl.ramsolutions.sw.magik.analysis.typing.types.TypeString;

/**
 * Base class for definitions.
 */
public abstract class Definition {

    private final String moduleName;
    private final AstNode node;
    private final TypeString name;

    /**
     * Constructor.
     * @param node Node.
     * @param name Name of definition, if applicable.
     * @param moduleName Name of the module this definition resides in.
     */
    protected Definition(final @Nullable String moduleName, final AstNode node, final TypeString name) {
        if (!name.isSingle()) {
            throw new IllegalStateException();
        }

        this.moduleName = moduleName;
        this.node = node;
        this.name = name;
    }

    /**
     * Get the name of the module this definition resides in.
     * @return Module name.
     */
    public String getModuleName() {
        return this.moduleName;
    }

    /**
     * Get parsed node.
     * @return
     */
    public AstNode getNode() {
        return this.node;
    }

    /**
     * Get name of definition.
     * @return Name of definition.
     */
    public String getName() {
        return this.name.getIdentifier();
    }

    /**
     * Get name of package this definition lives in.
     * @return Package name.
     */
    public String getPackage() {
        return this.name.getPakkage();
    }

    @Override
    public String toString() {
        return String.format(
            "%s@%s(%s, %s)",
            this.getClass().getName(), Integer.toHexString(this.hashCode()),
            this.getPackage(), this.getName());
    }

}
