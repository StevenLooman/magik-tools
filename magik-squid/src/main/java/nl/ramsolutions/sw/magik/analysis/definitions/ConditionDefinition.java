package nl.ramsolutions.sw.magik.analysis.definitions;

import com.sonar.sslr.api.AstNode;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import nl.ramsolutions.sw.magik.Location;

/**
 * Condition definition.
 */
public class ConditionDefinition extends Definition {

    private final String name;
    private final String parent;
    private final List<String> dataNames;

    /**
     * Constructor.
     * @param moduleName Module name.
     * @param node Node.
     * @param name Name.
     * @param parent Parent.
     * @param dataNames Data name list.
     * @param doc Doc.
     */
    public ConditionDefinition(
            final @Nullable Location location,
            final @Nullable String moduleName,
            final @Nullable AstNode node,
            final String name,
            final String parent,
            final List<String> dataNames,
            final String doc) {
        super(location, moduleName, node, doc);
        this.name = name;
        this.parent = parent;
        this.dataNames = dataNames;
    }

    @Override
    public String getName() {
        return this.name;
    }

    public String getParent() {
        return this.parent;
    }

    public List<String> getDataNames() {
        return Collections.unmodifiableList(this.dataNames);
    }

    @Override
    public String getPackage() {
        return null;
    }

}
