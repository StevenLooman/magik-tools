package nl.ramsolutions.sw.magik.analysis.typing.reasoner.restrictions;

import java.util.Map;
import nl.ramsolutions.sw.magik.analysis.scope.ScopeEntry;
import nl.ramsolutions.sw.magik.analysis.typing.types.AbstractType;

/**
 * Type restriction.
 */
public abstract class TypeRestriction {

    /**
     * Get the inverse of ourselves.
     * @return
     */
    public abstract TypeRestriction not();

    /**
     * Get the restricted nodes.
     * @return Restricted nodes.
     */
    public abstract Map<ScopeEntry, AbstractType> getRestrictions();

    public boolean isDeterminable() {
        return true;
    }

}
