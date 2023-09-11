package nl.ramsolutions.sw.magik.typedchecks;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import nl.ramsolutions.sw.magik.checks.DisabledByDefault;
import nl.ramsolutions.sw.magik.checks.MagikCheck;
import nl.ramsolutions.sw.magik.checks.MagikCheckFixer;
import nl.ramsolutions.sw.magik.checks.checks.TypeDocCheck;
import nl.ramsolutions.sw.magik.typedchecks.checks.GlobalKnownTypedCheck;
import nl.ramsolutions.sw.magik.typedchecks.checks.MethodArgumentCountTypedCheck;
import nl.ramsolutions.sw.magik.typedchecks.checks.MethodArgumentParameterTypedCheck;
import nl.ramsolutions.sw.magik.typedchecks.checks.MethodExistsTypedCheck;
import nl.ramsolutions.sw.magik.typedchecks.checks.MethodReturnMatchesDocTypedCheck;
import nl.ramsolutions.sw.magik.typedchecks.checks.SlotExistsTypedCheck;
import nl.ramsolutions.sw.magik.typedchecks.checks.TypeDocTypeExistsTypeCheck;
import nl.ramsolutions.sw.magik.typedchecks.checks.UndefinedMethodResultTypedCheck;
import nl.ramsolutions.sw.magik.typedchecks.fixers.TypeDocParameterFixer;
import nl.ramsolutions.sw.magik.typedchecks.fixers.TypeDocReturnTypeFixer;

/**
 * Check list.
 */
public final class CheckList {

    private CheckList() {
    }

    /**
     * Get the list of {@link MagikCheck}s.
     * @return List of with {@link MagikCheck}s
     */
    public static List<Class<?>> getChecks() {
        return List.of(
            GlobalKnownTypedCheck.class,
            MethodArgumentCountTypedCheck.class,
            MethodArgumentParameterTypedCheck.class,
            MethodExistsTypedCheck.class,
            MethodReturnMatchesDocTypedCheck.class,
            TypeDocTypeExistsTypeCheck.class,
            SlotExistsTypedCheck.class,
            UndefinedMethodResultTypedCheck.class);
    }

    /**
     * Get the {@link MagikCheck}s which have a {@link MagikCheckFixer}.
     * @return
     */
    public static Map<Class<?>, List<Class<?>>> getFixers() {
        return Map.of(
            TypeDocCheck.class, List.of(
                TypeDocParameterFixer.class,
                TypeDocReturnTypeFixer.class));
    }

    /**
     * Get {@link MagikCheck}s which are disabled by default.
     * @return List of {@link MagikCheck}s.
     */
    public static List<Class<?>> getDisabledByDefaultChecks() {
        return getChecks().stream()
            .filter(checkClass -> checkClass.getAnnotation(DisabledByDefault.class) != null)
            .collect(Collectors.toList());
    }

}
