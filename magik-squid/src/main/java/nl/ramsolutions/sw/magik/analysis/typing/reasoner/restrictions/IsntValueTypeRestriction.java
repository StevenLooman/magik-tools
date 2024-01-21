package nl.ramsolutions.sw.magik.analysis.typing.reasoner.restrictions;

import java.util.Collections;
import java.util.Map;
import nl.ramsolutions.sw.magik.analysis.scope.ScopeEntry;
import nl.ramsolutions.sw.magik.analysis.typing.types.AbstractType;

/**
 * `_isnt <value>` type restriction.
 */
class IsntValueTypeRestriction extends TypeRestriction {

    private final ScopeEntry scopeEntry;
    private final AbstractType type;

    /**
     * Constructor.
     * @param scopeEntry Scope entry.
     * @param type Type of scope entry.
     */
    IsntValueTypeRestriction(final ScopeEntry scopeEntry, final AbstractType type) {
        this.scopeEntry = scopeEntry;
        this.type = type;
    }

    @Override
    public TypeRestriction not() {
        return new IsValueTypeRestriction(this.scopeEntry, this.type);
    }

    @Override
    public Map<ScopeEntry, AbstractType> getRestrictions() {
        return Collections.emptyMap();
    }

}
