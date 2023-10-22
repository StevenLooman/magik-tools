package nl.ramsolutions.sw.magik.analysis.definitions;

import com.sonar.sslr.api.AstNode;
import java.util.Collections;
import java.util.List;
import nl.ramsolutions.sw.magik.analysis.typing.types.TypeString;

/**
 * Package definition.
 */
public class PackageDefinition extends Definition {

    private String name;
    private final List<String> uses;

    /**
     * Constructor.
     * @param moduleName Module where this package is defined.
     * @param node Node of package definition.
     * @param name Name of package.
     * @param uses Uses by package.
     */
    public PackageDefinition(final String moduleName, final AstNode node, final String name, final List<String> uses) {
        super(moduleName, node, TypeString.UNDEFINED);
        this.name = name;
        this.uses = List.copyOf(uses);
    }

    @Override
    public String getName() {
        return this.name;
    }

    public List<String> getUses() {
        return Collections.unmodifiableList(this.uses);
    }

}
