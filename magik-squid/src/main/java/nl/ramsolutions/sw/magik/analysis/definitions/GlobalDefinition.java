package nl.ramsolutions.sw.magik.analysis.definitions;

import com.sonar.sslr.api.AstNode;
import javax.annotation.Nullable;
import nl.ramsolutions.sw.magik.analysis.typing.types.TypeString;

/**
 * Definition of a global.
 */
public class GlobalDefinition extends Definition {

    public GlobalDefinition(final @Nullable String moduleName, final AstNode node, final TypeString name) {
        super(moduleName, node, name);
    }

}
