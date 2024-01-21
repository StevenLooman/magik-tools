package nl.ramsolutions.sw.magik.analysis.typing.reasoner.restrictions;

import java.util.Map;
import nl.ramsolutions.sw.magik.analysis.scope.ScopeEntry;
import nl.ramsolutions.sw.magik.analysis.typing.types.AbstractType;

/**
 * `a ~= b` type restriction.
 */
class IsntVariableTypeRestriction extends TypeRestriction {

    private final ScopeEntry leftScopeEntry;
    private final AbstractType leftType;
    private final ScopeEntry rightScopeEntry;
    private final AbstractType rightType;

    /**
     * Constructor.
     * @param leftScopeEntry Left scope entry.
     * @param leftType Left type.
     * @param rightScopeEntry Right scope entry.
     * @param rightType Right type.
     */
    IsntVariableTypeRestriction(
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
        // TODO: Ugh, this becomes a mess with unset/maybe.
        return new IsVariableTypeRestriction(
            this.leftScopeEntry, this.leftType,
            this.rightScopeEntry, this.rightType);
    }

    @Override
    public Map<ScopeEntry, AbstractType> getRestrictions() {
        // TODO: Only unset/maybe can be disjoint.
        return Map.of(
            this.leftScopeEntry, this.leftType,
            this.rightScopeEntry, this.rightType);
    }

}
