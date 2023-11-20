package nl.ramsolutions.sw.magik.checks;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import nl.ramsolutions.sw.magik.MagikFile;
import nl.ramsolutions.sw.magik.analysis.scope.GlobalScope;
import nl.ramsolutions.sw.magik.analysis.scope.Scope;
import nl.ramsolutions.sw.magik.parser.CommentInstructionReader;

/**
 * Check if {@link MagikIssue} is disabled via an annotation/comment.
 */
public final class MagikIssueDisabledChecker {

    private static final String NAME_MLINT = "mlint";
    private static final String KEY_DISABLE = "disable";
    private static final CommentInstructionReader.Instruction MLINT_STATEMENT_INSTRUCTION =
        new CommentInstructionReader.Instruction(
            NAME_MLINT,
            CommentInstructionReader.Instruction.Sort.STATEMENT);
    private static final CommentInstructionReader.Instruction MLINT_SCOPE_INSTRUCTION =
        new CommentInstructionReader.Instruction(
            NAME_MLINT,
            CommentInstructionReader.Instruction.Sort.SCOPE);

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
        final String checkKey = holder.getCheckKeyKebabCase();

        final Integer issueLineNo = magikIssue.startLine();  // 1-based.
        final Integer columnNo = magikIssue.startColumn();
        final Integer fileLineNo = issueLineNo - 1;  // 0-based.

        final Map<String, String> statementInstructions =
            magikFile.getStatementInstructions(MLINT_STATEMENT_INSTRUCTION).getOrDefault(
                fileLineNo,
                Collections.emptyMap());
        final String[] statementDisableds =
            statementInstructions.getOrDefault(MagikIssueDisabledChecker.KEY_DISABLE, "").split(",");
        final boolean statementDisabled = List.of(statementDisableds).contains(checkKey);
        if (statementDisabled) {
            return true;
        }

        final GlobalScope globalScope = magikFile.getGlobalScope();
        final Scope issueScope = globalScope.getScopeForLineColumn(issueLineNo, columnNo);
        Objects.requireNonNull(issueScope);
        return issueScope.getSelfAndAncestorScopes().stream()
            .map(scope ->
                magikFile.getScopeInstructions(MLINT_SCOPE_INSTRUCTION).getOrDefault(scope, Collections.emptyMap()))
            .map(scopeInstructions ->
                scopeInstructions.getOrDefault(MagikIssueDisabledChecker.KEY_DISABLE, "").split(","))
            .flatMap(Arrays::stream)
            .anyMatch(disabled -> disabled.equals(checkKey));
    }

}
