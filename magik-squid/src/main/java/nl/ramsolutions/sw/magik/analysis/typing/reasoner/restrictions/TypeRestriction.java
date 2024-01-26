package nl.ramsolutions.sw.magik.analysis.typing.reasoner.restrictions;

import java.util.Map;
import nl.ramsolutions.sw.magik.analysis.scope.ScopeEntry;
import nl.ramsolutions.sw.magik.analysis.typing.types.AbstractType;

/**
 * Type restriction.
 */
public interface TypeRestriction {

    /**
     * Get the inverse of ourselves.
     * @return Inverse of ourselves
     */
    TypeRestriction not();

    /**
     * Get the restricted ScopeEntry with its reasoned type.
     * @return Restricted scope entry with its reasoned type.
     */
    Map.Entry<ScopeEntry, AbstractType> getRestriction();

}
