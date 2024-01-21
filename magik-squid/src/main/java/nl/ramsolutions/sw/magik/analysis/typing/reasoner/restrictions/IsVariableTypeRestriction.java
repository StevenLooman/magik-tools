package nl.ramsolutions.sw.magik.analysis.typing.reasoner.restrictions;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import nl.ramsolutions.sw.magik.analysis.scope.ScopeEntry;
import nl.ramsolutions.sw.magik.analysis.typing.types.AbstractType;

/**
 * `a = b` type restriction.
 */
class IsVariableTypeRestriction extends TypeRestriction {

    private final ScopeEntry leftScopeEntry;
    private final AbstractType leftType;
    private final ScopeEntry rightScopeEntry;
    private final AbstractType rightType;

    /**
     * Constructor.
     * @param leftScopeEntry Left {@link ScopeEntry}.
     * @param leftType Left {@link AbstractType}.
     * @param rightScopeEntry Right {@link ScopeEntry}.
     * @param rightType Right {@link AbstractType}.
     */
    IsVariableTypeRestriction(
            final ScopeEntry leftScopeEntry,
            final AbstractType leftType,
            final ScopeEntry rightScopeEntry,
            final AbstractType rightType) {
        this.leftScopeEntry = leftScopeEntry;
        this.leftType = leftType;
        this.rightScopeEntry = rightScopeEntry;
        this.rightType = rightType;
    }

    @Override
    public TypeRestriction not() {
        return new IsntVariableTypeRestriction(
            this.leftScopeEntry, this.leftType,
            this.rightScopeEntry, this.rightType);
    }

    @Override
    public Map<ScopeEntry, AbstractType> getRestrictions() {
        final AbstractType intersectionType = AbstractType.intersection(this.leftType, this.rightType);
        if (intersectionType == null) {
            // No intersection, so we cannot determine the type.
            return Collections.emptyMap();
        }

        final Map<ScopeEntry, AbstractType> restrictions = new HashMap<>();
        if (this.leftScopeEntry != null) {
            restrictions.put(this.leftScopeEntry, intersectionType);
        }
        if (this.rightScopeEntry != null) {
            restrictions.put(this.rightScopeEntry, intersectionType);
        }
        return restrictions;
    }

}
