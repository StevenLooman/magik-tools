package nl.ramsolutions.sw.magik.analysis.typing;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.CheckForNull;
import nl.ramsolutions.sw.magik.analysis.definitions.Definition;
import nl.ramsolutions.sw.magik.analysis.definitions.ExemplarDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.GlobalDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.PackageDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.ProcedureDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.TypeStringDefinition;
import nl.ramsolutions.sw.magik.analysis.typing.types.TypeString;

/**
 * {@link TypeString} resolver tools.
 */
public class TypeStringResolver {

    private final IDefinitionKeeper definitionKeeper;

    public TypeStringResolver(final IDefinitionKeeper definitionKeeper) {
        this.definitionKeeper = definitionKeeper;
    }

    private List<PackageDefinition> getPackageHierarchy(final TypeString typeString) {
        final Deque<String> packages = new ArrayDeque<>();
        final String startPackage = typeString.getPakkage();
        packages.push(startPackage);

        // Iterate through package structure.
        final List<PackageDefinition> seen = new ArrayList<>();
        while (!packages.isEmpty()) {
            final String packageName = packages.pop();
            this.definitionKeeper.getPackageDefinitions(packageName).stream()
                .filter(def -> !seen.contains(def))
                .map(def -> {
                    seen.add(def);
                    return def;
                })
                .flatMap(def -> def.getUses().stream())
                .forEach(packages::push);
        }

        return seen;
    }

    private Collection<ExemplarDefinition> findExemplarDefinitions(final TypeString typeString) {
        return this.getPackageHierarchy(typeString).stream()
            .sequential()
            .flatMap(def -> {
                final String packageName = def.getName();
                final TypeString pkgTypeString = TypeString.ofIdentifier(typeString.getIdentifier(), packageName);
                return this.definitionKeeper.getExemplarDefinitions(pkgTypeString).stream();
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    }

    private Collection<ProcedureDefinition> findProcedureDefinitions(final TypeString typeString) {
        return this.getPackageHierarchy(typeString).stream()
            .sequential()
            .flatMap(def -> {
                final String packageName = def.getName();
                final TypeString pkgTypeString = TypeString.ofIdentifier(typeString.getIdentifier(), packageName);
                return this.definitionKeeper.getProcedureDefinitions(pkgTypeString).stream();
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    }

    private Collection<GlobalDefinition> findGlobalDefinitions(final TypeString typeString) {
        return this.getPackageHierarchy(typeString).stream()
            .sequential()
            .flatMap(def -> {
                final String packageName = def.getName();
                final TypeString pkgTypeString = TypeString.ofIdentifier(typeString.getIdentifier(), packageName);
                return this.definitionKeeper.getGlobalDefinitions(pkgTypeString).stream();
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    }

    /**
     * Get the {@link Definition} for the given {@link TypeString}, following package uses.
     * @param typeString Identifier to look for.
     * @return A {@link ExemplarDefinition}/{@link ProcedureDefinition}/{@link GlobalDefinition}.
     */
    public Collection<TypeStringDefinition> resolve(final TypeString typeString) {
        final Collection<ExemplarDefinition> exemplarDefinitions = this.findExemplarDefinitions(typeString);
        final Collection<ProcedureDefinition> procedureDefinitions = this.findProcedureDefinitions(typeString);
        final Collection<GlobalDefinition> globalDefinitions = this.findGlobalDefinitions(typeString);
        return Stream.concat(
                exemplarDefinitions.stream(),
                Stream.concat(
                    procedureDefinitions.stream(),
                    globalDefinitions.stream()))
            .collect(Collectors.toSet());
    }

    /**
     * Get the {@link ExemplarDefinition} from a {@link TypeString}.
     *
     * Note that this gives only a singular {@link ExemplarDefinition},
     * even though there might be multiple known.
     * @param typeString {@link TypeString} to resolve.
     * @return Found {@link ExemplarDefinition}, or null.
     */
    @CheckForNull
    public ExemplarDefinition getExemplarDefinition(final TypeString typeString) {
        // TODO: Does this work ok? E.g., for show subtypes of, we jump to sw:procedure.
        Collection<TypeStringDefinition> definitions = this.resolve(typeString);
        if (definitions.isEmpty()) {
            return null;
        }

        // Resolve global first.
        TypeStringDefinition definition = definitions.iterator().next();
        while (definition instanceof GlobalDefinition) {
            final GlobalDefinition globalDefinition0 = (GlobalDefinition) definition;
            final TypeString aliasedTypeString = globalDefinition0.getAliasedTypeName();
            definitions = this.resolve(aliasedTypeString);
            if (definitions.isEmpty()) {
                return null;
            }

            definition = definitions.iterator().next();
        }

        // Treat a procedure definition as the exemplar `procedure`.
        if (definition instanceof ProcedureDefinition) {
            definitions = this.resolve(TypeString.SW_PROCEDURE);
            if (definitions.isEmpty()) {
                return null;
            }

            definition = definitions.iterator().next();
        }

        return definition instanceof ExemplarDefinition
            ? (ExemplarDefinition) definition
            : null;
    }

    /**
     * Test if {@link typeString1} is kind of {@link typeString2}.
     * @param typeString1 The thing to test.
     * @param typeString2 The kind to test for.
     * @return True if is kind of, false otherwise.
     */
    public boolean isKindOf(final TypeString typeString1, final TypeString typeString2) {
        final TypeStringDefinition definition1 = this.resolve(typeString1).stream()
            .findAny()
            .orElse(null);
        if (definition1 == null) {
            return false;
        }

        final TypeStringDefinition definition2 = this.resolve(typeString2).stream()
            .findAny()
            .orElse(null);
        if (definition2 == null) {
            return false;
        }

        return this.isKindOf(definition1, definition2);
    }

    /**
     * Test if {@link definition1} is kind of {@link definition2}.
     * @param definition1 The thing to test.
     * @param definition2 The kind to test for.
     * @return True if is kind of, false otherwise.
     */
    public boolean isKindOf(final TypeStringDefinition definition1, final TypeStringDefinition definition2) {
        final TypeString typeString1 = definition1.getTypeString();
        final TypeString typeString2 = definition2.getTypeString();
        if (typeString1.equals(typeString2)) {
            return true;
        }

        return this.getParents(definition1).stream()
            .anyMatch(parentTypeString1 -> this.isKindOf(parentTypeString1, typeString2));
    }

    private Collection<TypeString> getParents(final Definition definition) {
        if (definition instanceof ExemplarDefinition) {
            return ((ExemplarDefinition) definition).getParents();
        } else if (definition instanceof ProcedureDefinition) {
            return Set.of(TypeString.SW_PROCEDURE);
        } else if (definition instanceof GlobalDefinition) {
            final TypeString typeString = ((GlobalDefinition) definition).getAliasedTypeName();
            final Definition aliasedDefinition = this.resolve(typeString).stream()
                .findAny()
                .orElse(null);
            if (aliasedDefinition == null) {
                return Collections.emptySet();
            }

            return this.getParents(aliasedDefinition);
        }

        throw new IllegalStateException();
    }

}
