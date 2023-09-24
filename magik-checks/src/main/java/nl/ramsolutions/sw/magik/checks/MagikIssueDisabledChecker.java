package nl.ramsolutions.sw.magik.checks;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import nl.ramsolutions.sw.magik.MagikFile;
import nl.ramsolutions.sw.magik.analysis.scope.Scope;
import nl.ramsolutions.sw.magik.parser.CommentInstructionReader;

/**
 * Check if {@link MagikIssue} is disabled via an annotation/comment.
 */
public final class MagikIssueDisabledChecker {

    private static final String KEY_DISABLE = "disable";
    private static final CommentInstructionReader.InstructionType MLINT_LINE_INSTRUCTION =
        CommentInstructionReader.InstructionType.createStatementInstructionType("mlint");
    private static final CommentInstructionReader.InstructionType MLINT_SCOPE_INSTRUCTION =
        CommentInstructionReader.InstructionType.createScopeInstructionType("mlint");

    private MagikIssueDisabledChecker() {
        // Utility class.
    }

    /**
     * Check if a found issue/infraction is NOT disable via line or scope.
     * @param magikFile Magik file.
     * @param magikIssue Issue to check.
     * @return true if issue is disabled at line.
     */
    public static boolean issueDisabled(final MagikFile magikFile, final MagikIssue magikIssue) {
        final MagikCheckHolder holder = magikIssue.check().getHolder();
        Objects.requireNonNull(holder);

        final Integer line = magikIssue.startLine();
        final Scope scope = magikFile.getGlobalScope().getScopeForLineColumn(line, Integer.MAX_VALUE);
        final Map<String, String> scopeInstructions =
            magikFile.getScopeInstructions(MLINT_SCOPE_INSTRUCTION).getOrDefault(scope, Collections.emptyMap());
        final Map<String, String> lineInstructions =
            magikFile.getLineInstructions(MLINT_LINE_INSTRUCTION).getOrDefault(line, Collections.emptyMap());
        final String[] scopeDisableds =
            scopeInstructions.getOrDefault(MagikIssueDisabledChecker.KEY_DISABLE, "").split(",");
        final String[] lineDisableds =
            lineInstructions.getOrDefault(MagikIssueDisabledChecker.KEY_DISABLE, "").split(",");
        final String checkKey = holder.getCheckKeyKebabCase();
        return List.of(scopeDisableds).contains(checkKey)
            || List.of(lineDisableds).contains(checkKey);
    }

}
