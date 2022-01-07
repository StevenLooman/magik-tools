package nl.ramsolutions.sw.magik.analysis.definitions;

import com.sonar.sslr.api.AstNode;
import java.util.Collections;
import java.util.List;

/**
 * Package definition.
 */
public class PackageDefinition extends Definition {

    private final List<String> uses;

    public PackageDefinition(
            final AstNode node, final String pakkage, final String name, final List<String> uses) {
        super(node, pakkage, name);
        this.uses = List.copyOf(uses);
    }

    public List<String> getUses() {
        return Collections.unmodifiableList(this.uses);
    }

}
