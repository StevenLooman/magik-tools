package nl.ramsolutions.sw.magik.analysis.typing;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.CheckForNull;
import nl.ramsolutions.sw.magik.analysis.definitions.ConditionDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;

/**
 * {@link ConditionDefinition} resolver tools.
 */
public class ConditionResolver {

    private final IDefinitionKeeper definitionKeeper;

    public ConditionResolver(final IDefinitionKeeper definitionKeeper) {
        this.definitionKeeper = definitionKeeper;
    }

    /**
     * Get the {@link ConditionDefinition} ancestry for a given condition name, including the condition itself.
     * In case multiple conditions with the same name exist, the first one found is used.
     * @param conditionName Condition to search for.
     * @return List of conditions, or null if a condition (of conditionName, or one of its ancestors) was not found.
     */
    @CheckForNull
    public List<ConditionDefinition> getTaxonomy(final String conditionName) {
        final List<ConditionDefinition> taxonomy = new ArrayList<>();

        ConditionDefinition currentConditionDefinition = this.definitionKeeper.getConditionDefinitions(conditionName)
            .stream()
            .findAny()
            .orElse(null);
        if (currentConditionDefinition == null) {
            return null;
        }

        taxonomy.add(currentConditionDefinition);
        while (currentConditionDefinition.getParent() != null) {
            final String parentConditionName = currentConditionDefinition.getParent();
            final ConditionDefinition parentConditionDefinition =
                this.definitionKeeper.getConditionDefinitions(parentConditionName).stream()
                .findAny()
                .orElse(null);
            if (parentConditionDefinition == null) {
                return null;
            }

            taxonomy.add(parentConditionDefinition);
            currentConditionDefinition = parentConditionDefinition;
        }

        return taxonomy;
    }

    public boolean conditionIs(final String conditionName, final String isConditionName) {
        return this.getTaxonomy(conditionName).stream()
            .anyMatch(def -> def.getName().equals(isConditionName));
    }

}
