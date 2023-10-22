package nl.ramsolutions.sw.magik.analysis.definitions;

import com.sonar.sslr.api.AstNode;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import nl.ramsolutions.sw.magik.analysis.typing.types.TypeString;

/**
 * Mixin definition.
 */
public class MixinDefinition extends Definition {

    private final List<TypeString> parents;

    protected MixinDefinition(
            final @Nullable String moduleName,
            final AstNode node,
            final TypeString name,
            final List<TypeString> parents) {
        super(moduleName, node, name);
        this.parents = List.copyOf(parents);
    }

    public List<TypeString> getParents() {
        return Collections.unmodifiableList(this.parents);
    }

    public TypeString getTypeString() {
        return TypeString.ofIdentifier(this.getName(), this.getPackage());
    }

}
