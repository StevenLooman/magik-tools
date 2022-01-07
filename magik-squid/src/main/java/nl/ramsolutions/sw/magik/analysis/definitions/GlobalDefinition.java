package nl.ramsolutions.sw.magik.analysis.definitions;

import com.sonar.sslr.api.AstNode;

/**
 * Definition of a global.
 */
public class GlobalDefinition extends Definition {

    public GlobalDefinition(final AstNode node, final String pakkage, final String name) {
        super(node, pakkage, name);
    }

}
