package nl.ramsolutions.sw.magik.analysis.definitions;

import com.sonar.sslr.api.AstNode;
import nl.ramsolutions.sw.magik.analysis.typing.types.TypeString;

/**
 * Base class for definitions.
 */
public abstract class Definition {

    private final AstNode node;
    private final TypeString name;

    /**
     * Constructor.
     * @param node Node.
     * @param name Name of definition, if applicable.
     */
    protected Definition(final AstNode node, final TypeString name) {
        if (!name.isSingle()) {
            throw new IllegalStateException();
        }

        this.node = node;
        this.name = name;
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
