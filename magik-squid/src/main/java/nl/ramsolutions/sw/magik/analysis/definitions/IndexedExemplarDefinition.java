package nl.ramsolutions.sw.magik.analysis.definitions;

import com.sonar.sslr.api.AstNode;
import java.util.Collections;
import java.util.List;
import nl.ramsolutions.sw.magik.analysis.typing.types.TypeString;

/**
 * Indexed exemplar definition.
 */
public class IndexedExemplarDefinition extends Definition {

    private final List<TypeString> parents;

    public IndexedExemplarDefinition(
            final AstNode node,
            final TypeString name,
            final List<TypeString> parents) {
        super(node, name);
        this.parents = List.copyOf(parents);
    }

    public List<TypeString> getParents() {
        return Collections.unmodifiableList(this.parents);
    }

    public TypeString getTypeString() {
        return TypeString.ofIdentifier(this.getName(), this.getPackage());
    }

}
