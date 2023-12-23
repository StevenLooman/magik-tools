package nl.ramsolutions.sw.magik.analysis.definitions;

import com.sonar.sslr.api.AstNode;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.analysis.typing.types.TypeString;

/**
 * A resolvable type definition.
 */
public abstract class TypeStringDefinition extends Definition {

    protected TypeStringDefinition(
            final Location location,
            final String moduleName,
            final String doc,
            final AstNode node) {
        super(location, moduleName, doc, node);
    }

    public abstract TypeString getTypeString();

    // TODO: Parents.

}
