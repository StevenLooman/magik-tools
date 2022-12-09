package nl.ramsolutions.sw.magik.typedchecks;

import java.util.List;
import java.util.stream.Collectors;
import nl.ramsolutions.sw.magik.checks.DisabledByDefault;
import nl.ramsolutions.sw.magik.checks.TemplatedMagikCheck;
import nl.ramsolutions.sw.magik.typedchecks.checks.GlobalKnownTypedCheck;
import nl.ramsolutions.sw.magik.typedchecks.checks.MethodArgumentCountTypedCheck;
import nl.ramsolutions.sw.magik.typedchecks.checks.MethodArgumentParameterTypedCheck;
import nl.ramsolutions.sw.magik.typedchecks.checks.MethodExistsTypedCheck;
import nl.ramsolutions.sw.magik.typedchecks.checks.MethodReturnMatchesDocTypedCheck;
import nl.ramsolutions.sw.magik.typedchecks.checks.NewDocTypeExistsTypeCheck;
import nl.ramsolutions.sw.magik.typedchecks.checks.SlotExistsTypedCheck;
import nl.ramsolutions.sw.magik.typedchecks.checks.UndefinedMethodResultTypedCheck;

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
            NewDocTypeExistsTypeCheck.class,
            SlotExistsTypedCheck.class,
            UndefinedMethodResultTypedCheck.class);
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

    /**
     * Get {@link MagikCheck}s which are templated.
     * @return List of {@link MagikCheck}s.
     */
    public static List<Class<?>> getTemplatedChecks() {
        return getChecks().stream()
            .filter(checkClass -> checkClass.getAnnotation(TemplatedMagikCheck.class) != null)
            .collect(Collectors.toList());
    }

}
