package nl.ramsolutions.sw.magik.analysis.definitions;

import com.sonar.sslr.api.AstNode;
import edu.umd.cs.findbugs.annotations.Nullable;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.analysis.typing.types.TypeString;

/**
 * A resolvable type definition.
 */
public abstract class TypeStringDefinition extends Definition {

    protected TypeStringDefinition(
            final @Nullable Location location,
            final @Nullable String moduleName,
            final @Nullable String doc,
            final @Nullable AstNode node) {
        super(location, moduleName, doc, node);
    }

    public abstract TypeString getTypeString();

    // TODO: Parents.

}
