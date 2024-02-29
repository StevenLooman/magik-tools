package nl.ramsolutions.sw.magik.typedchecks;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import nl.ramsolutions.sw.magik.checks.DisabledByDefault;
import nl.ramsolutions.sw.magik.checks.MagikCheck;
import nl.ramsolutions.sw.magik.checks.checks.TypeDocCheck;
import nl.ramsolutions.sw.magik.typedchecks.checks.ConditionalExpressionIsFalseTypedCheck;
import nl.ramsolutions.sw.magik.typedchecks.checks.GlobalExistsTypedCheck;
import nl.ramsolutions.sw.magik.typedchecks.checks.MethodArgumentCountMatchesParameterCountTypedCheck;
import nl.ramsolutions.sw.magik.typedchecks.checks.MethodArgumentTypeMatchesParameterTypeTypedCheck;
import nl.ramsolutions.sw.magik.typedchecks.checks.MethodExistsTypedCheck;
import nl.ramsolutions.sw.magik.typedchecks.checks.MethodReturnTypesMatchDocTypedCheck;
import nl.ramsolutions.sw.magik.typedchecks.checks.ModuleRequiredForGlobalTypedCheck;
import nl.ramsolutions.sw.magik.typedchecks.checks.SlotExistsTypedCheck;
import nl.ramsolutions.sw.magik.typedchecks.checks.SwChar16VectorEvaluateInvocationCheck;
import nl.ramsolutions.sw.magik.typedchecks.checks.TypeDocTypeExistsTypedCheck;
import nl.ramsolutions.sw.magik.typedchecks.checks.UndefinedMethodCallResultTypedCheck;
import nl.ramsolutions.sw.magik.typedchecks.fixers.TypeDocParameterFixer;
import nl.ramsolutions.sw.magik.typedchecks.fixers.TypeDocReturnTypeFixer;

/** Check list. */
public final class CheckList {

  private CheckList() {}

  /**
   * Get the list of {@link MagikCheck}s.
   *
   * @return List of with {@link MagikCheck}s
   */
  public static List<Class<? extends MagikCheck>> getChecks() {
    return List.of(
        ConditionalExpressionIsFalseTypedCheck.class,
        GlobalExistsTypedCheck.class,
        MethodArgumentCountMatchesParameterCountTypedCheck.class,
        MethodArgumentTypeMatchesParameterTypeTypedCheck.class,
        MethodExistsTypedCheck.class,
        MethodReturnTypesMatchDocTypedCheck.class,
        ModuleRequiredForGlobalTypedCheck.class,
        TypeDocTypeExistsTypedCheck.class,
        SlotExistsTypedCheck.class,
        SwChar16VectorEvaluateInvocationCheck.class,
        UndefinedMethodCallResultTypedCheck.class);
  }

  /**
   * Get the {@link MagikCheck}s which have a {@link MagikTypedCheckFixer}.
   *
   * @return
   */
  public static Map<Class<? extends MagikCheck>, List<Class<? extends MagikTypedCheckFixer>>>
      getFixers() {
    return Map.of(
        TypeDocCheck.class, List.of(TypeDocParameterFixer.class, TypeDocReturnTypeFixer.class));
  }

  /**
   * Get {@link MagikCheck}s which are disabled by default.
   *
   * @return List of {@link MagikCheck}s.
   */
  public static List<Class<? extends MagikCheck>> getDisabledByDefaultChecks() {
    return getChecks().stream()
        .filter(checkClass -> checkClass.getAnnotation(DisabledByDefault.class) != null)
        .collect(Collectors.toList());
  }
}
