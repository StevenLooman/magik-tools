package nl.ramsolutions.sw.magik.analysis.definitions;

import com.sonar.sslr.api.AstNode;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import nl.ramsolutions.sw.magik.Location;

/**
 * Condition definition.
 */
public class ConditionDefinition extends Definition {

    private final String name;
    private final @Nullable String parent;
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
            final @Nullable String doc,
            final @Nullable AstNode node,
            final String name,
            final @Nullable String parent,
            final List<String> dataNames) {
        super(location, moduleName, doc, node);
        this.name = name;
        this.parent = parent;
        this.dataNames = List.copyOf(dataNames);
    }

    @Override
    public String getName() {
        return this.name;
    }

    @CheckForNull
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

    @Override
    public Definition getWithoutNode() {
        return new ConditionDefinition(
            this.getLocation(),
            this.getModuleName(),
            this.getDoc(),
            null,
            this.name,
            this.parent,
            this.dataNames);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            this.getLocation(),
            this.getModuleName(),
            this.getDoc(),
            this.name,
            this.parent,
            this.dataNames);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null) {
            return false;
        }

        if (this.getClass() != obj.getClass()) {
            return false;
        }

        final ConditionDefinition other = (ConditionDefinition) obj;
        return Objects.equals(this.getLocation(), other.getLocation())
            && Objects.equals(this.getModuleName(), other.getModuleName())
            && Objects.equals(this.getDoc(), other.getDoc())
            && Objects.equals(this.name, other.name)
            && Objects.equals(this.parent, other.parent)
            && Objects.equals(this.dataNames, other.dataNames);
    }

}
