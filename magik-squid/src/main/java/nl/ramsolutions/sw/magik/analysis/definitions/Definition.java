package nl.ramsolutions.sw.magik.analysis.definitions;

import com.sonar.sslr.api.AstNode;

/**
 * Base class for definitions.
 */
public abstract class Definition {

    private final AstNode node;
    private final String pakkage;
    private final String name;

    /**
     * Constructor.
     * @param node Node.
     * @param pakkage Package of definition.
     * @param name Name of definition.
     */
    protected Definition(final AstNode node, final String pakkage, final String name) {
        this.node = node;
        this.pakkage = pakkage;
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
        return this.name;
    }

    /**
     * Get name of package this definition lives in.
     * @return Package name.
     */
    public String getPackage() {
        return this.pakkage;
    }

    @Override
    public String toString() {
        return String.format(
            "%s@%s(%s, %s)",
            this.getClass().getName(), Integer.toHexString(this.hashCode()),
            this.getPackage(), this.getName());
    }

}
