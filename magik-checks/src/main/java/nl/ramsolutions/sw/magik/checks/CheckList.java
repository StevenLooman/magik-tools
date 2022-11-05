package nl.ramsolutions.sw.magik.checks;

import java.util.List;
import java.util.stream.Collectors;
import nl.ramsolutions.sw.magik.checks.checks.CommentRegularExpressionCheck;
import nl.ramsolutions.sw.magik.checks.checks.CommentedCodeCheck;
import nl.ramsolutions.sw.magik.checks.checks.DuplicateMethodInFileCheck;
import nl.ramsolutions.sw.magik.checks.checks.EmptyBlockCheck;
import nl.ramsolutions.sw.magik.checks.checks.ExemplarSlotCountCheck;
import nl.ramsolutions.sw.magik.checks.checks.FileNotInLoadListCheck;
import nl.ramsolutions.sw.magik.checks.checks.ForbiddenCallCheck;
import nl.ramsolutions.sw.magik.checks.checks.FormattingCheck;
import nl.ramsolutions.sw.magik.checks.checks.HidesVariableCheck;
import nl.ramsolutions.sw.magik.checks.checks.ImportMissingDefinitionCheck;
import nl.ramsolutions.sw.magik.checks.checks.LhsRhsComparatorEqualCheck;
import nl.ramsolutions.sw.magik.checks.checks.LineLengthCheck;
import nl.ramsolutions.sw.magik.checks.checks.LocalImportProcedureCheck;
import nl.ramsolutions.sw.magik.checks.checks.MethodComplexityCheck;
import nl.ramsolutions.sw.magik.checks.checks.NewDocCheck;
import nl.ramsolutions.sw.magik.checks.checks.NoSelfUseCheck;
import nl.ramsolutions.sw.magik.checks.checks.NoStatementAfterBodyExitCheck;
import nl.ramsolutions.sw.magik.checks.checks.ScopeCountCheck;
import nl.ramsolutions.sw.magik.checks.checks.SimplifyIfCheck;
import nl.ramsolutions.sw.magik.checks.checks.SizeZeroEmptyCheck;
import nl.ramsolutions.sw.magik.checks.checks.SwMethodDocCheck;
import nl.ramsolutions.sw.magik.checks.checks.SyntaxErrorCheck;
import nl.ramsolutions.sw.magik.checks.checks.TrailingWhitespaceCheck;
import nl.ramsolutions.sw.magik.checks.checks.UndefinedVariableCheck;
import nl.ramsolutions.sw.magik.checks.checks.UnusedVariableCheck;
import nl.ramsolutions.sw.magik.checks.checks.UseValueCompareCheck;
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
     * Get the list of {{MagikCheck}}s.
     * @return List of with {{MagikCheck}}s
     */
    public static List<Class<?>> getChecks() {
        return List.of(
            CommentRegularExpressionCheck.class,
            CommentedCodeCheck.class,
            DuplicateMethodInFileCheck.class,
            EmptyBlockCheck.class,
            ExemplarSlotCountCheck.class,
            FileNotInLoadListCheck.class,
            ForbiddenCallCheck.class,
            FormattingCheck.class,
            HidesVariableCheck.class,
            ImportMissingDefinitionCheck.class,
            LhsRhsComparatorEqualCheck.class,
            LineLengthCheck.class,
            LocalImportProcedureCheck.class,
            MethodComplexityCheck.class,
            NewDocCheck.class,
            NoSelfUseCheck.class,
            NoStatementAfterBodyExitCheck.class,
            ScopeCountCheck.class,
            SimplifyIfCheck.class,
            SizeZeroEmptyCheck.class,
            SwMethodDocCheck.class,
            SyntaxErrorCheck.class,
            TrailingWhitespaceCheck.class,
            UndefinedVariableCheck.class,
            UnusedVariableCheck.class,
            UseValueCompareCheck.class,
            VariableDeclarationUsageDistanceCheck.class,
            VariableNamingCheck.class,
            WarnedCallCheck.class,
            XPathCheck.class);
    }

    /**
     * Get {{MagikCheck}}s which are disabled by default.
     * @return List of {{MagikCheck}}s.
     */
    public static List<Class<?>> getDisabledByDefaultChecks() {
        return getChecks().stream()
            .filter(checkClass -> checkClass.getAnnotation(DisabledByDefault.class) != null)
            .collect(Collectors.toList());
    }

    /**
     * Get {{MagikCheck}}s which are templated.
     * @return List of {{MagikCheck}}s.
     */
    public static List<Class<?>> getTemplatedChecks() {
        return getChecks().stream()
            .filter(checkClass -> checkClass.getAnnotation(TemplatedMagikCheck.class) != null)
            .collect(Collectors.toList());
    }

}
