package nl.ramsolutions.sw.magik.analysis.definitions;

import com.sonar.sslr.api.AstNode;
import nl.ramsolutions.sw.magik.analysis.typing.types.TypeString;

/**
 * Definition of a global.
 */
public class GlobalDefinition extends Definition {

    public GlobalDefinition(final AstNode node, final TypeString name) {
        super(node, name);
    }

}
