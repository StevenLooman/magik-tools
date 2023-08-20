package nl.ramsolutions.sw.magik.checks;

import java.util.List;
import java.util.stream.Collectors;
import nl.ramsolutions.sw.magik.checks.checks.CommentRegularExpressionCheck;
import nl.ramsolutions.sw.magik.checks.checks.CommentedCodeCheck;
import nl.ramsolutions.sw.magik.checks.checks.DuplicateMethodInFileCheck;
import nl.ramsolutions.sw.magik.checks.checks.EmptyBlockCheck;
import nl.ramsolutions.sw.magik.checks.checks.ExemplarSlotCountCheck;
import nl.ramsolutions.sw.magik.checks.checks.FileMethodCountCheck;
import nl.ramsolutions.sw.magik.checks.checks.FileNotInLoadListCheck;
import nl.ramsolutions.sw.magik.checks.checks.ForbiddenCallCheck;
import nl.ramsolutions.sw.magik.checks.checks.ForbiddenGlobalUsageCheck;
import nl.ramsolutions.sw.magik.checks.checks.ForbiddenInheritanceCheck;
import nl.ramsolutions.sw.magik.checks.checks.FormattingCheck;
import nl.ramsolutions.sw.magik.checks.checks.HidesVariableCheck;
import nl.ramsolutions.sw.magik.checks.checks.ImportMissingDefinitionCheck;
import nl.ramsolutions.sw.magik.checks.checks.LhsRhsComparatorEqualCheck;
import nl.ramsolutions.sw.magik.checks.checks.LineLengthCheck;
import nl.ramsolutions.sw.magik.checks.checks.LocalImportProcedureCheck;
import nl.ramsolutions.sw.magik.checks.checks.MethodComplexityCheck;
import nl.ramsolutions.sw.magik.checks.checks.MethodLineCountCheck;
import nl.ramsolutions.sw.magik.checks.checks.NoSelfUseCheck;
import nl.ramsolutions.sw.magik.checks.checks.NoStatementAfterBodyExitCheck;
import nl.ramsolutions.sw.magik.checks.checks.ParameterCountCheck;
import nl.ramsolutions.sw.magik.checks.checks.ScopeCountCheck;
import nl.ramsolutions.sw.magik.checks.checks.SimplifyIfCheck;
import nl.ramsolutions.sw.magik.checks.checks.SizeZeroEmptyCheck;
import nl.ramsolutions.sw.magik.checks.checks.SwMethodDocCheck;
import nl.ramsolutions.sw.magik.checks.checks.SyntaxErrorCheck;
import nl.ramsolutions.sw.magik.checks.checks.TrailingWhitespaceCheck;
import nl.ramsolutions.sw.magik.checks.checks.TypeDocCheck;
import nl.ramsolutions.sw.magik.checks.checks.UndefinedVariableCheck;
import nl.ramsolutions.sw.magik.checks.checks.UnusedVariableCheck;
import nl.ramsolutions.sw.magik.checks.checks.UseValueCompareCheck;
import nl.ramsolutions.sw.magik.checks.checks.VariableCountCheck;
import nl.ramsolutions.sw.magik.checks.checks.VariableDeclarationUsageDistanceCheck;
import nl.ramsolutions.sw.magik.checks.checks.VariableNamingCheck;
import nl.ramsolutions.sw.magik.checks.checks.WarnedCallCheck;
import nl.ramsolutions.sw.magik.checks.checks.XPathCheck;

/**
 * Check list.
 */
public final class CheckList {

    @SuppressWarnings("checkstyle:JavadocVariable")
    public static final String REPOSITORY_KEY = "magik";

    @SuppressWarnings("checkstyle:JavadocVariable")
    public static final String PROFILE_DIR = "nl/ramsolutions/sw/sonar/l10n/magik/rules";

    @SuppressWarnings("checkstyle:JavadocVariable")
    public static final String PROFILE_LOCATION = PROFILE_DIR + "/Sonar_way_profile.json";

    private CheckList() {
    }

    /**
     * Get the list of {@link MagikCheck}s.
     * @return List of {@link MagikCheck}s.
     */
    public static List<Class<?>> getChecks() {
        return List.of(
            CommentRegularExpressionCheck.class,
            CommentedCodeCheck.class,
            DuplicateMethodInFileCheck.class,
            EmptyBlockCheck.class,
            ExemplarSlotCountCheck.class,
            FileMethodCountCheck.class,
            FileNotInLoadListCheck.class,
            ForbiddenCallCheck.class,
            ForbiddenGlobalUsageCheck.class,
            ForbiddenInheritanceCheck.class,
            FormattingCheck.class,
            HidesVariableCheck.class,
            ImportMissingDefinitionCheck.class,
            LhsRhsComparatorEqualCheck.class,
            LineLengthCheck.class,
            LocalImportProcedureCheck.class,
            MethodComplexityCheck.class,
            MethodLineCountCheck.class,
            NoSelfUseCheck.class,
            NoStatementAfterBodyExitCheck.class,
            ParameterCountCheck.class,
            ScopeCountCheck.class,
            SimplifyIfCheck.class,
            SizeZeroEmptyCheck.class,
            SwMethodDocCheck.class,
            SyntaxErrorCheck.class,
            TrailingWhitespaceCheck.class,
            TypeDocCheck.class,
            UndefinedVariableCheck.class,
            UnusedVariableCheck.class,
            UseValueCompareCheck.class,
            VariableCountCheck.class,
            VariableDeclarationUsageDistanceCheck.class,
            VariableNamingCheck.class,
            WarnedCallCheck.class,
            XPathCheck.class);
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
